package clearvolume.renderer.cleargl;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import clearvolume.controller.OculusRiftController;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.Quaternion;

import cleargl.ClearGLEventListener;
import cleargl.ClearGLWindow;
import cleargl.GLAttribute;
import cleargl.GLError;
import cleargl.GLFloatArray;
import cleargl.GLMatrix;
import cleargl.GLPixelBufferObject;
import cleargl.GLProgram;
import cleargl.GLTexture;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;
import cleargl.util.recorder.GLVideoRecorder;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.controller.RotationControllerWithRenderNotification;
import clearvolume.renderer.ClearVolumeRendererBase;
import clearvolume.renderer.cleargl.overlay.Overlay;
import clearvolume.renderer.cleargl.overlay.Overlay2D;
import clearvolume.renderer.cleargl.overlay.Overlay3D;
import clearvolume.renderer.cleargl.overlay.o3d.BoxOverlay;
import clearvolume.renderer.cleargl.utils.ScreenToEyeRay;
import clearvolume.renderer.cleargl.utils.ScreenToEyeRay.EyeRay;
import clearvolume.renderer.listeners.EyeRayListener;
import coremem.types.NativeTypeEnum;
import scenery.*;

/**
 * Abstract Class ClearGLVolumeRenderer
 *
 * Classes that derive from this abstract class are provided with basic
 * JOGL-based display capability for implementing a ClearVolumeRenderer.
 *
 * @author Loic Royer 2014
 *
 */
public abstract class ClearGLVolumeRenderer	extends
																						ClearVolumeRendererBase	implements
																																		ClearGLEventListener
{

	static
	{
		// attempt at solving Jug's Dreadlock bug:
		final GLProfile lProfile = GLProfile.get(GLProfile.GL3);
		// System.out.println( lProfile );
	}

	// ClearGL Window.
	private volatile ClearGLWindow mClearGLWindow;
	private final String mWindowName;

	private NewtCanvasAWT mNewtCanvasAWT;
	private volatile int mLastWindowWidth, mLastWindowHeight;
	private volatile int mViewportWidth, mViewportHeight;
	
	// Overlays:
	protected final Map<String, Overlay> mOverlayMap = new ConcurrentHashMap<String, Overlay>();

	// Recorder:
	private final GLVideoRecorder mGLVideoRecorder = new GLVideoRecorder(new File(SystemUtils.USER_HOME,
																																								"Videos/ClearVolume"));

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name and its dimensions.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 */
	public ClearGLVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					NativeTypeEnum.UnsignedByte);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 */
	public ClearGLVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final NativeTypeEnum pNativeTypeEnum)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pNativeTypeEnum,
					768,
					768);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 * @param pMaxRenderWidth
	 *          max render width
	 * @param pMaxRenderHeight
	 *          max render height
	 */
	public ClearGLVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final NativeTypeEnum pNativeTypeEnum,
																final int pMaxRenderWidth,
																final int pMaxRenderHeight)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pNativeTypeEnum,
					pMaxRenderWidth,
					pMaxRenderHeight,
					1);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 * @param pMaxRenderWidth
	 *          max render width
	 * @param pMaxRenderHeight
	 *          max render height
	 * @param pUseInCanvas
	 *          if true, this Renderer will not be displayed in a window of it's
	 *          own, but must be embedded in a GUI as Canvas.
	 */
	public ClearGLVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final NativeTypeEnum pNativeTypeEnum,
																final int pMaxRenderWidth,
																final int pMaxRenderHeight,
																final boolean pUseInCanvas)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pNativeTypeEnum,
					pMaxRenderWidth,
					pMaxRenderHeight,
					1,
					pUseInCanvas);
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, number of bytes-per-voxel, max texture width, height
	 * and number of render layers.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 * @param pMaxTextureWidth
	 *          max render width
	 * @param pMaxTextureHeight
	 *          max render height
	 * @param pNumberOfRenderLayers
	 *          number of render layers
	 */
	public ClearGLVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final NativeTypeEnum pNativeTypeEnum,
																final int pMaxTextureWidth,
																final int pMaxTextureHeight,
																final int pNumberOfRenderLayers)
	{
		this(	pWindowName,
					pWindowWidth,
					pWindowHeight,
					pNativeTypeEnum,
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
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 * @param pMaxRenderWidth
	 *          max render width
	 * @param pMaxRenderHeight
	 *          max render height
	 * @param pNumberOfRenderLayers
	 *          number of render layers
	 * @param pUseInCanvas
	 *          if true, this Renderer will not be displayed in a window of it's
	 *          own, but must be embedded in a GUI as Canvas.
	 */
	@SuppressWarnings("unchecked")
	public ClearGLVolumeRenderer(	final String pWindowName,
																final int pWindowWidth,
																final int pWindowHeight,
																final NativeTypeEnum pNativeTypeEnum,
																final int pMaxRenderWidth,
																final int pMaxRenderHeight,
																final int pNumberOfRenderLayers,
																final boolean pUseInCanvas)
	{
		super(pNumberOfRenderLayers);

		mViewportWidth = pWindowWidth;
		mViewportHeight = pWindowHeight;

		//TODO: set max render width and height in Volume render modules.

		mWindowName = pWindowName;
		mLastWindowWidth = pWindowWidth;
		mLastWindowHeight = pWindowHeight;
		setNumberOfRenderLayers(pNumberOfRenderLayers);

		resetBrightnessAndGammaAndTransferFunctionRanges();
		resetRotationTranslation();
		setNativeType(pNativeTypeEnum);

		// addOverlay(new BoxOverlay(0, .2f, false, "box_plain"));
		addOverlay(new BoxOverlay(this, 10, 1.f, true, "box"));

		mClearGLWindow = new ClearGLWindow(	pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				this);
		mClearGLWindow.setFPS(60);

		mClearGLWindow.start();

		if (pUseInCanvas)
		{
			mNewtCanvasAWT = mClearGLWindow.getNewtCanvasAWT();
			mNewtCanvasAWT.setShallUseOffscreenLayer(true);
		}
		else
		{
			mNewtCanvasAWT = null;
		}

		// Initialize the mouse controls
		final MouseControl lMouseControl = new MouseControl(this);
		mClearGLWindow.addMouseListener(lMouseControl);

		// Initialize the keyboard controls
		final KeyboardControl lKeyboardControl = new KeyboardControl(	this,
																																	lMouseControl);
		mClearGLWindow.addKeyListener(lKeyboardControl);

		mClearGLWindow.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowDestroyNotify(final WindowEvent pE)
			{
				super.windowDestroyNotify(pE);
			};
		});

	}

	@Override
	public void addOverlay(Overlay pOverlay)
	{
		mOverlayMap.put(pOverlay.getName(), pOverlay);
	}

