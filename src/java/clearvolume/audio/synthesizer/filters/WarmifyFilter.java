package clearvolume.audio.synthesizer.filters;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;

import clearvolume.audio.synthesizer.sources.Source;

/**
 * Warmify filter that adds harmonics to the signal by adding some fractional
 * power of the samples.
 *
 * @author Loic Royer (2015)
 *
 */
public class WarmifyFilter extends FilterBase
{

	private volatile float mPower;

	/**
	 * Default constructor. Amplitude = 1 and power = 2.
	 */
	public WarmifyFilter()
	{
		this(1, 2);
	}

	/**
	 * Constructor that takes the power exponent and has a default amplitude of
	 * 0.2.
	 * 
	 * @param pPower
	 *            power
	 */
	public WarmifyFilter(float pPower)
	{
		this(0.2f, pPower);
	}

	/**
	 * Constructor that takes the amplitude and the power exponent.
	 * 
	 * @param pApmitude
	 *            amplitude
	 * @param pPower
	 *            power exponent
	 */
	public WarmifyFilter(float pApmitude, float pPower)
	{
		super(pApmitude);
		mPower = pPower;
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		Source lSource = getSource();
		float lInSample = lSource.next();
		float lOutSample = (float) (lInSample + getAmplitude() * (signum(lInSample) * pow(	abs(lInSample),
																							mPower)));
		return lOutSample;
	}

	/**
	 * Returns the power exponent of this filter.
	 * 
	 * @return power power exponent
	 */
	public double getPower()
	{
		return mPower;
	}

	/**
	 * Sets the power exponent of this filter.
	 * 
	 * @param pPower
	 *            power exponent
	 */
	public void setPower(float pPower)
	{
		mPower = pPower;
	}

}
