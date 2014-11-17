package clearvolume.volume.source;

import clearvolume.volume.Volume;

public interface VolumeSourceInterface
{
	public Volume<?> requestVolume();
}
