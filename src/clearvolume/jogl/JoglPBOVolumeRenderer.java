package clearvolume.jogl;

import static jcuda.driver.JCudaDriver.cuGLRegisterBufferObject;
import static jcuda.driver.JCudaDriver.cuGLUnregisterBufferObject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.nativewindow.util.Point;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jcuda.Sizeof;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.transfertf.ProjectionAlgorithm;
import clearvolume.transfertf.TransfertFunction;
import clearvolume.transfertf.TransfertFunctions;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

public abstract class JoglPBOVolumeRenderer	implements
																						GLEventListener,
																						Closeable
{
	private static GLCapabilities sCapabilities;
	static
	{
		final GLProfile lProfile = GLProfile.getMaxFixedFunc(true);
		sCapabilities = new GLCapabilities(lProfile);
	}

	private RotationControllerInterface mRotationController;

	private final String mWindowName;
	private volatile int mWindowWidth = 0;
	private volatile int mWindowHeight = 0;
	private JFrame mControlFrame;
	private final GLWindow mGlWindow;
	protected int mPixelBufferObjectId = 0;

	private int mBytesPerVoxel = 1;

	private ProjectionAlgorithm mProjectionAlgorythm = ProjectionAlgorithm.MaxProjection;

	private TransfertFunction mTransferFunction = TransfertFunctions.getDefaultTransfertFunction();

	protected volatile float mTranslationX = 0, mTranslationY = 0,
			mTranslationZ = 0;
	protected volatile float mRotationX = 0, mRotationY = 0;

	private volatile double mScaleX = 1.0f, mScaleY = 1.0f,
			mScaleZ = 1.0f;
	private volatile float mDensity, mBrightness = 1,
			mTransferRangeMin = 0, mTransferRangeMax = 1, mGamma = 1;

	private volatile boolean mUpdateVolumeRenderingParameters = true;

	private final float mModelViewMatrix[] = new float[16];

	private final Object mSetVolumeDataBufferLock = new Object();
	private volatile ByteBuffer mVolumeDataByteBuffer;
	private volatile int mVolumeSizeX, mVolumeSizeY, mVolumeSizeZ;
	private volatile boolean mVolumeDimensionsChanged;
	private int mTextureId;
	private final int mTextureWidth;
	private final int mTextureHeight;

	protected CountDownLatch mDataBufferCopyFinished;

	private final Executor mRenderingExecutor = Executors.newSingleThreadExecutor();

	public JoglPBOVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight)
	{
		this(pWindowName, pWindowWidth, pWindowHeight, 1);
	}

	public JoglPBOVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final int pBytesPerPixel)
	{
		resetDensityBrightnessOffsetScale();
		resetRotationTranslation();

		mWindowName = pWindowName;
		mWindowWidth = pWindowWidth;
		mWindowHeight = pWindowHeight;
		mBytesPerVoxel = pBytesPerPixel;

		mTextureWidth = Math.min(768, mWindowWidth);
		mTextureHeight = Math.min(768, mWindowHeight);

		// Initialize the GL component

		final NewtFactory lNewtFactory = new NewtFactory();

		mWindow = NewtFactory.createWindow(sCapabilities);
		mGlWindow = GLWindow.create(mWindow);
		mGlWindow.setTitle(mWindowName);
		mGlWindow.addGLEventListener(this);
		mGlWindow.setSize(mWindowWidth, mWindowHeight);

		if (System.getProperty("os.name").toLowerCase().contains("mac"))
		{

		}

		// Initialize the mouse controls
		final MouseControl lMouseControl = new MouseControl(this);
		mGlWindow.addMouseListener(lMouseControl);

		// Initialize the keyboard controls
		final KeyboardControl lKeyboardControl = new KeyboardControl(this);
		mGlWindow.addKeyListener(lKeyboardControl);

		mGlWindow.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowDestroyNotify(final WindowEvent pE)
			{
				// mAnimator.stop();
				super.windowDestroyNotify(pE);
			};
		});

	}

	private void setupControlFrame()
	{
		mControlFrame = new JFrame("ClearVolume Rendering Parameters");
		mControlFrame.setLayout(new BorderLayout());
		mControlFrame.add(createControlPanel(), BorderLayout.SOUTH);
		mControlFrame.pack();
		// mControlFrame.setAlwaysOnTop(true);
		mControlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mControlFrame.setVisible(true);
	}

	@Override
	public void close() throws IOException
	{
		if (mGlWindow.isRealized())
			mGlWindow.destroy();
	}

	public boolean isShowing()
	{
		return mGlWindow.isVisible();
	}

	public void setVisible(final boolean pIsVisible)
	{
		mGlWindow.setVisible(pIsVisible);
	}

	public String getWindowName()
	{
		return mWindowName;
	}

	public boolean getIsUpdateVolumeParameters()
	{
		return mUpdateVolumeRenderingParameters;
	}

	public void notifyUpdateOfVolumeParameters()
	{
		mUpdateVolumeRenderingParameters = true;
	}

	public void clearIsUpdateVolumeParameters()
	{
		mUpdateVolumeRenderingParameters = false;
	}

	public void setVolumeSize(final double pVolumeSizeX,
														final double pVolumeSizeY,
														final double pVolumeSizeZ)
	{
		final double lMaxXYZ = Math.max(Math.max(	pVolumeSizeX,
																							pVolumeSizeY),
																		pVolumeSizeZ);

		setScaleX(pVolumeSizeX / lMaxXYZ);
		setScaleY(pVolumeSizeY / lMaxXYZ);
		setScaleZ(pVolumeSizeZ / lMaxXYZ);
	}

	public void setScaleX(final double pScaleX)
	{
		mScaleX = (float) pScaleX;
		mUpdateVolumeRenderingParameters = true;
	}

	public void setScaleY(final double pScaleY)
	{
		mScaleY = (float) pScaleY;
		mUpdateVolumeRenderingParameters = true;
	}

	public void setScaleZ(final double pScaleZ)
	{
		mScaleZ = (float) pScaleZ;
		mUpdateVolumeRenderingParameters = true;
	}

	public void resetDensityBrightnessOffsetScale()
	{
		mDensity = 0.05f;
		mBrightness = 1.0f;
		mTransferRangeMin = 0.0f;
		mTransferRangeMax = 1.0f;
	}

	public void setDensity(final double pDensity)
	{
		mDensity = (float) clamp(pDensity, 0, 1);
		mUpdateVolumeRenderingParameters = true;
	}

	public void setBrightness(final double pBrightness)
	{
		mBrightness = (float) clamp(pBrightness,
																0,
																getBytesPerVoxel() == 1 ? 16 : 256);
		mUpdateVolumeRenderingParameters = true;
	}

	public void setTransferRange(	final double pTransferRangeMin,
																final double pTransferRangeMax)
	{
		mTransferRangeMin = (float) clamp(pTransferRangeMin, 0, 1);
		mTransferRangeMax = (float) clamp(pTransferRangeMax, 0, 1);
		mUpdateVolumeRenderingParameters = true;
	}

	public void setTransferRangeMin(final double pTransferRangeMin)
	{
		mTransferRangeMin = (float) clamp(pTransferRangeMin, 0, 1);
		mUpdateVolumeRenderingParameters = true;
	}

	public void setTransferRangeMax(final double pTransferRangeMax)
	{
		mTransferRangeMax = (float) clamp(pTransferRangeMax, 0, 1);
		mUpdateVolumeRenderingParameters = true;
	}

	public void setGamma(final double pGamma)
	{
		mGamma = (float) pGamma;
		mUpdateVolumeRenderingParameters = true;
	}

	public void addDensity(final double pDensityDelta)
	{
		setDensity(mDensity + pDensityDelta);
	}

	public void addBrightness(final double pBrightnessDelta)
	{
		setBrightness(mBrightness + pBrightnessDelta);
	}

	public void addTransferRangePosition(final double pTransferRangePositionDelta)
	{
		addTransferRangeMin(pTransferRangePositionDelta);
		addTransferRangeMax(pTransferRangePositionDelta);
	}

	public void addTransferRangeWidth(final double pTransferRangeWidthDelta)
	{
		addTransferRangeMin(-pTransferRangeWidthDelta);
		addTransferRangeMax(pTransferRangeWidthDelta);
	}

	public void addTransferRangeMin(final double pDelta)
	{
		setTransferRangeMin(mTransferRangeMin + pDelta);
	}

	public void addTransferRangeMax(final double pDelta)
	{
		setTransferRangeMax(mTransferRangeMax + pDelta);
	}

	private double clamp(	final double pValue,
												final double pMin,
												final double pMax)
	{
		return Math.min(Math.max(pValue, pMin), pMax);
	}

	public double getScaleX()
	{
		return mScaleX;
	}

	public double getScaleY()
	{
		return mScaleY;
	}

	public double getScaleZ()
	{
		return mScaleZ;
	}

	public double getDensity()
	{
		return mDensity;
	}

	public double getBrightness()
	{
		return mBrightness;
	}

	public double getTransferRangeMin()
	{
		return mTransferRangeMin;
	}

	public double getTransferRangeMax()
	{
		return mTransferRangeMax;
	}

	public double getGamma()
	{
		return mGamma;
	}

	public ByteBuffer getVolumeDataBuffer()
	{
		return mVolumeDataByteBuffer;
	}

	public boolean isVolumeDataAvailable()
	{
		return mVolumeSizeX * mVolumeSizeY * mVolumeSizeZ > 0;
	}

	public int getVolumeSizeX()
	{
		return mVolumeSizeX;
	}

	public int getVolumeSizeY()
	{
		return mVolumeSizeY;
	}

	public int getVolumeSizeZ()
	{
		return mVolumeSizeZ;
	}

	public Object getSetVolumeDataBufferLock()
	{
		return mSetVolumeDataBufferLock;
	}

	public boolean hasVolumeDimensionsChanged()
	{
		return mVolumeDimensionsChanged;
	}

	public void clearVolumeDimensionsChanged()
	{
		mVolumeDimensionsChanged = false;
	}

	public int getWindowWidth()
	{
		return mWindowWidth;
	}

	public int getWindowHeight()
	{
		return mWindowHeight;
	}

	public int getTextureWidth()
	{
		return mTextureWidth;
	}

	public int getTextureHeight()
	{
		return mTextureHeight;
	}

	protected float[] getTransfertFunctionArray()
	{
		return mTransferFunction.getArray();
	}

	public void setTransfertFunction(final TransfertFunction pTransfertFunction)
	{
		mTransferFunction = pTransfertFunction;
	}

	public ProjectionAlgorithm getProjectionAlgorythm()
	{
		return mProjectionAlgorythm;
	}

	public void setProjectionAlgorythm(final ProjectionAlgorithm pProjectionAlgorithm)
	{
		mProjectionAlgorythm = pProjectionAlgorithm;
	}

	public void resetRotationTranslation()
	{
		mRotationX = 0;
		mRotationY = 0;
		mTranslationX = 0;
		mTranslationY = 0;
		mTranslationZ = -4;
	}

	/**
	 * Create the control panel containing the sliders for setting the
	 * visualization parameters.
	 * 
	 * @return The control panel
	 */
	private JPanel createControlPanel()
	{
		final JPanel controlPanel = new JPanel(new GridLayout(2, 2));
		JPanel panel = null;
		JSlider slider = null;

		// Density
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Density:"));
		slider = new JSlider(0, 100, 5);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				setDensity(a);
				requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Brightness
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Brightness:"));
		slider = new JSlider(0, 100, 10);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				setBrightness(a * 10);
				requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Transfer offset
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Transfer Range Min:"));
		slider = new JSlider(0, 100, 55);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				setTransferRangeMin(a);
				requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Transfer scale
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Transfer Range Max:"));
		slider = new JSlider(0, 100, 10);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				setTransferRangeMax(a);
				requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		return controlPanel;
	}

	/**
	 * Implementation of GLEventListener: Called to initialize the GLAutoDrawable.
	 * This method will initialize the JCudaDriver and cause the initialization of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
		// Perform the default GL initialization
		final GL2 gl = drawable.getGL().getGL2();
		gl.setSwapInterval(0);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_DEPTH_BUFFER);
		gl.glDisable(GL2.GL_STENCIL_TEST);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_TEXTURE_2D);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		setupDefaultView(drawable);

		mGlWindow.runOnEDTIfAvail(true, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if (initVolumeRenderer())
					{
						initPixelBufferObject(gl);
						initTexture(gl);
					}
				}
				catch (final Throwable e)
				{
					e.printStackTrace();
				}
			}
		});

	}

	protected abstract boolean initVolumeRenderer();

	void initPixelBufferObject(final GL gl)
	{
		if (mPixelBufferObjectId != 0)
		{
			cuGLUnregisterBufferObject(mPixelBufferObjectId);
			gl.glDeleteBuffers(1, new int[]
			{ mPixelBufferObjectId }, 0);
			mPixelBufferObjectId = 0;
		}

		// Create and bind a pixel buffer object with the current
		// width and height of the rendering component.
		final int pboArray[] = new int[1];
		gl.glGenBuffers(1, pboArray, 0);
		mPixelBufferObjectId = pboArray[0];
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, mPixelBufferObjectId);
		gl.glBufferData(GL2.GL_PIXEL_UNPACK_BUFFER,
										mTextureWidth * mTextureHeight * Sizeof.BYTE * 4,
										null,
										GL.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);

		// Register the PBO for usage with CUDA
		cuGLRegisterBufferObject(mPixelBufferObjectId);

	}

	private void initTexture(final GL gl)
	{
		final IntBuffer lTextureIdBuffer = IntBuffer.allocate(1);
		gl.glGenTextures(1, lTextureIdBuffer);
		mTextureId = lTextureIdBuffer.get();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, mTextureId);
		gl.glTexParameterf(	GL2.GL_TEXTURE_2D,
												GL2.GL_TEXTURE_MAG_FILTER,
												GL2.GL_LINEAR);
		gl.glTexParameterf(	GL2.GL_TEXTURE_2D,
												GL2.GL_TEXTURE_MIN_FILTER,
												GL2.GL_LINEAR);
		gl.glTexParameterf(	GL2.GL_TEXTURE_2D,
												GL2.GL_TEXTURE_WRAP_S,
												GL2.GL_CLAMP);
		gl.glTexParameterf(	GL2.GL_TEXTURE_2D,
												GL2.GL_TEXTURE_WRAP_T,
												GL2.GL_CLAMP);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D,
										0,
										GL2.GL_RGBA8,
										mTextureWidth,
										mTextureHeight,
										0,
										GL2.GL_RGBA,
										GL2.GL_UNSIGNED_BYTE,
										ByteBuffer.wrap(new byte[mTextureWidth * mTextureHeight
																							* 4]));
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
	}

	void setupDefaultView(final GLAutoDrawable drawable)
	{
		final GL2 gl = drawable.getGL().getGL2();

		gl.glViewport(0, 0, drawable.getWidth(), drawable.getHeight());

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(	0.0,
								drawable.getWidth(),
								0.0,
								drawable.getHeight(),
								0.0,
								1);
	}

	/**
	 * Implementation of GLEventListener: Called when the given GLAutoDrawable is
	 * to be displayed.
	 */
	@Override
	public void display(final GLAutoDrawable drawable)
	{
		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		gl.glRotatef(-mRotationX, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(-mRotationY, 0.0f, 1.0f, 0.0f);
		if (hasRotationController())
		{
			getRotationController().rotateGL(gl);
			notifyUpdateOfVolumeParameters();
		}
		gl.glTranslatef(-mTranslationX, -mTranslationY, -mTranslationZ);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mModelViewMatrix, 0);
		gl.glPopMatrix();

		mGlWindow.runOnEDTIfAvail(true, new Runnable()
		{
			@Override
			public void run()
			{
				if (!Thread.currentThread().getName().contains("AWT"))
				{
					renderVolume(gl, mModelViewMatrix);
					renderedImageHook(gl, mPixelBufferObjectId);
				}
			}
		});

	}

	protected abstract void renderVolume(	final GL2 gl,
																				final float[] modelView);

	public abstract void renderedImageHook(	final GL2 gl,
																					int pPixelBufferObjectId);

	public void drawPBOToScreen(final GL2 gl)
	{

		// Draw the image from the PBO
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glRasterPos2i(0, 0);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, mPixelBufferObjectId);
		gl.glDrawPixels(mWindowWidth,
										mWindowHeight,
										GL.GL_RGBA,
										GL.GL_UNSIGNED_BYTE,
										0);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);

		updateFrameRateDisplay();
	}

	public void drawPBOToTextureToScreen(final GL2 gl)
	{

		// Draw the image from the PBO
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		copyPBOToTexture(gl);
		drawQuad(gl);

		updateFrameRateDisplay();
	}

	public void copyPBOToTexture(final GL2 gl)
	{
		gl.glBindTexture(gl.GL_TEXTURE_2D, mTextureId);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, mPixelBufferObjectId);

		gl.glTexSubImage2D(	gl.GL_TEXTURE_2D,
												0,
												0,
												0,
												mTextureWidth,
												mTextureHeight,
												gl.GL_RGBA,
												gl.GL_UNSIGNED_BYTE,
												0);

		gl.glBindTexture(gl.GL_TEXTURE_2D, 0);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);
	}

	public void drawQuad(final GL2 gl)
	{
		final double wt = 1;
		final double ht = 1;
		final double wv = Math.max(mWindowWidth, mWindowHeight);
		final double hv = Math.max(mWindowWidth, mWindowHeight);

		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(gl.GL_TEXTURE_2D, mTextureId);
		gl.glBegin(GL2.GL_QUADS);

		gl.glTexCoord2d(0.0, 0.0);
		gl.glVertex2d(0.0, 0.0);

		gl.glTexCoord2d(wt, 0.0);
		gl.glVertex2d(wv, 0.0);

		gl.glTexCoord2d(wt, wt);
		gl.glVertex2d(wv, hv);

		gl.glTexCoord2d(0.0, ht);
		gl.glVertex2d(0.0, hv);

		gl.glEnd();
		gl.glBindTexture(gl.GL_TEXTURE_2D, 0);
	}

	private int step = 0;
	private long prevTimeNS = -1;

	private void updateFrameRateDisplay()
	{
		step++;
		final long currentTime = System.nanoTime();
		if (prevTimeNS == -1)
		{
			prevTimeNS = currentTime;
		}
		final long diff = currentTime - prevTimeNS;
		if (diff > 1e9)
		{
			final double fps = (diff / 1e9) * step;
			String t = getWindowName() + "- ";
			t += String.format("%.2f", fps) + " FPS";
			setWindowTitle(t);
			prevTimeNS = currentTime;
			step = 0;
		}
	}

	private void setWindowTitle(final String pTitleString)
	{
		mGlWindow.setTitle(pTitleString);
	}

	@Override
	public void reshape(final GLAutoDrawable drawable,
											final int x,
											final int y,
											final int width,
											final int height)
	{
		this.mWindowWidth = width;
		this.mWindowHeight = height;

		mGlWindow.runOnEDTIfAvail(true, new Runnable()
		{
			@Override
			public void run()
			{
				initPixelBufferObject(drawable.getGL());
				setupDefaultView(drawable);
				display(drawable);
			}
		});

	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose(final GLAutoDrawable arg0)
	{
	}

	public void setVolumeDataBuffer(final ByteBuffer pByteBuffer,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ)
	{
		setVolumeDataBuffer(pByteBuffer,
												pSizeX,
												pSizeY,
												pSizeZ,
												1,
												((double) pSizeY) / pSizeX,
												((double) pSizeZ) / pSizeX);
	}

	public void setVolumeDataBuffer(final ByteBuffer pByteBuffer,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ,
																	final double pVolumeSizeX,
																	final double pVolumeSizeY,
																	final double pVolumeSizeZ)
	{
		synchronized (getSetVolumeDataBufferLock())
		{
			mDataBufferCopyFinished = new CountDownLatch(1);

			if (mVolumeSizeX != pSizeX || mVolumeSizeY != pSizeY
					|| mVolumeSizeZ != pSizeZ)
			{
				mVolumeDimensionsChanged = true;
			}
			mVolumeSizeX = pSizeX;
			mVolumeSizeY = pSizeY;
			mVolumeSizeZ = pSizeZ;

			mScaleX = pVolumeSizeX;
			mScaleY = pVolumeSizeY;
			mScaleZ = pVolumeSizeZ;

			mVolumeDataByteBuffer = pByteBuffer;

			notifyUpdateOfVolumeParameters();

		}
	}

	public boolean waitToFinishDataBufferCopy()
	{
		try
		{
			mDataBufferCopyFinished.await();
			return true;
		}
		catch (final InterruptedException e)
		{
			return false;
		}
	}

	private int mWindowX, mWindowY;

	private final Window mWindow;

	public void toggleFullScreen()
	{
		try
		{
			if (mGlWindow.isFullscreen())
			{
				mUpdateVolumeRenderingParameters = true;
				mGlWindow.setFullscreen(false);
				mGlWindow.display();
				// mNewtWindow.setPosition(mWindowX, mWindowY);
			}
			else
			{
				// mAnimator.stop();
				mUpdateVolumeRenderingParameters = true;
				final Point lPoint = new Point();
				mGlWindow.getLocationOnScreen(lPoint);
				mWindowX = lPoint.getX();
				mWindowY = lPoint.getY();
				mGlWindow.setFullscreen(true);
				mGlWindow.display();
				// mAnimator.start();
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isFullScreen()
	{
		return mGlWindow.isFullscreen();
	}

	public RotationControllerInterface getRotationController()
	{
		return mRotationController;
	}

	public boolean hasRotationController()
	{
		return mRotationController != null;
	}

	public void setQuaternionController(final RotationControllerInterface quaternionController)
	{
		mRotationController = quaternionController;
	}

	public int getBytesPerVoxel()
	{
		return mBytesPerVoxel;
	}

	public void requestDisplay()
	{
		mGlWindow.runOnEDTIfAvail(false, new Runnable()
		{
			@Override
			public void run()
			{
				mGlWindow.display();
			}
		});
	}

	public void disableClose()
	{
		mGlWindow.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
	}
}