package clearvolume.renderer;

import java.nio.ByteBuffer;

import coremem.types.NativeTypeEnum;

public interface VolumeCaptureListener
{

	void capturedVolume(ByteBuffer[] pCaptureBuffers,
											NativeTypeEnum pNativeTypeEnum,
											long pVolumeWidth,
											long pVolumeHeight,
											long pVolumeDepth,
											double pVoxelWidth,
											double pVoxelHeight,
											double pVoxelDepth);

}
