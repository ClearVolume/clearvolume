package clearvolume.volume.sink.timeshift;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;


public class SwitchableSoftReference<T> extends SoftReference<T>
{

	private volatile T mHardReference;

	private final Runnable mCleaningRunnable;

	protected SwitchableSoftReference(T pReferent,
																		ReferenceQueue<T> pReferenceQueue,
																		Runnable pCleaningRunnable)
	{
		super(pReferent, pReferenceQueue);
		mHardReference = pReferent;
		mCleaningRunnable = pCleaningRunnable;
	}

	public SwitchableSoftReference(T pReferent)
	{
		super(pReferent);
		mHardReference = pReferent;
		mCleaningRunnable = null;
	}

	@Override
	public T get()
	{
		if (mHardReference != null)
			return mHardReference;
		else
			return super.get();
	}

	public void soften()
	{
		mHardReference = null;
	}

	public boolean harden()
	{
		T lSoftlyReferred = super.get();
		if (lSoftlyReferred == null)
			return false;
		mHardReference = lSoftlyReferred;
		return true;
	}

	public boolean isGone()
	{
		return get() == null;
	}

	public Runnable getCleanUpRunnable()
	{
		return mCleaningRunnable;
	}

}
