package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

/**
 * Filter that remomes the DC component
 *
 * @author Loic Royer (2015)
 *
 */
public class DCRemoverFilter extends FilterBase
{

	private final LowPassFilter mLowPassFilter;

	/**
	 * Default constructor with alpha = 0.1.
	 */
	public DCRemoverFilter()
	{
		this(0.0001f);
	}

	/**
	 * Constructor that allows to set alpha for the low-pass filter that is used
	 * to remove the DC component.
	 * 
	 * @param pAlpha
	 *          alpha
	 */
	public DCRemoverFilter(float pAlpha)
	{
		this(1, pAlpha);
	}

	/**
	 * Constructor that allows to set the amplitude and alpha for the low-pass
	 * filter that is used to remove the DC component.
	 * 
	 * @param pAmplitude
	 *          amplitude
	 * @param pAlpha
	 *          alpha
	 */
	public DCRemoverFilter(float pAmplitude, float pAlpha)
	{
		super(pAmplitude);
		mLowPassFilter = new LowPassFilter(pAlpha);
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public void setSource(Source pSource)
	{
		super.setSource(pSource);
		mLowPassFilter.setSource(pSource);
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		final Source lSource = getSource();
		final float lInSample = lSource.next();

		final float lOutSample = lInSample - getAmplitude()
															* mLowPassFilter.next();

		return lOutSample;
	}

	/**
	 * Returns alpha.
	 * 
	 * @return alpha
	 */
	public double getAlpha()
	{
		return mLowPassFilter.getAlpha();
	}

	/**
	 * Sets alpha.
	 * 
	 * @param pAlpha
	 *          alpha
	 */
	public void setAlpha(double pAlpha)
	{
		mLowPassFilter.setAlpha(pAlpha);
	}

}
