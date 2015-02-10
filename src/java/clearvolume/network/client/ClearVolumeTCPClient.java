package clearvolume.network.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeTCPClient implements AutoCloseable
{

	public static final int cSocketBufferLength = 64 * 1024 * 1024;
	private static final int cMaxInUseVolumes = 20;
	private final VolumeSinkInterface mVolumeSink;
	private SocketChannel mSocketChannel;

	private ClearVolumeTCPClientRunnable lRunnable;
	private Thread mRunnableThread;

	public ClearVolumeTCPClient(VolumeSinkInterface pVolumeSink)
	{
		super();
		mVolumeSink = pVolumeSink;
	}

	public boolean open(SocketAddress pSocketAddress) throws IOException
	{
		if (mSocketChannel != null && mSocketChannel.isConnected())
			return false;
		mSocketChannel = SocketChannel.open();
		mSocketChannel.configureBlocking(true);
		mSocketChannel.socket().setReceiveBufferSize(cSocketBufferLength);
		final boolean lConnected = mSocketChannel.connect(pSocketAddress);

		return lConnected;
	}

	@Override
	public void close() throws IOException
	{
		if (mSocketChannel == null || !mSocketChannel.isConnected())
		{
			mSocketChannel = null;
		}

		mSocketChannel.close();
		mSocketChannel = null;
	}

	public boolean start()
	{
		lRunnable = new ClearVolumeTCPClientRunnable(	mSocketChannel,
																									mVolumeSink,
																									cMaxInUseVolumes);
		mRunnableThread = new Thread(	lRunnable,
																	ClearVolumeTCPClientRunnable.class.getSimpleName() + "Thread");
		mRunnableThread.setDaemon(true);
		mRunnableThread.start();
		return true;
	}

	public boolean stop()
	{
		lRunnable.requestStop();
		return true;
	}

}
