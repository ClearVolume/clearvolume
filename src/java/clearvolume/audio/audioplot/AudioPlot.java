package clearvolume.audio.audioplot;

import static java.lang.Math.pow;

import java.util.concurrent.locks.ReentrantLock;

import clearvolume.audio.sound.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
import clearvolume.audio.synthesizer.filters.LowPassFilter;
import clearvolume.audio.synthesizer.filters.NoiseFilter;
import clearvolume.audio.synthesizer.filters.ReverbFilter;
import clearvolume.audio.synthesizer.filters.WarmifyFilter;
import clearvolume.audio.synthesizer.sources.Guitar;

public class AudioPlot
{

	private double mSlowPeriod;
	private double mFastPeriod;
	private double mLowFreq;
	private double mHighFreq;
	private boolean mInvertRange;

	private Runnable mDeamonThreadRunnable;
	private volatile boolean mStopSignal = false;
	private ReentrantLock mReentrantLock = new ReentrantLock();

	private Guitar mGuitar;

	private volatile double mPeriodInSeconds = 1;

	public AudioPlot()
	{
		this(1, 0.1, 110, 110 * 4, false);
	}

	public AudioPlot(	double pSlowPeriod,
										double pFastPeriod,
										double pLowFreqHz,
										double pHighFreqHz)
	{
		this(pSlowPeriod, pFastPeriod, pLowFreqHz, pHighFreqHz, false);
	}

	public AudioPlot(	double pSlowPeriod,
										double pFastPeriod,
										double pLowFreqHz,
										double pHighFreqHz,
										boolean pInvertRange)
	{
		super();
		mSlowPeriod = pSlowPeriod;
		mFastPeriod = pFastPeriod;
		mLowFreq = pLowFreqHz;
		mHighFreq = pHighFreqHz;
		setInvertRange(pInvertRange);

		mGuitar = new Guitar();

		NoiseFilter lNoiseFilter = new NoiseFilter(0.01f);
		lNoiseFilter.setSource(mGuitar);

		WarmifyFilter lWarmifyFilter = new WarmifyFilter(1f);
		lWarmifyFilter.setSource(lNoiseFilter);

		ReverbFilter lReverbFilter = new ReverbFilter(0.001f);
		lReverbFilter.setSource(lWarmifyFilter);/**/

		LowPassFilter lLowPassFilter = new LowPassFilter();
		lLowPassFilter.setSource(lReverbFilter);

		mGuitar.setAmplitude(0.5f);

		final SoundOut lSoundOut = new SoundOut();

		final Synthesizer lSynthesizer = new Synthesizer(	lLowPassFilter,
																											lSoundOut);

		mDeamonThreadRunnable = new Runnable()
		{
			private volatile long mNewDeadline = Long.MIN_VALUE;

			@Override
			public void run()
			{
				try
				{
					lSoundOut.start();
					while (!mStopSignal)
					{
						long lTimeNow;

						while ((lTimeNow = System.nanoTime()) < mNewDeadline)
						{
							try
							{
								mReentrantLock.lock();
								lSynthesizer.playSamples(128, mReentrantLock);
							}
							finally
							{
								if (mReentrantLock.isHeldByCurrentThread())
									mReentrantLock.unlock();
							}
							Thread.yield();
						}

						mReentrantLock.lock();
						try
						{
							mGuitar.strike(0.5f);
							long lPeriodInNanoseconds = (long) ((1000L * 1000L * 1000L) * mPeriodInSeconds);
							// System.out.println("mPeriodInSeconds=" + mPeriodInSeconds);
							// System.out.println("lPeriodInNanoseconds=" +
							// lPeriodInNanoseconds);
							mNewDeadline = lTimeNow + lPeriodInNanoseconds;

						}
						finally
						{
							if (mReentrantLock.isHeldByCurrentThread())
								mReentrantLock.unlock();
						}
						Thread.yield();

					}
					lSoundOut.stop();
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}

			}
		};

	}

	public void start()
	{
		Thread lThread = new Thread(mDeamonThreadRunnable,
																AudioPlot.class.getSimpleName() + ".PlayThread");
		lThread.setDaemon(true);
		lThread.start();
	};

	public void stop()
	{
		mStopSignal = true;
	}

	public void setValue(double pValue)
	{
		if (isInvertRange())
			pValue = 1 - pValue;

		double lValueQuadratic = pow(pValue, 2);
		double lValueCubic = pow(pValue, 3);

		float lFrequency = (float) (mLowFreq + lValueCubic * (mHighFreq - mLowFreq));
		double lBeatPeriodInSeconds = (float) (mSlowPeriod + lValueQuadratic * (mFastPeriod - mSlowPeriod));

		try
		{
			mReentrantLock.lock();
			/*System.out.format("f=%g, bpis=%g \n",
												lFrequency,
												lBeatPeriodInSeconds);/**/
			mGuitar.setFrequencyInHertz(lFrequency);
			mPeriodInSeconds = lBeatPeriodInSeconds;
		}
		finally
		{
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}

	}

	public boolean isInvertRange()
	{
		return mInvertRange;
	}

	public void setInvertRange(boolean pInvertRange)
	{
		mInvertRange = pInvertRange;
	}

}
