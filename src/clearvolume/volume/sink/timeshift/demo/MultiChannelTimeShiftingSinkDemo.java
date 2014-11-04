package clearvolume.volume.sink.timeshift.demo;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.ClearVolumeRendererSink;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.timeshift.MultiChannelTimeShiftingSink;
import clearvolume.volume.sink.timeshift.gui.MultiChannelTimeShiftingSinkJFrame;

public class MultiChannelTimeShiftingSinkDemo
{
	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 128 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor + 1;
	private static final int cDepth = 128 * cSizeMultFactor + 3;

	@Test
	public void test() throws InterruptedException
	{
		ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"TimeShift And MultiChannel Demo",
																																																		512,
																																																		512,
																																																		1);
		lClearVolumeRenderer.setVisible(true);

		ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																										lClearVolumeRenderer.createCompatibleVolumeManager(200),
																																										100,
																																										TimeUnit.MILLISECONDS);
		lClearVolumeRendererSink.setRelaySink(new NullVolumeSink());

		MultiChannelTimeShiftingSink lMultiChannelTimeShiftingSink = new MultiChannelTimeShiftingSink(50,
																																																	100);

		MultiChannelTimeShiftingSinkJFrame.launch(lMultiChannelTimeShiftingSink);

		lMultiChannelTimeShiftingSink.setRelaySink(lClearVolumeRendererSink);

		VolumeManager lManager = lMultiChannelTimeShiftingSink.getManager();

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

			ByteBuffer lVolumeData = lVolume.getVolumeData();

			lVolumeData.rewind();

			final int lDepth = (int) (cDepth * (1.0 * i / lMaxVolumesSent));

			for (int z = 0; z < lDepth; z++)
				for (int y = 0; y < cHeight; y++)
					for (int x = 0; x < cWidth; x++)
					{
						final int lIndex = x + cWidth * y + cWidth * cHeight * z;

						byte lByteValue = (byte) (((byte) lTimePoint ^ (byte) x
																				^ (byte) y ^ (byte) z) / (lChannel == 1	? 8
																																								: 1));

						lVolumeData.put(lIndex, lByteValue);
					}/**/

			lVolume.setIndex(lTimePoint);
			lVolume.setVolumeChannelID(lChannel);

			lMultiChannelTimeShiftingSink.sendVolume(lVolume);

			Thread.sleep(50);
		}
	}
}
