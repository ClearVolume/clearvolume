package clearvolume.audio.audioplot;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

import java.util.concurrent.locks.ReentrantLock;

import clearvolume.audio.sound.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
import clearvolume.audio.synthesizer.filters.LowPassFilter;
import clearvolume.audio.synthesizer.filters.NoiseFilter;
import clearvolume.audio.synthesizer.filters.ReverbFilter;
import clearvolume.audio.synthesizer.filters.WarmifyFilter;
import clearvolume.audio.synthesizer.sources.Guitar;

/**
 * AudioPlot - this class implements the mapping of a size in the range [0,1]
 * into an audio range represented by varying beats per second and tone
 * frequency.
 *
 * This class is thread safe and can be directly used in a multi-threaded
 * environment.
 *
 * @author Loic Royer (2015)
 *
 */
public class AudioPlot implements AutoCloseable
{

	private double mSlowPeriod;
	private double mFastPeriod;
	private double mLowFreq;
	private double mHighFreq;
	private boolean mInvertRange;

	private volatile SoundOut mSoundOut;

	private Runnable mDeamonThreadRunnable;
	private volatile boolean mStopSignal = false;
	private volatile boolean mThreadStoppedSignal = true;
	private ReentrantLock mReentrantLock = new ReentrantLock();

	private Guitar mGuitar;

	private volatile double mPeriodInSeconds = 1;
	private volatile Thread mThread;
	private Object mStartStopLock = new Object();

	/**
	 * Default constructor.
	 */
	public AudioPlot()
	{
		this(1, 0.1, 110, 110 * 4, false);
	}

	/**
	 * Constructor that takes: the slow and fast period, and the low and high
	 * frequency.
	 * 
	 * @param pSlowPeriod
	 *          slow period
	 * @param pFastPeriod
	 *          fast period
	 * @param pLowFreqHz
	 *          low frequency
	 * @param pHighFreqHz
	 *          high frequency
	 */
	public AudioPlot(	double pSlowPeriod,
										double pFastPeriod,
										double pLowFreqHz,
										double pHighFreqHz)
	{
		this(pSlowPeriod, pFastPeriod, pLowFreqHz, pHighFreqHz, false);
	}

	/**
	 * Constructor that takes: the slow and fast period, the low and high
	 * frequency, and a flag thatcontrols whether the range is inverted or not.
	 * 
	 * @param pSlowPeriod
	 *          slow period
	 * @param pFastPeriod
	 *          fast period
	 * @param pLowFreqHz
	 *          low frequency
	 * @param pHighFreqHz
	 *          high frequency
	 *
	 * @param pInvertRange
	 *          if true the audio range is inverted with respect to the default
	 */
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

		ReverbFilter lReverbFilter = new ReverbFilter(1, 0.001f);
		lReverbFilter.setSource(lWarmifyFilter);/**/

		LowPassFilter lLowPassFilter = new LowPassFilter();
		lLowPassFilter.setSource(lReverbFilter);

		mGuitar.setAmplitude(0.5f);

		mSoundOut = new SoundOut();

		final Synthesizer lSynthesizer = new Synthesizer(	lLowPassFilter,
																											mSoundOut);

		mDeamonThreadRunnable = new Runnable()
		{
			private volatile long mNewDeadline = Long.MIN_VALUE;

			@Override
			public void run()
			{
				try
				{
					mStopSignal = false;
					mThreadStoppedSignal = false;
					mSoundOut.start();
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

					mSoundOut.stop();
					mThreadStoppedSignal = true;

				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}

			}
		};

	}

	/**
	 * starts playing the sound.
	 */
	public void start()
	{
		synchronized (mStartStopLock)
		{
			waitForStop();

			mThread = new Thread(	mDeamonThreadRunnable,
														AudioPlot.class.getSimpleName() + ".PlayThread");
			mThread.setDaemon(true);
			mThread.start();
		}

	}

	public void waitForStop()
	{
		while (!mThreadStoppedSignal)
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
	};

	/**
	 * Stops.
	 */
	public void stop()
	{
		synchronized (mStartStopLock)
		{
			mStopSignal = true;
			mThread = null;
		}
	}

	/**
	 * Sets the new current size. The size must be normalized within the range
	 * [0,1].
	 * 
	 * @param pValue
	 *          size within range [0,1]
	 */
	public void setValue(double pValue)
	{
		pValue = max(min(pValue, 1), 0);
		if (Double.isNaN(pValue) || Double.isInfinite(pValue))
			pValue = 0;

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

	/**
	 * Returns true if the audio range is inverted.
	 * 
	 * @return true if inverted.
	 */
	public boolean isInvertRange()
	{
		return mInvertRange;
	}

	/**
	 * Sets the flag that determines whether the audio range is inverted.
	 * 
	 * @param pInvertRange
	 *          true to invert range
	 */
	public void setInvertRange(boolean pInvertRange)
	{
		mInvertRange = pInvertRange;
	}

	public void close()
	{
		stop();
		waitForStop();
		mSoundOut.close();
	}

}
