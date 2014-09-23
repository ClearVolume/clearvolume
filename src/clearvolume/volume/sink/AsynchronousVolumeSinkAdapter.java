package clearvolume.volume.sink;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import clearvolume.volume.Volume;

public class AsynchronousVolumeSinkAdapter implements VolumeSinkInterface
{

	private final VolumeSinkInterface mDelegatedVolumeSink;

	private final BlockingQueue<Volume> mVolumeQueue;

	private volatile boolean mStopSignal;
	private volatile boolean mStoppedSignal;


	public AsynchronousVolumeSinkAdapter(	VolumeSinkInterface pDelegatedVolumeSink,
																				int pMaxCapacity)
	{
		super();
		mDelegatedVolumeSink = pDelegatedVolumeSink;
		mVolumeQueue = new ArrayBlockingQueue<Volume>(pMaxCapacity);
	}

	@Override
	public void sendVolume(Volume pVolume)
	{
		mVolumeQueue.offer(pVolume);
	}

	public boolean start()
	{
		Runnable lRunnable = () ->
		{
			while(!mStopSignal)
			{
				try
				{
					Volume lVolume = mVolumeQueue.take();
					mDelegatedVolumeSink.sendVolume(lVolume);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
			mStoppedSignal=true;
		};
		
		Thread lThread = new Thread(lRunnable, this.getClass().getSimpleName());
		lThread.setDaemon(true);
		lThread.start();
		return true;
	}

	public boolean stop()
	{
		mStopSignal = true;
		return true;
	}

	public boolean waitForStop()
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
		return true;
	}
}
