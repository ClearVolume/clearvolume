package clearvolume.audio.audioplot.demo;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

import org.junit.Test;

import clearvolume.audio.audioplot.AudioPlot;

public class AudioPlotDemo
{

	@Test
	public void test3Levels() throws InterruptedException
	{
		AudioPlot lAudioPlot = new AudioPlot();

		lAudioPlot.start();
		System.out.println("start");

		System.out.println("0");
		lAudioPlot.setValue(0);
		Thread.sleep(4000);

		System.out.println("0.5");
		lAudioPlot.setValue(0.5);
		Thread.sleep(4000);

		System.out.println("1");
		lAudioPlot.setValue(1);
		Thread.sleep(4000);

		lAudioPlot.stop();

	}

	@Test
	public void testRamp() throws InterruptedException
	{
		AudioPlot lAudioPlot = new AudioPlot();

		lAudioPlot.start();
		System.out.println("start");

		for (float v = 0; v < 1; v += 0.001)
		{
			System.out.println(v);
			lAudioPlot.setValue(v);
			Thread.sleep(100);
		}

		lAudioPlot.stop();

	}

	@Test
	public void testGaussianLikeWithPlateauResponseSweep() throws InterruptedException
	{
		AudioPlot lAudioPlot = new AudioPlot();
		lAudioPlot.setInvertRange(true);

		lAudioPlot.start();
		System.out.println("start");

		for (float v = -10; v < 10; v += 0.05)
		{
			double g = 1 / (1 + pow(v, 2));
			System.out.println(v);
			lAudioPlot.setValue(g);
			Thread.sleep(100);

			if (abs(v) < 0.05)
				Thread.sleep(4000);
		}

		lAudioPlot.stop();

	}

}
