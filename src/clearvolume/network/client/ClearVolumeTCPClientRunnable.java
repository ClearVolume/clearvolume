package clearvolume.network.client;

import java.nio.channels.SocketChannel;

import clearvolume.network.ringbuffer.RingBuffer;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeTCPClientRunnable implements Runnable
{
	private static final int cMaxpreAllocatedVolumes = 10;

	private SocketChannel mSocketChannel;
	private VolumeSinkInterface mVolumeSink;

	private volatile boolean mStopSignal = false;
	private volatile boolean mStoppedSignal = false;

	private RingBuffer<Volume<?>> mVolumeRingBuffer = new RingBuffer<Volume<?>>(cMaxpreAllocatedVolumes);

	public ClearVolumeTCPClientRunnable(SocketChannel pSocketChannel,
																			VolumeSinkInterface pVolumeSink)
	{
		mSocketChannel = pSocketChannel;
		mVolumeSink = pVolumeSink;
	}

	public void requestStop()
	{
		mStopSignal = true;
	}

	@Override
	public void run()
	{
		try
		{

			while (!mStopSignal)
			{
				Volume<?> lVolume = mVolumeRingBuffer.get();

				lVolume = ClearVolumeSerialization.deserialize(	mSocketChannel,
																												lVolume);
				mVolumeRingBuffer.set(lVolume);
				mVolumeRingBuffer.advance();
				mVolumeSink.sendVolume(lVolume);
			}

		}
		catch (Throwable e)
		{
			handleError(e);
		}
		finally
		{
			mStoppedSignal = true;
		}
	}

	private void handleError(Throwable pE)
	{
		if (pE instanceof java.nio.channels.AsynchronousCloseException)
			return;

		pE.printStackTrace();
	}

	public void waitForStop()
	{
		while (!mStoppedSignal)
		{
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

}
