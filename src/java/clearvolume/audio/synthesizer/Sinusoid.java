package clearvolume.audio.synthesizer;

public class Sinusoid extends SourceBase implements Source
{

	private volatile float mFrequencyInHertz = 440;

	@Override
	public float next()
	{
		float lPeriodInSamples = getSamplingFrequency() / mFrequencyInHertz;
		return 0;
	}

}
