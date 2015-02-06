package clearvolume.audio.synthesizer;

public abstract class SourceBase
{
	protected float mSamplingFrequency = 44100f;

	protected float mSourceTime = 0;

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

	


}
