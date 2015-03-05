package clearvolume.audio.synthesizer.sources;

/**
 * Abstract class providing basic functionality for all Source implementations.
 *
 * @author Loic Royer (2015)
 *
 */
public abstract class SourceBase
{
	/**
	 * Sampling frequency.
	 */
	protected float mSamplingFrequency = 44100f;

	/**
	 * Signal amplitude.
	 */
	private volatile float mAmplitude;

	/**
	 * Default constructor with a default sampling frequency of 44100 Hz and
	 * amplitude of 0.5.
	 */
	public SourceBase()
	{
		super();
	}

	/**
	 * Default constructor with a given amplitude.
	 */
	public SourceBase(float pAmplitude)
	{
		super();
		mAmplitude = pAmplitude;
	}


	/**
	 * Returns the sampling frequency.
	 * 
	 * @return sampling frequency in Hz.
	 */
	public float getSamplingFrequency()
	{
		return mSamplingFrequency;
	}

	/**
	 * Sets the sampling frequency.
	 * 
	 * @param pSamplingFrequency
	 *          sampling frequency in Hz.
	 */
	public void setSamplingFrequency(float pSamplingFrequency)
	{
		mSamplingFrequency = pSamplingFrequency;
	}

	/**
	 * Returns the signal's amplitude.
	 * 
	 * @return the current amplitude of the signal.
	 */
	public float getAmplitude()
	{
		return mAmplitude;
	}

	/**
	 * Sets the signal's amplitude.
	 * 
	 * @param pAmplitude
	 *          amplitude.
	 */
	public void setAmplitude(float pAmplitude)
	{
		mAmplitude = pAmplitude;
	}

}
