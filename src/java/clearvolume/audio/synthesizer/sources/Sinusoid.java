package clearvolume.audio.synthesizer.sources;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public class Sinusoid extends ToneBase implements Source
{

	private volatile float mTime = 0;

	public Sinusoid()
	{
		super();
	}

	public Sinusoid(float pFrequency)
	{
		super(pFrequency);
	}


	@Override
	public float next()
	{
		float lSample = (float) (getAmplitude() * sin(mTime));

		float lPeriodInSamples = getSamplingFrequency() / getFrequencyInHertz();
		mTime += (2 * PI) / lPeriodInSamples;
		if (mTime > 2 * PI)
			mTime -= 2 * PI;

		return lSample;
	}

}
