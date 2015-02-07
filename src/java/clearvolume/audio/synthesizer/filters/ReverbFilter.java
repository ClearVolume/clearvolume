package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

public class ReverbFilter extends FilterBase
{

	private float[] mReverbBuffer;
	private float[] mWorkBuffer;

	public ReverbFilter()
	{
		this(0.001f);
	}

	public ReverbFilter(float pReverbPeriodInSeconds)
	{
		super();

		int lReverbPeriodInSamples = Math.round(getSamplingFrequency() * pReverbPeriodInSeconds);
		mReverbBuffer = new float[lReverbPeriodInSamples];
		mWorkBuffer = new float[lReverbPeriodInSamples];
		setDefaulKernel();
	}

	public void setDiracKernel()
	{
		mReverbBuffer[0] = 1;

		mReverbBuffer[mReverbBuffer.length / 2] = 0.5f;
	}

	public void setDefaulKernel()
	{

		for (int r = 0; r < mReverbBuffer.length / 4; r++)
		{
			mReverbBuffer[0] = 1;
			mReverbBuffer[mReverbBuffer.length / 2] = 0.5f;

			mReverbBuffer[0] = 0.5f * (mReverbBuffer[0] + mReverbBuffer[1]);
			for (int j = 1; j < mReverbBuffer.length - 1; j++)
				mReverbBuffer[j] = 0.99f * (mReverbBuffer[j - 1] + 2
														* mReverbBuffer[j] + mReverbBuffer[j + 1]) / 4;/**/
			mReverbBuffer[mReverbBuffer.length - 1] = 0.5f * (mReverbBuffer[mReverbBuffer.length - 2] + mReverbBuffer[mReverbBuffer.length - 1]);
		}

		mReverbBuffer[0] = 1;
	}

	public void setReverbKernel(float[] pReverbKernel)
	{
		mReverbBuffer = pReverbKernel;
	}

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
