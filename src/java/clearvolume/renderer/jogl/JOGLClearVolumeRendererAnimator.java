package clearvolume.renderer.jogl;

import java.util.ArrayList;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.AnimatorBase;

public class JOGLClearVolumeRendererAnimator extends AnimatorBase	implements
																																	AutoCloseable
{

	private final ArrayList<GLAutoDrawable> mGLAutoDrawableArrayList = new ArrayList<GLAutoDrawable>();

	private final JOGLClearVolumeRenderer mJoglClearVolumeRenderer;

	private final Thread mDisplayRequestDeamonThread;
	private volatile boolean mDeamonThreadStopSignal = false;
	private volatile boolean mDeamonThreadTriggerSignal = false;

	private final Object mLock = new Object();

	private final Runnable mDisplayRequestDeamonRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			while (!mDeamonThreadStopSignal)
			{
				while (!mDeamonThreadTriggerSignal)
				{
					try
					{
						Thread.sleep(5);
					}
					catch (final InterruptedException e)
					{
					}
				}
				synchronized (mLock)
				{
					mDeamonThreadTriggerSignal = false;
					System.out.println("mDisplayRequestDeamonRunnable.requestDisplayInternal");
					mJoglClearVolumeRenderer.display();
				}
				Thread.yield();
			}

		}
	};

	public JOGLClearVolumeRendererAnimator(JOGLClearVolumeRenderer pJoglClearVolumeRenderer)
	{
		mJoglClearVolumeRenderer = pJoglClearVolumeRenderer;
		mDisplayRequestDeamonThread = new Thread(	mDisplayRequestDeamonRunnable,
																							JOGLClearVolumeRenderer.class.getSimpleName() + ".DisplayRequestDeamon");
		mDisplayRequestDeamonThread.setDaemon(true);
		mDisplayRequestDeamonThread.start();
	}

	@Override
	public boolean isAnimating()
	{
		return true;
	}

	@Override
	public boolean isPaused()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean start()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pause()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resume()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws Exception
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected String getBaseName(String pPrefix)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
