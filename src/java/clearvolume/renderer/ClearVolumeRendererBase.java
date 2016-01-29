package clearvolume.renderer;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.math.Quaternion;

import clearvolume.ClearVolumeCloseable;
import clearvolume.controller.AutoRotationController;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.controller.TranslationRotationControllerInterface;
import clearvolume.renderer.cleargl.overlay.Overlay;
import clearvolume.renderer.listeners.EyeRayListener;
import clearvolume.renderer.listeners.ParameterChangeListener;
import clearvolume.renderer.listeners.VolumeCaptureListener;
import clearvolume.renderer.panels.ControlPanelJFrame;
import clearvolume.renderer.panels.HasGUIPanel;
import clearvolume.renderer.panels.ParametersListPanelJFrame;
import clearvolume.renderer.processors.ProcessorInterface;
import clearvolume.transferf.TransferFunction;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemory;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;
import coremem.util.Size;
import scenery.Scene;

/**
 * Class ClearVolumeRendererBase
 *
 * Instances of this class ...
 *
 * @author Loic Royer (2014), Florian Jug (2015)
 *
 */

public abstract class ClearVolumeRendererBase	implements
																							ClearVolumeRendererInterface,
																							ClearVolumeCloseable
{

	protected Scene scene;

	/**
	 * Default FOV
	 */
	public static final float cDefaultFOV = .785f;
	public static final float cOrthoLikeFOV = .01f;
	public static final float cMinimalFOV = cOrthoLikeFOV;
	public static final float cMaximalFOV = (float) (0.75 * PI);

	// Timeout:
	private static final long cDefaultSetVolumeDataBufferTimeout = 5;

	/**
	 * Clamps the value pValue to the interval [pMin,pMax]
	 *
	 * @param pValue
	 *          to be clamped
	 * @param pMin
	 *          minimum
	 * @param pMax
	 *          maximum
	 * @return clamped size
	 */
	public static double clamp(	final double pValue,
															final double pMin,
															final double pMax)
	{
		return Math.min(Math.max(pValue, pMin), pMax);
	}

	/**
	 * Number of render layers.
	 */
	private int mNumberOfRenderLayers;

	private volatile int mCurrentRenderLayerIndex = 0;

	/**
	 * Number of bytes per voxel used by this renderer
	 */
	private volatile NativeTypeEnum mNativeType = NativeTypeEnum.Byte;

	/**
	 * Rotation controller in addition to the mouse
	 */
	private final ArrayList<RotationControllerInterface> mRotationControllerList = new ArrayList<RotationControllerInterface>();

	/**
	 * Translation/rotation controllers, e.g. VR glasses
	 */
	private final ArrayList<TranslationRotationControllerInterface> mTransRotControllerList = new ArrayList<TranslationRotationControllerInterface>();

	/**
	 * Auto rotation controller
	 */
	private final AutoRotationController mAutoRotationController;

	// Render algorithm per layer:
	private final RenderAlgorithm[] mRenderAlgorithm;
	// render parameters per layer;

	private volatile boolean mVolumeDataUpdateAllowed = true;
	// volume dimensions settings

	// Processors:
	protected Map<String, ProcessorInterface<?>> mProcessorInterfacesMap = new ConcurrentHashMap<>();

	// List of Capture Listeners
	protected ArrayList<VolumeCaptureListener> mVolumeCaptureListenerList = new ArrayList<VolumeCaptureListener>();

	protected volatile boolean mVolumeCaptureFlag = false;

	// Adaptive LOD controller:
	protected AdaptiveLODController mAdaptiveLODController;

	// Eye ray listeners:
	protected CopyOnWriteArrayList<EyeRayListener> mEyeRayListenerList = new CopyOnWriteArrayList<EyeRayListener>();

	// Eye ray listeners:
	protected CopyOnWriteArrayList<ParameterChangeListener> mParameterChangeListenerList = new CopyOnWriteArrayList<ParameterChangeListener>();

	// Display lock:
	protected final ReentrantLock mDisplayReentrantLock = new ReentrantLock(true);

	// GUI Frames:
	private ControlPanelJFrame mControlFrame;

	private final ParametersListPanelJFrame mParametersListFrame;

	public ClearVolumeRendererBase(final int pNumberOfRenderLayers)
	{
		super();

		mNumberOfRenderLayers = pNumberOfRenderLayers;
		mRenderAlgorithm = new RenderAlgorithm[pNumberOfRenderLayers];

		for (int i = 0; i < pNumberOfRenderLayers; i++)
		{
			mRenderAlgorithm[i] = RenderAlgorithm.MaxProjection;
		}

		mAdaptiveLODController = new AdaptiveLODController();

		mAutoRotationController = new AutoRotationController();
		mRotationControllerList.add(mAutoRotationController);

		mParametersListFrame = new ParametersListPanelJFrame();
	}

	/**
	 * Adds a translation/rotation controller.
	 *
	 * @param pTranslationRotationControllerInterface
	 *          translation/rotation controller
	 */
	@Override
	public void addTranslationRotationController(TranslationRotationControllerInterface pTranslationRotationControllerInterface)
	{
		mTransRotControllerList.add(pTranslationRotationControllerInterface);
	}

	public ArrayList<RotationControllerInterface> getRotationControllers()
	{
		return mRotationControllerList;
	}

	/**
	 * Returns the current list of rotation controllers.
	 *
	 * @return currently used rotation controller.
	 */
	@Override
	public ArrayList<TranslationRotationControllerInterface> getTranslationRotationControllers()
	{
		return mTransRotControllerList;
	}

	/**
	 * Adds a eye ray listener to this renderer.
	 *
	 * @param pEyeRayListener
	 *          eye ray listener
	 */
	@Override
	public void addEyeRayListener(EyeRayListener pEyeRayListener)
	{
		mEyeRayListenerList.add(pEyeRayListener);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void addFOV(double pDelta)
	{
		// TODO: add Delta to FOF in scene graph renderer.
		// setFOV(mFOV + pDelta);
	}

	@Override
	public void addParameterChangeListener(ParameterChangeListener pParameterChangeListener)
	{
		mParameterChangeListenerList.add(pParameterChangeListener);
	}

	@Override
	public void addProcessor(final ProcessorInterface<?> pProcessor)
	{
		mProcessorInterfacesMap.put(pProcessor.getName(), pProcessor);
		if (pProcessor instanceof HasGUIPanel)
		{
			final HasGUIPanel lHasGUIPanel = (HasGUIPanel) pProcessor;
			final JPanel lPanel = lHasGUIPanel.getPanel();
			if (lPanel != null)
				mParametersListFrame.addPanel(lPanel);
		}
	}

	@Override
	public void addProcessors(final Collection<ProcessorInterface<?>> pProcessors)
	{
		for (final ProcessorInterface<?> lProcessor : pProcessors)
			addProcessor(lProcessor);
	}

	/**
	 * Adds a rotation controller.
	 *
	 * @param pRotationControllerInterface
	 *          rotation controller
	 */
	@Override
	public void addRotationController(RotationControllerInterface pRotationControllerInterface)
	{
		mRotationControllerList.add(pRotationControllerInterface);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationX(double)
	 */
	@Override
	public void addTranslationX(final double pDX)
	{
		getDisplayLock().lock();
		try
		{
			setTranslationX(getTranslationX() + pDX);
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationY(double)
	 */
	@Override
	public void addTranslationY(final double pDY)
	{
		getDisplayLock().lock();
		try
		{
			setTranslationY(getTranslationY() + pDY);
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationZ(double)
	 */
	@Override
	public void addTranslationZ(final double pDZ)
	{
		getDisplayLock().lock();
		try
		{
			setTranslationZ(getTranslationZ() + pDZ);
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	@Override
	public void addVolumeCaptureListener(final VolumeCaptureListener pVolumeCaptureListener)
	{
		if (pVolumeCaptureListener != null)
			mVolumeCaptureListenerList.add(pVolumeCaptureListener);
	}

	@Override
	public void close()
	{
		if (mControlFrame != null)
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{

					@Override
					public void run()
					{
						if (mControlFrame != null)
							try
							{
								mControlFrame.dispose();
								mControlFrame = null;
							}
							catch (final Throwable e)
							{
								e.printStackTrace();
							}
					}
				});
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
			}

	}

	@Override
	public VolumeManager createCompatibleVolumeManager(final int pMaxAvailableVolumes)
	{
		return new VolumeManager(pMaxAvailableVolumes);
	}

	/**
	 * Cycles through rendering algorithms for all layers
	 */
	@Override
	public void cycleRenderAlgorithm()
	{
		getDisplayLock().lock();
		try
		{
			int i = 0;
			for (final RenderAlgorithm lRenderAlgorithm : mRenderAlgorithm)
				mRenderAlgorithm[i++] = lRenderAlgorithm.next();
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Cycles through rendering algorithms for all layers
	 */
	@Override
	public void cycleRenderAlgorithm(int pRenderLayerIndex)
	{
		getDisplayLock().lock();
		try
		{
			mRenderAlgorithm[pRenderLayerIndex] = mRenderAlgorithm[pRenderLayerIndex].next();
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	@Override
	public boolean getAdaptiveLODActive()
	{
		return getAdaptiveLODController().isActive();
	}

	/**
	 * Returns the Adaptive level-of-detail(LOD) controller.
	 * 
	 * @return LOD controller
	 */
	@Override
	public AdaptiveLODController getAdaptiveLODController()
	{
		return mAdaptiveLODController;
	}

	/**
	 * Returns the auto rotation controller.
	 *
	 * @return auto rotation controller
	 */
	@Override
	public AutoRotationController getAutoRotateController()
	{
		return mAutoRotationController;
	}

	/**
	 * 
	 * Returns the brightness level of the current render layer.
	 *
	 * @return brightness level.
	 */
	@Override
	public double getBrightness()
	{
		return getBrightness(getCurrentRenderLayerIndex());
	}

	/**
	 * Returns the brightness level of a given render layer index.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return brightness level.
	 */
	@Override
	public double getBrightness(final int pRenderLayerIndex)
	{
		// TODO: asks scene graph about brightness
		// return mBrightness[pRenderLayerIndex];
		return 0;
	}

	/**
	 * Returns the number of bytes per voxel for this renderer.
	 * 
	 * @return bytes per voxel
	 */
	@Override
	public long getBytesPerVoxel()
	{
		return Size.of(mNativeType);
	}

	/**
	 * Returns current render layer.
	 * 
	 * @return current render layer index
	 */
	@Override
	public int getCurrentRenderLayerIndex()
	{
		return mCurrentRenderLayerIndex;
	}

	@Override
	public ReentrantLock getDisplayLock()
	{
		return mDisplayReentrantLock;
	}

	/**
	 * Returns the amount of dithering [0,1] for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return dithering
	 */
	@Override
	public float getDithering(int pRenderLayerIndex)
	{
		// TODO: asks scene graph about dithering
		// return mDithering[pRenderLayerIndex];
		return 0;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public float getFOV()
	{
		// TODO: asks scene graph about FOV
		// return mFOV;
		return 0;
	}

	/**
	 * Returns the Gamma size.
	 *
	 * @return gamma size
	 */
	@Override
	public double getGamma()
	{
		return getGamma(getCurrentRenderLayerIndex());
	}

	/**
	 * Returns the Gamma value.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return gamma value for layer
	 */
	@Override
	public double getGamma(final int pRenderLayerIndex)
	{
		// TODO: asks scene graph about gamma
		// return mGamma[pRenderLayerIndex];
		return 0;
	};

	/**
	 * Returns the maximal number of steps during ray casting for a given layer.
	 * This size depends on the volume dimension and quality.
	 * 
	 * @param pRenderLayerIndex
	 *          renderlayer index
	 * @return maximal number of steps
	 */
	public int getMaxSteps(final int pRenderLayerIndex)
	{
		return (int) (sqrt(getVolumeSizeX(pRenderLayerIndex) * getVolumeSizeX(pRenderLayerIndex)
												+ getVolumeSizeY(pRenderLayerIndex)
												* getVolumeSizeY(pRenderLayerIndex)
												+ getVolumeSizeZ(pRenderLayerIndex)
												* getVolumeSizeZ(pRenderLayerIndex)) * getQuality(pRenderLayerIndex));
	};

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getNativeType()
	 */
	@Override
	public NativeTypeEnum getNativeType()
	{
		return mNativeType;
	};

	/**
	 * Returns number of render layers.
	 * 
	 * @return current render layer index
	 */
	@Override
	public int getNumberOfRenderLayers()
	{
		return mNumberOfRenderLayers;
	};

	@Override
	public Collection<ProcessorInterface<?>> getProcessors()
	{
		return mProcessorInterfacesMap.values();
	}

	/**
	 * Returns the quality level [0,1] for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return quality level
	 */
	@Override
	public float getQuality(int pRenderLayerIndex)
	{
		// TODO: asks scene graph about gamma
		// return mQuality[pRenderLayerIndex];
		return 0;
	}

	/**
	 * Returns currently used mProjectionMatrix algorithm.
	 *
	 * @return currently used mProjectionMatrix algorithm
	 */
	@Override
	public RenderAlgorithm getRenderAlgorithm(final int pRenderLayerIndex)
	{
		return mRenderAlgorithm[pRenderLayerIndex];
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTransferFunction(int)
	 */
	@Override
	public TransferFunction getTransferFunction(final int pRenderLayerIndex)
	{
		// TODO: ask scene graph about layer's transfer function
		// return mTransferFunctions[pRenderLayerIndex];
		return null;
	}

	/**
	 * Returns currently used transfer function.
	 *
	 * @return currently used transfer function
	 */
	@Override
	public float[] getTransferFunctionArray()
	{
		// TODO: ask scene grah for transfer functions and make an array:
		// return mTransferFunctions[getCurrentRenderLayerIndex()].getArray();
		return null;
	}

	/**
	 * 
	 * Returns the maximum of the transfer function range for the current render
	 * layer index.
	 *
	 * @return minimum
	 */
	@Override
	public double getTransferRangeMax()
	{
		return getTransferRangeMax(getCurrentRenderLayerIndex());
	}

	/**
	 * Returns the maximum of the transfer function range for a given render layer
	 * index.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return maximum of transfer function range
	 */
	@Override
	public double getTransferRangeMax(final int pRenderLayerIndex)
	{
		// TODO: ask scene graph for the TransferRangeMax
		// return mTransferFunctionRangeMax[pRenderLayerIndex];
		return 0;
	}

	/**
	 * Returns the minimum of the transfer function range for the current render
	 * layer.
	 *
	 * @return minimum
	 */
	@Override
	public double getTransferRangeMin()
	{
		return getTransferRangeMin(getCurrentRenderLayerIndex());
	}

	/**
	 * Returns the minimum of the transfer function range for a given render
	 * layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return minimum of transfer function range
	 */
	@Override
	public double getTransferRangeMin(final int pRenderLayerIndex)
	{
		// TODO: ask scene graph for the TransferRangeMin
		// return mTransferFunctionRangeMin[pRenderLayerIndex];
		return 0;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationX()
	 */
	@Override
	public float getTranslationX()
	{
		// TODO: asks scene graph about translation vector
		return 0;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationY()
	 */
	@Override
	public float getTranslationY()
	{
		// TODO: asks scene graph about translation vector
		return 0;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public float getTranslationZ()
	{
		// TODO: asks scene graph about translation vector
		return 0;
	}

	/**
	 * Returns for a given index the corresponding volume data buffer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return data buffer for a given render layer.
	 */
	public FragmentedMemoryInterface getVolumeDataBuffer(final int pRenderLayerIndex)
	{
		// TODO: ask scene graph volume nodes to give the buffer
		// return mVolumeDataByteBuffers[pRenderLayerIndex];
		return null;
	}

	/**
	 * Returns the volume size along x axis for a given render layer.
	 *
	 * @return volume size along x
	 */
	public long getVolumeSizeX(final int pRenderLayerIndex)
	{
		// TODO: asks volume node about voxel size
		return 0;
	}

	/**
	 * Returns the volume size along y axis for a given render layer.
	 *
	 * @return volume size along y
	 */
	public long getVolumeSizeY(final int pRenderLayerIndex)
	{
		// TODO: asks volume node about voxel size
		return 0;
	}

	/**
	 * Returns the volume size along z axis for a given render layer.
	 *
	 * @return volume size along z
	 */
	public long getVolumeSizeZ(final int pRenderLayerIndex)
	{
		// TODO: asks volume node about voxel size
		return 0;
	}

	/**
	 * Returns the voxel size along X in A.U. for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index.
	 * @return voxel size in A.U.
	 */
	public double getVoxelSizeX(final int pRenderLayerIndex)
	{
		// TODO: asks volume node about voxel size
		return 0;
	}

	/**
	 * Returns the voxel size along Y in A.U. for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index.
	 * @return voxel size in A.U.
	 */
	public double getVoxelSizeY(final int pRenderLayerIndex)
	{
		// TODO: asks volume node about voxel size
		return 0;
	}

	/**
	 * Returns the voxel size along Z in A.U. for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index.
	 * @return voxel size in A.U.
	 */
	public double getVoxelSizeZ(final int pRenderLayerIndex)
	{
		// TODO: asks volume node about voxel size
		return 0;
	}

	/**
	 * Gets active flag for the current render layer.
	 *
	 * @return true if layer visible
	 */
	@Override
	public boolean isLayerVisible()
	{
		return isLayerVisible(getCurrentRenderLayerIndex());
	}

	/**
	 * Gets active flag for the given render layer.
	 *
	 * @return true if layer visible
	 */
	@Override
	public boolean isLayerVisible(final int pRenderLayerIndex)
	{
		// TODO: ask scene graph volume render nodes if the layers are visible.
		return false;
	}

	/**
	 * Returns the state of the flag that allows/disallows the update of volume
	 * data.
	 * 
	 * @return flag state,
	 */
	@Override
	public boolean isVolumeDataUpdateAllowed()
	{
		return mVolumeDataUpdateAllowed;
	}

	public void notifyVolumeCaptureListeners(	ByteBuffer pCaptureBuffer,
																						NativeTypeEnum pNativeType,
																						long pVolumeWidth,
																						long pVolumeHeight,
																						long pVolumeDepth,
																						double pVoxelWidth,
																						double pVoxelHeight,
																						double pVoxelDepth)
	{
		for (final VolumeCaptureListener lVolumeCaptureListener : mVolumeCaptureListenerList)
		{
			lVolumeCaptureListener.capturedVolume(pCaptureBuffer,
																						pNativeType,
																						pVolumeWidth,
																						pVolumeHeight,
																						pVolumeDepth,
																						pVoxelWidth,
																						pVoxelHeight,
																						pVoxelDepth);
		}
	}

	/**
	 * Removes a eye ray listener to this renderer.
	 *
	 * @param pEyeRayListener
	 *          eye ray listener
	 */
	@Override
	public void removeEyeRayListener(EyeRayListener pEyeRayListener)
	{
		mEyeRayListenerList.remove(pEyeRayListener);
	}

	@Override
	public void removeParameterChangeListener(ParameterChangeListener pParameterChangeListener)
	{
		mParameterChangeListenerList.remove(pParameterChangeListener);
	}

	@Override
	public void removeProcessor(final ProcessorInterface<?> pProcessor)
	{
		mProcessorInterfacesMap.remove(pProcessor.getName());
		if (pProcessor instanceof HasGUIPanel)
		{
			final HasGUIPanel lHasGUIPanel = (HasGUIPanel) pProcessor;
			final JPanel lPanel = lHasGUIPanel.getPanel();
			if (lPanel != null)
				mParametersListFrame.removePanel(lPanel);
		}
	}

	/**
	 * Removes a rotation controller.
	 *
	 * @param pRotationControllerInterface
	 *          rotation controller
	 */
	@Override
	public void removeRotationController(RotationControllerInterface pRotationControllerInterface)
	{
		mRotationControllerList.remove(pRotationControllerInterface);
	}

	/**
	 * Requests capture of the current displayed volume (Preferably of all layers
	 * but possibly just of the current layer.)
	 */
	@Override
	public void requestVolumeCapture()
	{
		mVolumeCaptureFlag = true;
		requestDisplay();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#resetBrightnessAndGammaAndTransferFunctionRanges()
	 */
	@Override
	public void resetBrightnessAndGammaAndTransferFunctionRanges()
	{
		getDisplayLock().lock();
		try
		{
			for (int i = 0; i < getNumberOfRenderLayers(); i++)
			{
				setBrightness(i, 1.0f);
				setGamma(i, 1.0f);
				setTransferFunctionRange(i, 0.0f, 1.0f);
			}
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}

	}

	/**
	 * Reset rotation and translation.
	 */
	@Override
	public void resetRotationTranslation()
	{
		getDisplayLock().lock();
		try
		{
			// TODO: reset camera position
			// mRotationQuaternion.setIdentity();
			// setTranslationX(0);
			// setTranslationY(0);
			// setDefaultTranslationZ();
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	@Override
	public void setAdaptiveLODActive(boolean pAdaptiveLOD)
	{
		if (mAdaptiveLODController != null)
			mAdaptiveLODController.setActive(pAdaptiveLOD);
	}

	/**
	 * Sets brightness.
	 *
	 * @param pBrightness
	 *          brightness level
	 */
	@Override
	public void setBrightness(final double pBrightness)
	{
		setBrightness(getCurrentRenderLayerIndex(), pBrightness);
	}

	/**
	 * Sets brightness for a given render layer index.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pBrightness
	 *          brightness level
	 */
	@Override
	public void setBrightness(final int pRenderLayerIndex,
														final double pBrightness)
	{
		getDisplayLock().lock();
		try
		{
			float lBrightness = (float) clamp(pBrightness,
																				0,
																				getNativeType() == NativeTypeEnum.UnsignedByte ? 16
																																											: 256);

			// TODO: set brightness
			// mBrightness[pRenderLayerIndex] = lBrightness;

		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}

	}

	/**
	 * Sets the clip box bounds ClipBox(xmin, xmax, ymin, ymax, zmin, zmax).
	 * 
	 * @param pRenderLayerIndex
	 * @param pClipBox
	 */
	@Override
	public void setClipBox(int pRenderLayerIndex, ClipBox pClipBox)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set clip box
			/*mVolumeClipBox = Arrays.copyOf(	pVolumeClipBox,
											pVolumeClipBox.length);/**/

		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * @param pRenderLayer
	 * @return
	 */
	@Override
	public ClipBox getClipBox(final int pRenderLayerIndex)
	{
		// TODO: get current clipbox for layer.
		return null;
	}

	/**
	 * Sets current render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          current render layer index
	 */
	@Override
	public void setCurrentRenderLayer(final int pRenderLayerIndex)
	{
		mCurrentRenderLayerIndex = pRenderLayerIndex;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void setDefaultTranslationZ()
	{
		setTranslationZ(-4 / getFOV());
	}

	/**
	 * Sets dithering value [0,1].
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pDithering
	 *          new dithering level for render layer
	 */
	@Override
	public void setDithering(int pRenderLayerIndex, double pDithering)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set dithering
			// mDithering[pRenderLayerIndex] = (float) pDithering;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void setFOV(double pFOV)
	{
		getDisplayLock().lock();
		try
		{
			final double lNewFOV = min(cMaximalFOV, max(cMinimalFOV, pFOV));
			// final double lFactor = mFOV / lNewFOV;
			/*System.out.format("old:%f new%f factor=%f \n",
												mFOV,
												lNewFOV,
												lFactor);/**/
			// TODO: set FOV
			// mFOV = (float) lNewFOV;

			// setTranslationZ(lFactor * getTranslationZ());
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets the gamma for the current render layer index.
	 *
	 * @param pGamma
	 *          gamma value
	 */
	@Override
	public void setGamma(final double pGamma)
	{
		setGamma(getCurrentRenderLayerIndex(), pGamma);
	}

	/**
	 * Sets the gamma for a given render layer index.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pGamma
	 *          gamma value
	 */
	@Override
	public void setGamma(	final int pRenderLayerIndex,
												final double pGamma)

	{
		getDisplayLock().lock();
		try
		{
			// TODO: set gamma
			// mGamma[pRenderLayerIndex] = (float) pGamma;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets active flag for the current render layer.
	 *
	 * @param pVisible
	 *          true to set layer visible, false to set it invisible
	 */
	@Override
	public void setLayerVisible(boolean pVisible)
	{
		setLayerVisible(getCurrentRenderLayerIndex(), pVisible);
	}

	/**
	 * Sets active flag for the given render layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pVisible
	 *          true to set layer visible, false to set it invisible
	 */
	@Override
	public void setLayerVisible(final int pRenderLayerIndex,
															final boolean pVisible)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set layer visibility
			// mLayerVisiblityFlagArray[pRenderLayerIndex] = pVisible;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets the native type of this renderer. This is _usually_ set at
	 * construction time and should not be modified later
	 *
	 * @param pNativeTypeEnum
	 *          native type
	 */
	@Override
	public void setNativeType(final NativeTypeEnum pNativeTypeEnum)

	{
		mNativeType = pNativeTypeEnum;
	}

	/**
	 * Returns current render layer.
	 * 
	 * @param pNumberOfRenderLayers
	 *          number of render layers
	 */
	@Override
	public void setNumberOfRenderLayers(final int pNumberOfRenderLayers)
	{
		mNumberOfRenderLayers = pNumberOfRenderLayers;
	}

	/**
	 * Sets the render quality [0,1] for a given render layer.
	 * 
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pQuality
	 *          new quality level for render layer
	 */
	@Override
	public void setQuality(int pRenderLayerIndex, double pQuality)
	{
		getDisplayLock().lock();
		try
		{
			pQuality = max(min(pQuality, 1), 0);
			// TODO: set render quality for layer
			// mQuality[pRenderLayerIndex] = (float) pQuality;

		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setRenderAlgorithm(int, clearvolume.renderer.RenderAlgorithm)
	 */
	@Override
	public void setRenderAlgorithm(	final int pRenderLayerIndex,
																	final RenderAlgorithm pRenderAlgorithm)
	{
		getDisplayLock().lock();
		try
		{
			mRenderAlgorithm[pRenderLayerIndex] = pRenderAlgorithm;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setRenderAlgorithm(int, clearvolume.renderer.RenderAlgorithm)
	 */
	@Override
	public void setRenderAlgorithm(final RenderAlgorithm pRenderAlgorithm)
	{
		getDisplayLock().lock();
		try
		{
			for (int i = 0; i < mRenderAlgorithm.length; i++)
				mRenderAlgorithm[i] = pRenderAlgorithm;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setTransferFunction(int,
	 *      clearvolume.transferf.TransferFunction)
	 */
	@Override
	public void setTransferFunction(final int pRenderLayerIndex,
																	final TransferFunction pTransfertFunction)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set transfer function
			// mTransferFunctions[pRenderLayerIndex] = pTransfertFunction;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setTransferFunction(clearvolume.transferf.TransferFunction)
	 */
	@Override
	public void setTransferFunction(final TransferFunction pTransfertFunction)
	{
		setTransferFunction(getCurrentRenderLayerIndex(),
												pTransfertFunction);
	}

	/**
	 * Sets the transfer function range min and max for the current render layer
	 * index.
	 *
	 * @param pTransferRangeMin
	 *          transfer range min
	 * @param pTransferRangeMax
	 *          transfer range max
	 */
	@Override
	public void setTransferFunctionRange(	final double pTransferRangeMin,
																				final double pTransferRangeMax)
	{
		setTransferFunctionRange(	getCurrentRenderLayerIndex(),
															pTransferRangeMin,
															pTransferRangeMax);
	}

	/**
	 * Sets the transfer function range min and max for a given render layer
	 * index.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransferRangeMin
	 *          transfer range min
	 * @param pTransferRangeMax
	 *          transfer range max
	 */
	@Override
	public void setTransferFunctionRange(	final int pRenderLayerIndex,
																				final double pTransferRangeMin,
																				final double pTransferRangeMax)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set transfer function
			/*mTransferFunctionRangeMin[pRenderLayerIndex] = (float) clamp(	pTransferRangeMin,
																			0,
																			1);
			mTransferFunctionRangeMax[pRenderLayerIndex] = (float) clamp(	pTransferRangeMax,
																			0,
																			1);/**/
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets transfer function range maximum, must be within [0,1].
	 *
	 * @param pTransferRangeMax
	 *          maximum
	 */
	@Override
	public void setTransferFunctionRangeMax(final double pTransferRangeMax)
	{
		setTransferFunctionRangeMax(getCurrentRenderLayerIndex(),
																pTransferRangeMax);
	}

	/**
	 * Sets transfer function range maximum, must be within [0,1].
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransferRangeMax
	 *          transfer range max
	 */
	@Override
	public void setTransferFunctionRangeMax(final int pRenderLayerIndex,
																					final double pTransferRangeMax)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set transfer function
			/*mTransferFunctionRangeMax[pRenderLayerIndex] = (float) clamp(	pTransferRangeMax,
																			0,
																			1);/**/
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets transfer range minimum, must be within [0,1].
	 *
	 * @param pTransferRangeMin
	 *          minimum
	 */
	@Override
	public void setTransferFunctionRangeMin(final double pTransferRangeMin)
	{
		setTransferFunctionRangeMin(getCurrentRenderLayerIndex(),
																pTransferRangeMin);
	}

	/**
	 * Sets transfer range minimum, must be within [0,1].
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransferRangeMin
	 *          transfer range min
	 */
	@Override
	public void setTransferFunctionRangeMin(final int pRenderLayerIndex,
																					final double pTransferRangeMin)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set transfer function
			/*mTransferFunctionRangeMin[pRenderLayerIndex] = (float) clamp(	pTransferRangeMin,
																			0,
																			1);/**/

		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationX()
	 */
	@Override
	public void setTranslationX(double pTranslationX)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set translation in scene graph
			// mTranslationX = (float) pTranslationX;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationY()
	 */
	@Override
	public void setTranslationY(double pTranslationY)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set translation in scene graph
			// mTranslationY = (float) pTranslationY;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void setTranslationZ(double pTranslationZ)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set translation in scene graph
			// mTranslationZ = (float) pTranslationZ;
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pWaitForCopy
	 *          set to true for waiting for data to be copied.
	 * @param pRenderLayerIndex
	 *          render pByteBuffer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * @param pVoxelSizeX
	 *          voxel dimension along X
	 * @param pVoxelSizeY
	 *          voxel dimension along Y
	 * @param pVoxelSizeZ
	 *          voxel dimension along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	boolean pWaitForCopy,
																			long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		// TODO: post the data to the corresponding volume node in the scene graph,
		// and wait for the data to be uploaded - or not (depending on flag and
		// timeout).

		return true;
	}

	/**
	 * Sets volume data buffer. Voxels are assumed to be isotropic.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          byte buffer
	 * @param pVolumeSizeX
	 *          size in voxels along X
	 * @param pVolumeSizeY
	 *          size in voxels along Y
	 * @param pVolumeSizeZ
	 *          size in voxels along Z
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																pByteBuffer,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          byte buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * @param pVoxelSizeX
	 *          voxel dimension along X
	 * @param pVoxelSizeY
	 *          voxel dimension along Y
	 * @param pVoxelSizeZ
	 *          voxel dimension along Z
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	cDefaultSetVolumeDataBufferTimeout,
																TimeUnit.SECONDS,
																pRenderLayerIndex,
																pByteBuffer,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pContiguousMemoryInterface
	 *          contguous buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final ContiguousMemoryInterface pContiguousMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																FragmentedMemory.wrap(pContiguousMemoryInterface),
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pContiguousMemoryInterface
	 *          contiguous buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * @param pVoxelSizeX
	 *          voxel dimension along X
	 * @param pVoxelSizeY
	 *          voxel dimension along Y
	 * @param pVoxelSizeZ
	 *          voxel dimension along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final ContiguousMemoryInterface pContiguousMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																FragmentedMemory.wrap(pContiguousMemoryInterface),
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																pFragmentedMemoryInterface,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render pByteBuffer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * @param pVoxelSizeX
	 *          voxel dimension along X
	 * @param pVoxelSizeY
	 *          voxel dimension along Y
	 * @param pVoxelSizeZ
	 *          voxel dimension along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	cDefaultSetVolumeDataBufferTimeout,
																TimeUnit.SECONDS,
																pRenderLayerIndex,
																pFragmentedMemoryInterface,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render pByteBuffer index
	 * @param pVolume
	 *          volume
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final Volume pVolume)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																pVolume.getDataBuffer(),
																pVolume.getWidthInVoxels(),
																pVolume.getHeightInVoxels(),
																pVolume.getDepthInVoxels(),
																pVolume.getVoxelWidthInRealUnits(),
																pVolume.getVoxelHeightInRealUnits(),
																pVolume.getVoxelDepthInRealUnits());
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pTimeOut
	 *          time out duration
	 * @param pTimeUnit
	 *          time unit for time out duration
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          byte buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ)
	{
		return setVolumeDataBuffer(	pTimeOut,
																pTimeUnit,
																pRenderLayerIndex,
																pByteBuffer,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render pByteBuffer index
	 * @param pByteBuffer
	 *          NIO byte buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * @param pVoxelSizeX
	 *          voxel dimension along X
	 * @param pVoxelSizeY
	 *          voxel dimension along Y
	 * @param pVoxelSizeZ
	 *          voxel dimension along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{

		FragmentedMemoryInterface lFragmentedMemoryInterface;
		if (!pByteBuffer.isDirect())
		{
			final OffHeapMemory lOffHeapMemory = new OffHeapMemory(pByteBuffer.capacity());
			lOffHeapMemory.copyFrom(pByteBuffer);
			lFragmentedMemoryInterface = FragmentedMemory.wrap(lOffHeapMemory);
		}
		else
		{
			final OffHeapMemory lOffHeapMemory = OffHeapMemory.wrapBuffer(pByteBuffer);
			lFragmentedMemoryInterface = FragmentedMemory.wrap(lOffHeapMemory);
		}

		return setVolumeDataBuffer(	pTimeOut,
																pTimeUnit,
																pRenderLayerIndex,
																lFragmentedMemoryInterface,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);

	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pTimeOut
	 *          time out duration
	 * @param pTimeUnit
	 *          time unit for time out duration
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pContiguousMemoryInterface
	 *          contiguous memory
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final ContiguousMemoryInterface pContiguousMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ)
	{
		return setVolumeDataBuffer(	pTimeOut,
																pTimeUnit,
																pRenderLayerIndex,
																FragmentedMemory.wrap(pContiguousMemoryInterface),
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pTimeOut
	 *          time out duration
	 * @param pTimeUnit
	 *          time unit for time out duration
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          byte buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ)
	{
		return setVolumeDataBuffer(	pTimeOut,
																pTimeUnit,
																pRenderLayerIndex,
																pFragmentedMemoryInterface,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																1,
																1,
																1);
	};

	/**
	 * Sets volume data buffer.
	 * 
	 * @param pRenderLayerIndex
	 *          render pByteBuffer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size in voxels along X
	 * @param pVolumeSizeY
	 *          volume size in voxels along Y
	 * @param pVolumeSizeZ
	 *          volume size in voxels along Z
	 * @param pVoxelSizeX
	 *          voxel dimension along X
	 * @param pVoxelSizeY
	 *          voxel dimension along Y
	 * @param pVoxelSizeZ
	 *          voxel dimension along Z
	 * 
	 * 
	 * @return true if transfer was completed (no time out)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	true,
																pTimeOut,
																pTimeUnit,
																pRenderLayerIndex,
																pFragmentedMemoryInterface,
																pVolumeSizeX,
																pVolumeSizeY,
																pVolumeSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	/**
	 * Sets the flag that allows/disallows the update of volume data. This is
	 * usefull when rendering multi-channel data.
	 */
	@Override
	public void setVolumeDataUpdateAllowed(boolean pVolumeDataUpdateAllowed)
	{
		mVolumeDataUpdateAllowed = pVolumeDataUpdateAllowed;
	}

	/**
	 * Sets the voxel size.
	 * 
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * 
	 */
	@Override
	public void setVoxelSize(	final double pVoxelSizeX,
														final double pVoxelSizeY,
														final double pVoxelSizeZ)
	{
		getDisplayLock().lock();
		try
		{
			for (int l = 0; l < getNumberOfRenderLayers(); l++)
			{
				// TODO: set voxel size in scene graph
			}
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Sets the voxel size for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * 
	 */
	@Override
	public void setVoxelSize(	final int pRenderLayerIndex,
														final double pVoxelSizeX,
														final double pVoxelSizeY,
														final double pVoxelSizeZ)
	{
		getDisplayLock().lock();
		try
		{
			// TODO: set voxel size in scene graph
		}
		finally
		{
			if (getDisplayLock().isHeldByCurrentThread())
				getDisplayLock().unlock();
		}
	}

	/**
	 * Toggle on/off the adaptive Level-Of-Detail engine
	 */
	@Override
	public void toggleAdaptiveLOD()
	{
		setAdaptiveLODActive(!getAdaptiveLODActive());
	}

	@Override
	public void toggleControlPanelDisplay()
	{
		if (mControlFrame != null)
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					mControlFrame.setVisible(!mControlFrame.isVisible());
				}
			});
	}

	@Override
	public void toggleParametersListFrame()
	{
		if (mParametersListFrame != null)
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					mParametersListFrame.setVisible(!mParametersListFrame.isVisible());
				}
			});
	}

	/**
	 * Waits until volume data copy completes all layers.
	 *
	 * @return true is completed, false if it timed-out.
	 */
	@Override
	public boolean waitToFinishAllDataBufferCopy(	final long pTimeOut,
																								final TimeUnit pTimeUnit)
	{
		boolean lNoTimeOut = true;
		for (int i = 0; i < getNumberOfRenderLayers(); i++)
			lNoTimeOut &= waitToFinishDataBufferCopy(	getCurrentRenderLayerIndex(),
																								pTimeOut,
																								pTimeUnit);

		return lNoTimeOut;
	}

	/**
	 * Waits until volume data copy completes for a given layer
	 *
	 * @return true is completed, false if it timed-out.
	 */
	@Override
	public boolean waitToFinishDataBufferCopy(final int pRenderLayerIndex,
																						final long pTimeOut,
																						final TimeUnit pTimeUnit)

	{
		// TODO: wait for buffer copy. (this must happen in the volume render
		// modules)
		return true;
	}

}
