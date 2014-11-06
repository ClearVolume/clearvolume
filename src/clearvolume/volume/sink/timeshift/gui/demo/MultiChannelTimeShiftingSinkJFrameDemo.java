package clearvolume.volume.sink.timeshift.gui.demo;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.VolumeSinkInterface;
import clearvolume.volume.sink.timeshift.MultiChannelTimeShiftingSink;
import clearvolume.volume.sink.timeshift.gui.MultiChannelTimeShiftingSinkJFrame;

public class MultiChannelTimeShiftingSinkJFrameDemo
{

	@Test
	public void demo() throws InterruptedException
	{
		VolumeSinkInterface lVolumeSinkInterface = new VolumeSinkInterface()
		{

			@Override
			public void sendVolume(Volume<?> pVolume)
			{
				System.out.format("---> Received timepoint=%d channel=%d \n",
													pVolume.getTimeIndex(),
													pVolume.getChannelID());
			}

			@Override
			public VolumeManager getManager()
			{
				return new VolumeManager(200);
			}
		};

		MultiChannelTimeShiftingSink lMultiChannelTimeShiftingSink = new MultiChannelTimeShiftingSink(50,
																																																	100);
		MultiChannelTimeShiftingSinkJFrame.launch(lMultiChannelTimeShiftingSink);

		lMultiChannelTimeShiftingSink.setRelaySink(lVolumeSinkInterface);

		VolumeManager lManager = lMultiChannelTimeShiftingSink.getManager();

		for (int i = 0; i < 10000000; i++)
		{

			Volume<Byte> lVolume = lManager.requestAndWaitForVolume(1,
																															TimeUnit.MILLISECONDS,
																															Byte.class,
																															1,
																															10,
																															10,
																															10);

			final int lTimePoint = i / 2;
			final int lChannel = i % 2;

			lVolume.setTimeIndex(lTimePoint);
			lVolume.setChannelID(lChannel);

			System.out.format("Sending timepoint=%d channel=%d \n",
												lTimePoint,
												lChannel);
			lMultiChannelTimeShiftingSink.sendVolume(lVolume);

			Thread.sleep(200);
		}

	}

}
