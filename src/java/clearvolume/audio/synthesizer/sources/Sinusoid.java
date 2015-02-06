package clearvolume.audio.synthesizer.sources;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public class Sinusoid extends SourceBase implements Source
{

	private volatile float mFrequencyInHertz = 440;

	private volatile float mAmplitude = 0.1f;

	public Sinusoid()
	{
		super();
	}

	public Sinusoid(float pFrequencyInHertz)
	{
		super();
		mFrequencyInHertz = pFrequencyInHertz;
	}

	public Sinusoid(float pFrequencyInHertz, float pAmplitude)
	{
		super();
		mFrequencyInHertz = pFrequencyInHertz;
		mAmplitude = pAmplitude;
	}

	public float getFrequencyInHertz()
	{
		return mFrequencyInHertz;
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

	@Override
	public float next()
	{
		float lTime = getTimeAndIncrement();
		float lPeriodInSamples = getSamplingFrequency() / getFrequencyInHertz();
		return (float) (getAmplitude() * sin((2 * PI * (lTime++)) / lPeriodInSamples));
	}

}
