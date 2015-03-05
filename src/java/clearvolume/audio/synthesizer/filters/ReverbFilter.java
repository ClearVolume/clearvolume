package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

/**
 * Reverb filter. This filter is implemented by using an online convolution
 * kernel.
 *
 * @author Loic Royer (2015)
 *
 */
public class ReverbFilter extends FilterBase
{

	private float[] mReverbBuffer;
	private float[] mWorkBuffer;

	/**
	 * Default constructor.
	 */
	public ReverbFilter()
	{
		this(1, 0.001f);
	}

	/**
	 * Constructor that takes the amplitude of the reverb contribution.
	 */
	public ReverbFilter(float pAmplitude)
	{
		this(pAmplitude, 0.001f);
	}

	/**
	 * Constructor that takes the amplitude of the reverb contribution as well as
	 * the reverb period in seconds.
	 * 
	 * @param pAmplitude
	 *          reverb amplitude
	 * @param pReverbPeriodInSeconds
	 *          reverb period in seconds
	 */
	public ReverbFilter(float pAmplitude, float pReverbPeriodInSeconds)
	{
		super(pAmplitude);

		int lReverbPeriodInSamples = Math.round(getSamplingFrequency() * pReverbPeriodInSeconds);
		mReverbBuffer = new float[lReverbPeriodInSamples];
		mWorkBuffer = new float[lReverbPeriodInSamples];
		setDefaulKernel();
	}

	/**
	 * Sets the reverb kernel to a dirac i.e. no reverb.
	 */
	public void setDiracKernel()
	{
		mReverbBuffer[0] = 1;

		mReverbBuffer[mReverbBuffer.length / 2] = 0.5f;
	}

	/**
	 * Default reverb kernel.
	 */
	public void setDefaulKernel()
	{

		for (int r = 0; r < mReverbBuffer.length / 4; r++)
		{
			mReverbBuffer[0] = 1;
			mReverbBuffer[mReverbBuffer.length / 2] = getAmplitude();

			mReverbBuffer[0] = 0.5f * (mReverbBuffer[0] + mReverbBuffer[1]);
			for (int j = 1; j < mReverbBuffer.length - 1; j++)
				mReverbBuffer[j] = 0.99f * (mReverbBuffer[j - 1] + 2
																		* mReverbBuffer[j] + mReverbBuffer[j + 1]) / 4;/**/
			mReverbBuffer[mReverbBuffer.length - 1] = 0.5f * (mReverbBuffer[mReverbBuffer.length - 2] + mReverbBuffer[mReverbBuffer.length - 1]);
		}

		mReverbBuffer[0] = 1;
	}

	/**
	 * Sets the reverb kernel in the form of a float array.
	 * 
	 * @param pReverbKernel
	 */
	public void setReverbKernel(float[] pReverbKernel)
	{
		mReverbBuffer = pReverbKernel;
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		Source lSource = getSource();
		float lInSample = lSource.next();

		// System.out.println(lInSample);

		for (int i = 0; i < mWorkBuffer.length; i++)
			mWorkBuffer[i] += lInSample * mReverbBuffer[i];

		float lOutSample = mWorkBuffer[0];

		for (int i = 0; i < mWorkBuffer.length - 1; i++)
			mWorkBuffer[i] = mWorkBuffer[i + 1];
		mWorkBuffer[mWorkBuffer.length - 1] = 0;

		/*if (lInSample != lOutSample)
			System.out.println("DIFFERENT");/**/

		return lOutSample;
	}

}
