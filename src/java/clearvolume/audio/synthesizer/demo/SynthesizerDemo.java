package clearvolume.audio.synthesizer.demo;

import org.junit.Test;

import clearvolume.audio.SoundOut;
import clearvolume.audio.synthesizer.Synthesizer;
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
			lSynthesizer.playSamples(1024);
		lSoundOut.stop();
	}

	@Test
	public void testShepardRissetGlissando()
	{
		ShepardRissetGlissando lShepardRissetGlissando = new ShepardRissetGlissando();

		SoundOut lSoundOut = new SoundOut();

		Synthesizer lSynthesizer = new Synthesizer(	lShepardRissetGlissando,
																								lSoundOut);

		lSoundOut.start();
		for (int i = 0; i < 10000; i++)
		{
			lSynthesizer.playSamples(440);
			lShepardRissetGlissando.changeVirtualFrequency(1);
		}
		lSoundOut.stop();
	}

}
