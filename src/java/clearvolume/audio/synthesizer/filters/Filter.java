package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

public interface Filter extends Source
{
	public void setSource(Source pSource);

	public Source getSource();
}
