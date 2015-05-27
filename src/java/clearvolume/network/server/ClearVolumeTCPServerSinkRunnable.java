package clearvolume.network.server;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import clearvolume.volume.source.SourceToSinkBufferedAdapter;

public class ClearVolumeTCPServerSinkRunnable implements Runnable
{
	private final ClearVolumeTCPServerSink mClearVolumeTCPServerSink;
	private final ServerSocketChannel mServerSocketChannel;
	private final SourceToSinkBufferedAdapter mVolumeSource;

	private volatile boolean mStopSignal = false;
	private volatile boolean mStoppedSignal = false;
	private ByteBuffer mByteBuffer;

	public ClearVolumeTCPServerSinkRunnable(ClearVolumeTCPServerSink pClearVolumeTCPServerSink,
																					ServerSocketChannel pSocketChannel,
																					SourceToSinkBufferedAdapter pVolumeSource)
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
				final SocketChannel lSocketChannel = mServerSocketChannel.accept();
				// System.out.println("connection accepted");
				lSocketChannel.setOption(	StandardSocketOptions.SO_SNDBUF,
																	ClearVolumeTCPClient.cSocketBufferLength);

				try
				{
					if (lSocketChannel.isOpen() && lSocketChannel.isConnected()
							&& mClearVolumeTCPServerSink.getLastVolumeSeen() != null)
					{
						// System.out.println("sending last seen volume: " +
						// mClearVolumeTCPServerSink.getLastVolumeSeen());
						sendVolumeToClient(	lSocketChannel,
																mClearVolumeTCPServerSink.getLastVolumeSeen(),
																false);
					}

					while (lSocketChannel.isOpen() && lSocketChannel.isConnected()
									&& !mStopSignal)
					{
						final Volume lVolumeToSend = mVolumeSource.requestVolumeAndWait(10,
																																						TimeUnit.MILLISECONDS);
						if (lVolumeToSend != null)
							sendVolumeToClient(lSocketChannel, lVolumeToSend, true);

					}
				}
				catch (final java.io.IOException e1)
				{
					continue;
				}
				catch (final Throwable e)
				{
					e.printStackTrace();
				}

			}

		}
		catch (final java.nio.channels.AsynchronousCloseException e)
		{
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

	private void sendVolumeToClient(SocketChannel lSocketChannel,
																	Volume lVolumeToSend,
																	boolean pReleaseOrForward) throws IOException
	{
		mByteBuffer = ClearVolumeSerialization.serialize(	lVolumeToSend,
																											mByteBuffer);
		mByteBuffer.rewind();
		if (lSocketChannel.isConnected() && lSocketChannel.isOpen())
		{
			while (mByteBuffer.hasRemaining())
				lSocketChannel.write(mByteBuffer);

			if (pReleaseOrForward)
			{
				if (mClearVolumeTCPServerSink.getRelaySink() == null)
					lVolumeToSend.makeAvailableToManager();
				else
					mClearVolumeTCPServerSink.getRelaySink()
																		.sendVolume(lVolumeToSend);
			}
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
			catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

}
