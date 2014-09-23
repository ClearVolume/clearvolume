package clearvolume.network.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeTCPClient implements Closeable
{

	private VolumeSinkInterface mVolumeSink;
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
		boolean lConnected = mSocketChannel.connect(pSocketAddress);
		
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
																									mVolumeSink);
		mRunnableThread = new Thread(	lRunnable,
																	"ClearVolumeClientRunnableThread");
		mRunnableThread.setDaemon(true);
		mRunnableThread.start();
		return true;
	}

	public boolean stop()
	{
		lRunnable.requestStop();
		lRunnable.waitForStop();
		return true;
	}


}
