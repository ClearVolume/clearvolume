package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

/**
 * A filter is a source that relays the samples from an other source and
 * processes/filters them.
 *
 * @author Loic Royer (2015)
 *
 */
public interface Filter extends Source
{
	/**
	 * Sets the filetered source .
	 * 
	 * @param pSource
	 *            filtered source
	 */
	public void setSource(Source pSource);

	/**
	 * Returns the filtered source.
	 * 
	 * @return filtered source.
	 */
	public Source getSource();
}
