package clearvolume.audio.synthesizer.filters;

import java.util.concurrent.ThreadLocalRandom;

import clearvolume.audio.synthesizer.sources.Source;

public class NoiseFilter extends FilterBase
{

	double mAmplitude;

	public NoiseFilter()
	{
		this(0.02f);
	}

	public NoiseFilter(float pAmplitude)
	{
		super();
		mAmplitude = pAmplitude;
	}

	@Override
	public float next()
	{
		Source lSource = getSource();
		float lInValue = lSource.next();

		ThreadLocalRandom lThreadLocalRandom = ThreadLocalRandom.current();

		double lAdditiveNoise = 2 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lMultiplicativeNoise = 0.2 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lOutValue = lInValue + mAmplitude
												* (lAdditiveNoise + lInValue * lMultiplicativeNoise);

		return (float) lOutValue;
	}

}
