package clearvolume.audio.synthesizer.demo;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

import javax.sound.sampled.LineUnavailableException;

import org.junit.Test;

import clearvolume.audio.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
import clearvolume.audio.synthesizer.filters.LowPassFilter;
import clearvolume.audio.synthesizer.filters.NoiseFilter;
import clearvolume.audio.synthesizer.filters.ReverbFilter;
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
		for (int i = 0; i < 100000; i++)
		{
			lSynthesizer.playSamples();

			lGuitar.setFrequencyInHertz((float) (440 + 220 * sin(2 * PI
																														* i
																														/ 10000)));
			if (i % 200 == 0)
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
			lSynthesizer.playSamples(440);
			lShepardRissetGlissando.changeVirtualFrequency(+1f);
		}
		lSoundOut.stop();
	}

}
