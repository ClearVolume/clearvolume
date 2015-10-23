package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

/**
 * Low pass filter based on a exponential moving average. The smoothing
 * parameter alpha needs to be provided.
 * 
 * y(t) = (1-alpha)*y(t-1) + alpha*x(t)
 *
 * @author Loic Royer (2015)
 *
 */
public class LowPassFilter extends FilterBase
{

	private volatile double mValue;
	private volatile double mAlpha;

	/**
	 * Default constructor with alpha = 0.1.
	 */
	public LowPassFilter()
	{
		this(0.1f);
	}

	/**
	 * Constructor that allows to set alpha.
	 * 
	 * @param pAlpha
	 *            alpha
	 */
	public LowPassFilter(double pAlpha)
	{
		super();
		mAlpha = pAlpha;
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		Source lSource = getSource();
		float lSample = lSource.next();

		mValue = getAlpha() * lSample + (1 - getAlpha()) * mValue;

		return (float) mValue;
	}

	/**
	 * Returns alpha.
	 * 
	 * @return alpha
	 */
	public double getAlpha()
	{
		return mAlpha;
	}

	/**
	 * Sets alpha.
	 * 
	 * @param pAlpha
	 *            alpha
	 */
	public void setAlpha(double pAlpha)
	{
		mAlpha = pAlpha;
	}

}
