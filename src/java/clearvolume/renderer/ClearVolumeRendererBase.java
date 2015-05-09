package clearvolume.renderer;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;

import clearvolume.ClearVolumeCloseable;
import clearvolume.controller.AutoRotationController;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.renderer.listeners.EyeRayListener;
import clearvolume.renderer.listeners.ParameterChangeListener;
import clearvolume.renderer.listeners.VolumeCaptureListener;
import clearvolume.renderer.processors.Processor;
import clearvolume.transferf.TransferFunction;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

import com.jogamp.opengl.math.Quaternion;

import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemory;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;
import coremem.util.Size;

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
	 * Auto rotation controller
	 */
	private final AutoRotationController mAutoRotationController;

	/**
	 * Transfer functions used
	 */
	private final TransferFunction[] mTransferFunctions;

	private volatile boolean[] mLayerVisiblityFlagArray;

	// geometric, brigthness an contrast settings.
	private final Quaternion mRotationQuaternion = new Quaternion();
	private volatile float mTranslationX = 0;
	private volatile float mTranslationY = 0;
	private volatile float mTranslationZ = 0;

	private volatile float mFOV = cDefaultFOV;

	// Render algorithm per layer:
	private final RenderAlgorithm[] mRenderAlgorithm;

	// render parameters per layer;
	private final float[] mBrightness;
	private final float[] mTransferFunctionRangeMin;
	private final float[] mTransferFunctionRangeMax;
	private final float[] mGamma;
	private final float[] mQuality;
	private final float[] mDithering;

	private volatile boolean mVolumeRenderingParametersChanged = true;

	// volume dimensions settings
	private volatile long mVolumeSizeX;
	private volatile long mVolumeSizeY;
	private volatile long mVolumeSizeZ;

	private volatile double mVoxelSizeX;
	private volatile double mVoxelSizeY;
	private volatile double mVoxelSizeZ;

	private volatile boolean mVolumeDimensionsChanged;

	// data copy locking and waiting
	private final Object[] mSetVolumeDataBufferLocks;
	private volatile FragmentedMemoryInterface[] mVolumeDataByteBuffers;
	private final CountDownLatch[] mDataBufferCopyIsFinishedArray;

	// Control frame:
	private ControlPanelJFrame mControlFrame;

	// Map of processors:
	protected Map<String, Processor<?>> mProcessorsMap = new ConcurrentHashMap<>();

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


	public ClearVolumeRendererBase(final int pNumberOfRenderLayers)
	{
		super();

		mNumberOfRenderLayers = pNumberOfRenderLayers;
		mSetVolumeDataBufferLocks = new Object[pNumberOfRenderLayers];
		mVolumeDataByteBuffers = new FragmentedMemoryInterface[pNumberOfRenderLayers];
		mDataBufferCopyIsFinishedArray = new CountDownLatch[pNumberOfRenderLayers];
		mTransferFunctions = new TransferFunction[pNumberOfRenderLayers];
		mLayerVisiblityFlagArray = new boolean[pNumberOfRenderLayers];
		mRenderAlgorithm = new RenderAlgorithm[pNumberOfRenderLayers];
		mBrightness = new float[pNumberOfRenderLayers];
		mTransferFunctionRangeMin = new float[pNumberOfRenderLayers];
		mTransferFunctionRangeMax = new float[pNumberOfRenderLayers];
		mGamma = new float[pNumberOfRenderLayers];
		mQuality = new float[pNumberOfRenderLayers];
		mDithering = new float[pNumberOfRenderLayers];

		for (int i = 0; i < pNumberOfRenderLayers; i++)
		{
			mSetVolumeDataBufferLocks[i] = new Object();
			mTransferFunctions[i] = TransferFunctions.getGradientForColor(i);
			mLayerVisiblityFlagArray[i] = true;
			mRenderAlgorithm[i] = RenderAlgorithm.MaxProjection;
			mBrightness[i] = 1;
			mTransferFunctionRangeMin[i] = 0f;
			mTransferFunctionRangeMax[i] = 1f;
			mGamma[i] = 1f;
			mQuality[i] = 1f;
			mDithering[i] = 1f;
		}

		if (pNumberOfRenderLayers == 1)
			mTransferFunctions[0] = TransferFunctions.getDefault();

		mAdaptiveLODController = new AdaptiveLODController();

		mAutoRotationController = new AutoRotationController();
		mRotationControllerList.add(mAutoRotationController);

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
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getNativeType()
	 */
	@Override
	public NativeTypeEnum getNativeType()
	{
		return mNativeType;
	}

	/**
	 * Returns the number of bytes per voxel for this renderer.
	 * 
	 * @return
	 */
	@Override
	public long getBytesPerVoxel()
	{
		return Size.of(mNativeType);
	}

	/**
	 * Returns the state of the flag indicating whether current/new rendering
	 * parameters have been used for last rendering.
	 *
	 * @return true if rendering parameters up-to-date.
	 */
	public boolean haveVolumeRenderingParametersChanged()
	{
		return mVolumeRenderingParametersChanged;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#notifyChangeOfVolumeRenderingParameters()
	 */
	@Override
	public void notifyChangeOfVolumeRenderingParameters()
	{
		for (final ParameterChangeListener lParameterChangeListener : mParameterChangeListenerList)
		{
			lParameterChangeListener.notifyParameterChange(this);
		}
		
		mVolumeRenderingParametersChanged = true;
		getAdaptiveLODController().notifyUserInteractionInProgress();
	}

	@Override
	public void addParameterChangeListener(ParameterChangeListener pParameterChangeListener)
	{
		mParameterChangeListenerList.add(pParameterChangeListener);
	}

	@Override
	public void removeParameterChangeListener(ParameterChangeListener pParameterChangeListener)
	{
		mParameterChangeListenerList.remove(pParameterChangeListener);
	}

	/**
	 * Clears the state of the update-volume-parameters flag
	 */
	public void clearChangeOfVolumeParametersFlag()
	{
		mVolumeRenderingParametersChanged = false;
	}

	/**
	 * Returns the volume size along x axis.
	 *
	 * @return volume size along x
	 */
	public long getVolumeSizeX()
	{
		return mVolumeSizeX;
	}

	/**
	 * Returns the volume size along y axis.
	 *
	 * @return volume size along y
	 */
	public long getVolumeSizeY()
	{
		return mVolumeSizeY;
	}

	/**
	 * Returns the volume size along z axis.
	 *
	 * @return volume size along z
	 */
	public long getVolumeSizeZ()
	{
		return mVolumeSizeZ;
	}

	public double getVoxelSizeX()
	{
		return mVoxelSizeX;
	}

	public double getVoxelSizeY()
	{
		return mVoxelSizeY;
	}

	public double getVoxelSizeZ()
	{
		return mVoxelSizeZ;
	}

	/**
	 * Returns whether the volume dimensions have been changed since last data
	 * upload.
	 *
	 * @return true if volume dimensions changed.
	 */
	public boolean haveVolumeDimensionsChanged()
	{
		return mVolumeDimensionsChanged;
	}

	/**
	 *
	 */
	public void clearVolumeDimensionsChanged()
	{
		mVolumeDimensionsChanged = false;
	}

	/**
	 * Gets active flag for the current render layer.
	 *
	 * @return
	 */
	@Override
	public boolean isLayerVisible()
	{
		return isLayerVisible(getCurrentRenderLayerIndex());
	}

	/**
	 * Gets active flag for the given render layer.
	 *
	 * @return
	 */
	@Override
	public boolean isLayerVisible(final int pRenderLayerIndex)
	{
		return mLayerVisiblityFlagArray[pRenderLayerIndex];
	}

	/**
	 * Sets active flag for the current render layer.
	 *
	 * @param pVisble
	 */
	@Override
	public void setLayerVisible(boolean pVisble)
	{
		setLayerVisible(getCurrentRenderLayerIndex(), pVisble);
	}

	/**
	 * Sets active flag for the given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pVisble
	 */
	@Override
	public void setLayerVisible(final int pRenderLayerIndex,
															final boolean pVisble)
	{
		mLayerVisiblityFlagArray[pRenderLayerIndex] = pVisble;
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#resetBrightnessAndGammaAndTransferFunctionRanges()
	 */
	@Override
	public void resetBrightnessAndGammaAndTransferFunctionRanges()
	{
		for (int i = 0; i < getNumberOfRenderLayers(); i++)
		{
			mBrightness[i] = 1.0f;
			mGamma[i] = 1.0f;
			mTransferFunctionRangeMin[i] = 0.0f;
			mTransferFunctionRangeMax[i] = 1.0f;
		}
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Adds to the brightness of the image
	 *
	 * @param pBrightnessDelta
	 */
	@Override
	public void addBrightness(final double pBrightnessDelta)
	{
		addBrightness(getCurrentRenderLayerIndex(), pBrightnessDelta);

	}

	/**
	 * Adds to the brightness of the image for a given render layer index
	 *
	 * @param pRenderLayer
	 * @param pBrightnessDelta
	 */
	@Override
	public void addBrightness(final int pRenderLayerIndex,
														final double pBrightnessDelta)
	{
		setBrightness(pRenderLayerIndex,
									getBrightness() + pBrightnessDelta);
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
	 * @return brightness level.
	 */
	@Override
	public double getBrightness(final int pRenderLayerIndex)
	{
		return mBrightness[pRenderLayerIndex];
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
	 * @param pBrightness
	 *          brightness level
	 */
	@Override
	public void setBrightness(final int pRenderLayerIndex,
														final double pBrightness)
	{
		mBrightness[pRenderLayerIndex] = (float) clamp(	pBrightness,
																										0,
																										getNativeType() == NativeTypeEnum.UnsignedByte ? 16
																																																	: 256);

		notifyChangeOfVolumeRenderingParameters();
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
	 * Returns the Gamma size.
	 *
	 * @param pRenderLayerIndex
	 * @return
	 */
	@Override
	public double getGamma(final int pRenderLayerIndex)
	{
		return mGamma[pRenderLayerIndex];
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setGamma(double)
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
	 * @param pGamma
	 */
	@Override
	public void setGamma(	final int pRenderLayerIndex,
												final double pGamma)

	{
		mGamma[pRenderLayerIndex] = (float) pGamma;
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * @param pRenderLayerIndex
	 * @param pDithering
	 *          new dithering level for render layer
	 */
	@Override
	public void setDithering(int pRenderLayerIndex, double pDithering)
	{
		mDithering[pRenderLayerIndex] = (float) pDithering;
		notifyChangeOfVolumeRenderingParameters();
	};

	/**
	 * Returns samount of dithering [0,1] for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 * @return dithering
	 */
	@Override
	public float getDithering(int pRenderLayerIndex)
	{
		return mDithering[pRenderLayerIndex];
	};

	/**
	 * Sets the amount of dithering [0,1] for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 * 
	 * @param pRenderLayerIndex
	 * @param pQuality
	 *          new quality level for render layer
	 */
	@Override
	public void setQuality(int pRenderLayerIndex, double pQuality)
	{
		pQuality = max(min(pQuality, 1), 0);
		mQuality[pRenderLayerIndex] = (float) pQuality;
		notifyChangeOfVolumeRenderingParameters();
	};

	/**
	 * Returns the quality level [0,1] for a given render layer.
	 * 
	 * @param pRenderLayerIndex
	 * @return quality level
	 */
	@Override
	public float getQuality(int pRenderLayerIndex)
	{
		return mQuality[pRenderLayerIndex];
	};

	/**
	 * Returns the maximal number of steps during ray casting for a given layer.
	 * This size depends on the volume dimension and quality.
	 * 
	 * @param pRenderLayerIndex
	 * @return maximal number of steps
	 */
	public int getMaxSteps(final int pRenderLayerIndex)
	{
		return (int) (sqrt(getVolumeSizeX() * getVolumeSizeX()
												+ getVolumeSizeY()
												* getVolumeSizeY()
												+ getVolumeSizeZ()
												* getVolumeSizeZ()) * getQuality(pRenderLayerIndex));
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
	 * @return
	 */
	@Override
	public double getTransferRangeMin(final int pRenderLayerIndex)
	{
		return mTransferFunctionRangeMin[pRenderLayerIndex];
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
	 * @return
	 */
	@Override
	public double getTransferRangeMax(final int pRenderLayerIndex)
	{
		return mTransferFunctionRangeMax[pRenderLayerIndex];
	}

	/**
	 * Sets the transfer function range min and max for the current render layer
	 * index.
	 *
	 * @param pTransferRangeMin
	 * @param pTransferRangeMax
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
	 * @param pTransferRangeMin
	 * @param pTransferRangeMax
	 */
	@Override
	public void setTransferFunctionRange(	final int pRenderLayerIndex,
																				final double pTransferRangeMin,
																				final double pTransferRangeMax)
	{
		mTransferFunctionRangeMin[pRenderLayerIndex] = (float) clamp(	pTransferRangeMin,
																																	0,
																																	1);
		mTransferFunctionRangeMax[pRenderLayerIndex] = (float) clamp(	pTransferRangeMax,
																																	0,
																																	1);
		notifyChangeOfVolumeRenderingParameters();
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
	 * @param pTransferRangeMin
	 */
	@Override
	public void setTransferFunctionRangeMin(final int pRenderLayerIndex,
																					final double pTransferRangeMin)
	{
		mTransferFunctionRangeMin[pRenderLayerIndex] = (float) clamp(	pTransferRangeMin,
																																	0,
																																	1);

		notifyChangeOfVolumeRenderingParameters();
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
	 * @param pTransferRangeMax
	 */
	@Override
	public void setTransferFunctionRangeMax(final int pRenderLayerIndex,
																					final double pTransferRangeMax)
	{
		mTransferFunctionRangeMax[pRenderLayerIndex] = (float) clamp(	pTransferRangeMax,
																																	0,
																																	1);
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Translates the minimum of the transfer function range.
	 *
	 * @param pDelta
	 *          translation amount
	 */
	@Override
	public void addTransferFunctionRangeMin(final double pDelta)
	{
		setTransferFunctionRangeMin(getCurrentRenderLayerIndex(), pDelta);
	}

	/**
	 * Translates the minimum of the transfer function range.
	 *
	 * @param pRenderLayerIndex
	 * @param pDelta
	 */
	@Override
	public void addTransferFunctionRangeMin(final int pRenderLayerIndex,
																					final double pDelta)
	{
		setTransferFunctionRangeMin(getTransferRangeMin(pRenderLayerIndex) + pDelta);
	}

	/**
	 * Translates the maximum of the transfer function range.
	 *
	 * @param pDelta
	 *          translation amount
	 */
	@Override
	public void addTransferFunctionRangeMax(final double pDelta)
	{
		addTransferFunctionRangeMax(getCurrentRenderLayerIndex(), pDelta);
	}

	/**
	 * Translates the maximum of the transfer function range.
	 *
	 * @param pRenderLayerIndex
	 * @param pDelta
	 */
	@Override
	public void addTransferFunctionRangeMax(final int pRenderLayerIndex,
																					final double pDelta)
	{
		setTransferFunctionRangeMax(pRenderLayerIndex,
																getTransferRangeMax(pRenderLayerIndex) + pDelta);
	}

	/**
	 * Translates the transfer function range by a given amount.
	 *
	 * @param pTransferRangePositionDelta
	 *          amount of translation added
	 */
	@Override
	public void addTransferFunctionRangePosition(final double pTransferRangePositionDelta)
	{
		addTransferFunctionRangeMin(pTransferRangePositionDelta);
		addTransferFunctionRangeMax(pTransferRangePositionDelta);
	}

	/**
	 * Adds a certain amount (possibly negative) to the width of the transfer
	 * function range.
	 *
	 * @param pTransferRangeWidthDelta
	 *          size added to the width
	 */
	@Override
	public void addTransferFunctionRangeWidth(final double pTransferRangeWidthDelta)
	{
		addTransferFunctionRangeMin(-pTransferRangeWidthDelta);
		addTransferFunctionRangeMax(pTransferRangeWidthDelta);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationX(double)
	 */
	@Override
	public void addTranslationX(final double pDX)
	{
		mTranslationX += pDX;
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationY(double)
	 */
	@Override
	public void addTranslationY(final double pDY)
	{
		mTranslationY += pDY;
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationZ(double)
	 */
	@Override
	public void addTranslationZ(final double pDZ)
	{
		mTranslationZ += pDZ;
		notifyChangeOfVolumeRenderingParameters();
	}

	@Override
	public Quaternion getQuaternion()
	{
		return mRotationQuaternion;
	}

	@Override
	public void setQuaternion(Quaternion pQuaternion)
	{
		mRotationQuaternion.set(pQuaternion);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationX()
	 */
	@Override
	public void setTranslationX(double pTranslationX)
	{
		mTranslationX = (float) pTranslationX;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationY()
	 */
	@Override
	public void setTranslationY(double pTranslationY)
	{
		mTranslationY = (float) pTranslationY;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void setTranslationZ(double pTranslationZ)
	{
		mTranslationZ = (float) pTranslationZ;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void setDefaultTranslationZ()
	{
		mTranslationZ = -4 / getFOV();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationX()
	 */
	@Override
	public float getTranslationX()
	{
		return mTranslationX;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationY()
	 */
	@Override
	public float getTranslationY()
	{
		return mTranslationY;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public float getTranslationZ()
	{
		return mTranslationZ;
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
			final double lFactor = mFOV / lNewFOV;
			/*System.out.format("old:%f new%f factor=%f \n",
												mFOV,
												lNewFOV,
												lFactor);/**/
			mFOV = (float) lNewFOV;
			setTranslationZ(lFactor * getTranslationZ());
			notifyChangeOfVolumeRenderingParameters();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
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
	public float getFOV()
	{
		return mFOV;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTranslationZ()
	 */
	@Override
	public void addFOV(double pDelta)
	{
		setFOV(mFOV + pDelta);
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
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setTransferFunction(int,
	 *      clearvolume.transferf.TransferFunction)
	 */
	@Override
	public void setTransferFunction(final int pRenderLayerIndex,
																	final TransferFunction pTransfertFunction)
	{
		mTransferFunctions[pRenderLayerIndex] = pTransfertFunction;
	}

	/**
	 * Interface method implementation
	 *
	 * @return
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getTransferFunction(int)
	 */
	@Override
	public TransferFunction getTransferFunction(final int pRenderLayerIndex)
	{
		return mTransferFunctions[pRenderLayerIndex];
	}

	/**
	 * Returns currently used transfer function.
	 *
	 * @return currently used transfer function
	 */
	@Override
	public TransferFunction getTransferFunction()
	{
		return mTransferFunctions[getCurrentRenderLayerIndex()];
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
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setRenderAlgorithm(int, clearvolume.renderer.RenderAlgorithm)
	 */
	@Override
	public void setRenderAlgorithm(	final int pRenderLayerIndex,
																	final RenderAlgorithm pRenderAlgorithm)
	{
		mRenderAlgorithm[pRenderLayerIndex] = pRenderAlgorithm;
		notifyChangeOfVolumeRenderingParameters();
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setRenderAlgorithm(int, clearvolume.renderer.RenderAlgorithm)
	 */
	@Override
	public void setRenderAlgorithm(final RenderAlgorithm pRenderAlgorithm)
	{
		for (int i = 0; i < mRenderAlgorithm.length; i++)
			mRenderAlgorithm[i] = pRenderAlgorithm;
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Cycles through rendering algorithms for all layers
	 */
	@Override
	public void cycleRenderAlgorithm()
	{
		int i = 0;
		for (final RenderAlgorithm lRenderAlgorithm : mRenderAlgorithm)
			mRenderAlgorithm[i++] = lRenderAlgorithm.next();
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Cycles through rendering algorithms for all layers
	 */
	@Override
	public void cycleRenderAlgorithm(int pRenderLayerIndex)
	{
		mRenderAlgorithm[pRenderLayerIndex] = mRenderAlgorithm[pRenderLayerIndex].next();
		notifyChangeOfVolumeRenderingParameters();
	}

	/**
	 * Returns for a given index the corresponding volume data buffer.
	 *
	 * @return data buffer for a given render layer.
	 */
	public boolean isNewVolumeDataAvailable()
	{
		for (final FragmentedMemoryInterface lFragmentedMemoryInterface : mVolumeDataByteBuffers)
			if (lFragmentedMemoryInterface != null)
				return true;
		return false;
	}

	/**
	 * Returns for a given index the corresponding volume data buffer.
	 *
	 * @return data buffer for a given render layer.
	 */
	public FragmentedMemoryInterface getVolumeDataBuffer(final int pVolumeDataBufferIndex)
	{
		return mVolumeDataByteBuffers[pVolumeDataBufferIndex];
	}

	/**
	 * Clears volume data buffer.
	 *
	 */
	public void clearVolumeDataBufferReference(final int pVolumeDataBufferIndex)
	{
		mVolumeDataByteBuffers[pVolumeDataBufferIndex] = null;
	}

	/**
	 * Returns object used for locking volume data copy for a given layer.
	 *
	 * @param pRenderLayerIndex
	 *
	 * @return locking object
	 */
	public Object getSetVolumeDataBufferLock(final int pRenderLayerIndex)
	{
		return mSetVolumeDataBufferLocks[pRenderLayerIndex];
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#resetRotationTranslation()
	 */
	@Override
	public void resetRotationTranslation()
	{
		mRotationQuaternion.setIdentity();
		mTranslationX = 0;
		mTranslationY = 0;
		setDefaultTranslationZ();
	}

	@Override
	public void setCurrentRenderLayer(final int pLayerIndex)
	{
		mCurrentRenderLayerIndex = pLayerIndex;
	}

	@Override
	public int getCurrentRenderLayerIndex()
	{
		return mCurrentRenderLayerIndex;
	}

	@Override
	public void setNumberOfRenderLayers(final int pNumberOfRenderLayers)
	{
		mNumberOfRenderLayers = pNumberOfRenderLayers;
	}

	@Override
	public int getNumberOfRenderLayers()
	{
		return mNumberOfRenderLayers;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVoxelSize(double,
	 *      double, double)
	 */
	@Override
	public void setVoxelSize(	final double pVoxelSizeX,
														final double pVoxelSizeY,
														final double pVoxelSizeZ)
	{
		mVoxelSizeX = pVoxelSizeX;
		mVoxelSizeY = pVoxelSizeY;
		mVoxelSizeZ = pVoxelSizeZ;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																pByteBuffer,
																pSizeX,
																pSizeY,
																pSizeZ,
																1,
																1,
																1);
	}

	@Override
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
																			long pSizeX,
																			long pSizeY,
																			long pSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	cDefaultSetVolumeDataBufferTimeout,
																TimeUnit.SECONDS,
																pRenderLayerIndex,
																pByteBuffer,
																pSizeX,
																pSizeY,
																pSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ)
	{
		return setVolumeDataBuffer(	pTimeOut,
																pTimeUnit,
																pRenderLayerIndex,
																pByteBuffer,
																pSizeX,
																pSizeY,
																pSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																pFragmentedMemoryInterface,
																pSizeX,
																pSizeY,
																pSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final ContiguousMemoryInterface pContiguousMemoryInterface,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																FragmentedMemory.wrap(pContiguousMemoryInterface),
																pSizeX,
																pSizeY,
																pSizeZ,
																1,
																1,
																1);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long, double, double, double)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final ContiguousMemoryInterface pContiguousMemoryInterface,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	pRenderLayerIndex,
																FragmentedMemory.wrap(pContiguousMemoryInterface),
																pSizeX,
																pSizeY,
																pSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long, double, double, double)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ,
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
																pSizeX,
																pSizeY,
																pSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);

	}

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
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long, double, double, double)
	 */
	@Override
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		return setVolumeDataBuffer(	cDefaultSetVolumeDataBufferTimeout,
																TimeUnit.SECONDS,
																pRenderLayerIndex,
																pFragmentedMemoryInterface,
																pSizeX,
																pSizeY,
																pSizeZ,
																pVoxelSizeX,
																pVoxelSizeY,
																pVoxelSizeZ);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long, double, double, double)
	 */
	@Override
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final FragmentedMemoryInterface pFragmentedMemoryInterface,
																			final long pSizeX,
																			final long pSizeY,
																			final long pSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ)
	{
		synchronized (getSetVolumeDataBufferLock(pRenderLayerIndex))
		{

			if (mVolumeSizeX != pSizeX || mVolumeSizeY != pSizeY
					|| mVolumeSizeZ != pSizeZ)
			{
				mVolumeDimensionsChanged = true;
			}

			mVolumeSizeX = pSizeX;
			mVolumeSizeY = pSizeY;
			mVolumeSizeZ = pSizeZ;

			mVoxelSizeX = pVoxelSizeX;
			mVoxelSizeY = pVoxelSizeY;
			mVoxelSizeZ = pVoxelSizeZ;

			clearCompletionOfDataBufferCopy(pRenderLayerIndex);
			mVolumeDataByteBuffers[pRenderLayerIndex] = pFragmentedMemoryInterface;

			notifyChangeOfVolumeRenderingParameters();
		}

		// System.out.print("Waiting...");
		final boolean lWaitResult = waitToFinishDataBufferCopy(	pRenderLayerIndex,
																														pTimeOut,
																														pTimeUnit);
		// if (!lWaitResult)
		// System.err.println("TIMEOUT!");
		// System.out.println(" finished!");

		return lWaitResult;
	}

	@Override
	public VolumeManager createCompatibleVolumeManager(final int pMaxAvailableVolumes)
	{
		return new VolumeManager(pMaxAvailableVolumes);
	}

	/**
	 * Notifies the volume data copy completion.
	 */
	protected void notifyCompletionOfDataBufferCopy(final int pRenderLayerIndex)
	{
		mDataBufferCopyIsFinishedArray[pRenderLayerIndex].countDown();
	}

	/**
	 * Clears data copy buffer flag.
	 */
	protected void clearCompletionOfDataBufferCopy(final int pRenderLayerIndex)
	{
		mDataBufferCopyIsFinishedArray[pRenderLayerIndex] = new CountDownLatch(1);
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
		try
		{
			// final long lStartNs = System.nanoTime();
			if (mDataBufferCopyIsFinishedArray[pRenderLayerIndex] == null)
				return true;
			final boolean lAwaitResult = mDataBufferCopyIsFinishedArray[pRenderLayerIndex].await(	pTimeOut,
																																														pTimeUnit);
			// final long lStopNs = System.nanoTime();
			// System.out.println("ELPASED:" + (lStopNs - lStartNs) / 1.0e6);
			return lAwaitResult;
		}
		catch (final InterruptedException e)
		{
			return waitToFinishDataBufferCopy(pRenderLayerIndex,
																				pTimeOut,
																				pTimeUnit);
		}
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
	 * Returns the current list of rotation controllers.
	 *
	 * @return currently used rotation controller.
	 */
	@Override
	public ArrayList<RotationControllerInterface> getRotationControllers()
	{
		return mRotationControllerList;
	}

	/**
	 * Toggles the display of the Control Frame;
	 */
	@Override
	public void toggleControlPanelDisplay()
	{
		if (mControlFrame != null)
			mControlFrame.setVisible(!mControlFrame.isVisible());
	}

	/**
	 * Toggles the display of the Control Frame;
	 */
	@Override
	public void addProcessor(final Processor<?> pProcessor)
	{
		mProcessorsMap.put(pProcessor.getName(), pProcessor);
	}

	/**
	 * Toggles the display of the Control Frame;
	 */
	@Override
	public void addProcessors(final Collection<Processor<?>> pProcessors)
	{
		for (final Processor<?> lProcessor : pProcessors)
			addProcessor(lProcessor);
	}

	@Override
	public Collection<Processor<?>> getProcessors()
	{
		return mProcessorsMap.values();
	}

	/**
	 * Toggles the display of the Control Frame;
	 */
	@Override
	public void addVolumeCaptureListener(final VolumeCaptureListener pVolumeCaptureListener)
	{
		if (pVolumeCaptureListener != null)
			mVolumeCaptureListenerList.add(pVolumeCaptureListener);
	}

	public void notifyVolumeCaptureListeners(	ByteBuffer[] pCaptureBuffer,
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
	 * Requests capture of the current displayed volume (Preferably of all layers
	 * but possibly just of the current layer.)
	 */
	@Override
	public void requestVolumeCapture()
	{
		mVolumeCaptureFlag = true;
		requestDisplay();
	};

	@Override
	public void setAdaptiveLODActive(boolean pAdaptiveLOD)
	{
		if (mAdaptiveLODController != null)
			mAdaptiveLODController.setActive(pAdaptiveLOD);
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
	 * Toggle on/off the adaptive Level-Of-Detail engine
	 */
	@Override
	public void toggleAdaptiveLOD()
	{
		setAdaptiveLODActive(!getAdaptiveLODActive());
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
	public ReentrantLock getDisplayLock()
	{
		return mDisplayReentrantLock;
	}

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

}
