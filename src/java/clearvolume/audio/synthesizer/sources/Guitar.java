package clearvolume.audio.synthesizer.sources;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Source generating the sound of a single guitar string. The string length can
 * be modulated to change the fundamental frequency. The attenuation of the
 * string vibration can also be adjusted.
 * 
 * This class is not thread safe, you have to make sure that calls to strike or
 * changes to the frequency do not happen at the same time as the syntheziser
 * processes samples - i.e. method playSamples(...) .
 *
 * @author Loic Royer (2015)
 *
 */
public class Guitar extends ToneBase implements Source
{

	private volatile float mAttenuation = 0.99f;

	private volatile int mTime = 0;

	private float[] mNoiseBuffer;
	private float[] mWorkBuffer;

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		final int lPeriodInSamples = getPeriodInSamples();

		ensureCorrectBuffersAllocated(lPeriodInSamples);

		if (mTime >= lPeriodInSamples)
		{
			mTime = 0;
			updateWorkBuffer(lPeriodInSamples);
		}

		mWorkBuffer[mTime] *= getAttenuation();

		final float lSample = getAmplitude() * mWorkBuffer[mTime];

		mTime++;

		return lSample;
	}

	/**
	 * Strike the string of the Guitar with a given intensity.
	 * 
	 * @param pIntensity
	 *          intensity
	 */
	public void strike(float pIntensity)
	{
		final int lPeriodInSamples = getPeriodInSamples();
		ensureCorrectBuffersAllocated(lPeriodInSamples);
		regenerateNoise(lPeriodInSamples, 1f);
		for (int j = 0; j < lPeriodInSamples; j++)
			mWorkBuffer[j] += pIntensity * mNoiseBuffer[j];
	}

	/**
	 * Internal method: updates the work buffer by applying the blurring kernel.
	 * 
	 * @param pPeriodInSamples
	 */
	private void updateWorkBuffer(int pPeriodInSamples)
	{
		mWorkBuffer[pPeriodInSamples - 1] = 0;
		for (int j = 1; j < pPeriodInSamples - 1; j++)
			mWorkBuffer[j] = (mWorkBuffer[j - 1] + 2 * mWorkBuffer[j] + mWorkBuffer[j + 1]) / 4;
	}

	/**
	 * Internal method: ensures correct length of buffers.
	 * 
	 * @param pPeriodInSamples
	 */
	private void ensureCorrectBuffersAllocated(int pPeriodInSamples)
	{

		if (mNoiseBuffer == null)
		{
			mNoiseBuffer = new float[pPeriodInSamples];
			regenerateNoise(pPeriodInSamples, 1f);
		}

		if (mNoiseBuffer.length < pPeriodInSamples)
		{
			final int lOldLength = mWorkBuffer.length;
			final int lNewLength = pPeriodInSamples;

			final float[] lNewNoiseBuffer = new float[pPeriodInSamples];

			for (int i = 0; i < lNewLength - 1; i++)
				lNewNoiseBuffer[i] = mNoiseBuffer[(i * lOldLength) / lNewLength];
			mNoiseBuffer = lNewNoiseBuffer;
			mNoiseBuffer[lNewLength - 1] = 0;

			// regenerateNoise(pPeriodInSamples, 1f);
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
			mTime = (mTime * lOldLength) / lNewLength;
		}
	}

	/**
	 * @param pPeriodInSamples
	 * @param pAlpha
	 */
	private void regenerateNoise(int pPeriodInSamples, float pAlpha)
	{
		final ThreadLocalRandom lThreadLocalRandom = ThreadLocalRandom.current();

		for (int i = 1; i < pPeriodInSamples - 1; i++)
			mNoiseBuffer[i] = (1 - pAlpha) * mNoiseBuffer[i]
												+ pAlpha
												* 2
												* (float) (lThreadLocalRandom.nextFloat() - 0.5);
		mNoiseBuffer[0] = 0;
		mNoiseBuffer[pPeriodInSamples - 1] = 0;
	}

	/**
	 * @return attenuation
	 */
	public float getAttenuation()
	{
		return mAttenuation;
	}

	/**
	 * @param pAttenuation
	 *          attenuation
	 */
	public void setAttenuation(float pAttenuation)
	{
		mAttenuation = pAttenuation;
	}

}
