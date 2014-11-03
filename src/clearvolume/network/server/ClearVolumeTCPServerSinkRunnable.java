package clearvolume.network.server;

import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import clearvolume.volume.source.VolumeSourceInterface;

public class ClearVolumeTCPServerSinkRunnable implements Runnable
{
	private ClearVolumeTCPServerSink mClearVolumeTCPServerSink;
	private ServerSocketChannel mServerSocketChannel;
	private VolumeSourceInterface mVolumeSource;

	private volatile boolean mStopSignal = false;
	private volatile boolean mStoppedSignal = false;
	private ByteBuffer mByteBuffer;


	public ClearVolumeTCPServerSinkRunnable(ClearVolumeTCPServerSink pClearVolumeTCPServerSink,
																					ServerSocketChannel pSocketChannel,
																			VolumeSourceInterface pVolumeSource)
	{
		mClearVolumeTCPServerSink = pClearVolumeTCPServerSink;
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
				lSocketChannel.setOption(	StandardSocketOptions.SO_SNDBUF,
																	ClearVolumeTCPClient.cSocketBufferLength);

				try
				{
					while (lSocketChannel.isOpen() && lSocketChannel.isConnected()
									&& !mStopSignal)
					{
						Volume<?> lVolumeToSend = mVolumeSource.requestVolume();

						mByteBuffer = ClearVolumeSerialization.serialize(	lVolumeToSend,
																															mByteBuffer);
						mByteBuffer.rewind();
						lSocketChannel.write(mByteBuffer);

						if (mClearVolumeTCPServerSink.getRelaySink() == null)
							lVolumeToSend.makeAvailableToManager();
						else
							mClearVolumeTCPServerSink.getRelaySink()
																				.sendVolume(lVolumeToSend);

					}
				}
				catch (java.io.IOException e1)
				{
					continue;
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
				e.printStackTrace();
			}
		}
	}

}
