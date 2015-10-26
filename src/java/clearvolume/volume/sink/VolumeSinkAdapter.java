package clearvolume.volume.sink;

import clearvolume.volume.VolumeManager;

public abstract class VolumeSinkAdapter	implements
										VolumeSinkInterface
{
	VolumeManager mVolumeManager;

	public VolumeSinkAdapter(int pMaxAvailableVolumes)
	{
		super();
		mVolumeManager = new VolumeManager(pMaxAvailableVolumes);
	}

	@Override
	public VolumeManager getManager()
	{
		return mVolumeManager;
	}

}
