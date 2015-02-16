package clearvolume.renderer.jogl;

public class AdaptiveLODController implements AutoCloseable
{

	private final int[] cFibonacci = new int[]
	{ 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144 };

	private final JOGLClearVolumeRenderer mJoglClearVolumeRenderer;

	private volatile boolean mMultiPassRenderingInProgress;
	private volatile boolean mDisplayRequestReceived;
	private volatile boolean mRenderingParametersOrVolumeDataChanged = true;

	private volatile int mFibonacciPassNumber;
	private volatile int mGenerator;
	private volatile int mPassIndex;

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

	public AdaptiveLODController(JOGLClearVolumeRenderer pJoglClearVolumeRenderer)
	{
		mJoglClearVolumeRenderer = pJoglClearVolumeRenderer;
		setFibonacciPassNumber(6);

		mDisplayRequestDeamonThread = new Thread(	mDisplayRequestDeamonRunnable,
																							JOGLClearVolumeRenderer.class.getSimpleName() + ".DisplayRequestDeamon");
		mDisplayRequestDeamonThread.setDaemon(true);
		mDisplayRequestDeamonThread.start();
	}

	private void setFibonacciPassNumber(final int pFibonacciPassNumber)
	{
		mFibonacciPassNumber = pFibonacciPassNumber;
		mGenerator = cFibonacci[mFibonacciPassNumber - 1];
		resetMultiPassRendering();
	}

	public void requestDisplay()
	{
		System.out.println(this.getClass().getSimpleName() + ".requestDisplay");

		if (mMultiPassRenderingInProgress)
		{
			mDisplayRequestReceived = true;
			System.out.println(this.getClass().getSimpleName() + ".requestDisplay -> multi pass active setting mDisplayRequestReceived flag to true");
		}
		else
			triggerDeamonThreadToRequestRender();

	}

	private void triggerDeamonThreadToRequestRender()
	{
		System.out.println(this.getClass().getSimpleName() + ".triggerDeamonThreadToRequestRender");
		mDisplayRequestReceived = false;
		mDeamonThreadTriggerSignal = true;
	}

	public boolean isKernelRunNeeded()
	{
		return mMultiPassRenderingInProgress;
	}

	public float getPhase()
	{
		return computePhase(getNumberOfPasses(), mGenerator, mPassIndex);
	}

	public int getNumberOfPasses()
	{
		return cFibonacci[mFibonacciPassNumber];
	}

	public boolean isBufferClearingNeeded()
	{
		return mPassIndex == 0;
	}

	public boolean isRedrawNeeded()
	{
		return mMultiPassRenderingInProgress;
	}

	public void renderingParametersOrVolumeDataChanged()
	{
		mMultiPassRenderingInProgress = true;
		mRenderingParametersOrVolumeDataChanged = true;
	}

	private void resetMultiPassRendering()
	{
		mPassIndex = 0;
	}

	public void renderingFinished()
	{
		synchronized (mLock)
		{
			System.out.println(this.getClass().getSimpleName() + ".renderingFinished");
			if (mMultiPassRenderingInProgress)
			{
				System.out.println(this.getClass().getSimpleName() + ".renderingFinished -> multi-pass is active");
				if (mDisplayRequestReceived && mRenderingParametersOrVolumeDataChanged)
				{
					// multipass rendering needs to restart from scratch:
					System.out.println(this.getClass().getSimpleName() + ".renderingFinished -> multi-pass needs to be restarted");
					resetMultiPassRendering();
					mDisplayRequestReceived = false;
					mRenderingParametersOrVolumeDataChanged = false;
					triggerDeamonThreadToRequestRender();
				}
				else
				{
					proceedWithMultiPass();
				}
			}
			else
			{
				System.out.println(this.getClass().getSimpleName() + ".renderingFinished -> multi-pass not active");
			}

		}
	}

	private void proceedWithMultiPass()
	{
		// multi-pass continues:
		System.out.println(this.getClass().getSimpleName() + ".renderingFinished -> continues with pass #"
												+ mPassIndex);
		mPassIndex++;
		if (mPassIndex < getNumberOfPasses())
		{
			// still need torender more passes:
			System.out.println(this.getClass().getSimpleName() + ".renderingFinished -> more passes to do");
			triggerDeamonThreadToRequestRender();
		}
		else
		{
			// we are done:
			System.out.println(this.getClass().getSimpleName() + ".renderingFinished -> all passes done! finished!");
			mMultiPassRenderingInProgress = false;
			resetMultiPassRendering();
		}
	}

	private static float computePhase(int pNumberOfPasses,
																		int pGenerator,
																		int pPassIndex)
	{
		final float lPhase = ((float) (pPassIndex * pGenerator) % pNumberOfPasses) / pNumberOfPasses;
		return lPhase;
	}

	@Override
	public void close() throws Exception
	{
		mDeamonThreadStopSignal = true;
	}

}
