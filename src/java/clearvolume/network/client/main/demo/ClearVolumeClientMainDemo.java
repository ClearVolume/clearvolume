package clearvolume.network.client.main.demo;

import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.network.client.main.ClearVolumeClientMain;
import clearvolume.renderer.VolumeCaptureListener;

public class ClearVolumeClientMainDemo
{

	@Test
	public void demoStartClientGUIWithVolumeCaptureListener() throws InterruptedException
	{
		ClearVolumeClientMain.launchClientGUI(new VolumeCaptureListener()
		{

			@Override
			public void capturedVolume(	ByteBuffer[] pCaptureBuffers,
																	boolean pFloatType,
																	int pBytesPerVoxel,
																	long pVolumeWidth,
																	long pVolumeHeight,
																	long pVolumeDepth,
																	double pVoxelWidth,
																	double pVoxelHeight,
																	double pVoxelDepth)
			{
				System.out.format("Captured %d volume %s bpv=%d (%d, %d, %d) (%g, %g, %g) %s\n",
													pCaptureBuffers.length,
													pFloatType ? "float" : "int",
													pBytesPerVoxel,
													pVolumeWidth,
													pVolumeHeight,
													pVolumeDepth,
													pVoxelWidth,
													pVoxelHeight,
													pVoxelDepth,
													pCaptureBuffers[0].toString());
			}
		});

		Thread.sleep(1000100);
	}

}
