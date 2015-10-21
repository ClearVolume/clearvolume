package clearvolume.renderer.listeners;

import java.nio.ByteBuffer;

import coremem.types.NativeTypeEnum;

public interface VolumeCaptureListener
{

	void capturedVolume(ByteBuffer pCaptureBuffer,
											NativeTypeEnum pNativeTypeEnum,
											long pVolumeWidth,
											long pVolumeHeight,
											long pVolumeDepth,
											double pVoxelWidth,
											double pVoxelHeight,
											double pVoxelDepth);

}
