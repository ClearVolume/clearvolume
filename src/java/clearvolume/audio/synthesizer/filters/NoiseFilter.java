package clearvolume.audio.synthesizer.filters;

import java.util.concurrent.ThreadLocalRandom;

import clearvolume.audio.synthesizer.sources.Source;

/**
 * Filter that adds additive and multiplicative noise to a source.
 *
 * @author Loic Royer (2015)
 *
 */
public class NoiseFilter extends FilterBase
{

	/**
	 * Default constructor.
	 */
	public NoiseFilter()
	{
		this(0.02f);
	}

	/**
	 * Constructor thats sets the amplitude of the noise.
	 * 
	 * @param pAmplitude
	 *            amplitude
	 */
	public NoiseFilter(float pAmplitude)
	{
		super(pAmplitude);
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		Source lSource = getSource();
		float lInValue = lSource.next();

		ThreadLocalRandom lThreadLocalRandom = ThreadLocalRandom.current();

		double lAdditiveNoise = 2 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lMultiplicativeNoise = 0.2 * (lThreadLocalRandom.nextFloat() - 0.5);
		double lOutValue = lInValue + getAmplitude()
							* (lAdditiveNoise + lInValue * lMultiplicativeNoise);

		return (float) lOutValue;
	}

}
