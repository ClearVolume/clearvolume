package clearvolume.renderer.jogl;

import cleargl.*;
import cleargl.util.recorder.GLVideoRecorder;
import clearvolume.renderer.ClearVolumeRendererBase;
import clearvolume.renderer.jogl.overlay.Overlay;
import clearvolume.renderer.jogl.overlay.Overlay2D;
import clearvolume.renderer.jogl.overlay.std.BoxOverlay;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLProfile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.max;

/**
 * Abstract Class JoglPBOVolumeRenderer
 *
 * Classes that derive from this abstract class are provided with basic
 * JOGL-based display capability for implementing a ClearVolumeRenderer.
 *
 * @author Loic Royer 2014
 *
 */
public abstract class JOGLClearVolumeRenderer	extends
																							ClearVolumeRendererBase	implements
																																			ClearGLEventListener
{

	static
	{
		// attempt at solving Jug's Dreadlock bug:
		final GLProfile lProfile = GLProfile.get(GLProfile.GL4);
		// System.out.println( lProfile );
	}

	// ClearGL Window.
	private volatile ClearGLWindow mClearGLWindow;
	private final NewtCanvasAWT mNewtCanvasAWT;
	private volatile int mLastWindowWidth, mLastWindowHeight;
	private final ReentrantLock mDisplayReentrantLock = new ReentrantLock();

	// pixelbuffer objects.
	protected GLPixelBufferObject[] mPixelBufferObjects;

	// texture and its dimensions.
	private final GLTexture<Byte>[] mLayerTextures;

	// Internal fields for calculating FPS.
	private volatile int step = 0;
	private volatile long prevTimeNS = -1;

	// Overlay stuff:
	private Map<String, Overlay2D> mOverlay2DMap = new ConcurrentHashMap<String, Overlay2D>();
	private Map<String, Overlay> mOverlay3DMap = new ConcurrentHashMap<String, Overlay>();

	// Window:
	private final String mWindowName;
	private GLProgram mGLProgram;

	// Shader attributes, uniforms and arrays:
	private GLAttribute mPositionAttribute;
	private GLVertexArray mQuadVertexArray;
	private GLVertexAttributeArray mPositionAttributeArray;
	private GLUniform mQuadProjectionMatrixUniform;
	private GLAttribute mTexCoordAttribute;
	private GLUniform[] mTexUnits;
	private GLVertexAttributeArray mTexCoordAttributeArray;

	private final GLMatrix mBoxModelViewMatrix = new GLMatrix();
	private final GLMatrix mVolumeViewMatrix = new GLMatrix();
	private final GLMatrix mQuadProjectionMatrix = new GLMatrix();

	private final int mMaxTextureWidth = 768, mMaxTextureHeight = 768;
	private final int mTextureWidth, mTextureHeight;

	protected boolean mUsePBOs = true;
	
	//Recorder:
	private final GLVideoRecorder mGLVideoRecorder = new GLVideoRecorder(new File(SystemUtils.USER_HOME,
																																								"Videos/ClearVolume"));

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name and its dimensions.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 */
	public JOGLClearVolumeRenderer(	final String pWindowName,
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
	 * @param pBytesPerVoxel
	 */
	public JOGLClearVolumeRenderer(	final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final int pBytesPerVoxel)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					768,
					768);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 */
	public JOGLClearVolumeRenderer(	final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final int pBytesPerVoxel,
																	final int pMaxTextureWidth,
																	final int pMaxTextureHeight)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					pMaxTextureWidth,
					pMaxTextureHeight,
					1);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param useInCanvas
	 *          if true, this Renderer will not be displayed in a window of it's
	 *          own, but must be embedded in a GUI as Canvas.
	 */
	public JOGLClearVolumeRenderer(	final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final int pBytesPerVoxel,
																	final int pMaxTextureWidth,
																	final int pMaxTextureHeight,
																	final boolean useInCanvas)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					pMaxTextureWidth,
					pMaxTextureHeight,
					1,
					useInCanvas);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, number of bytes-per-voxel, max texture width, height
	 * and number of render layers.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param pNumberOfRenderLayers
	 */
	public JOGLClearVolumeRenderer(	final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final int pBytesPerVoxel,
																	final int pMaxTextureWidth,
																	final int pMaxTextureHeight,
																	final int pNumberOfRenderLayers)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					pMaxTextureWidth,
					pMaxTextureHeight,
					1,
					false);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, number of bytes-per-voxel, max texture width, height
	 * and number of render layers.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param pNumberOfRenderLayers
	 * @param pUseInCanvas
	 *          if true, this Renderer will not be displayed in a window of it's
	 *          own, but must be embedded in a GUI as Canvas.
	 */
	@SuppressWarnings("unchecked")
	public JOGLClearVolumeRenderer(	final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final int pBytesPerVoxel,
																	final int pMaxTextureWidth,
																	final int pMaxTextureHeight,
																	final int pNumberOfRenderLayers,
																	final boolean pUseInCanvas)
	{
		super(pNumberOfRenderLayers);

		mTextureWidth = Math.min(mMaxTextureWidth, pWindowWidth);
		mTextureHeight = Math.min(mMaxTextureHeight, pWindowHeight);

		mWindowName = pWindowName;
		mLastWindowWidth = pWindowWidth;
		mLastWindowHeight = pWindowHeight;
		setNumberOfRenderLayers(pNumberOfRenderLayers);

		mLayerTextures = new GLTexture[getNumberOfRenderLayers()];
		mPixelBufferObjects = new GLPixelBufferObject[getNumberOfRenderLayers()];

		resetBrightnessAndGammaAndTransferFunctionRanges();
		resetRotationTranslation();
		setBytesPerVoxel(pBytesPerVoxel);

		addOverlay(new BoxOverlay());

		mClearGLWindow = new ClearGLWindow(	pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				this);

		mClearGLWindow.getGLWindow()
									.addWindowListener(new WindowAdapter()
									{
										@Override
										public void windowDestroyNotify(WindowEvent pE)
										{
											mClearGLWindow = null;
											super.windowDestroyNotify(pE);
										}

									});

		if (pUseInCanvas)
		{
			mNewtCanvasAWT = new NewtCanvasAWT(mClearGLWindow.getGLWindow());
		}
		else
		{
			mNewtCanvasAWT = null;
		}

		// Initialize the mouse controls
		final MouseControl lMouseControl = new MouseControl(this);
		mClearGLWindow.getGLWindow().addMouseListener(lMouseControl);

		// Initialize the keyboard controls
		final KeyboardControl lKeyboardControl = new KeyboardControl(this);
		mClearGLWindow.getGLWindow().addKeyListener(lKeyboardControl);

		mClearGLWindow.getGLWindow()
									.addWindowListener(new WindowAdapter()
									{

										@Override
										public void windowDestroyNotify(final WindowEvent pE)
										{
											super.windowDestroyNotify(pE);
										};
									});
	}

	@Override
	public void setClearGLWindow(final ClearGLWindow pClearGLWindow)
	{

		mClearGLWindow = pClearGLWindow;
	}

	@Override
	public ClearGLWindow getClearGLWindow()
	{
		return mClearGLWindow;
	};

	@Override
	public void close()
	{
		super.close();
		try
		{
			if (mClearGLWindow == null)
				return;
			mClearGLWindow.close();
			mClearGLWindow = null;
		}
		catch (NullPointerException e)
		{
		}
		catch (final Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isShowing()
	 */
	@Override
	public boolean isShowing()
	{
		try
		{
			if (mClearGLWindow == null)
				return false;
			return mClearGLWindow.getGLWindow().isVisible();
		}
		catch (NullPointerException e)
		{
			return false;
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVisible(boolean)
	 */
	@Override
	public void setVisible(final boolean pIsVisible)
	{
		mClearGLWindow.getGLWindow().setVisible(pIsVisible);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowName()
	 */
	@Override
	public String getWindowName()
	{
		return mWindowName;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowWidth()
	 */
	@Override
	public int getWindowWidth()
	{
		return mClearGLWindow.getGLWindow().getWidth();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowHeight()
	 */
	@Override
	public int getWindowHeight()
	{
		return mClearGLWindow.getGLWindow().getHeight();
	}

	/**
	 * Returns the render texture width.
	 *
	 * @return texture width
	 */
	public int getTextureWidth()
	{
		return mTextureWidth;
	}

	/**
	 * Returns the render texture height.
	 *
	 * @return texture height
	 */
	public int getTextureHeight()
	{
		return mTextureHeight;
	}

	/**
	 * Implementation of GLEventListener: Called to initialize the GLAutoDrawable.
	 * This method will initialize the JCudaDriver and cause the initialization of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
		final GL4 lGL4 = drawable.getGL().getGL4();
		lGL4.setSwapInterval(1);
		lGL4.glDisable(GL4.GL_DEPTH_TEST);
    lGL4.glEnable(GL4.GL_BLEND);
		lGL4.glDisable(GL4.GL_STENCIL_TEST);

		lGL4.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		lGL4.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

		// getClearGLWindow().setOrthoProjectionMatrix(0,
		// drawable.getSurfaceWidth(),
		// 0,
		// drawable.getSurfaceHeight(),
		// 0,
		// 1);

		setDefaultProjectionMatrix();

		/*lGL4.glViewport((pWidth - lViewPortWidth) / 2,
										(pHeight - lViewPortWidth) / 2,
										lViewPortWidth,
										lViewPortWidth);/**/

		mQuadProjectionMatrix.setOrthoProjectionMatrix(	-1,
																										1,
																										-1,
																										1,
																										0,
																										1000);

		if (initVolumeRenderer())
		{
			/*
			 * if (mPixelBufferObject != null)
			 * {
			 * unregisterPBO(mPixelBufferObject.getId());
			 * mPixelBufferObject.close();
			 * mPixelBufferObject = null;
			 * }
			 *
			 * /*
			 * if (mTexture != null)
			 * {
			 * mTexture.close();
			 * mTexture = null;
			 * }/*
			 */

			// texture display: construct the program and related objects
			try
			{
				final InputStream lVertexShaderResourceAsStream = JOGLClearVolumeRenderer.class.getResourceAsStream("shaders/tex_vert.glsl");
				final InputStream lFragmentShaderResourceAsStream = JOGLClearVolumeRenderer.class.getResourceAsStream("shaders/tex_frag.glsl");

				final String lVertexShaderSource = IOUtils.toString(lVertexShaderResourceAsStream,
																														"UTF-8");
				String lFragmentShaderSource = IOUtils.toString(lFragmentShaderResourceAsStream,
																												"UTF-8");

				for (int i = 1; i < getNumberOfRenderLayers(); i++)
				{
					final String lStringToInsert1 = String.format("uniform sampler2D texUnit%d; \n//insertpoin1t",
																												i);
					final String lStringToInsert2 = String.format("tempOutColor = max(tempOutColor,texture(texUnit%d, ftexcoord));\n//insertpoint2",
																												i);

					lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint1",
																																lStringToInsert1);
					lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint2",
																																lStringToInsert2);
				}
				// System.out.println(lFragmentShaderSource);

				mGLProgram = GLProgram.buildProgram(lGL4,
																						lVertexShaderSource,
																						lFragmentShaderSource);
				mQuadProjectionMatrixUniform = mGLProgram.getUniform("projection");
				mPositionAttribute = mGLProgram.getAtribute("position");
				mTexCoordAttribute = mGLProgram.getAtribute("texcoord");
				mTexUnits = new GLUniform[getNumberOfRenderLayers()];
				for (int i = 0; i < getNumberOfRenderLayers(); i++)
				{
					mTexUnits[i] = mGLProgram.getUniform("texUnit" + i);
					mTexUnits[i].set(i);
				}

				mQuadVertexArray = new GLVertexArray(mGLProgram);
				mQuadVertexArray.bind();
				mPositionAttributeArray = new GLVertexAttributeArray(	mPositionAttribute,
																															4);

				final GLFloatArray lVerticesFloatArray = new GLFloatArray(6,

        																																	4);

				lVerticesFloatArray.add(-1, -1, 0, 1);
				lVerticesFloatArray.add(1, -1, 0, 1);
				lVerticesFloatArray.add(1, 1, 0, 1);
				lVerticesFloatArray.add(-1, -1, 0, 1);
				lVerticesFloatArray.add(1, 1, 0, 1);
				lVerticesFloatArray.add(-1, 1, 0, 1);

				mQuadVertexArray.addVertexAttributeArray(	mPositionAttributeArray,
																									lVerticesFloatArray.getFloatBuffer());

				mTexCoordAttributeArray = new GLVertexAttributeArray(	mTexCoordAttribute,
																															2);

				final GLFloatArray lTexCoordFloatArray = new GLFloatArray(6,
																																	2);
				lTexCoordFloatArray.add(0, 0);
				lTexCoordFloatArray.add(1, 0);
				lTexCoordFloatArray.add(1, 1);
				lTexCoordFloatArray.add(0, 0);
				lTexCoordFloatArray.add(1, 1);
				lTexCoordFloatArray.add(0, 1);

				mQuadVertexArray.addVertexAttributeArray(	mTexCoordAttributeArray,
																									lTexCoordFloatArray.getFloatBuffer());

				for (int i = 0; i < getNumberOfRenderLayers(); i++)
				{
					mLayerTextures[i] = new GLTexture<Byte>(mGLProgram,
																									Byte.class,
																									4,
																									mTextureWidth,
																									mTextureHeight,
																									1,
																									true,
																									3);

					mPixelBufferObjects[i] = new GLPixelBufferObject(	mGLProgram,
																														mTextureWidth,
																														mTextureHeight);

					mPixelBufferObjects[i].copyFrom(null);

					registerPBO(i, mPixelBufferObjects[i].getId());
				}

			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}

			for (Overlay2D lOverlay2D : mOverlay2DMap.values())
			{
				try
				{
					lOverlay2D.init(lGL4, this);
				}
				catch (final Throwable e)
				{
					e.printStackTrace();
				}
			}

			for (Overlay lOverlay : mOverlay3DMap.values())
			{
				try
				{
					lOverlay.init(lGL4, this);
				}
				catch (final Throwable e)
				{
					e.printStackTrace();
				}
			}

			/*
			Runnable lDisplayRequestRunnable = new Runnable()
			{
				@Override
				public void run()
				{
					requestDisplay();
				}
			};
			mGLVideoRecorder.startDisplayRequestDeamonThread(lDisplayRequestRunnable);
			/**/

		}

	}

	private void setDefaultProjectionMatrix()
	{
		getClearGLWindow().setPerspectiveProjectionMatrix(.785f,
																											1,
																											.1f,
																											1000);
	}

	/**
	 * @return true if the implemented renderer initialized successfully.
	 */
	protected abstract boolean initVolumeRenderer();

	/**
	 * Register PBO object with any descendant of this abstract class.
	 *
	 * @param pRenderLayerIndex
	 * @param pPixelBufferObjectId
	 */
	protected abstract void registerPBO(int pRenderLayerIndex,
																			int pPixelBufferObjectId);

	/**
	 * Unregisters PBO object with any descendant of this abstract class.
	 *
	 * @param pRenderLayerIndex
	 * @param pPixelBufferObjectId
	 */
	protected abstract void unregisterPBO(int pRenderLayerIndex,
																				int pPixelBufferObjectId);

	public void copyBufferToTexture(final int pRenderLayerIndex,
																	final ByteBuffer pByteBuffer)
	{
		pByteBuffer.rewind();
		mLayerTextures[pRenderLayerIndex].copyFrom(pByteBuffer);
	}

	/**
	 * Implementation of GLEventListener: Called when the given GLAutoDrawable is
	 * to be displayed.
	 */
	@Override
	public void display(final GLAutoDrawable pDrawable)
	{
		final GL4 lGL4 = pDrawable.getGL().getGL4();
		lGL4.glClearColor(0, 0, 0, 1);
		lGL4.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

		setDefaultProjectionMatrix();

		mQuadProjectionMatrix.setOrthoProjectionMatrix(	-1,
																										1,
																										-1,
																										1,
																										0,
																										1000);

		// scaling...

		final double scaleX = getVolumeSizeX() * getVoxelSizeX();
		final double scaleY = getVolumeSizeY() * getVoxelSizeY();
		final double scaleZ = getVolumeSizeZ() * getVoxelSizeZ();

		final double maxScale = max(max(scaleX, scaleY), scaleZ);

		// building up the inverse Modelview

		final GLMatrix eulerMat = new GLMatrix();

		eulerMat.euler(getRotationX() * 0.01, getRotationY() * 0.01, 0.0f);
		if (hasRotationController())
		{
			getRotationController().rotate(eulerMat);
			notifyUpdateOfVolumeRenderingParameters();
		}

		final GLMatrix lInvVolumeMatrix = new GLMatrix();
		lInvVolumeMatrix.setIdentity();
		lInvVolumeMatrix.translate(	-getTranslationX(),
																-getTranslationY(),
																-getTranslationZ());
		lInvVolumeMatrix.transpose();

		lInvVolumeMatrix.mult(eulerMat);

		lInvVolumeMatrix.scale(	(float) (maxScale / scaleX),
														(float) (maxScale / scaleY),
														(float) (maxScale / scaleZ));

		final GLMatrix lInvProjection = new GLMatrix();
		lInvProjection.copy(getClearGLWindow().getProjectionMatrix());
		lInvProjection.transpose();
		lInvProjection.invert();

		final boolean[] lUpdatedLayersArray = renderVolume(	lInvVolumeMatrix.getFloatArray(),
																												lInvProjection.getFloatArray());

		final boolean lOverlay2DChanged = isOverlay2DChanged();
		final boolean lOverlay3DChanged = isOverlay3DChanged();

		if (lUpdatedLayersArray != null || lOverlay2DChanged
				|| lOverlay3DChanged)
		{
			if (mUsePBOs)
				for (int i = 0; i < getNumberOfRenderLayers(); i++)
					if (lUpdatedLayersArray[i])
						mLayerTextures[i].copyFrom(mPixelBufferObjects[i]);

			mGLProgram.use(lGL4);

			for (int i = 0; i < getNumberOfRenderLayers(); i++)
				mLayerTextures[i].bind(i);

			mQuadProjectionMatrixUniform.setFloatMatrix(mQuadProjectionMatrix.getFloatArray(),
																									false);

			mQuadVertexArray.draw(GL.GL_TRIANGLES);

			/*getClearGLWindow().getProjectionMatrix()
												.mult(0, 0, mQuadProjectionMatrix.get(0, 0));
			getClearGLWindow().getProjectionMatrix()
												.mult(1, 1, mQuadProjectionMatrix.get(1, 1));/**/

			renderOverlays(lGL4, lInvVolumeMatrix);

			updateFrameRateDisplay();

			mGLVideoRecorder.screenshot(pDrawable);

		}/**/

	}

	private boolean isOverlay2DChanged()
	{
		boolean lHasAnyChanged = false;
		for (Overlay2D lOverlay2D : mOverlay2DMap.values())
			lHasAnyChanged |= lOverlay2D.hasChanged();
		return lHasAnyChanged;
	}

	private boolean isOverlay3DChanged()
	{
		boolean lHasAnyChanged = false;
		for (Overlay lOverlay3D : mOverlay3DMap.values())
			lHasAnyChanged |= lOverlay3D.hasChanged();
		return lHasAnyChanged;
	}

	private void renderOverlays(final GL4 lGL4,
															final GLMatrix lInvVolumeMatrix)
	{
		for (Overlay lOverlay : mOverlay3DMap.values())
		{
			try
			{
				lOverlay.render(lGL4,
												getClearGLWindow().getProjectionMatrix(),
												lInvVolumeMatrix);
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
			}
		}

		for (Overlay2D lOverlay2D : mOverlay2DMap.values())
		{
			try
			{
				lOverlay2D.render(lGL4,
													getClearGLWindow().getProjectionMatrix(),
													lInvVolumeMatrix);
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param pModelViewMatrix
	 *          Model-view matrix as float array
	 * @param pProjectionMatrix
	 *          Projection matrix as float array
	 * @return boolean array indicating for each layer if it was updated.
	 */
	protected abstract boolean[] renderVolume(final float[] pModelViewMatrix,
																						final float[] pProjectionMatrix);

	/**
	 * Updates the display of the framerate.
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
			String t = getWindowName() + " (";
			t += String.format("%.2f", fps) + " fps)";
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
		mClearGLWindow.getGLWindow().setTitle(pTitleString);
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
											final int pWidth,
											int pHeight)
	{
		if (pHeight < 8)
			pHeight = 8;

		final GL4 lGL4 = drawable.getGL().getGL4();

		int lViewPortWidth = max(pWidth, pHeight);

		lGL4.glViewport((pWidth - lViewPortWidth) / 2,
										(pHeight - lViewPortWidth) / 2,
										lViewPortWidth,
										lViewPortWidth);

		/*final float lAspectRatio = 1; // (1.0f * pWidth) / pHeight;

		/*if (lAspectRatio >= 1)
			mQuadProjectionMatrix.setOrthoProjectionMatrix(	-1,
																											1,
																											-1	/ lAspectRatio,
																											1 / lAspectRatio,
																											0,
																											1000);
		else
			mQuadProjectionMatrix.setOrthoProjectionMatrix(	-lAspectRatio,
																											lAspectRatio,
																											-1,
																											1,
																											0,
																											1000);/**/
	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose(final GLAutoDrawable arg0)
	{
		for (int i = 0; i < getNumberOfRenderLayers(); i++)
		{
			mLayerTextures[i].close();
			mPixelBufferObjects[i].close();
		}

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#toggleFullScreen()
	 */
	@Override
	public void toggleFullScreen()
	{
		try
		{
			if (mClearGLWindow.getGLWindow().isFullscreen())
			{
				if (mLastWindowWidth > 0 && mLastWindowHeight > 0)
					mClearGLWindow.getGLWindow().setSize(	mLastWindowWidth,
																								mLastWindowHeight);
				mClearGLWindow.getGLWindow().setFullscreen(false);
			}
			else
			{
				mLastWindowWidth = getWindowWidth();
				mLastWindowHeight = getWindowHeight();
				mClearGLWindow.getGLWindow().setFullscreen(true);
			}
			// notifyUpdateOfVolumeRenderingParameters();
			requestDisplay();
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
	@Override
	public boolean isFullScreen()
	{
		return mClearGLWindow.getGLWindow().isFullscreen();
	}

	/**
	 * Toggles box display.
	 */
	@Override
	public void toggleBoxDisplay()
	{
		mOverlay3DMap.get("box").toggleDisplay();
	}

	/**
	 * Toggles recording of rendered window frames.
	 */
	@Override
	public void toggleRecording()
	{
		mGLVideoRecorder.toggleActive();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.DisplayRequestInterface#requestDisplay()
	 */
	@Override
	public void requestDisplay()
	{
		final boolean lLocked = mDisplayReentrantLock.tryLock();
		if (lLocked)
		{
			try
			{
				if (mClearGLWindow == null)
					return;
				mClearGLWindow.getGLWindow().display();
				if (mNewtCanvasAWT != null)
				{
					mClearGLWindow.setVisible(true);
				}
				else
				{
					setVisible(true);
				}
			}
			catch (NullPointerException e)
			{
			}
			catch (Throwable e)
			{
				System.err.println("REQUESTED DISPLAY AFTER EDT SHUTDOWN (Warning = it's ok): " + e.getClass()
																																														.getSimpleName()
														+ "->"
														+ e.getLocalizedMessage());
			}
			finally
			{
				mDisplayReentrantLock.unlock();
			}
		}
	}

	@Override
	public void addOverlay(Overlay pOverlay)
	{
		mOverlay3DMap.put(pOverlay.getName(), pOverlay);
	}

	@Override
	public void addOverlay2D(Overlay2D pOverlay2D)
	{
		mOverlay2DMap.put(pOverlay2D.getName(), pOverlay2D);
	}

	@Override
	public void disableClose()
	{
		mClearGLWindow.getGLWindow()
									.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
	}

	private boolean anyIsTrue(final boolean[] pBooleanArray)
	{
		for (final boolean lBoolean : pBooleanArray)
			if (lBoolean)
				return true;
		return false;
	}

	/**
	 * @return the mNewtCanvasAWT
	 */
	@Override
	public NewtCanvasAWT getNewtCanvasAWT()
	{
		return mNewtCanvasAWT;
	}

}
