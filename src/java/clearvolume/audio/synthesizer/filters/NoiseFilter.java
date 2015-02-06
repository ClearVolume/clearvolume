package clearvolume.audio.synthesizer.filters;

import static java.lang.Math.pow;

import java.util.concurrent.ThreadLocalRandom;

import clearvolume.audio.synthesizer.sources.Source;

public class WarmFilter extends FilterBase
{

	@Override
	public float next()
	{
		Source lSource = getSource();
		float lInValue = lSource.next();

		ThreadLocalRandom lThreadLocalRandom = ThreadLocalRandom.current();

		double lAdditiveNoise = 0.1 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lMultiplicativeNoise = 0.01 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lPowerNoise = 0.001 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lOutValue = lInValue + lAdditiveNoise
												* lInValue
												+ lMultiplicativeNoise
												* lInValue
												+ pow(lInValue, lPowerNoise);

		return (float) lOutValue;
	}

}
