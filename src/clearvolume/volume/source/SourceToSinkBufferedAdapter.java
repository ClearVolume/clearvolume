package clearvolume.volume.source;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import clearvolume.volume.Volume;
import clearvolume.volume.sink.VolumeSinkInterface;

public class SourceToSinkBufferedAdapter implements
																						VolumeSinkInterface,
																						VolumeSourceInterface
{

	private final BlockingQueue<Volume<?>> mVolumeQueue;


	public SourceToSinkBufferedAdapter(int pMaxCapacity)
	{
		super();
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

}
