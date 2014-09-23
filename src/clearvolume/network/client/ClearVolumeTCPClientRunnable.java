package clearvolume.network.client;

import java.nio.channels.SocketChannel;

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
			while (!mStopSignal)
			{
				// ByteBuffer lProtocolHeader =
				// ByteBuffer.allocateDirect(ClearVolumeProtocol.cProtocolHeaderLengthInBytes);
				// mSocketChannel.r
				// mSocketChannel.read(lProtocolHeader);
				// TODO: rest of

				Volume lVolume = null;
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
		// TODO Auto-generated method stub

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
