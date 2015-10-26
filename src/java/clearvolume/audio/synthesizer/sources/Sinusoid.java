package clearvolume.audio.synthesizer.sources;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

/**
 * Sinusoid source.
 *
 * @author Loic Royer (2015)
 *
 */
public class Sinusoid extends ToneBase implements Source
{

	private volatile float mTime = 0;

	/**
	 * Constructs a sinusoid source with default frequency (440 Hz).
	 */
	public Sinusoid()
	{
		super();
	}

	/**
	 * Constructs a sinusoid source with given frequency.
	 * 
	 * @param pFrequency
	 *            frequency
	 */
	public Sinusoid(float pFrequency)
	{
		super(pFrequency);
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		final float lSample = (float) (getAmplitude() * sin(mTime));

		final float lPeriodInSamples = getSamplingFrequency() / getFrequencyInHertz();
		mTime += (2 * PI) / lPeriodInSamples;
		if (mTime > 2 * PI)
			mTime -= 2 * PI;

		return lSample;
	}

}
