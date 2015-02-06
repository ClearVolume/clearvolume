package clearvolume.audio.synthesizer.sources;


public abstract class ToneBase extends SourceBase implements Source
{

	private volatile float mFrequencyInHertz;
	private volatile float mAmplitude;


	public ToneBase()
	{
		this(440f, 0.1f);
	}

	public ToneBase(float pFrequencyInHertz)
	{
		super();
		mFrequencyInHertz = pFrequencyInHertz;
	}

	public ToneBase(float pFrequencyInHertz, float pAmplitude)
	{
		super();
		mFrequencyInHertz = pFrequencyInHertz;
		mAmplitude = pAmplitude;
	}

	public float getFrequencyInHertz()
	{
		return mFrequencyInHertz;
	}

	@Override
	public int getPeriodInSamples()
	{
		return Math.round(getSamplingFrequency() / getFrequencyInHertz());
	}

	public void setFrequencyInHertz(float pFrequencyInHertz)
	{
		mFrequencyInHertz = pFrequencyInHertz;
	}

	public void addFrequencyInHertz(float pDeltaFrequencyHertz)
	{
		mFrequencyInHertz += pDeltaFrequencyHertz;
	}

	public float getAmplitude()
	{
		return mAmplitude;
	}

	public void setAmplitude(float pAmplitude)
	{
		mAmplitude = pAmplitude;
	}


}
