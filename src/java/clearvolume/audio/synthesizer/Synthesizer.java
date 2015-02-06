package clearvolume.audio.synthesizer;

import clearvolume.audio.SoundOut;
import clearvolume.audio.synthesizer.sources.Source;

public class Synthesizer
{

	private Source mSource;

	private float[] mBuffer;
	private SoundOut mSoundOut;

	public Synthesizer(Source pSource, SoundOut pSoundOut)
	{
		super();
		mSource = pSource;
		mSoundOut = pSoundOut;
	}

	public void playSamples()
	{
		int lPreferredNumberOfSamples = mSource.getPeriodInSamples();
		playSamples(lPreferredNumberOfSamples);
	}

	public void playSamples(int pNumberOfSamples)
	{
		if (mBuffer == null || mBuffer.length < pNumberOfSamples)
			mBuffer = new float[pNumberOfSamples];

		for (int i = 0; i < pNumberOfSamples; i++)
		{
			float lValue = mSource.next();
			mBuffer[i] = lValue;
		}

		mSoundOut.play(mBuffer, pNumberOfSamples);

	}

}
