package clearvolume.volume.sink;

import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

public interface VolumeSinkInterface
{
	public void sendVolume(Volume pVolume);

	public VolumeManager getManager();
}
