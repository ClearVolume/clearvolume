package clearvolume.renderer;

import static java.lang.Math.max;

import java.awt.BorderLayout;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.swing.JFrame;
import javax.swing.JPanel;

import clearvolume.controller.RotationControllerInterface;
import clearvolume.projections.ProjectionAlgorithm;
import clearvolume.transferf.TransferFunction;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

/**
 * Class ClearVolumeRendererBase
 * 
 * Abstract class providing basic functionality for classes implementing the
 * ClearVolumeRendere interface
 *
 * @author Loic Royer 2014
 *
 */
/**
 * Class ClearVolumeRendererBase 
 * 
 * Instances of this class ...
 *
 * @author Loic Royer
 * 2014
 *
 */
/**
 * Class ClearVolumeRendererBase 
 * 
 * Instances of this class ...
 *
 * @author Loic Royer
 * 2014
 *
 */
/**
 * Class ClearVolumeRendererBase
 * 
 * Instances of this class ...
 *
 * @author Loic Royer 2014
 *
 */
public abstract class ClearVolumeRendererBase	implements
																							ClearVolumeRendererInterface
{

	/**
	 * Number of render layers.
	 */
	private int mNumberOfRenderLayers;
	private volatile int mCurrentRenderLayerIndex = 0;

	/**
	 * Number of bytes per voxel used by this renderer
	 */
	private volatile int mBytesPerVoxel = 1;

	/**
	 * Rotation controller in addition to the mouse
	 */
	private RotationControllerInterface mRotationController;

	/**
	 * Projection algorithm used
	 */
	private ProjectionAlgorithm mProjectionAlgorithm = ProjectionAlgorithm.MaxProjection;

	/**
	 * Transfer functions used
	 */
	private TransferFunction[] mTransferFunctions;

	// geometric, brigthness an contrast settings.
	private volatile float mTranslationX = 0;
	private volatile float mTranslationY = 0;
	private volatile float mTranslationZ = 0;
	private volatile float mRotationX = 0;
	private volatile float mRotationY = 0;
	private volatile float mScaleX = 1.0f;
	private volatile float mScaleY = 1.0f;
	private volatile float mScaleZ = 1.0f;
	// private volatile float mDensity;
	private volatile float mBrightness = 1;
	private volatile float mTransferFunctionRangeMin = 0;
	private volatile float mTransferFunctionRangeMax = 1;
	private volatile float mGamma = 1;
	private volatile boolean mUpdateVolumeRenderingParameters = true;

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
	private volatile ByteBuffer[] mVolumeDataByteBuffers;
	private AtomicIntegerArray mDataBufferCopyIsFinished;

	// Control frame:
	private JFrame mControlFrame;

	public ClearVolumeRendererBase(final int pNumberOfRenderLayers)
	{
		super();
		mNumberOfRenderLayers = pNumberOfRenderLayers;
		mSetVolumeDataBufferLocks = new Object[pNumberOfRenderLayers];
		mVolumeDataByteBuffers = new ByteBuffer[pNumberOfRenderLayers];
		mDataBufferCopyIsFinished = new AtomicIntegerArray(pNumberOfRenderLayers);
		mTransferFunctions = new TransferFunction[pNumberOfRenderLayers];
		for (int i = 0; i < pNumberOfRenderLayers; i++)
		{
			mSetVolumeDataBufferLocks[i] = new Object();
			mDataBufferCopyIsFinished.set(i, 0);
			mTransferFunctions[i] = TransferFunctions.getGradientForColor(i);
		}

	}

	/**
	 * Sets the number of bytes per voxel for this renderer. This is _usually_ set
	 * at construction time and should not be modified later
	 * 
	 * @param pBytesPerVoxel
	 *          bytes-per-voxel
	 */
	public void setBytesPerVoxel(int pBytesPerVoxel)
	{
		mBytesPerVoxel = pBytesPerVoxel;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getBytesPerVoxel()
	 */
	@Override
	public int getBytesPerVoxel()
	{
		return mBytesPerVoxel;
	}

	/**
	 * Returns the state of the flag indicating whether current/new rendering
	 * parameters have been used for last rendering.
	 * 
	 * @return true if rednering parameters up-to-date.
	 */
	public boolean getIsUpdateVolumeRenderingParameters()
	{
		return mUpdateVolumeRenderingParameters;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#notifyUpdateOfVolumeRenderingParameters()
	 */
	@Override
	public void notifyUpdateOfVolumeRenderingParameters()
	{
		mUpdateVolumeRenderingParameters = true;
	}

	/**
	 * Clears the state of the update-volume-parameters flag
	 */
	public void clearIsUpdateVolumeParameters()
	{
		mUpdateVolumeRenderingParameters = false;
	}

	/**
	 * Sets the volume size in 'real' units of the volume (um, cm, ...) The apsect
	 * ratio for the volume is set accordingly.
	 * 
	 * @param pVolumeSizeX
	 * @param pVolumeSizeY
	 * @param pVolumeSizeZ
	 */
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
	 * Sets the scale factor for the volume along the x axis.
	 * 
	 * @param pScaleX
	 *          scale factor along x
	 */
	public void setScaleX(final double pScaleX)
	{
		mScaleX = (float) pScaleX;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Sets the scale factor for the volume along the y axis.
	 * 
	 * @param pScaleY
	 *          scale factor along y
	 */
	public void setScaleY(final double pScaleY)
	{
		mScaleY = (float) pScaleY;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Sets the scale factor for the volume along the z axis.
	 * 
	 * @param pScaleZ
	 *          scale factor along z
	 */
	public void setScaleZ(final double pScaleZ)
	{
		mScaleZ = (float) pScaleZ;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#resetBrightnessAndGammaAndTransferFunctionRanges()
	 */
	@Override
	public void resetBrightnessAndGammaAndTransferFunctionRanges()
	{
		mBrightness = 1.0f;
		mGamma = 1f;
		mTransferFunctionRangeMin = 0.0f;
		mTransferFunctionRangeMax = 1.0f;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Adds to the brightness of the image
	 * 
	 * @param pBrightnessDelta
	 */
	public void addBrightness(final double pBrightnessDelta)
	{
		setBrightness(mBrightness + pBrightnessDelta);
	}

	/**
	 * Returns the brightness level.
	 * 
	 * @return brightness level.
	 */
	public double getBrightness()
	{
		return mBrightness;
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
		mBrightness = (float) clamp(pBrightness,
																0,
																getBytesPerVoxel() == 1 ? 16 : 256);
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Returns the Gamma value.
	 * 
	 * @return gamma value
	 */
	public double getGamma()
	{
		return mGamma;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setGamma(double)
	 */
	@Override
	public void setGamma(final double pGamma)
	{
		mGamma = (float) pGamma;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Returns the minimum of the transfer function range.
	 * 
	 * @return minimum
	 */
	public double getTransferRangeMin()
	{
		return mTransferFunctionRangeMin;
	}

	/**
	 * Returns the maximum of the transfer function range.
	 * 
	 * @return minimum
	 */
	public double getTransferRangeMax()
	{
		return mTransferFunctionRangeMax;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setTransferFunctionRange(double,
	 *      double)
	 */
	@Override
	public void setTransferFunctionRange(	final double pTransferRangeMin,
																				final double pTransferRangeMax)
	{
		mTransferFunctionRangeMin = (float) clamp(pTransferRangeMin, 0, 1);
		mTransferFunctionRangeMax = (float) clamp(pTransferRangeMax, 0, 1);
		notifyUpdateOfVolumeRenderingParameters();
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
		mTransferFunctionRangeMin = (float) clamp(pTransferRangeMin, 0, 1);
		notifyUpdateOfVolumeRenderingParameters();
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
		mTransferFunctionRangeMax = (float) clamp(pTransferRangeMax, 0, 1);
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Translates the transfer function range by a given amount.
	 * 
	 * @param pTransferRangePositionDelta
	 *          amount of translation added
	 */
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
	 *          value added to the width
	 */
	public void addTransferFunctionRangeWidth(final double pTransferRangeWidthDelta)
	{
		addTransferFunctionRangeMin(-pTransferRangeWidthDelta);
		addTransferFunctionRangeMax(pTransferRangeWidthDelta);
	}

	/**
	 * Translates the minimum of the transfer function range.
	 * 
	 * @param pDelta
	 *          translation amount
	 */
	public void addTransferFunctionRangeMin(final double pDelta)
	{
		setTransferFunctionRangeMin(mTransferFunctionRangeMin + pDelta);
	}

	/**
	 * Translates the maximum of the transfer function range.
	 * 
	 * @param pDelta
	 *          translation amount
	 */
	public void addTransferFunctionRangeMax(final double pDelta)
	{
		setTransferFunctionRangeMax(mTransferFunctionRangeMax + pDelta);
	}


		/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationX(double)
	 */
	@Override
	public void addTranslationX(double pDX)
	{
		mTranslationX += pDX;
		notifyUpdateOfVolumeRenderingParameters();
	}


		/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationY(double)
	 */
	@Override
	public void addTranslationY(double pDY)
	{
		mTranslationY += pDY;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationZ(double)
	 */
	@Override
	public void addTranslationZ(double pDZ)
	{
		mTranslationZ += pDZ;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addRotationX(int)
	 */
	@Override
	public void addRotationX(int pDRX)
	{
		mRotationX += pDRX;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addRotationY(int)
	 */
	@Override
	public void addRotationY(int pDRY)
	{
		mRotationY += pDRY;
		notifyUpdateOfVolumeRenderingParameters();
	}

	/**
	 * Returns volume scale along x.
	 * 
	 * @return scale along x
	 */
	public double getScaleX()
	{
		return mScaleX;
	}

	/**
	 * Returns volume scale along y.
	 * 
	 * @return scale along y
	 */
	public double getScaleY()
	{
		return mScaleY;
	}

	/**
	 * Returns volume scale along z.
	 * 
	 * @return scale along z
	 */
	public double getScaleZ()
	{
		return mScaleZ;
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
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getRotationY()
	 */
	@Override
	public float getRotationY()
	{
		return mRotationX;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getRotationX()
	 */
	@Override
	public float getRotationX()
	{
		return mRotationY;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setTransferFunction(clearvolume.transferf.TransferFunction)
	 */
	@Override
	public void setTransferFunction(final TransferFunction pTransfertFunction)
	{
		mTransferFunctions[getCurrentRenderLayer()] = pTransfertFunction;
	}

	/**
	 * Returns transfer function for a given render layer.
	 * 
	 * @return currently used transfer function
	 */
	public TransferFunction getTransfertFunction(final int pRenderLayerIndex)
	{
		return mTransferFunctions[pRenderLayerIndex];
	}

	/**
	 * Returns currently used transfer function.
	 * 
	 * @return currently used transfer function
	 */
	public TransferFunction getTransfertFunction()
	{
		return mTransferFunctions[getCurrentRenderLayer()];
	}

	/**
	 * Returns currently used projection algorithm.
	 * 
	 * @return currently used projection algorithm
	 */
	public ProjectionAlgorithm getProjectionAlgorithm()
	{
		return mProjectionAlgorithm;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setProjectionAlgorithm(clearvolume.projections.ProjectionAlgorithm)
	 */
	@Override
	public void setProjectionAlgorithm(final ProjectionAlgorithm pProjectionAlgorithm)
	{
		mProjectionAlgorithm = pProjectionAlgorithm;
	}

	/**
	 * Returns current volume data buffer.
	 * 
	 * @return current data buffer.
	 */
	public ByteBuffer getVolumeDataBuffer()
	{
		return getVolumeDataBuffer(getCurrentRenderLayer());
	}

	/**
	 * Returns for a given index the corresponding volume data buffer.
	 * 
	 * @return data buffer for a given render layer.
	 */
	public ByteBuffer getVolumeDataBuffer(final int pVolumeDataBufferIndex)
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
	public Object getSetVolumeDataBufferLock(int pRenderLayerIndex)
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
		mRotationX = 0;
		mRotationY = 0;
		mTranslationX = 0;
		mTranslationY = 0;
		mTranslationZ = -4;
	}

	@Override
	public void setCurrentRenderLayer(int pLayerIndex)
	{
		mCurrentRenderLayerIndex = pLayerIndex;
	}

	@Override
	public int getCurrentRenderLayer()
	{
		return mCurrentRenderLayerIndex;
	}

	@Override
	public void setNumberOfRenderLayers(int pNumberOfRenderLayers)
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
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long)
	 */
	@Override
	public void setVolumeDataBuffer(final ByteBuffer pByteBuffer,
																	final long pSizeX,
																	final long pSizeY,
																	final long pSizeZ)
	{
		setVolumeDataBuffer(pByteBuffer, pSizeX, pSizeY, pSizeZ, 1, 1, 1);
	}


		/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVoxelSize(double,
	 *      double, double)
	 */
	public void setVoxelSize(	double pVoxelSizeX,
														double pVoxelSizeY,
														double pVoxelSizeZ)
	{
		mVoxelSizeX = pVoxelSizeX;
		mVoxelSizeY = pVoxelSizeY;
		mVoxelSizeZ = pVoxelSizeZ;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVolumeDataBuffer(java.nio.ByteBuffer,
	 *      long, long, long, double, double, double)
	 */
	@Override
	public void setVolumeDataBuffer(final ByteBuffer pByteBuffer,
																	final long pSizeX,
																	final long pSizeY,
																	final long pSizeZ,
																	final double pVoxelSizeX,
																	final double pVoxelSizeY,
																	final double pVoxelSizeZ)
	{
		synchronized (getSetVolumeDataBufferLock(getCurrentRenderLayer()))
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

			double lMaxSize = max(max(mVolumeSizeX, mVolumeSizeY),
														mVolumeSizeZ);

			mScaleX = (float) (pVoxelSizeX * mVolumeSizeX / lMaxSize);
			mScaleY = (float) (pVoxelSizeY * mVolumeSizeY / lMaxSize);
			mScaleZ = (float) (pVoxelSizeZ * mVolumeSizeZ / lMaxSize);

			mVolumeDataByteBuffers[getCurrentRenderLayer()] = pByteBuffer;

			clearCompletionOfDataBufferCopy(getCurrentRenderLayer());
			notifyUpdateOfVolumeRenderingParameters();
		}
	}

	@Override
	public void setVolumeDataBuffer(Volume<?> pVolume)
	{
		synchronized (getSetVolumeDataBufferLock(getCurrentRenderLayer()))
		{
			setVolumeDataBuffer(pVolume.getDataBuffer(),
													pVolume.getWidthInVoxels(),
													pVolume.getHeightInVoxels(),
													pVolume.getDepthInVoxels(),
													pVolume.getVoxelWidthInRealUnits(),
													pVolume.getVoxelHeightInRealUnits(),
													pVolume.getVoxelDepthInRealUnits());
		}
	}

	@Override
	public VolumeManager createCompatibleVolumeManager(int pMaxAvailableVolumes)
	{
		return new VolumeManager(pMaxAvailableVolumes);
	}

	/**
	 * Notifies the volume data copy completion.
	 */
	public void notifyCompletionOfDataBufferCopy(final int pRenderLayerIndex)
	{
		mDataBufferCopyIsFinished.set(pRenderLayerIndex, 1);
	}

	/**
	 * Clears data copy buffer flag.
	 */
	public void clearCompletionOfDataBufferCopy(final int pRenderLayerIndex)
	{
		mDataBufferCopyIsFinished.set(pRenderLayerIndex, 0);
	}

	/**
	 * Waits until volume data copy completes all layers.
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	@Override
	public boolean waitToFinishAllDataBufferCopy(	long pTimeOut,
																								TimeUnit pTimeUnit)
	{
		boolean lNoTimeOut = true;
		for (int i = 0; i < getNumberOfRenderLayers(); i++)
			lNoTimeOut &= waitToFinishDataBufferCopy(	getCurrentRenderLayer(),
																								pTimeOut,
																								pTimeUnit);

		return lNoTimeOut;
	}

	/**
	 * Waits until volume data copy completes for current layer.
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	@Override
	public boolean waitToFinishDataBufferCopy(long pTimeOut,
																						TimeUnit pTimeUnit)
	{
		return waitToFinishDataBufferCopy(getCurrentRenderLayer(),
																			pTimeOut,
																			pTimeUnit);
	}

	/**
	 * Waits until volume data copy completes for a given layer
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	@Override
	public boolean waitToFinishDataBufferCopy(final int pRenderLayerIndex,
																						long pTimeOut,
																						TimeUnit pTimeUnit)
	{
		boolean lNoTimeOut = true;
		long lStartTimeInNanoseconds = System.nanoTime();
		long lTimeOutTimeInNanoseconds = lStartTimeInNanoseconds + TimeUnit.NANOSECONDS.convert(pTimeOut,
																																														pTimeUnit);
		while ((lNoTimeOut = System.nanoTime() < lTimeOutTimeInNanoseconds) && mDataBufferCopyIsFinished.get(pRenderLayerIndex) == 0)
		{
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		return !lNoTimeOut;
	}

	/**
	 * Returns the currently used rotation controller.
	 * 
	 * @return currently used rotation controller.
	 */
	public RotationControllerInterface getRotationController()
	{
		return mRotationController;
	}

	/**
	 * Checks whether there is a rotation controller used (in addition to the
	 * mouse).
	 * 
	 * @return true if it has a rotation controller
	 */
	public boolean hasRotationController()
	{
		return mRotationController != null ? mRotationController.isActive()
																			: false;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setQuaternionController(clearvolume.controller.RotationControllerInterface)
	 */
	@Override
	public void setQuaternionController(final RotationControllerInterface quaternionController)
	{
		mRotationController = quaternionController;
	}

	public void openControlFrame()
	{
		mControlFrame = new JFrame("ClearVolume Rendering Parameters");
		mControlFrame.setLayout(new BorderLayout());
		mControlFrame.add(createControlPanel(), BorderLayout.SOUTH);
		mControlFrame.pack();
		// mControlFrame.setAlwaysOnTop(true);
		mControlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mControlFrame.setVisible(true);
	}

	/**
	 * Create the control panel containing the sliders for setting the
	 * visualization parameters.
	 * 
	 * @return The control panel
	 */
	private JPanel createControlPanel()
	{
		ControlPanel lControlPanel = new ControlPanel(this);
		return lControlPanel;
	}

	/**
	 * Clamps the value pValue to e interval [pMin,pMax]
	 * 
	 * @param pValue
	 *          to be clamped
	 * @param pMin
	 *          minimum
	 * @param pMax
	 *          maximum
	 * @return clamped value
	 */
	public static double clamp(	final double pValue,
															final double pMin,
															final double pMax)
	{
		return Math.min(Math.max(pValue, pMin), pMax);
	}

}
