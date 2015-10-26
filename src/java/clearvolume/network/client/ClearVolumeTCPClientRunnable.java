package clearvolume.network.client;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeTCPClientRunnable implements Runnable
{
	private static final int cMaxpreAllocatedVolumes = 10;

	private final SocketChannel mSocketChannel;
	private final VolumeSinkInterface mVolumeSink;

	private volatile boolean mStopSignal = false;
	private volatile boolean mStoppedSignal = false;

	private final VolumeManager mVolumeManager;

	public ClearVolumeTCPClientRunnable(SocketChannel pSocketChannel,
										VolumeSinkInterface pVolumeSink,
										int pMaxInUseVolumes)
	{
		mSocketChannel = pSocketChannel;
		mVolumeSink = pVolumeSink;
		mVolumeManager = mVolumeSink.getManager();
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
				try
				{
					Volume lVolume = mVolumeManager.requestAndWaitForNextAvailableVolume(	1,
																							TimeUnit.MILLISECONDS);

					lVolume = ClearVolumeSerialization.deserialize(	mSocketChannel,
																	lVolume);

					lVolume.setManager(mVolumeManager);

					mVolumeSink.sendVolume(lVolume);
				}
				catch (final OutOfMemoryError e)
				{
					System.gc();
					e.printStackTrace();
				}

			}

		}
		catch (final Throwable e)
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

		if (pE instanceof ClosedChannelException)
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
			catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

}
