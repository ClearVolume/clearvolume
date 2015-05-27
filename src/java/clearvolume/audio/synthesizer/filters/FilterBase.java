package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;
import clearvolume.audio.synthesizer.sources.SourceBase;

/**
 * Abstract class providing basic filter functionality.
 *
 * @author Loic Royer (2015)
 *
 */
public abstract class FilterBase extends SourceBase implements Filter
{
	private Source mSource;

	/**
	 * Default constructor.
	 */
	public FilterBase()
	{
		super();
	}

	/**
	 * Constructor that takes the amplitude of the filter's signal contribution.
	 * Ideally if the amplitude of the filter is 0, the source samples are
	 * unchanged going through the filter.
	 * 
	 * @param pAmplitude
	 *          amplitude
	 */
	public FilterBase(float pAmplitude)
	{
		super(pAmplitude);
	}

	/**
	 * Sets the filtered source.
	 * 
	 * @param pSource
	 *          filtered source
	 */
	@Override
	public void setSource(Source pSource)
	{
		mSource = pSource;
	};

	/**
	 * Returns the filtered source.
	 * 
	 * @return filetered source
	 */
	@Override
	public Source getSource()
	{
		return mSource;
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#getPeriodInSamples()
	 */
	@Override
	public int getPeriodInSamples()
	{
		return getSource().getPeriodInSamples();
	}
}
