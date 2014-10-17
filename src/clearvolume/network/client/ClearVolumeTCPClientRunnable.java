package clearvolume.network.client;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeTCPClientRunnable implements Runnable
{

	private SocketChannel mSocketChannel;
	private VolumeSinkInterface mVolumeSink;

	private volatile boolean mStopSignal = false;
	private volatile boolean mStoppedSignal = false;

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
			ByteBuffer lScratchBuffer = null;

			while (!mStopSignal)
			{
				@SuppressWarnings("rawtypes")
				Volume<?> lVolume = new Volume();
				lVolume = ClearVolumeSerialization.deserialize(	mSocketChannel,
																												lScratchBuffer,
																												lVolume);
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