//	private void applyControllersTransform()
//	{
//		if (getRotationControllers().size() > 0)
//		{
//			final Quaternion lQuaternion = new Quaternion();
//
//			for (final RotationControllerInterface lRotationController : getRotationControllers())
//				if (lRotationController.isActive())
//				{
//					if (lRotationController instanceof RotationControllerWithRenderNotification)
//					{
//						final RotationControllerWithRenderNotification lRenderNotification = (RotationControllerWithRenderNotification) lRotationController;
//						lRenderNotification.notifyRender(this);
//					}
//					lQuaternion.mult(lRotationController.getQuaternion());
//
//					notifyChangeOfVolumeRenderingParameters();
//				}
//
//			lQuaternion.mult(getQuaternion());
//			setQuaternion(lQuaternion);
//		}
//	}

	@Override
	public void close()
	{

		if (mNewtCanvasAWT != null)
		{
			mNewtCanvasAWT = null;

			return;
		}

		try
		{
			mClearGLWindow.close();
		}
		catch (final NullPointerException e)
		{
		}
		catch (final Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
		}

		super.close();

	}

	@Override
	public void disableClose()
	{
		mClearGLWindow.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
	}

	/**
	 * Implementation of GLEventListener: Called when the given GLAutoDrawable is
	 * to be displayed.
	 */
	@Override
	public void display(final GLAutoDrawable pDrawable)
	{
		displayInternal(pDrawable);
	}

	private void displayInternal(final GLAutoDrawable pDrawable)
	{

		final boolean lTryLock = true;

		mDisplayReentrantLock.lock();

		if (lTryLock)
			try
			{

				//TODO: ask scene graph whether rendering parameters or volume data changed...
				if (true)//(haveVolumeRenderingParametersChanged() || isNewVolumeDataAvailable())
					getAdaptiveLODController().renderingParametersOrVolumeDataChanged();

				final boolean lLastRenderPass = getAdaptiveLODController().beforeRendering();

				//TODO: render scene here

				getAdaptiveLODController().afterRendering();

				if (lLastRenderPass)
					mGLVideoRecorder.screenshot(pDrawable,
																			!getAutoRotateController().isRotating());

			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (mDisplayReentrantLock.isHeldByCurrentThread())
					mDisplayReentrantLock.unlock();
			}

	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose(final GLAutoDrawable arg0)
	{
		mClearGLWindow.stop();
	}

	@Override
	public ClearGLWindow getClearGLWindow()
	{
		return mClearGLWindow;
	}

	public float[] getLightVector()
	{
		// TODO: get light vector from scene
		return null;
	}


	/**
	 * @return the mNewtCanvasAWT
	 */
	@Override
	public NewtCanvasAWT getNewtCanvasAWT()
	{
		return mNewtCanvasAWT;
	}

	@Override
	public Collection<Overlay> getOverlays()
	{
		return mOverlayMap.values();
	}

	public int getViewportHeight()
	{
		return mViewportHeight;
	}

	public int getViewportWidth()
	{
		return mViewportWidth;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowHeight()
	 */
	@Override
	public int getWindowHeight()
	{
		return mClearGLWindow.getHeight();
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
		return mClearGLWindow.getWidth();
	}

	/**
	 * Implementation of GLEventListener: Called to initialize the GLAutoDrawable.
	 * This method will initialize the JCudaDriver and cause the initialization of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
		final GL lGL = drawable.getGL();
		lGL.setSwapInterval(1);

		lGL.glDisable(GL.GL_DEPTH_TEST);
		lGL.glEnable(GL.GL_BLEND);
		lGL.glDisable(GL.GL_STENCIL_TEST);

		lGL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		lGL.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		// Call scene graph initialization code here (load programs and such)

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isFullScreen()
	 */
	@Override
	public boolean isFullScreen()
	{
		return mClearGLWindow.isFullscreen();
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
			if (mNewtCanvasAWT != null)
				return mNewtCanvasAWT.isVisible();

			if (mClearGLWindow != null)
				return mClearGLWindow.isVisible();
		}
		catch (final NullPointerException e)
		{
			return false;
		}

		return false;
	}

	abstract protected void notifyChangeOfTextureDimensions();

	/**
	 * Notifies eye ray listeners.
	 * 
	 * @param pRenderer
	 *          renderer that calls listeners
	 * @param pMouseEvent
	 *          associated mouse event.
	 * @return true if event captured
	 */
	public boolean notifyEyeRayListeners(	ClearGLVolumeRenderer pRenderer,
																				MouseEvent pMouseEvent)
	{
		if (mEyeRayListenerList.isEmpty())
			return false;

		final int lX = pMouseEvent.getX();
		final int lY = pMouseEvent.getY();

		
		
		final GLMatrix lInverseModelViewMatrix = null; 
		// " getModelViewMatrix().clone().invert(); " <- this needs to be provided by scene graph 
		
		final GLMatrix lInverseProjectionMatrix = getClearGLWindow().getProjectionMatrix()
																																.clone()
																																.invert();

		final GLMatrix lAspectRatioCorrectedProjectionMatrix = null; 
		// " getAspectRatioCorrectedProjectionMatrix().invert(); " <- this needs to be provided by scene graph 

		// lInverseModelViewMatrix.transpose();
		// lInverseProjectionMatrix.invert();
		// lInverseProjectionMatrix.transpose();

		final EyeRay lEyeRay = ScreenToEyeRay.convert(getViewportWidth(),
																									getViewportHeight(),
																									lX,
																									lY,
																									lInverseModelViewMatrix,
																									lAspectRatioCorrectedProjectionMatrix);

		boolean lPreventOtherDisplayChanges = false;

		for (final EyeRayListener lEyeRayListener : mEyeRayListenerList)
		{
			lPreventOtherDisplayChanges |= lEyeRayListener.notifyEyeRay(pRenderer,
																																	pMouseEvent,
																																	lEyeRay);
			if (lPreventOtherDisplayChanges)
				break;
		}
		return lPreventOtherDisplayChanges;
	}




	/* (non-Javadoc)
	 * @see com.jogamp.opengl.GLEventListener#reshape(com.jogamp.opengl.GLAutoDrawable, int, int, int, int)
	 */
	@Override
	public void reshape(final GLAutoDrawable pDrawable,
											final int x,
											final int y,
											int pWidth,
											int pHeight)
	{
		try
		{
			getAdaptiveLODController().notifyUserInteractionInProgress();

			setViewportWidth(pWidth);
			setViewportHeight(pHeight);

			// TODO: tell scene graph renderer about reshape event.

			// TODO: inform the volume rendering module about viewport resolution so
			// that render textures are changed
			// in size (code can be found in original renderer)

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public void setClearGLWindow(final ClearGLWindow pClearGLWindow)
	{
		mClearGLWindow = pClearGLWindow;
	}

	public void setLightVector(final float[] pLight)
	{
		// TODO: set light vector in scene
	}

	public void setViewportHeight(int pViewportHeight)
	{
		mViewportHeight = pViewportHeight;
	}

	public void setViewportWidth(int pViewportWidth)
	{
		mViewportWidth = pViewportWidth;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVisible(boolean)
	 */
	@Override
	public void setVisible(final boolean pIsVisible)
	{
		if (mNewtCanvasAWT == null)
			mClearGLWindow.setVisible(pIsVisible);
	}

	/**
	 * @param pTitleString
	 */
	public void setWindowTitle(final String pTitleString)
	{
		mClearGLWindow.setWindowTitle(pTitleString);
	}

	/**
	 * Toggles box display.
	 */
	@Override
	public void toggleBoxDisplay()
	{
		mOverlayMap.get("box").toggle();
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
			if (mClearGLWindow.isFullscreen())
			{
				if (mLastWindowWidth > 0 && mLastWindowHeight > 0)
					mClearGLWindow.setSize(mLastWindowWidth, mLastWindowHeight);
				mClearGLWindow.setFullscreen(false);
			}
			else
			{
				mLastWindowWidth = getWindowWidth();
				mLastWindowHeight = getWindowHeight();
				mClearGLWindow.setFullscreen(true);
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
	 * Toggles recording of rendered window frames.
	 */
	@Override
	public void toggleRecording()
	{
		mGLVideoRecorder.toggleActive();
	}

	@Override
	public GL getGL()
	{
		if (mClearGLWindow != null)
		{
			return mClearGLWindow.getGL();
		}
		else
		{
			return null;
		}
	}

}
