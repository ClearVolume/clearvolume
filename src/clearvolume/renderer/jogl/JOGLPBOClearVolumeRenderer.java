package clearvolume.renderer.jogl;

import static jcuda.driver.JCudaDriver.cuGLRegisterBufferObject;
import static jcuda.driver.JCudaDriver.cuGLUnregisterBufferObject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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
import clearvolume.renderer.ClearVolumeRendererBase;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;

/**
 * Abstract Class JoglPBOVolumeRenderer
 * 
 * Classes that derive from this abstract class are provided with basic
 * JOGL-based display capability for implementing a ClearVolumeRenderer.
 *
 * @author Loic Royer 2014
 *
 */
public abstract class JOGLPBOClearVolumeRenderer	extends
																						ClearVolumeRendererBase	implements
																																		GLEventListener
{
	/**
	 * JOGL capabilities object.
	 */
	private static GLCapabilities sCapabilities;
	static
	{
		final GLProfile lProfile = GLProfile.getMaxFixedFunc(true);
		sCapabilities = new GLCapabilities(lProfile);
	}

	// Window name, dimensions, positions, JFrame and GLWindow.
	private final String mWindowName;
	private volatile int mWindowWidth = 0, mWindowHeight = 0;
	private volatile int mWindowX, mWindowY;
	private JFrame mControlFrame;
	private final GLWindow mGlWindow;

	// modelview matrix
	private final float mModelViewMatrix[] = new float[16];

	// pixelbuffer object.
	protected int mPixelBufferObjectId = 0;

	// texture and its dimensions.
	private int mTextureId;
	private final int mTextureWidth, mTextureHeight;

	// Internal fields for calculating FPS.
	private int step = 0;
	private long prevTimeNS = -1;



	private final Window mWindow;


	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name and its dimensions.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 */
	public JOGLPBOClearVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight)
	{
		this(pWindowName, pWindowWidth, pWindowHeight, 1);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerPixel
	 */
	public JOGLPBOClearVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final int pBytesPerVoxel)
	{
		resetBrightnessAndGammaAndTransferFunctionRanges();
		resetRotationTranslation();

		mWindowName = pWindowName;
		mWindowWidth = pWindowWidth;
		mWindowHeight = pWindowHeight;
		setBytesPerVoxel(pBytesPerVoxel);

		mTextureWidth = Math.min(768, mWindowWidth);
		mTextureHeight = Math.min(768, mWindowHeight);

		// Initialize the GL component

		mWindow = NewtFactory.createWindow(sCapabilities);
		mGlWindow = GLWindow.create(mWindow);
		mGlWindow.setTitle(mWindowName);
		mGlWindow.addGLEventListener(this);
		mGlWindow.setSize(mWindowWidth, mWindowHeight);

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
	public void close()
	{
		if (mGlWindow.isRealized())
			mGlWindow.destroy();
	}

	/**
	 * Interface method implementation
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isShowing()
	 */
	public boolean isShowing()
	{
		return mGlWindow.isVisible();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVisible(boolean)
	 */
	public void setVisible(final boolean pIsVisible)
	{
		mGlWindow.setVisible(pIsVisible);
	}


		/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowName()
	 */
	public String getWindowName()
	{
		return mWindowName;
	}


	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowWidth()
	 */
	public int getWindowWidth()
	{
		return mWindowWidth;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowHeight()
	 */
	public int getWindowHeight()
	{
		return mWindowHeight;
	}

	/**
	 * @return
	 */
	public int getTextureWidth()
	{
		return mTextureWidth;
	}

	/**
	 * @return
	 */
	public int getTextureHeight()
	{
		return mTextureHeight;
	}

	/**
	 * @return
	 */
	protected float[] getTransfertFunctionArray()
	{
		return getTransfertFunction().getArray();
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
				setTransferFunctionRangeMin(a);
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
				setTransferFunctionRangeMax(a);
				requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		return controlPanel;
	}

	/**
	 * Implementation of GLEventListener: Called to initialise the GLAutoDrawable.
	 * This method will initialise the JCudaDriver and cause the initialisation of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
		synchronized (mGlWindow)
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
						synchronized (mGlWindow)
						{
							if (initVolumeRenderer())
							{
								initPixelBufferObject(gl);
								initTexture(gl);
							}
						}
					}
					catch (final Throwable e)
					{
						e.printStackTrace();
					}
				}
			});
		}

	}

	/**
	 * @return
	 */
	protected abstract boolean initVolumeRenderer();

	/**
	 * @param gl
	 */
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

		synchronized (mGlWindow)
		{
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();

			gl.glRotatef(-getRotationX(), 1.0f, 0.0f, 0.0f);
			gl.glRotatef(-getRotationY(), 0.0f, 1.0f, 0.0f);
			if (hasRotationController())
			{
				getRotationController().rotateGL(gl);
				notifyUpdateOfVolumeRenderingParameters();
			}
			gl.glTranslatef(-getTranslationX(),
											-getTranslationY(),
											-getTranslationZ());
			gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mModelViewMatrix, 0);
			gl.glPopMatrix();

			mGlWindow.runOnEDTIfAvail(true, new Runnable()
			{
				@Override
				public void run()
				{
					if (!Thread.currentThread().getName().contains("AWT"))
					{
						synchronized (mGlWindow)
						{
							renderVolume(gl, mModelViewMatrix);
							renderedImageHook(gl, mPixelBufferObjectId);
						}
					}
				}
			});

		}

	}

	/**
	 * @param gl
	 * @param modelView
	 */
	protected abstract void renderVolume(	final GL2 gl,
																				final float[] modelView);

	/**
	 * @param gl
	 * @param pPixelBufferObjectId
	 */
	public abstract void renderedImageHook(	final GL2 gl,
																					int pPixelBufferObjectId);

	/**
	 * @param gl
	 */
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

	/**
	 * @param gl
	 */
	public void drawPBOToTextureToScreen(final GL2 gl)
	{

		// Draw the image from the PBO
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		copyPBOToTexture(gl);
		drawQuad(gl);

		updateFrameRateDisplay();
	}

	/**
	 * @param gl
	 */
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

	/**
	 * @param gl
	 */
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


	/**
	 * 
	 */
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

	/**
	 * @param pTitleString
	 */
	private void setWindowTitle(final String pTitleString)
	{
		mGlWindow.setTitle(pTitleString);
	}

	/**
	 * Interface method implementation
	 * 
	 * @see javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable,
	 *      int, int, int, int)
	 */
	@Override
	public void reshape(final GLAutoDrawable drawable,
											final int x,
											final int y,
											final int width,
											final int height)
	{

		synchronized (mGlWindow)
		{
			this.mWindowWidth = width;
			this.mWindowHeight = height;

			mGlWindow.runOnEDTIfAvail(true, new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (mGlWindow)
					{
						initPixelBufferObject(drawable.getGL());
						setupDefaultView(drawable);
						display(drawable);
					}
				}
			});
		}

	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose(final GLAutoDrawable arg0)
	{
	}



	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#toggleFullScreen()
	 */
	public void toggleFullScreen()
	{
		try
		{
			if (mGlWindow.isFullscreen())
			{
				notifyUpdateOfVolumeRenderingParameters();
				mGlWindow.setFullscreen(false);
				mGlWindow.display();
				// mNewtWindow.setPosition(mWindowX, mWindowY);
			}
			else
			{
				// mAnimator.stop();
				notifyUpdateOfVolumeRenderingParameters();
				final Point lPoint = new Point();
				mGlWindow.getLocationOnScreen(lPoint);
				mWindowX = lPoint.getX();
				mWindowY = lPoint.getY();
				mGlWindow.setSize(mWindowWidth, mWindowHeight);
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

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isFullScreen()
	 */
	public boolean isFullScreen()
	{
		return mGlWindow.isFullscreen();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.DisplayRequest#requestDisplay()
	 */
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

	/**
	 * 
	 */
	public void disableClose()
	{
		mGlWindow.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
	}
}