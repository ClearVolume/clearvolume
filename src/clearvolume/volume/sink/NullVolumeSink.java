package clearvolume.volume.sink;

import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

public class NullVolumeSink implements VolumeSinkInterface
{

	private VolumeManager mVolumeManager;

	public NullVolumeSink()
	{
		super();
	}

	public NullVolumeSink(VolumeManager pVolumeManager)
	{
		super();
		mVolumeManager = pVolumeManager;
	}

	@Override
	public void sendVolume(Volume<?> pVolume)
	{
		if (mVolumeManager != null)
			pVolume.makeAvailableToManager();
	}

	@Override
	public VolumeManager getManager()
	{
		return mVolumeManager;
	}

}
