package clearvolume.network.client.main.demo;

import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.network.client.main.ClearVolumeClientMain;
import clearvolume.renderer.listeners.VolumeCaptureListener;
import coremem.types.NativeTypeEnum;


public class ClearVolumeClientMainDemo
{

	@Test
	public void demoStartClientGUIWithVolumeCaptureListener() throws InterruptedException
	{
		ClearVolumeClientMain.launchClientGUI(new VolumeCaptureListener()
		{

			@Override
			public void capturedVolume(	ByteBuffer[] pCaptureBuffers,
																	NativeTypeEnum pNativeTypeEnum,
																	long pVolumeWidth,
																	long pVolumeHeight,
																	long pVolumeDepth,
																	double pVoxelWidth,
																	double pVoxelHeight,
																	double pVoxelDepth)
			{
				System.out.format("Captured %d volume of type=%s (%d, %d, %d) (%g, %g, %g) %s\n",
													pCaptureBuffers.length,
													pNativeTypeEnum,
													pVolumeWidth,
													pVolumeHeight,
													pVolumeDepth,
													pVoxelWidth,
													pVoxelHeight,
													pVoxelDepth,
													pCaptureBuffers[0].toString());
			}
		},
																					true);

		Thread.sleep(1000100);
	}

}
