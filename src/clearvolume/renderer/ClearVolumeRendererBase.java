package clearvolume.renderer;

import java.awt.BorderLayout;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JPanel;

import clearvolume.ProjectionAlgorithm;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.transfertf.TransfertFunction;
import clearvolume.transfertf.TransfertFunctions;

/**
 * Class ClearVolumeRendererBase
 * 
 * Abstract class providing basic functionality for classes implementing the
 * ClearVolumeRendere interface
 *
 * @author Loic Royer 2014
 *
 */
public abstract class ClearVolumeRendererBase	implements
																							ClearVolumeRendererInterface
{

	/**
	 * Number of bytes per voxel used by this renderer
	 */
	private volatile int mBytesPerVoxel = 1;

	/**
	 * Rotation controller in addition to the mouse
	 */
	private RotationControllerInterface mRotationController;

	/**
	 * Pojection algorythm used
	 */
	private ProjectionAlgorithm mProjectionAlgorythm = ProjectionAlgorithm.MaxProjection;

	/**
	 * Transfer function used
	 */
	private TransfertFunction mTransferFunction = TransfertFunctions.getGrayLevel();

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
	private volatile boolean mVolumeDimensionsChanged;

	// data copy locking and waiting
	private final Object mSetVolumeDataBufferLock = new Object();
	private volatile ByteBuffer mVolumeDataByteBuffer;
	private ReentrantLock mDataBufferCopyFinishedLock = new ReentrantLock();
	private final Condition mDataBufferCopyFinishedCondition = mDataBufferCopyFinishedLock.newCondition();

	// Control frame:
	private JFrame mControlFrame;


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
	 * @return
	 */
	public boolean getIsUpdateVolumeParameters()
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

	/**
	 * @return
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
	 * Returns
	 * 
	 * @return
	 */
	public double getBrightness()
	{
		return mBrightness;
	}

	/**
	 * Sets brightness.
	 * 
	 * @param pBrightness
	 *          brightness
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
	 * @return
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
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationX(float)
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
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#addTranslationY(float)
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
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setTransfertFunction(clearvolume.transfertf.TransfertFunction)
	 */
	@Override
	public void setTransfertFunction(final TransfertFunction pTransfertFunction)
	{
		mTransferFunction = pTransfertFunction;
	}

	/**
	 * Returns currently used transfer function.
	 * 
	 * @return currently used transfer function
	 */
	public TransfertFunction getTransfertFunction()
	{
		return mTransferFunction;
	}

	/**
	 * Returns currently used projection algorithm.
	 * 
	 * @return
	 */
	public ProjectionAlgorithm getProjectionAlgorythm()
	{
		return mProjectionAlgorythm;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setProjectionAlgorythm(clearvolume.ProjectionAlgorithm)
	 */
	@Override
	public void setProjectionAlgorythm(final ProjectionAlgorithm pProjectionAlgorithm)
	{
		mProjectionAlgorythm = pProjectionAlgorithm;
	}

	/**
	 * Returns current volume data buffer.
	 * 
	 * @return
	 */
	public ByteBuffer getVolumeDataBuffer()
	{
		return mVolumeDataByteBuffer;
	}

	/**
	 * Clears volume data buffer.
	 * 
	 */
	public void clearVolumeDataBufferReference()
	{
		mVolumeDataByteBuffer = null;
	}

	/**
	 * Checks whether volume data buffer is available for display.
	 * 
	 * @return true if volume data is available
	 */
	public boolean isVolumeDataAvailable()
	{
		// convention for implementors: if the volume is non-zero there is data
		// available!
		return mVolumeSizeX * mVolumeSizeY * mVolumeSizeZ > 0;
	}

	/**
	 * Returns object used for locking volume data copy.
	 * 
	 * @return locking object
	 */
	public Object getSetVolumeDataBufferLock()
	{
		return mSetVolumeDataBufferLock;
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
		setVolumeDataBuffer(pByteBuffer,
												pSizeX,
												pSizeY,
												pSizeZ,
												1,
												((double) pSizeY) / pSizeX,
												((double) pSizeZ) / pSizeX);
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
																	final double pVolumeSizeX,
																	final double pVolumeSizeY,
																	final double pVolumeSizeZ)
	{
		synchronized (getSetVolumeDataBufferLock())
		{

			if (mVolumeSizeX != pSizeX || mVolumeSizeY != pSizeY
					|| mVolumeSizeZ != pSizeZ)
			{
				mVolumeDimensionsChanged = true;
			}
			mVolumeSizeX = pSizeX;
			mVolumeSizeY = pSizeY;
			mVolumeSizeZ = pSizeZ;

			mScaleX = (float) pVolumeSizeX;
			mScaleY = (float) pVolumeSizeY;
			mScaleZ = (float) pVolumeSizeZ;

			mVolumeDataByteBuffer = pByteBuffer;

			notifyUpdateOfVolumeRenderingParameters();
		}
	}

	/**
	 * Notifies the volume data copy completion.
	 */
	public void notifyCompletionOfDataBufferCopy()
	{
		mDataBufferCopyFinishedLock.lock();
		try
		{
			mDataBufferCopyFinishedCondition.signal();
		}
		finally
		{
			mDataBufferCopyFinishedLock.unlock();
		}
	}

	/**
	 * Waits until volume data copy completes.
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	@Override
	public boolean waitToFinishDataBufferCopy(long pTimeOut,
																						TimeUnit pTimeUnit)
	{
		mDataBufferCopyFinishedLock.lock();
		try
		{
			try
			{
				return mDataBufferCopyFinishedCondition.await(pTimeOut,
																											pTimeUnit);
			}
			catch (InterruptedException e)
			{
				return waitToFinishDataBufferCopy(pTimeOut, pTimeUnit);
			}
		}
		finally
		{
			mDataBufferCopyFinishedLock.unlock();
		}
	}

	/**
	 * Returns the currently used rotation controller.
	 * 
	 * @return
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
	 * @param pValuevalue
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
