package clearvolume.audio.synthesizer.filters;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;

import java.util.concurrent.ThreadLocalRandom;

import clearvolume.audio.synthesizer.sources.Source;

public class NoiseFilter extends FilterBase
{

	double mAmplitude = 0.1;

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

		double lAdditiveNoise = 0.2 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lMultiplicativeNoise = 0.02 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lPowerNoise = 1 + 0.002 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lOutValue = lInValue + mAmplitude
												* (lAdditiveNoise * lInValue
														+ lMultiplicativeNoise
														* lInValue + signum(lInValue) * pow(abs(lInValue),
																																lPowerNoise));

		return (float) lOutValue;
	}

}
