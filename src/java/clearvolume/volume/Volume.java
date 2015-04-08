package clearvolume.volume;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import clearvolume.ClearVolumeCloseable;
import clearvolume.utils.ToIntExact;
import coremem.types.NativeTypeEnum;
import coremem.util.Size;

public class Volume implements ClearVolumeCloseable
{
	private volatile VolumeManager mVolumeManager;

	private ByteBuffer mDataBuffer;
	private NativeTypeEnum mType;
	private long[] mDimensionsInVoxels;
	private double[] mVoxelSizeInRealUnits;
	private volatile String mRealUnitName;

	private volatile int mChannelID;
	private volatile String mChannelName = "noname";
	private float[] mColor = null;
	private float[] mViewMatrix = null;

	private volatile long mTimeIndex;
	private volatile double mTimeInSeconds;

	public Volume()
	{
	}

	public Volume(NativeTypeEnum pType, long... pDimensions)
	{
		super();
		mType = pType;
		mDimensionsInVoxels = pDimensions;
		final long lBufferLength = getDataSizeInBytes();
		final int lBufferLengthInt = ToIntExact.toIntExact(lBufferLength);
		mDataBuffer = ByteBuffer.allocateDirect(lBufferLengthInt)
														.order(ByteOrder.nativeOrder());
	}

	public void setManager(VolumeManager pVolumeManager)
	{
		mVolumeManager = pVolumeManager;
	}

	public void makeAvailableToManager()
	{
		mVolumeManager.makeAvailable(this);
	}

	public <LT> boolean isCompatibleWith(	NativeTypeEnum pType,
																				long... pDimensions)
	{
		return (mType == pType && Arrays.equals(pDimensions,
																						mDimensionsInVoxels));
	}

	public long getNumberOfVoxels()
	{
		long lNumberOfVoxels = 1;
		for (int i = 1; i < getDimensionsInVoxels().length; i++)
			lNumberOfVoxels *= getDimensionsInVoxels()[i];
		return lNumberOfVoxels;
	}

	public long getNumberOfElements()
	{
		long lNumberOfElements = 1;
		for (int i = 0; i < getDimensionsInVoxels().length; i++)
			lNumberOfElements *= getDimensionsInVoxels()[i];
		return lNumberOfElements;
	}

	public long getDataSizeInBytes()
	{
		long lVolumeDataSize = getBytesPerVoxel();
		for (int i = 0; i < getDimensionsInVoxels().length; i++)
			lVolumeDataSize *= getDimensionsInVoxels()[i];
		return lVolumeDataSize;
	}

	public void setDimensionsInVoxels(long... pDimensionsInVoxels)
	{
		mDimensionsInVoxels = pDimensionsInVoxels;
	}

	public void setVoxelSizeInRealUnits(String pRealUnitName,
																			double... pVoxelSizeInRealUnits)
	{
		setRealUnitName(pRealUnitName);
		mVoxelSizeInRealUnits = pVoxelSizeInRealUnits;

	}

	public void setDataBuffer(ByteBuffer pByteBuffer)
	{
		mDataBuffer = pByteBuffer;
	}

	public ByteBuffer getDataBuffer()
	{
		return mDataBuffer;
	}

	public void setType(String pType)
	{
		if (pType.equalsIgnoreCase("Byte"))
			setType(NativeTypeEnum.Byte);

		if (pType.equalsIgnoreCase("UnsignedByte"))
			setType(NativeTypeEnum.UnsignedByte);

		else if (pType.equalsIgnoreCase("Short"))
			setType(NativeTypeEnum.Short);

		else if (pType.equalsIgnoreCase("UnsignedShort"))
			setType(NativeTypeEnum.UnsignedShort);

		else if (pType.equalsIgnoreCase("Integer") || pType.equalsIgnoreCase("Int"))
			setType(NativeTypeEnum.Int);

		else if (pType.equalsIgnoreCase("UnsignedInteger") || pType.equalsIgnoreCase("UnsignedInt"))
			setType(NativeTypeEnum.UnsignedInt);

		else if (pType.equalsIgnoreCase("Long"))
			setType(NativeTypeEnum.Long);

		else if (pType.equalsIgnoreCase("UnsignedLong"))
			setType(NativeTypeEnum.UnsignedLong);

		else if (pType.equalsIgnoreCase("Float"))
			setType(NativeTypeEnum.Float);

		else if (pType.equalsIgnoreCase("Double"))
			setType(NativeTypeEnum.Double);

	}

	@SuppressWarnings("unchecked")
	public void setType(NativeTypeEnum pType)
	{
		mType = pType;
	}

	public NativeTypeEnum getNativeType()
	{
		return mType;
	}

	public NativeTypeEnum getTypeName()
	{
		return mType;
	}

	public void setDimension(int pDim)
	{
		mDimensionsInVoxels = new long[pDim + 1];
		mVoxelSizeInRealUnits = new double[pDim + 1];
	}

	public int getDimension()
	{
		return mDimensionsInVoxels.length - 1;
	}

	public long[] getDimensionsInVoxels()
	{
		return mDimensionsInVoxels;
	}

	public int getBytesPerVoxel()
	{
		return Size.of(mType);
	}

	public long getElementSize()
	{
		return mDimensionsInVoxels[0];
	}

