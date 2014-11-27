package clearvolume.volume.sink.filter.demo;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.filter.ChannelFilterSink;
import clearvolume.volume.sink.filter.gui.ChannelFilterSinkJFrame;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;

public class FilterSinkDemo
{
	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 128 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor + 1;
	private static final int cDepth = 128 * cSizeMultFactor + 3;

	@Test
	public void demo() throws InterruptedException
	{
		ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"TimeShift Demo",
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
		lClearVolumeRendererSink.setRelaySink(new NullVolumeSink());

		ChannelFilterSink lChannelFilterSink = new ChannelFilterSink();

		ChannelFilterSinkJFrame.launch(lChannelFilterSink);

		lChannelFilterSink.setRelaySink(lClearVolumeRendererSink);

		VolumeManager lManager = lChannelFilterSink.getManager();

		final int lNumberOfChannels = 1;
		final int lMaxVolumesSent = 1000;
		for (int i = 0; i < lMaxVolumesSent; i++)
		{
			final int lTimePoint = i / lNumberOfChannels;
			final int lChannel = i % lNumberOfChannels;

			Volume<Byte> lVolume = lManager.requestAndWaitForVolume(1,
																															TimeUnit.MILLISECONDS,
																															Byte.class,
																															1,
																															cWidth,
																															cHeight,
																															cDepth);

			ByteBuffer lVolumeData = lVolume.getDataBuffer();

			lVolumeData.rewind();
			for (int j = 0; j < cWidth * cHeight * cDepth; j++)
				lVolumeData.put((byte) 0);

			lVolumeData.rewind();

			final int lDepth = (int) ((cDepth - 10) * (1. * i / lMaxVolumesSent));

			for (int z = 0; z < 10 + lDepth; z++)
				for (int y = 0; y < cHeight / (1 + lChannel); y++)
					for (int x = 0; x < cWidth / (1 + lChannel); x++)
					{
						final int lIndex = x + cWidth * y + cWidth * cHeight * z;

						byte lByteValue = (byte) (((byte) x ^ (byte) y ^ (byte) z));

						lVolumeData.put(lIndex, lByteValue);
					}/**/

			lVolume.setTimeIndex(lTimePoint);
			lVolume.setChannelID(lChannel);
			lVolume.setChannelName("Channel " + lChannel);

			lChannelFilterSink.sendVolume(lVolume);

			Thread.sleep(20);
		}
	}
}
