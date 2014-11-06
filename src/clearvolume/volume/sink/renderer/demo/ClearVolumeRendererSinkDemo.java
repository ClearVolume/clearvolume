package clearvolume.volume.sink.renderer.demo;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;

public class ClearVolumeRendererSinkDemo
{

	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 128 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor + 1;
	private static final int cDepth = 128 * cSizeMultFactor + 3;

	@Test
	public void demo() throws InterruptedException
	{
		ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeRendererSink Demo",
																																																		512,
																																																		512,
																																																		1,
																																																		512,
																																																		512,
																																																		2);
		lClearVolumeRenderer.setVisible(true);

		ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																										lClearVolumeRenderer.createCompatibleVolumeManager(200),
																																										100,
																																										TimeUnit.MILLISECONDS);

		VolumeManager lManager = lClearVolumeRendererSink.getManager();

		final int lMaxVolumesSent = 1000;
		for (int i = 0; i < lMaxVolumesSent; i++)
		{
			final int lTimePoint = i / 2;
			final int lChannel = i % 2;

			Volume<Byte> lVolume = lManager.requestAndWaitForVolume(1,
																															TimeUnit.MILLISECONDS,
																															Byte.class,
																															1,
																															cWidth,
																															cHeight,
																															cDepth);

			ByteBuffer lVolumeData = lVolume.getDataBuffer();

			lVolumeData.rewind();
			for (int j = 0; j < lVolumeData.limit(); j++)
				lVolumeData.put((byte) 0);

			final int lDepth = (int) (cDepth * (1.0 * i / lMaxVolumesSent));

			lVolumeData.rewind();
			for (int z = 0; z < lDepth; z++)
				for (int y = 0; y < cHeight; y++)
					for (int x = 0; x < cWidth; x++)
					{
						final int lIndex = x + cWidth * y + cWidth * cHeight * z;

						byte lByteValue = (byte) (((byte) lTimePoint ^ (byte) x
																				^ (byte) y ^ (byte) z));

						if (lChannel == 1)
							lByteValue = (byte) ((byte) 117 ^ lByteValue);

						lVolumeData.put(lIndex, lByteValue);
					}/**/

			lVolume.setTimeIndex(lTimePoint);
			lVolume.setTimeInSeconds(0.1 * lTimePoint);
			lVolume.setChannelID(lChannel);

			// if (lChannel == 0)
			lClearVolumeRendererSink.sendVolume(lVolume);

			Thread.sleep(50);
		}
	}

}
