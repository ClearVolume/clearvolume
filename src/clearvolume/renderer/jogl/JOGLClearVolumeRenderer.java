package clearvolume.renderer.jogl;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import cleargl.ClearGLEventListener;
import cleargl.ClearGLWindow;
import cleargl.GLAttribute;
import cleargl.GLFloatArray;
import cleargl.GLMatrix;
import cleargl.GLPixelBufferObject;
import cleargl.GLProgram;
import cleargl.GLTexture;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;
import clearvolume.renderer.ClearVolumeRendererBase;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

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

	// ClearGL Window.
	private ClearGLWindow mClearGLWindow;
	private volatile int mLastWindowWidth, mLastWindowHeight;
	private ReentrantLock mDisplayReentrantLock = new ReentrantLock();

	// pixelbuffer object.
	protected GLPixelBufferObject mPixelBufferObject;

	// texture and its dimensions.
	private GLTexture mTexture;

	// Internal fields for calculating FPS.
	private volatile int step = 0;
	private volatile long prevTimeNS = -1;

	private String mWindowName;
	private GLProgram mGLProgram;
	private GLAttribute mPositionAttribute;
	private GLVertexArray mQuadVertexArray;
	private GLVertexAttributeArray mPositionAttributeArray;

	private GLMatrix mVolumeViewMatrix = new GLMatrix();
	private GLAttribute mTexCoordAttribute;
	private GLUniform mTexUnit;
	private GLVertexAttributeArray mTexCoordAttributeArray;
	private int mMaxTextureWidth = 768, mMaxTextureHeight = 768;

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
	 * @param pBytesPerPixel
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
		mWindowName = pWindowName;
		mLastWindowWidth = pMaxTextureWidth;
		mLastWindowHeight = pMaxTextureHeight;
		resetBrightnessAndGammaAndTransferFunctionRanges();
		resetRotationTranslation();
		setBytesPerVoxel(pBytesPerVoxel);

		// Initialize the GL component
		final GLProfile lProfile = GLProfile.getMaxFixedFunc(true);
		final GLCapabilities lCapabilities = new GLCapabilities(lProfile);

		mClearGLWindow = new ClearGLWindow(	pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				this);

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
	public void setClearGLWindow(ClearGLWindow pClearGLWindow)
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
		mClearGLWindow.close();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isShowing()
	 */
	@Override
	public boolean isShowing()
	{
		return mClearGLWindow.getGLWindow().isVisible();
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
	 * @return
	 */
	public int getTextureWidth()
	{
		return mTexture.getWidth();
	}

	/**
	 * @return
	 */
	public int getTextureHeight()
	{
		return mTexture.getHeight();
	}

	/**
	 * @return
	 */
	protected float[] getTransfertFunctionArray()
	{
		return getTransfertFunction().getArray();
	}

	/**
	 * Implementation of GLEventListener: Called to initialise the GLAutoDrawable.
	 * This method will initialise the JCudaDriver and cause the initialisation of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
		final GL3 lGL3 = drawable.getGL().getGL3();
		lGL3.setSwapInterval(0);
		lGL3.glDisable(GL3.GL_DEPTH_TEST);
		lGL3.glDisable(GL3.GL_STENCIL_TEST);
		lGL3.glEnable(GL3.GL_TEXTURE_2D);

		lGL3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		lGL3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

		getClearGLWindow().setOrthoProjectionMatrix(0,
																								drawable.getSurfaceWidth(),
																								0,
																								drawable.getSurfaceHeight(),
																								0,
																								1);

		if (initVolumeRenderer())
		{
			if (mPixelBufferObject != null)
			{
				unregisterPBO(mPixelBufferObject.getId());
				mPixelBufferObject.close();
				mPixelBufferObject = null;
			}

			if (mTexture != null)
			{
				mTexture.close();
				mTexture = null;
			}

			try
			{
				mGLProgram = GLProgram.buildProgram(lGL3,
																						JOGLClearVolumeRenderer.class,
																						"shaders/vertex.glsl",
																						"shaders/fragment.glsl");

				mPositionAttribute = mGLProgram.getAtribute("position");
				mTexCoordAttribute = mGLProgram.getAtribute("texcoord");
				mTexUnit = mGLProgram.getUniform("texUnit");
				mTexUnit.setInt(0);

				mQuadVertexArray = new GLVertexArray(mGLProgram);
				mQuadVertexArray.bind();
				mPositionAttributeArray = new GLVertexAttributeArray(	mPositionAttribute,
																															4);

				GLFloatArray lVerticesFloatArray = new GLFloatArray(6, 4);
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

				GLFloatArray lTexCoordFloatArray = new GLFloatArray(6, 2);
				lTexCoordFloatArray.add(0, 0);
				lTexCoordFloatArray.add(1, 0);
				lTexCoordFloatArray.add(1, 1);
				lTexCoordFloatArray.add(0, 0);
				lTexCoordFloatArray.add(1, 1);
				lTexCoordFloatArray.add(0, 1);

				mQuadVertexArray.addVertexAttributeArray(	mTexCoordAttributeArray,
																									lTexCoordFloatArray.getFloatBuffer());

				int lTextureWidth = Math.min(	mMaxTextureWidth,
																			getWindowWidth());
				int lTextureHeight = Math.min(mMaxTextureHeight,
																			getWindowHeight());
				mTexture = new GLTexture(	mGLProgram,
																	lTextureWidth,
																	lTextureHeight);

				mPixelBufferObject = new GLPixelBufferObject(	mGLProgram,
																											mTexture.getWidth(),
																											mTexture.getHeight());

				mPixelBufferObject.copyFrom(null);

				registerPBO(mPixelBufferObject.getId());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}

	}

	/**
	 * @return
	 */
	protected abstract boolean initVolumeRenderer();

	/**
	 * Register PBO object with any descendant of this abstract class.
	 * 
	 * @param pPixelBufferObjectId
	 */
	protected abstract void registerPBO(int pPixelBufferObjectId);

	/**
	 * Unregisters PBO object with any descendant of this abstract class.
	 * 
	 * @param pPixelBufferObjectId
	 */
	protected abstract void unregisterPBO(int pPixelBufferObjectId);

	/**
	 * Implementation of GLEventListener: Called when the given GLAutoDrawable is
	 * to be displayed.
	 */
	@Override
	public void display(final GLAutoDrawable drawable)
	{
		final GL3 lGL3 = drawable.getGL().getGL3();

		lGL3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

		mVolumeViewMatrix.euler(-getRotationX() * 0.01,
														-getRotationY() * 0.01,
														0.0f);
		if (hasRotationController())
		{
			getRotationController().rotate(mVolumeViewMatrix);
			notifyUpdateOfVolumeRenderingParameters();
		}
		mVolumeViewMatrix.translate(-getTranslationX(),
																-getTranslationY(),
																-getTranslationZ());

		if (renderVolume(mVolumeViewMatrix.getFloatArray()))
		{
			mTexture.copyFrom(mPixelBufferObject);

			mGLProgram.use(lGL3);

			mTexture.bind(mGLProgram);

			mQuadVertexArray.draw(GL.GL_TRIANGLES);

			updateFrameRateDisplay();
		}
	}

	/**
	 * @param gl
	 * @param modelView
	 * @return
	 */
	protected abstract boolean renderVolume(final float[] modelView);

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
		mClearGLWindow.getGLWindow().setTitle(pTitleString);
	}

	/**
	 * Interface method implementation
	 * 
	 * @see javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable,
	 *      int, int, int, int)
	 */
	@Override
	public void reshape(GLAutoDrawable drawable,
											int x,
											int y,
											int pWidth,
											int pHeight)
	{
	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose(final GLAutoDrawable arg0)
	{
		mTexture.close();
		mPixelBufferObject.close();
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
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.DisplayRequestInterface#requestDisplay()
	 */
	@Override
	public void requestDisplay()
	{
		boolean lLocked = mDisplayReentrantLock.tryLock();
		if (lLocked)
		{
			mClearGLWindow.getGLWindow().display();
			mDisplayReentrantLock.unlock();
		}
	}

	/**
	 * 
	 */
	public void disableClose()
	{
		mClearGLWindow.getGLWindow()
									.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
	}

}