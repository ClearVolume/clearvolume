package clearvolume.network.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeUnit;

import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.relay.RelaySinkAdapter;
import clearvolume.volume.sink.relay.RelaySinkInterface;
import clearvolume.volume.source.SourceToSinkBufferedAdapter;

public class ClearVolumeTCPServerSink extends RelaySinkAdapter	implements
																Closeable,
																RelaySinkInterface
{
	private ServerSocketChannel mServerSocketChannel;

	private ClearVolumeTCPServerSinkRunnable lRunnable;
	private Thread mRunnableThread;

	private final SourceToSinkBufferedAdapter mSourceToSinkBufferedAdapter;

	private final VolumeManager mManager = new VolumeManager(2);
	private volatile Volume mLastVolumeSeen;

	public ClearVolumeTCPServerSink(int pBufferMaxCapacity)
	{
		super();
		mSourceToSinkBufferedAdapter = new SourceToSinkBufferedAdapter(	getManager(),
																		pBufferMaxCapacity);
	}

	public boolean open(SocketAddress pSocketAddress) throws IOException
	{
		if (mServerSocketChannel != null && mServerSocketChannel.isOpen())
			return false;
		mServerSocketChannel = ServerSocketChannel.open();
		mServerSocketChannel.configureBlocking(true);
		mServerSocketChannel.setOption(	StandardSocketOptions.SO_RCVBUF,
										ClearVolumeTCPClient.cSocketBufferLength);
		mServerSocketChannel.socket().bind(pSocketAddress);

		return mServerSocketChannel.isOpen();
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			if (mServerSocketChannel != null && mServerSocketChannel.isOpen())
				mServerSocketChannel.close();

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

		mServerSocketChannel = null;
	}

	public boolean start()
	{
		lRunnable = new ClearVolumeTCPServerSinkRunnable(	this,
															mServerSocketChannel,
															mSourceToSinkBufferedAdapter);
		mRunnableThread = new Thread(	lRunnable,
										ClearVolumeTCPServerSinkRunnable.class.getSimpleName() + "Thread");
		mRunnableThread.setDaemon(true);
		mRunnableThread.start();
		return true;
	}

	public boolean stop()
	{
		lRunnable.requestStop();
		return true;
	}

	@Override
	public void sendVolume(Volume pVolume)
	{
		if (pVolume != null)
		{
			Volume lNewLastSeenVolume;
			if (mLastVolumeSeen == null)
			{
				lNewLastSeenVolume = mManager.requestAndWaitForVolumeLike(	1,
																			TimeUnit.MILLISECONDS,
																			pVolume);
			}
			else
				lNewLastSeenVolume = mLastVolumeSeen;

			lNewLastSeenVolume.copyMetaDataFrom(pVolume);
			lNewLastSeenVolume.copyDataFrom(pVolume);

			mLastVolumeSeen = lNewLastSeenVolume;
			// System.out.println(mLastVolumeSeen);

			final boolean lSucceededInSending = mSourceToSinkBufferedAdapter.sendVolumeWithFeedback(pVolume);
			if (!lSucceededInSending)
				if (getRelaySink() != null)
					getRelaySink().sendVolume(pVolume);
				else
					pVolume.makeAvailableToManager();
		}
	}

	@Override
	public VolumeManager getManager()
	{
		if (getRelaySink() != null)
			return getRelaySink().getManager();
		return null;
	}

	public Volume getLastVolumeSeen()
	{
		return mLastVolumeSeen;
	}

}
