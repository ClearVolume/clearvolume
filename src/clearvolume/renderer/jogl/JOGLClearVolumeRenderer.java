package clearvolume.renderer.jogl;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
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
	private GLTexture<Byte> mTexture;

	// Internal fields for calculating FPS.
	private volatile int step = 0;
	private volatile long prevTimeNS = -1;

	// Box
	private volatile boolean mRenderBox = true;
	private static final float cBoxLineWidth = 1.f; // only cBoxLineWidth = 1.f
																									// seems to be supported

	private static final FloatBuffer cBoxColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 1.f });

	// Window:
	private String mWindowName;
	private GLProgram mGLProgram;
	private GLProgram mBoxGLProgram;

	// Shader attributes, uniforms and arrays:
	private GLAttribute mPositionAttribute;
	private GLVertexArray mQuadVertexArray;
	private GLVertexAttributeArray mPositionAttributeArray;

	private GLAttribute mBoxPositionAttribute;
	private GLVertexArray mBoxVertexArray;
	private GLVertexAttributeArray mBoxPositionAttributeArray;
	private GLUniform mBoxColorUniform;

	private GLMatrix mBoxModelViewMatrix = new GLMatrix();
	private GLUniform mBoxModelViewMatrixUniform;

	private GLUniform mBoxProjectionMatrixUniform;

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
		try
		{
			mClearGLWindow.close();
		}
		catch (GLException e)
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
		final GL4 lGL4 = drawable.getGL().getGL4();
		lGL4.setSwapInterval(0);
		lGL4.glDisable(GL4.GL_DEPTH_TEST);
		lGL4.glDisable(GL4.GL_STENCIL_TEST);
		lGL4.glEnable(GL4.GL_TEXTURE_2D);

		lGL4.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		lGL4.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

		// getClearGLWindow().setOrthoProjectionMatrix(0,
		// drawable.getSurfaceWidth(),
		// 0,
		// drawable.getSurfaceHeight(),
		// 0,
		// 1);

		getClearGLWindow().setPerspectiveProjectionMatrix(.785f,
																											1,
																											.1f,
																											1000);

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

			// texture display: construct the program and related objects
			try
			{
				mGLProgram = GLProgram.buildProgram(lGL4,
																						JOGLClearVolumeRenderer.class,
																						"shaders/tex_vert.glsl",
																						"shaders/tex_frag.glsl");

				mPositionAttribute = mGLProgram.getAtribute("position");
				mTexCoordAttribute = mGLProgram.getAtribute("texcoord");
				mTexUnit = mGLProgram.getUniform("texUnit");
				mTexUnit.set(0);

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
				mTexture = new GLTexture<Byte>(	mGLProgram,
																				Byte.class,
																				4,
																				lTextureWidth,
																				lTextureHeight,
																				1,
																				true,
																				3);

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

			// box display: construct the program and related objects
			try
			{
				mBoxGLProgram = GLProgram.buildProgram(	lGL4,
																								JOGLClearVolumeRenderer.class,
																								"shaders/box_vert.glsl",
																								"shaders/box_frag.glsl");

				// set the line with of the box
				lGL4.glLineWidth(cBoxLineWidth);

				// get all the shaders uniform locations
				mBoxPositionAttribute = mBoxGLProgram.getAtribute("position");
				mBoxModelViewMatrixUniform = mBoxGLProgram.getUniform("modelview");
				mBoxProjectionMatrixUniform = mBoxGLProgram.getUniform("projection");
				mBoxColorUniform = mBoxGLProgram.getUniform("color");

				// set up the vertices of the box
				mBoxVertexArray = new GLVertexArray(mBoxGLProgram);
				mBoxVertexArray.bind();
				mBoxPositionAttributeArray = new GLVertexAttributeArray(mBoxPositionAttribute,
																																4);

				// FIXME this should be done with IndexArrays, but lets be lazy for
				// now...
				GLFloatArray lVerticesFloatArray = new GLFloatArray(24, 4);

				final float w = .5f;

				lVerticesFloatArray.add(w, w, w, w);
				lVerticesFloatArray.add(-w, w, w, w);
				lVerticesFloatArray.add(-w, w, w, w);
				lVerticesFloatArray.add(-w, -w, w, w);
				lVerticesFloatArray.add(-w, -w, w, w);
				lVerticesFloatArray.add(w, -w, w, w);
				lVerticesFloatArray.add(w, -w, w, w);
				lVerticesFloatArray.add(w, w, w, w);
				lVerticesFloatArray.add(w, w, -w, w);
				lVerticesFloatArray.add(-w, w, -w, w);
				lVerticesFloatArray.add(-w, w, -w, w);
				lVerticesFloatArray.add(-w, -w, -w, w);
				lVerticesFloatArray.add(-w, -w, -w, w);
				lVerticesFloatArray.add(w, -w, -w, w);
				lVerticesFloatArray.add(w, -w, -w, w);
				lVerticesFloatArray.add(w, w, -w, w);
				lVerticesFloatArray.add(w, w, w, w);
				lVerticesFloatArray.add(w, w, -w, w);
				lVerticesFloatArray.add(-w, w, w, w);
				lVerticesFloatArray.add(-w, w, -w, w);
				lVerticesFloatArray.add(-w, -w, w, w);
				lVerticesFloatArray.add(-w, -w, -w, w);
				lVerticesFloatArray.add(w, -w, w, w);
				lVerticesFloatArray.add(w, -w, -w, w);

				mBoxVertexArray.addVertexAttributeArray(mBoxPositionAttributeArray,
																								lVerticesFloatArray.getFloatBuffer());

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
		final GL4 lGL4 = drawable.getGL().getGL4();

		lGL4.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

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

		GLMatrix lInvVolumeMatrix = new GLMatrix();
		lInvVolumeMatrix.copy(mVolumeViewMatrix);
		lInvVolumeMatrix.transpose();

		GLMatrix lInvProjection = new GLMatrix();
		lInvProjection.copy(getClearGLWindow().getProjectionMatrix());
		lInvProjection.transpose();
		lInvProjection.invert();

		if (renderVolume(	lInvVolumeMatrix.getFloatArray(),
											lInvProjection.getFloatArray()))
		{
			mTexture.copyFrom(mPixelBufferObject);

			mGLProgram.use(lGL4);

			mTexture.bind(mGLProgram);

			mQuadVertexArray.draw(GL.GL_TRIANGLES);

		}

		// draw the box
		if (mRenderBox)
		{
			mBoxGLProgram.use(lGL4);

			// invert Matrix is the modelview used by renderer which is actually the
			// inverted modelview Matrix
			mBoxModelViewMatrix.copy(mVolumeViewMatrix);
			mBoxModelViewMatrix.invert();

			mBoxModelViewMatrixUniform.setFloatMatrix(mBoxModelViewMatrix.getFloatArray(),
																								false);

			mBoxProjectionMatrixUniform.setFloatMatrix(	getClearGLWindow().getProjectionMatrix()
																																		.getFloatArray(),
																									false);

			mBoxColorUniform.setFloatVector4(cBoxColor);

			mBoxVertexArray.draw(GL.GL_LINES);

		}

		updateFrameRateDisplay();

	}

	/**
	 * @param pModelViewMatrix
	 *          Model-view matrix as float array
	 * @param pProjectionMatrix
	 *          Projection matrix as float array
	 * @return true if volume was updated and rendered.
	 */
	protected abstract boolean renderVolume(final float[] pModelViewMatrix,
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
	 * Toggles box display.
	 */
	@Override
	public void toggleBoxDisplay()
	{
		mRenderBox = !mRenderBox;
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