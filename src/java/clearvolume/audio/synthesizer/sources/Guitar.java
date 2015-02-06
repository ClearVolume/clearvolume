package clearvolume.audio.synthesizer.sources;

import java.util.concurrent.ThreadLocalRandom;

public class Guitar extends ToneBase implements Source
{

	private volatile float mAttenuation = 0.999f;

	private volatile int mTime = 0;

	private float[] mNoiseBuffer;
	private float[] mWorkBuffer;

	@Override
	public float next()
	{
		int lPeriodInSamples = getPeriodInSamples();

		ensureCorrectBuffersAllocated(lPeriodInSamples);

		if (mTime >= lPeriodInSamples)
		{
			mTime = 0;
			updateWorkBuffer(lPeriodInSamples);
		}

		float lSample = getAmplitude() * mWorkBuffer[mTime++];

		return lSample;
	}

	public void strike(float pIntensity)
	{
		int lPeriodInSamples = getPeriodInSamples();
		ensureCorrectBuffersAllocated(lPeriodInSamples);
		regenerateNoise(lPeriodInSamples);
		for (int j = 0; j < lPeriodInSamples; j++)
			mWorkBuffer[j] += pIntensity * mNoiseBuffer[j];
	}

	private void updateWorkBuffer(int pPeriodInSamples)
	{
		mWorkBuffer[pPeriodInSamples - 1] = 0;
		for (int j = 1; j < pPeriodInSamples - 1; j++)
			mWorkBuffer[j] = mAttenuation * (mWorkBuffer[j - 1] + 2
																				* mWorkBuffer[j] + mWorkBuffer[j + 1])
												/ 4;
	}

	private void ensureCorrectBuffersAllocated(int pPeriodInSamples)
	{
		if (mNoiseBuffer == null || mNoiseBuffer.length < pPeriodInSamples)
		{
			mNoiseBuffer = new float[pPeriodInSamples];
			regenerateNoise(pPeriodInSamples);
		}

		if (mWorkBuffer == null)
		{
			mWorkBuffer = new float[pPeriodInSamples];
		}

		if (mWorkBuffer.length != pPeriodInSamples)
		{
			float[] lNewWorkBuffer;

			lNewWorkBuffer = new float[pPeriodInSamples];

			final int lOldLength = mWorkBuffer.length;
			final int lNewLength = pPeriodInSamples;

			for (int i = 0; i < lNewLength - 1; i++)
				lNewWorkBuffer[i] = mWorkBuffer[(i * lOldLength) / lNewLength];
			mWorkBuffer = lNewWorkBuffer;
			mWorkBuffer[lNewLength - 1] = 0;
		}
	}

	private void regenerateNoise(int pPeriodInSamples)
	{
		ThreadLocalRandom lThreadLocalRandom = ThreadLocalRandom.current();

		for (int i = 1; i < pPeriodInSamples - 1; i++)
			mNoiseBuffer[i] = (float) (1 * (lThreadLocalRandom.nextFloat() - 0.5));
		mNoiseBuffer[0] = 0;
		mNoiseBuffer[pPeriodInSamples - 1] = 0;
	}

}
