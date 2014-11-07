package clearvolume.volume.sink.timeshift.test;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.VolumeSinkInterface;
import clearvolume.volume.sink.timeshift.TimeShiftingSink;

public class TimeShiftingSinkTests
{

	@Test
	public void test() throws InterruptedException
	{
		VolumeSinkInterface lVolumeSinkInterface = new VolumeSinkInterface()
		{

			@Override
			public void sendVolume(Volume<?> pVolume)
			{
				/*System.out.format("---> Received timepoint=%d channel=%d \n",
													pVolume.getIndex(),
													pVolume.getVolumeChannelID());/**/
			}

			@Override
			public VolumeManager getManager()
			{
				return new VolumeManager(200);
			}
		};

		TimeShiftingSink lTimeShiftingSink = new TimeShiftingSink(50, 100);

		lTimeShiftingSink.setRelaySink(lVolumeSinkInterface);

		VolumeManager lManager = lTimeShiftingSink.getManager();

		for (int i = 0; i < 1000; i++)
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

			/*System.out.format("Sending timepoint=%d channel=%d \n",
												lTimePoint,
												lChannel);/**/
			lTimeShiftingSink.sendVolume(lVolume);

			if (lChannel == 0)
			{
				if (lTimePoint == 100)
					lTimeShiftingSink.setTimeShift(-50);

				if (lTimePoint == 200)
					lTimeShiftingSink.setTimeShift(-100);
			}

		}

	}

}
