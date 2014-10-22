package clearvolume.volume;

import java.lang.ref.SoftReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VolumeManager
{

	private final ArrayBlockingQueue<SoftReference<Volume<?>>> mAvailableVolumesQueue;

	private int mMaxAvailableVolumes;

	public VolumeManager(int pMaxAvailableVolumes)
	{
		super();
		mMaxAvailableVolumes = pMaxAvailableVolumes;
		mAvailableVolumesQueue = new ArrayBlockingQueue<SoftReference<Volume<?>>>(mMaxAvailableVolumes);
	}

	public <T> Volume<T> requestAndWaitForVolume(	long pTimeOut,
																								TimeUnit pTimeUnit,
																								Class<T> pType,
																								long... pDimensions)
	{
		do
		{
			try
			{
				SoftReference<Volume<?>> lPolledVolumeReference;
				// System.out.println("mAvailableVolumesQueue.size()=" +
				// mAvailableVolumesQueue.size());
				lPolledVolumeReference = mAvailableVolumesQueue.poll(	pTimeOut,
																															pTimeUnit);

				if (lPolledVolumeReference == null)
					return allocateAndUseNewVolume(pType, pDimensions);
				Volume<?> lVolume = lPolledVolumeReference.get();

				if (lVolume == null)
					return allocateAndUseNewVolume(pType, pDimensions);

				if (!lVolume.isCompatibleWith(pType, pDimensions))
					return allocateAndUseNewVolume(pType, pDimensions);

				return (Volume<T>) lVolume;
			}
			catch (InterruptedException e)
			{
			}
		}
		while (true);
	}

	public <T> Volume<?> requestAndWaitForNextAvailableVolume(long pTimeOut,
																														TimeUnit pTimeUnit)
	{
		do
		{
			try
			{
				SoftReference<Volume<?>> lPolledVolumeReference;
				// System.out.println("mAvailableVolumesQueue.size()=" +
				// mAvailableVolumesQueue.size());
				lPolledVolumeReference = mAvailableVolumesQueue.poll(	pTimeOut,
																															pTimeUnit);

				if (lPolledVolumeReference == null)
					return null;
				Volume<?> lVolume = lPolledVolumeReference.get();

				if (lVolume == null)
					return null;

				return lVolume;
			}
			catch (InterruptedException e)
			{
			}
		}
		while (true);
	}

	public <T> void makeAvailable(Volume<T> pVolume)
	{
		mAvailableVolumesQueue.offer(new SoftReference<Volume<?>>(pVolume));
	}

	private <T> Volume<T> allocateAndUseNewVolume(Class<T> pType,
																								long[] pDimensions)
	{
		Volume<T> lVolume = new Volume<T>(pType, pDimensions);
		lVolume.setManager(this);
		return lVolume;
	}

}
