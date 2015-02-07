package clearvolume.audio.synthesizer.sources;

/**
 * Abstract class for all sources that generate a signal with a defined
 * fundamental frequency or tone and with a certain amplitude.
 *
 * @author Loic Royer (2015)
 *
 */
public abstract class ToneBase extends SourceBase implements Source
{

	private static final float cMinimalFrequency = 1f;

	private volatile float mFrequencyInHertz;

	/**
	 * Default constructor with a fundamental frequency of 440Hz and an amplitude.
	 * of
	 */
	public ToneBase()
	{
		this(440f, 0.1f);
	}

	/**
	 * Constructor taking the fundamental frequency as parameter.
	 * 
	 * @param pFrequencyInHertz
	 *          frequency in Hz.
	 */
	public ToneBase(float pFrequencyInHertz)
	{
		super();
		mFrequencyInHertz = pFrequencyInHertz;
	}

	/**
	 * Constructor taking the fundamental frequency and amplitude as parameters.
	 * 
	 * @param pFrequencyInHertz
	 *          frequency in Hz.
	 * @param pAmplitude
	 *          amplitude.
	 */
	public ToneBase(float pFrequencyInHertz, float pAmplitude)
	{
		super(pAmplitude);
		mFrequencyInHertz = pFrequencyInHertz;
	}

	/**
	 * Returns the frequency in Hz.
	 * 
	 * @return frequency in Hz.
	 */
	public float getFrequencyInHertz()
	{
		return mFrequencyInHertz;
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#getPeriodInSamples()
	 */
	@Override
	public int getPeriodInSamples()
	{
		return Math.round(getSamplingFrequency() / getFrequencyInHertz());
	}

	/**
	 * Sets the frequency in Hz.
	 * 
	 * @param pFrequencyInHertz
	 *          frequency in Hz.
	 */
	public void setFrequencyInHertz(float pFrequencyInHertz)
	{
		if (pFrequencyInHertz < cMinimalFrequency)
			pFrequencyInHertz = cMinimalFrequency;
		mFrequencyInHertz = pFrequencyInHertz;
	}

}