	public long getWidthInVoxels()
	{
		return mDimensionsInVoxels[1];
	}

	public long getHeightInVoxels()
	{
		return mDimensionsInVoxels[2];
	}

	public long getDepthInVoxels()
	{
		return mDimensionsInVoxels[3];
	}

	public double[] getDimensionsInRealUnits()
	{
		return mVoxelSizeInRealUnits;
	}

	public double getVoxelWidthInRealUnits()
	{
		if (mVoxelSizeInRealUnits == null)
			return 1;
		return mVoxelSizeInRealUnits[0];
	}

	public double getVoxelHeightInRealUnits()
	{
		if (mVoxelSizeInRealUnits == null)
			return 1;
		return mVoxelSizeInRealUnits[1];
	}

	public double getVoxelDepthInRealUnits()
	{
		if (mVoxelSizeInRealUnits == null)
			return 1;
		return mVoxelSizeInRealUnits[2];
	}

	public String getRealUnitName()
	{
		if (mVoxelSizeInRealUnits == null)
			return "none";
		return mRealUnitName;
	}

	public void setRealUnitName(String pRealUnitName)
	{
		mRealUnitName = pRealUnitName;
	}

	public long getTimeIndex()
	{
		return mTimeIndex;
	}

	public void setTimeIndex(long pTimeIndex)
	{
		mTimeIndex = pTimeIndex;
	}

	public double getTimeInSeconds()
	{
		return mTimeInSeconds;
	}

	public void setTimeInSeconds(double pTimeInSeconds)
	{
		mTimeInSeconds = pTimeInSeconds;
	}

	public int getChannelID()
	{
		return mChannelID;
	}

	public void setChannelID(int pChannelID)
	{
		mChannelID = pChannelID;
	}

	public String getChannelName()
	{
		return mChannelName;
	}

	public void setChannelName(String pChannelName)
	{
		mChannelName = pChannelName;
	}

	public float[] getColor()
	{
		return mColor;
	}

	public void setColor(float... pColor)
	{
		mColor = pColor;
	}

	public float[] getViewMatrix()
	{
		return mViewMatrix;
	}

	public void setViewMatrix(float[] pViewMatrix)
	{
		mViewMatrix = pViewMatrix;
	}

	public void copyDataFrom(ByteBuffer pByteBuffer)
	{
		pByteBuffer.rewind();
		mDataBuffer.clear();
		mDataBuffer.put(pByteBuffer);
	}

	public void copyDataFrom(Volume pVolume)
	{
		if (mDataBuffer.capacity() != pVolume.mDataBuffer.capacity())
			mDataBuffer = ByteBuffer.allocateDirect(pVolume.mDataBuffer.capacity())
															.order(ByteOrder.nativeOrder());

		mDataBuffer.clear();
		pVolume.mDataBuffer.rewind();
		mDataBuffer.put(pVolume.mDataBuffer);
	}

	@SuppressWarnings("unchecked")
	public void copyMetaDataFrom(Volume pVolume)
	{
		mChannelID = pVolume.mChannelID;
		if (pVolume.mChannelName != null)
			mChannelName = new String(pVolume.mChannelName);

		if (pVolume.mColor != null)
			mColor = Arrays.copyOf(pVolume.mColor, pVolume.mColor.length);

		if (pVolume.mVoxelSizeInRealUnits != null)
			mVoxelSizeInRealUnits = Arrays.copyOf(pVolume.mVoxelSizeInRealUnits,
																						pVolume.mVoxelSizeInRealUnits.length);

		if (pVolume.mDimensionsInVoxels != null)
			mDimensionsInVoxels = Arrays.copyOf(pVolume.mDimensionsInVoxels,
																					pVolume.mDimensionsInVoxels.length);

		if (mRealUnitName != null)
			mRealUnitName = new String(pVolume.mRealUnitName);
		mTimeIndex = pVolume.mTimeIndex;
		mTimeInSeconds = pVolume.mTimeInSeconds;
		mType = pVolume.mType;
		if (pVolume.mViewMatrix != null)
			mViewMatrix = Arrays.copyOf(pVolume.mViewMatrix,
																	pVolume.mViewMatrix.length);
	}

	public void writeToByteBuffer(ByteBuffer pByteBuffer)
	{
		mDataBuffer.clear();
		pByteBuffer.put(mDataBuffer);
	}

	public void readFromByteBuffer(ByteBuffer pByteBuffer)
	{
		mDataBuffer.clear();
		mDataBuffer.put(pByteBuffer);
	}

	@Override
	public void close()
	{
	}

	@Override
	public String toString()
	{
		return String.format(	"Volume [mTimeIndex=%s, mTimeInSeconds=%s, mChannelID=%s, mViewMatrix=%s, mColor=%s, mType=%s, mDimensionsInVoxels=%s, mDimensionsInRealUnits=%s, mRealUnitName=%s, mVolumeManager=%s, mDataBuffer=%s]",
													mTimeIndex,
													mTimeInSeconds,
													mChannelID,
													Arrays.toString(mViewMatrix),
													Arrays.toString(mColor),
													mType,
													Arrays.toString(mDimensionsInVoxels),
													Arrays.toString(mVoxelSizeInRealUnits),
													mRealUnitName,
													mVolumeManager,
													mDataBuffer);
	}

}
