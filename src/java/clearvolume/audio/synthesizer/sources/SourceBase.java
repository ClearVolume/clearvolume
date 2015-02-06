package clearvolume.audio.synthesizer.sources;

public abstract class SourceBase
{
	protected float mSamplingFrequency = 44100f;

	private volatile long mTime = 0;

	public SourceBase()
	{
		super();
	}

	public SourceBase(float pSamplingFrequency)
	{
		super();
		setSamplingFrequency(pSamplingFrequency);
	}

	public float getSamplingFrequency()
	{
		return mSamplingFrequency;
	}

	public void setSamplingFrequency(float pSamplingFrequency)
	{
		mSamplingFrequency = pSamplingFrequency;
	}

	public long getTime()
	{
		return mTime;
	}

	public long getTimeAndIncrement()
	{
		return mTime++;
	}

	public void setSourceTime(long pTime)
	{
		mTime = pTime;
	}

	


}
