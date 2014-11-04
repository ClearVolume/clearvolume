package clearvolume.volume.sink;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

public class ClearVolumeRendererSink extends RelaySinkAdapter	implements
																															RelaySinkInterface
{

	private ClearVolumeRendererInterface mClearVolumeRendererInterface;
	private VolumeManager mVolumeManager;
	private long mWaitForCopyTimeout;
	private TimeUnit mTimeUnit;

	public ClearVolumeRendererSink(	ClearVolumeRendererInterface pClearVolumeRendererInterface,
																	VolumeManager pVolumeManager,
																	long pWaitForCopyTimeout,
																	TimeUnit pTimeUnit)
	{
		super();
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;
		mVolumeManager = pVolumeManager;
		mWaitForCopyTimeout = pWaitForCopyTimeout;
		mTimeUnit = pTimeUnit;

	}

	@Override
	public void sendVolume(Volume<?> pVolume)
	{
		final ByteBuffer lVolumeDataBuffer = pVolume.getVolumeData();
		final long lVoxelWidth = pVolume.getWidthInVoxels();
		final long lVoxelHeight = pVolume.getHeightInVoxels();
		final long lVoxelDepth = pVolume.getDepthInVoxels();

		final double lRealWidth = pVolume.getWidthInRealUnits();
		final double lRealHeight = pVolume.getHeightInRealUnits();
		final double lRealDepth = pVolume.getDepthInRealUnits();

		mClearVolumeRendererInterface.setVolumeDataBuffer(lVolumeDataBuffer,
																											lVoxelWidth,
																											lVoxelHeight,
																											lVoxelDepth,
																											lRealWidth,
																											lRealHeight,
																											lRealDepth);

		mClearVolumeRendererInterface.requestDisplay();
		mClearVolumeRendererInterface.waitToFinishDataBufferCopy(	mWaitForCopyTimeout,
																															mTimeUnit);

		/*if (getRelaySink() != null)
			getRelaySink().sendVolume(pVolume);
		else
			pVolume.makeAvailableToManager();/**/

	}

	@Override
	public VolumeManager getManager()
	{
		return mVolumeManager;
	}

}
