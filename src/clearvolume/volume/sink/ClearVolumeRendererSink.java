package clearvolume.volume.sink;

import java.nio.ByteBuffer;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.volume.Volume;

public class ClearVolumeRendererSink implements VolumeSinkInterface
{

	private ClearVolumeRendererInterface mClearVolumeRendererInterface;

	public ClearVolumeRendererSink(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		super();
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;

	}

	@Override
	public void sendVolume(Volume pVolume)
	{
		final ByteBuffer lVolumeDataBuffer = pVolume.getVolumeData();
		final long lWidth = pVolume.getWidthInVoxels();
		final long lHeight = pVolume.getHeightInVoxels();
		final long lDepth = pVolume.getDepthInVoxels();

		mClearVolumeRendererInterface.setVolumeDataBuffer(lVolumeDataBuffer,
																											lWidth,
																											lHeight,
																											lDepth);

		mClearVolumeRendererInterface.requestDisplay();

	}

}
