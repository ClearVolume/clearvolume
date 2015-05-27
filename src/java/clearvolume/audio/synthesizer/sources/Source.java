package clearvolume.audio.synthesizer.sources;

/**
 * A Source is capable or returning a sample and can provide an estimate (if
 * available) of the fundamental period of the generated signal.
 * 
 * @author Loic Royer (2015)
 *
 */
public interface Source
{
	/**
	 * Returns the fundamental period of the signal.
	 * 
	 * @return period in samples
	 */
	public int getPeriodInSamples();

	/**
	 * Returns the next sample in the signal.
	 * 
	 * @return next sample
	 */
	public float next();
}
