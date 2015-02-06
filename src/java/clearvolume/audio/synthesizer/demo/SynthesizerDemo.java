package clearvolume.audio.synthesizer.demo;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

import org.junit.Test;

import clearvolume.audio.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
import clearvolume.audio.synthesizer.filters.LowPassFilter;
import clearvolume.audio.synthesizer.filters.ReverbFilter;
import clearvolume.audio.synthesizer.filters.WarmFilter;
import clearvolume.audio.synthesizer.sources.Guitar;
import clearvolume.audio.synthesizer.sources.ShepardRissetGlissando;
import clearvolume.audio.synthesizer.sources.Sinusoid;

public class SynthesizerDemo
{

	@Test
	public void testSinusoid()
	{
		Sinusoid lSinusoid = new Sinusoid();

		SoundOut lSoundOut = new SoundOut();

		Synthesizer lSynthesizer = new Synthesizer(lSinusoid, lSoundOut);

		lSoundOut.start();
		for (int i = 0; i < 1000; i++)
			lSynthesizer.playSamples();
		lSoundOut.stop();
	}

	@Test
	public void testGuitar()
	{
		Guitar lGuitar = new Guitar();

		WarmFilter lWarmFilter = new WarmFilter();
		lWarmFilter.setSource(lGuitar);

		ReverbFilter lReverbFilter = new ReverbFilter(0.01f);
		lReverbFilter.setSource(lWarmFilter);

		LowPassFilter lLowPassFilter = new LowPassFilter();
		lLowPassFilter.setSource(lReverbFilter);/**/

		lGuitar.setAmplitude(0.1f);

		SoundOut lSoundOut = new SoundOut();

		Synthesizer lSynthesizer = new Synthesizer(	lLowPassFilter,
																								lSoundOut);

		lSoundOut.start();
		for (int i = 0; i < 10000; i++)
		{
			lSynthesizer.playSamples();

			lGuitar.setFrequencyInHertz((float) (440 + 400 * sin(2 * PI
																														* i
																														/ 1000)));
			if (i % 100 == 0)
				lGuitar.strike(0.5f);

		}

		lSoundOut.stop();
	}

	@Test
	public void testShepardRissetGlissando()
	{
		ShepardRissetGlissando lShepardRissetGlissando = new ShepardRissetGlissando();
		lShepardRissetGlissando.setAmplitude(0.05f);

		SoundOut lSoundOut = new SoundOut();

		Synthesizer lSynthesizer = new Synthesizer(	lShepardRissetGlissando,
																								lSoundOut);

		lSoundOut.start();
		for (int i = 0; i < 10000; i++)
		{
			lSynthesizer.playSamples(440);
			lShepardRissetGlissando.changeVirtualFrequency(+1f);
		}
		lSoundOut.stop();
	}

}
