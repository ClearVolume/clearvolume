package clearvolume.volume.sink.relay;

import clearvolume.volume.sink.VolumeSinkInterface;

public abstract class RelaySinkAdapter implements RelaySinkInterface
{
	VolumeSinkInterface mRelaySink;

	public RelaySinkAdapter()
	{
	}

	public RelaySinkAdapter(VolumeSinkInterface pRelaySink)
	{
		mRelaySink = pRelaySink;
	}

	@Override
	public void setRelaySink(VolumeSinkInterface pVolumeSinkInterface)
	{
		mRelaySink = pVolumeSinkInterface;
	}

	@Override
	public VolumeSinkInterface getRelaySink()
	{
		return mRelaySink;
	}

}
