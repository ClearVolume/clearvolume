package clearvolume.volume.sink;

import java.nio.ByteBuffer;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.volume.Volume;

public class ClearVolumeRendererSink implements
																				VolumeSinkInterface
{

	private ClearVolumeRendererInterface mClearVolumeRendererInterface;

	public ClearVolumeRendererSink(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		super();
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;

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

	}

}
