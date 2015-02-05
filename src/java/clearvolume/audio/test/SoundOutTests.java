package clearvolume.audio.test;

import org.junit.Test;

import clearvolume.audio.SoundOut;

public class SoundOutTests
{

	@Test
	public void test()
	{
		SoundOut lSoundOut = new SoundOut();

		lSoundOut.start();

		double[] lNoise = new double[128];
		for (int j = 1; j < lNoise.length - 1; j++)
			lNoise[j] += 1 * (Math.random() - 0.5);

		double[] lBuffer = new double[128];

		boolean lInversion = false;
		for (int i = 0; i < 10000; i++)
		{
			for (int j = 1; j < lBuffer.length - 1; j++)
				lBuffer[j] = (lBuffer[j - 1] + lBuffer[j] + lBuffer[j + 1]) / 3;

			if (i % 1000 == 0)
			{
				for (int j = 1; j < lBuffer.length - 1; j++)
					lBuffer[j] += (lInversion ? 1 : -1) * lNoise[j];
				lInversion = !lInversion;
			}

			lSoundOut.play(lBuffer, lBuffer.length);

		}

		lSoundOut.stop();
	}

}
