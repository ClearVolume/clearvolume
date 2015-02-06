package clearvolume.audio.synthesizer.sources;

public interface Source
{
	public int getPeriodInSamples();

	public float next();
}
