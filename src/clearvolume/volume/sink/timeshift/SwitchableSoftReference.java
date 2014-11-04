package clearvolume.volume.sink.timeshift;

import java.lang.ref.SoftReference;

public class SwitchableSoftReference<T> extends SoftReference<T>
{
	private volatile T mHardReference;

	public SwitchableSoftReference(T pReferent)
	{
		super(pReferent);
		mHardReference = pReferent;
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

}
