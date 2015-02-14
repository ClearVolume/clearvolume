package clearvolume.renderer;

import java.nio.ByteBuffer;

public interface VolumeCaptureListener
{

	void capturedVolume(ByteBuffer[] pCaptureBuffers,
											boolean pFloatType,
											int pBytesPerVoxel,
											long pVolumeWidth,
											long pVolumeHeight,
											long pVolumeDepth,
											double pVoxelWidth,
											double pVoxelHeight,
											double pVoxelDepth);

}
