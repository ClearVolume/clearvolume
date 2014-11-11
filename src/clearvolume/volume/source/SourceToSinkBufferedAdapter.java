package clearvolume.volume.source;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.VolumeSinkInterface;

public class SourceToSinkBufferedAdapter implements
																				VolumeSinkInterface,
																				VolumeSourceInterface
{

	private final BlockingQueue<Volume<?>> mVolumeQueue;
	private VolumeManager mVolumeManager;

	public SourceToSinkBufferedAdapter(	VolumeManager pVolumeManager,
																			int pMaxCapacity)
	{
		super();
		mVolumeManager = pVolumeManager;
		mVolumeQueue = new ArrayBlockingQueue<Volume<?>>(pMaxCapacity);
	}

	@Override
	public void sendVolume(Volume<?> pVolume)
	{
		sendVolumeWithFeedback(pVolume);
	}

	public boolean sendVolumeWithFeedback(Volume<?> pVolume)
	{
		return mVolumeQueue.offer(pVolume);
	}

	@Override
	public Volume<?> requestVolume()
	{
		try
		{
			Volume<?> lVolume = mVolumeQueue.take();
			return lVolume;
		}
		catch (InterruptedException e)
		{
			return requestVolume();
		}
	}

	public Volume<?> requestVolumeAndWait(int pTimeOut,
																				TimeUnit pTimeUnit)
	{
		try
		{
			Volume<?> lVolume = mVolumeQueue.poll(pTimeOut, pTimeUnit);
			return lVolume;
		}
		catch (InterruptedException e)
		{
			return requestVolume();
		}
	}

	public Volume<?> peekVolume()
	{
		Volume<?> lVolume = mVolumeQueue.peek();
		return lVolume;
	}

	@Override
	public VolumeManager getManager()
	{
		return mVolumeManager;
	}

}
