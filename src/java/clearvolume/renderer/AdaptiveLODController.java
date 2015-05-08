package clearvolume.renderer;

import static java.lang.Math.max;
import static java.lang.Math.min;
import clearvolume.utils.math.lowdiscrepancy.ModularSequence;

public class AdaptiveLODController
{

	private static final long cMarginTime = 1000 * 1000 * 100; // 10 ms
	private static final int cMaxNumberOfPasses = 9;

	private volatile boolean mActive = true;
	private volatile boolean mMultiPassRenderingInProgress;

	private volatile int mNewNumberOfPasses;
	private volatile int mNewGenerator;
	private volatile int mCurrentNumberOfPasses;
	private volatile int mCurrentGenerator;
	private volatile int mPassIndex;

	private volatile long mLastUserInputTime = Long.MIN_VALUE;
	private volatile long mFirstPassTimingStartTime = Long.MIN_VALUE,
			mFirstPassTimingStopTime;
	private volatile long mTargetTimeForFirstPassInMilliseconds = 33;
	private volatile long mTargetTimeHysteresis = 5;
	private volatile double mFilteredElapsedTimeInMs = mTargetTimeForFirstPassInMilliseconds;

	public AdaptiveLODController()
	{
		mCurrentNumberOfPasses = 3;
		mCurrentGenerator = 2;
		mNewNumberOfPasses = mCurrentNumberOfPasses;
		mNewGenerator = mCurrentGenerator;
		resetMultiPassRendering();
	}

	public void setActive(boolean pActive)
	{
		mActive = pActive;
	}

	public boolean isActive()
	{
		return mActive;
	}

	public void toggleActive()
	{
		mActive = !mActive;
	}

	public int getNumberOfPasses()
	{
		if (!mActive)
			return 1;
		return mCurrentNumberOfPasses;
	}

	public void setNumberOfPasses(final int pNumberOfPasses)
	{
		mNewNumberOfPasses = min(	cMaxNumberOfPasses,
															max(1, pNumberOfPasses));
		mNewGenerator = ModularSequence.findKWithBestGapScoreCached(pNumberOfPasses);
	}

	public void incrementNumberOfPasses()
	{
		println("incrementNumberOfPasses");
		setNumberOfPasses(getNumberOfPasses() + 1);
	}

	public void decrementNumberOfPasses()
	{
		println("decrementNumberOfPasses");
		setNumberOfPasses(getNumberOfPasses() - 1);
	}

	public boolean isKernelRunNeeded()
	{
		if (!mActive)
			return false;

		return mMultiPassRenderingInProgress;
	}

	public float getPhase()
	{
		if (!mActive)
			return 0;

		final float lPhase = computePhase(getNumberOfPasses(),
																			mCurrentGenerator,
																			mPassIndex);
		println("lPhase=" + lPhase);
		return lPhase;
	}

	public int getPassIndex()
	{
		if (!mActive)
			return 0;
		return mPassIndex;
	}

	public boolean isBufferClearingNeeded()
	{
		if (!mActive)
			return true;

		return mPassIndex == 0;
	}

	public boolean isRedrawNeeded()
	{
		if (!mActive)
			return false;

		return mMultiPassRenderingInProgress;
	}

	public boolean isLastMultiPassRender()
	{
		return false;
	}

	public void renderingParametersOrVolumeDataChanged()
	{
		println(this.getClass().getSimpleName() + ".renderingParametersOrVolumeDataChanged");
		mMultiPassRenderingInProgress = true;
	}

	private void resetMultiPassRendering()
	{
		mPassIndex = 0;
	}

	public boolean beforeRendering()
	{
		if (!mActive)
			return true;

		// println(this.getClass().getSimpleName() + ".beforeRendering");
		if (mMultiPassRenderingInProgress)
		{
			println(this.getClass().getSimpleName() + ".beforeRendering -> multi-pass is active");
			if (isUserInteractionInProgress())
			{
				// multipass rendering needs to restart from scratch:
				println(this.getClass().getSimpleName() + ".beforeRendering -> multi-pass needs to be restarted");
				resetMultiPassRendering();

				mCurrentNumberOfPasses = mNewNumberOfPasses;
				mCurrentGenerator = mNewGenerator;

				println("mCurrentNumberOfPasses=" + mCurrentNumberOfPasses);
				println("mCurrentGenerator=" + mCurrentGenerator);

				mFirstPassTimingStartTime = System.nanoTime();
				return true;
			}
			else
			{
				return proceedWithMultiPass();
			}
		}
		else
		{
			// println(this.getClass().getSimpleName() +
			// ".beforeRendering -> multi-pass not active");
			return false;
		}

	}

	public void afterRendering()
	{
		if (mPassIndex == 0 && mFirstPassTimingStartTime != Long.MIN_VALUE)
		{
			mFirstPassTimingStopTime = System.nanoTime();

			final double lElapsedTimeInMs = ((mFirstPassTimingStopTime - mFirstPassTimingStartTime) * 1e-6);

			mFilteredElapsedTimeInMs = 0.90 * mFilteredElapsedTimeInMs
																	+ 0.1
																	* lElapsedTimeInMs;

			format(	"elapsed= %.3f, filtered=%f \n",
							lElapsedTimeInMs,
							mFilteredElapsedTimeInMs);

			if (mFilteredElapsedTimeInMs < mTargetTimeForFirstPassInMilliseconds - mTargetTimeHysteresis)
			{
				// too fast
				decrementNumberOfPasses();
			}
			else if (mFilteredElapsedTimeInMs > mTargetTimeForFirstPassInMilliseconds + mTargetTimeHysteresis)
			{
				// too slow
				incrementNumberOfPasses();
			}

			mFirstPassTimingStartTime = Long.MIN_VALUE;
		}

	}

	private boolean proceedWithMultiPass()
	{

		// multi-pass continues:
		println(this.getClass().getSimpleName() + ".proceedWithMultiPass -> continues with pass #"
						+ mPassIndex);
		mPassIndex++;
		if (mPassIndex < getNumberOfPasses())
		{
			// still need torender more passes:
			println(this.getClass().getSimpleName() + ".proceedWithMultiPass -> more passes to do");
			// triggerDeamonThreadToRequestRender();
			return false;
		}
		else
		{
			// we are done:
			println(this.getClass().getSimpleName() + ".proceedWithMultiPass -> all passes done! finished!");
			mMultiPassRenderingInProgress = false;

			resetMultiPassRendering();
			return true;
		}
	}

	private static float computePhase(int pNumberOfPasses,
																		int pGenerator,
																		int pPassIndex)
	{
		final float lPhase = (((float) ((pPassIndex * pGenerator) % pNumberOfPasses))) / pNumberOfPasses;
		return lPhase;
	}

	public void notifyUserInteractionInProgress()
	{
		mLastUserInputTime = System.nanoTime();
	}

	public void notifyUserInteractionEnded()
	{
		mLastUserInputTime = Long.MIN_VALUE;
	}

	public void requestDisplay()
	{
		notifyUserInteractionInProgress();
	}

	public boolean isUserInteractionInProgress()
	{
		if (System.nanoTime() > mLastUserInputTime + cMarginTime)
			return false;
		return true;
	}

	private void println(String pString)
	{
		System.out.println(pString);
	}

	private void format(String format, Object... args)
	{
		System.out.format(format, args);
	}

}
