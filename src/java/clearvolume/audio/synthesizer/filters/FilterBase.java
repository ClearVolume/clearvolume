package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;
import clearvolume.audio.synthesizer.sources.SourceBase;

public abstract class FilterBase extends SourceBase implements Source
{
	private Source mSource;

	public void setSource(Source pSource)
	{
		mSource = pSource;
	};

	public Source getSource()
	{
		return mSource;
	}

	@Override
	public int getPeriodInSamples()
	{
		return getSource().getPeriodInSamples();
	}
}
