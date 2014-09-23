package clearvolume.volume;

import static java.lang.Math.toIntExact;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Volume
{
	private ByteBuffer mVolumeData;
	private long[] mVolumeDimensionsInVoxels;
	private double[] mVolumeDimensionsInRealUnits;
	private String mRealUnitName;

	private int mChannel;
	private float[] mViewMatrix;

	private long mVolumeIndex;
	private double mVolumeTime;

	public Volume(long... pDimensions)
	{
		super();
		mVolumeDimensionsInVoxels = pDimensions;
		final long lBufferLength = getVolumeDataSizeInBytes();
		final int lBufferLengthInt = toIntExact(lBufferLength);
		mVolumeData = ByteBuffer.allocateDirect(lBufferLengthInt)
														.order(ByteOrder.nativeOrder());
	}

	public long getVolumeDataSizeInBytes()
	{
		long lVolumeDataSize = 1;
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

	public long getBytesPerVoxels()
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
		return mVolumeDimensionsInRealUnits[1];
	}

	public double getHeightInRealUnits()
	{
		return mVolumeDimensionsInRealUnits[2];
	}

	public double getDepthInRealUnits()
	{
		return mVolumeDimensionsInRealUnits[3];
	}

	public String getRealUnitName()
	{
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

	public int getChannel()
	{
		return mChannel;
	}

	public void setChannel(int pChannel)
	{
		mChannel = pChannel;
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
		pByteBuffer.put(mVolumeData);
	}



}
