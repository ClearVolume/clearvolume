package clearvolume.volume;

import static java.lang.Math.toIntExact;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Volume<T>
{
	private volatile VolumeManager mVolumeManager;

	private ByteBuffer mVolumeData;
	private Class<T> mType;
	private long[] mVolumeDimensionsInVoxels;
	private double[] mVolumeDimensionsInRealUnits;
	private volatile String mRealUnitName;

	private volatile int mVolumeChannelID;
	private float[] mViewMatrix;

	private volatile long mVolumeIndex;
	private volatile double mVolumeTime;

	public Volume()
	{
	}

	public Volume(Class<T> pType, long... pDimensions)
	{
		super();
		mType = pType;
		mVolumeDimensionsInVoxels = pDimensions;
		final long lBufferLength = getVolumeDataSizeInBytes();
		final int lBufferLengthInt = toIntExact(lBufferLength);
		mVolumeData = ByteBuffer.allocateDirect(lBufferLengthInt)
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

	public <LT> boolean isCompatibleWith(	Class<LT> pType,
																				long... pDimensions)
	{
		return (mType == pType && Arrays.equals(pDimensions,
																						mVolumeDimensionsInVoxels));
	}

	public long getVolumeDataSizeInBytes()
	{
		long lVolumeDataSize = getBytesPerVoxel();
		for (int i = 0; i < getVolumeDimensionsInVoxels().length; i++)
			lVolumeDataSize *= getVolumeDimensionsInVoxels()[i];
		return lVolumeDataSize;
	}

	public void setVolumeDimensionsInVoxels(long... pDimensionsInVoxels)
	{
		mVolumeDimensionsInVoxels = pDimensionsInVoxels;
	}

	public void setVolumeDimensionsInRealUnits(	String pRealUnitName,
																							double... VolumeDimensionsInRealUnits)
	{
		setRealUnitName(pRealUnitName);
		mVolumeDimensionsInRealUnits = VolumeDimensionsInRealUnits;

	}

	public void setVolumeData(ByteBuffer pByteBuffer)
	{
		mVolumeData = pByteBuffer;
	}

	public ByteBuffer getVolumeData()
	{
		return mVolumeData;
	}

	public void setType(String pType)
	{
		if (pType.equalsIgnoreCase("Byte"))
			setType(Byte.class);
		else if (pType.equalsIgnoreCase("Character"))
			setType(Character.class);
		else if (pType.equalsIgnoreCase("Char"))
			setType(Character.class);
		else if (pType.equalsIgnoreCase("Short"))
			setType(Short.class);
		else if (pType.equalsIgnoreCase("Integer"))
			setType(Integer.class);
		else if (pType.equalsIgnoreCase("Long"))
			setType(Long.class);
		else if (pType.equalsIgnoreCase("Float"))
			setType(Float.class);
		else if (pType.equalsIgnoreCase("Double"))
			setType(Double.class);

	}

	@SuppressWarnings("unchecked")
	public void setType(Class<?> pType)
	{
		mType = (Class<T>) pType;
	}

	public Class<T> getType()
	{
		return mType;
	}

	public String getTypeName()
	{
		return mType.getTypeName().replaceAll("java.lang.", "");
	}

	public void setDimension(int pDim)
	{
		mVolumeDimensionsInVoxels = new long[pDim + 1];
		mVolumeDimensionsInRealUnits = new double[pDim + 1];
	}

	public int getDimension()
	{
		return mVolumeDimensionsInVoxels.length - 1;
	}

	public long[] getVolumeDimensionsInVoxels()
	{
		return mVolumeDimensionsInVoxels;
	}

	public long getBytesPerVoxel()
	{
		if (mType == Byte.class)
		{
			return Byte.BYTES;
		}
		else if (mType == Short.class)
		{
			return Short.BYTES;
		}
		else if (mType == Character.class)
		{
			return Character.BYTES;
		}
		else if (mType == Integer.class)
		{
			return Integer.BYTES;
		}
		else if (mType == Long.class)
		{
			return Long.BYTES;
		}
		else if (mType == Float.class)
		{
			return Float.BYTES;
		}
		else if (mType == Double.class)
		{
			return Double.BYTES;
		}
		return 0;
	}

	public long getElementSize()
	{
		return mVolumeDimensionsInVoxels[0];
	}

	public long getWidthInVoxels()
	{
		return mVolumeDimensionsInVoxels[1];
	}

	public long getHeightInVoxels()
	{
		return mVolumeDimensionsInVoxels[2];
	}

	public long getDepthInVoxels()
	{
		return mVolumeDimensionsInVoxels[3];
	}

	public double[] getVolumeDimensionsInRealUnits()
	{
		return mVolumeDimensionsInRealUnits;
	}

	public double getWidthInRealUnits()
	{
		if (mVolumeDimensionsInRealUnits == null)
			return 1;
		return mVolumeDimensionsInRealUnits[0];
	}

	public double getHeightInRealUnits()
	{
		if (mVolumeDimensionsInRealUnits == null)
			return 1;
		return mVolumeDimensionsInRealUnits[1];
	}

	public double getDepthInRealUnits()
	{
		if (mVolumeDimensionsInRealUnits == null)
			return 1;
		return mVolumeDimensionsInRealUnits[2];
	}

	public String getRealUnitName()
	{
		if (mVolumeDimensionsInRealUnits == null)
			return "none";
		return mRealUnitName;
	}

	public void setRealUnitName(String pRealUnitName)
	{
		mRealUnitName = pRealUnitName;
	}

	public long getIndex()
	{
		return mVolumeIndex;
	}

	public void setIndex(long pVolumeIndex)
	{
		mVolumeIndex = pVolumeIndex;
	}

	public double getTime()
	{
		return mVolumeTime;
	}

	public void setTime(double pVolumeTime)
	{
		mVolumeTime = pVolumeTime;
	}

	public int getVolumeChannelID()
	{
		return mVolumeChannelID;
	}

	public void setVolumeChannelID(int pVolumeChannel)
	{
		mVolumeChannelID = pVolumeChannel;
	}

	public float[] getViewMatrix()
	{
		return mViewMatrix;
	}

	public void setViewMatrix(float[] pViewMatrix)
	{
		mViewMatrix = pViewMatrix;
	}

	public void writeToByteBuffer(ByteBuffer pByteBuffer)
	{
		mVolumeData.clear();
		pByteBuffer.put(mVolumeData);
	}

	public void readFromByteBuffer(ByteBuffer pByteBuffer)
	{
		mVolumeData.clear();
		mVolumeData.put(pByteBuffer);
	}

	@Override
	public String toString()
	{
		return "Volume [mType=" + getTypeName()
						+ ", mVolumeDimensionsInVoxels="
						+ Arrays.toString(mVolumeDimensionsInVoxels)
						+ ", mRealUnitName="
						+ mRealUnitName
						+ ", mVolumeDimensionsInRealUnits="
						+ Arrays.toString(mVolumeDimensionsInRealUnits)
						+ ", mChannels="
						+ mVolumeChannelID
						+ ", mVolumeIndex="
						+ mVolumeIndex
						+ ", mVolumeTime="
						+ mVolumeTime
						+ ", mViewMatrix="
						+ Arrays.toString(mViewMatrix)
						+ "]";
	}

}
