package clearvolume.volume.source;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
		mVolumeQueue.offer(pVolume);
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

	@Override
	public VolumeManager getManager()
	{
		return mVolumeManager;
	}

}
