package clearvolume.volume.sink;

import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

public class NullVolumeSink implements VolumeSinkInterface
{

	@Override
	public void sendVolume(Volume<?> pVolume)
	{
		// no nothing - in purpose...
	}

	@Override
	public VolumeManager getManager()
	{
		return null;
	}

}
