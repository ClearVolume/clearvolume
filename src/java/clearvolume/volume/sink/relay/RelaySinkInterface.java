package clearvolume.volume.sink.relay;

import clearvolume.volume.sink.VolumeSinkInterface;

public interface RelaySinkInterface extends VolumeSinkInterface
{

	public void setRelaySink(VolumeSinkInterface pVolumeSinkInterface);

	public VolumeSinkInterface getRelaySink();

}
