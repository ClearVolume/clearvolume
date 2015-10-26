package clearvolume.volume;

import java.lang.ref.SoftReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import clearvolume.ClearVolumeCloseable;
import coremem.types.NativeTypeEnum;

public class VolumeManager implements ClearVolumeCloseable
{

	private final ArrayBlockingQueue<SoftReference<Volume>> mAvailableVolumesQueue;

	private final int mMaxAvailableVolumes;

	public VolumeManager(int pMaxAvailableVolumes)
	{
		super();
		mMaxAvailableVolumes = pMaxAvailableVolumes;
		mAvailableVolumesQueue = new ArrayBlockingQueue<SoftReference<Volume>>(mMaxAvailableVolumes);
	}

	public Volume requestAndWaitForVolumeLike(	int pTimeOut,
												TimeUnit pTimeUnit,
												Volume pVolume)
	{
		if (pVolume.getDataBuffer().remaining() == 2 * pVolume.getBytesPerVoxel()
													* pVolume.getNumberOfVoxels() && pVolume.getNativeType() == NativeTypeEnum.UnsignedByte)
		{
			return requestAndWaitForVolume(	pTimeOut,
											pTimeUnit,
											NativeTypeEnum.UnsignedShort,
											pVolume.getDimensionsInVoxels());
		}

		return requestAndWaitForVolume(	pTimeOut,
										pTimeUnit,
										pVolume.getNativeType(),
										pVolume.getDimensionsInVoxels());

	}

	public Volume requestAndWaitForVolume(	long pTimeOut,
											TimeUnit pTimeUnit,
											NativeTypeEnum pType,
											long... pDimensions)
	{
		do
		{
			try
			{
				SoftReference<Volume> lPolledVolumeReference;
				// System.out.println("mAvailableVolumesQueue.size()=" +
				// mAvailableVolumesQueue.size());
				lPolledVolumeReference = mAvailableVolumesQueue.poll(	pTimeOut,
																		pTimeUnit);

				if (lPolledVolumeReference == null)
					return allocateAndUseNewVolume(pType, pDimensions);
				final Volume lVolume = lPolledVolumeReference.get();

				if (lVolume == null)
					return allocateAndUseNewVolume(pType, pDimensions);

				if (!lVolume.isCompatibleWith(pType, pDimensions))
					return allocateAndUseNewVolume(pType, pDimensions);

				return lVolume;
			}
			catch (final InterruptedException e)
			{
			}
		}
		while (true);
	}

	public Volume requestAndWaitForNextAvailableVolume(	long pTimeOut,
														TimeUnit pTimeUnit)
	{
		do
		{
			try
			{
				SoftReference<Volume> lPolledVolumeReference;
				// System.out.println("mAvailableVolumesQueue.size()=" +
				// mAvailableVolumesQueue.size());
				lPolledVolumeReference = mAvailableVolumesQueue.poll(	pTimeOut,
																		pTimeUnit);

				if (lPolledVolumeReference == null)
					return null;
				final Volume lVolume = lPolledVolumeReference.get();

				if (lVolume == null)
					return null;

				return lVolume;
			}
			catch (final InterruptedException e)
			{
			}
		}
		while (true);
	}

	public <T> void makeAvailable(Volume pVolume)
	{
		mAvailableVolumesQueue.offer(new SoftReference<Volume>(pVolume));
	}

	private Volume allocateAndUseNewVolume(	NativeTypeEnum pType,
											long[] pDimensions)
	{
		final Volume lVolume = new Volume(pType, pDimensions);
		lVolume.setManager(this);
		return lVolume;
	}

	@Override
	public void close()
	{
		for (final SoftReference<Volume> lVolumeSoftReference : mAvailableVolumesQueue)
		{
			final Volume lVolume = lVolumeSoftReference.get();
			if (lVolume != null)
				lVolume.close();
		}
		mAvailableVolumesQueue.clear();
	}

}
