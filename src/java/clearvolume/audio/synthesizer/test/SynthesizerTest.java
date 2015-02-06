package clearvolume.audio.synthesizer.test;

import static java.lang.Math.PI;
import static java.lang.Math.sin;
import static org.junit.Assert.fail;
import clearvolume.audio.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
import clearvolume.audio.synthesizer.filters.LowPassFilter;
import clearvolume.audio.synthesizer.filters.NoiseFilter;
import clearvolume.audio.synthesizer.filters.ReverbFilter;
import clearvolume.audio.synthesizer.sources.Guitar;
import clearvolume.audio.synthesizer.sources.Sinusoid;

public class SynthesizerTest
{

	// @Test
	public void testSinusoid()
	{
		try
		{
			Sinusoid lSinusoid = new Sinusoid();

			SoundOut lSoundOut = new SoundOut();

			Synthesizer lSynthesizer = new Synthesizer(lSinusoid, lSoundOut);

			lSoundOut.start();
			for (int i = 0; i < 500; i++)
				lSynthesizer.playSamples();
			lSoundOut.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}

	// @Test
	public void testGuitar()
	{
		try
		{
			Guitar lGuitar = new Guitar();

			NoiseFilter lNoiseFilter = new NoiseFilter(0.1f);
			lNoiseFilter.setSource(lGuitar);

			ReverbFilter lReverbFilter = new ReverbFilter(0.001f);
			lReverbFilter.setSource(lNoiseFilter);/**/

			LowPassFilter lLowPassFilter = new LowPassFilter();
			lLowPassFilter.setSource(lReverbFilter);/**/

			lGuitar.setAmplitude(1f);

			SoundOut lSoundOut = new SoundOut();

			Synthesizer lSynthesizer = new Synthesizer(	lLowPassFilter,
																									lSoundOut);

			lSoundOut.start();
			for (int i = 0; i < 400; i++)
			{
				lSynthesizer.playSamples();

				lGuitar.setFrequencyInHertz((float) (440 + 220 * sin(2 * PI
																															* i
																															/ 100)));
				if (i % 50 == 0)
					lGuitar.strike(0.5f);

			}

			lSoundOut.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}

}
