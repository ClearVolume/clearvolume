package clearvolume.network.server;

import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import clearvolume.network.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import clearvolume.volume.source.VolumeSourceInterface;

public class ClearVolumeTCPServerRunnable implements Runnable
{

	private ServerSocketChannel mServerSocketChannel;
	private VolumeSourceInterface mVolumeSource;

	private volatile boolean mStopSignal = false;
	private volatile boolean mStoppedSignal = false;
	private ByteBuffer mByteBuffer;

	public ClearVolumeTCPServerRunnable(ServerSocketChannel pSocketChannel,
																			VolumeSourceInterface pVolumeSource)
	{
		mServerSocketChannel = pSocketChannel;
		mVolumeSource = pVolumeSource;
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
				SocketChannel lSocketChannel = mServerSocketChannel.accept();

				try
				{
					while (lSocketChannel.isOpen() && lSocketChannel.isConnected()
									&& !mStopSignal)
					{
						Volume lVolumeToSend = mVolumeSource.requestVolume();

						ClearVolumeSerialization.serialize(lVolumeToSend, mByteBuffer);
						mByteBuffer.clear();
						lSocketChannel.write(mByteBuffer);
					}
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
