package clearvolume.volume.sink.timeshift;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SwitchableSoftReferenceManager<T>
{
	private static final long cCleanUpPeriodInMilliseconds = 1000;

	private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

	private final ReferenceQueue<T> mReferenceQueue = new ReferenceQueue<T>();

	public SwitchableSoftReferenceManager()
	{
		super();
		Runnable lRunnable = () -> {
			while (true)
			{
				try
				{
					while (true)
					{
						Reference<? extends T> lReference = mReferenceQueue.poll();
						if (lReference == null)
							break;

						@SuppressWarnings("unchecked")
						SwitchableSoftReference<T> lSwitchableSoftReference = (SwitchableSoftReference<T>) lReference;

						Runnable lCleanUpRunnable = lSwitchableSoftReference.getCleanUpRunnable();

						if (lCleanUpRunnable != null)
							mExecutor.execute(lCleanUpRunnable);

					}

					Thread.sleep(cCleanUpPeriodInMilliseconds);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
		};
		Thread lCleanUpThread = new Thread(	lRunnable,
																				"SwitchableSoftReferenceCleanUpThread");
		lCleanUpThread.setDaemon(true);
		lCleanUpThread.setPriority(Thread.MIN_PRIORITY);
		lCleanUpThread.start();
	}

	public SwitchableSoftReference<T> wrapReference(T pReferent,
																									Runnable pCleaningRunnable)
	{
		return new SwitchableSoftReference<T>(pReferent,
																					mReferenceQueue,
																					pCleaningRunnable);
	}

}
