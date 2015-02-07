package clearvolume.audio.synthesizer.demo;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

import javax.sound.sampled.LineUnavailableException;

import org.junit.Test;

import clearvolume.audio.sound.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
import clearvolume.audio.synthesizer.filters.LowPassFilter;
import clearvolume.audio.synthesizer.filters.NoiseFilter;
import clearvolume.audio.synthesizer.filters.ReverbFilter;
import clearvolume.audio.synthesizer.filters.WarmifyFilter;
import clearvolume.audio.synthesizer.sources.Guitar;
import clearvolume.audio.synthesizer.sources.ShepardRissetGlissando;
import clearvolume.audio.synthesizer.sources.Sinusoid;

public class SynthesizerDemo
{

	@Test
	public void demoSinusoid() throws LineUnavailableException
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
	public void demoGuitar() throws LineUnavailableException
	{
		Guitar lGuitar = new Guitar();

		NoiseFilter lNoiseFilter = new NoiseFilter();
		lNoiseFilter.setSource(lGuitar);

		WarmifyFilter lWarmifyFilter = new WarmifyFilter(1f);
		lWarmifyFilter.setSource(lNoiseFilter);

		ReverbFilter lReverbFilter = new ReverbFilter();
		lReverbFilter.setSource(lWarmifyFilter);/**/

		LowPassFilter lLowPassFilter = new LowPassFilter();
		lLowPassFilter.setSource(lReverbFilter);/**/



		lGuitar.setAmplitude(0.5f);

		SoundOut lSoundOut = new SoundOut();

		Synthesizer lSynthesizer = new Synthesizer(	lLowPassFilter,
																								lSoundOut);

		lSoundOut.start();
		for (int i = 0; i < 100000; i++)
		{
			lSynthesizer.playSamples();

			lGuitar.setFrequencyInHertz((float) (220 + 440 + 440 * sin(2 * PI
																														* i
																														/ 1000)));
			if (i % 10 == 0)
				lGuitar.strike(0.5f);

		}

		lSoundOut.stop();
	}

	@Test
	public void demoShepardRissetGlissando() throws LineUnavailableException
	{
		ShepardRissetGlissando lShepardRissetGlissando = new ShepardRissetGlissando();
		lShepardRissetGlissando.setAmplitude(0.05f);

		SoundOut lSoundOut = new SoundOut();

		Synthesizer lSynthesizer = new Synthesizer(	lShepardRissetGlissando,
																								lSoundOut);

		lSoundOut.start();
		for (int i = 0; i < 10000; i++)
		{
			lSynthesizer.playSamples();
			lShepardRissetGlissando.changeVirtualFrequency(+1f);
		}
		lSoundOut.stop();
	}

}
