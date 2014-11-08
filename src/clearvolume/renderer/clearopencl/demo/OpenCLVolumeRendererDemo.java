package clearvolume.renderer.clearopencl.demo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearopencl.OpenCLVolumeRenderer;

public class OpenCLVolumeRendererDemo
{

	@Test
	public void demoWith8BitGeneratedDataset() throws InterruptedException,
																						IOException
	{
		int lBytesPerVoxel = 1;

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																																												512,
																																												512,
																																												lBytesPerVoxel,
																																												512,
																																												512,
																																												1);

		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getBlueGradient());

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(lResolutionX * lResolutionY
																												* lResolutionZ
																												* lBytesPerVoxel)
																				.order(ByteOrder.nativeOrder());

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final byte lValue = (byte) ((byte) x ^ (byte) y ^ (byte) z);
					lByteBuffer.put(lValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	lByteBuffer,
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith16BitGeneratedDataset()	throws InterruptedException,
																							IOException
	{
		int lBytesPerVoxel = 2;

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																																												512,
																																												512,
																																												lBytesPerVoxel,
																																												512,
																																												512,
																																												1);

		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getBlueGradient());

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(lResolutionX * lResolutionY
																												* lResolutionZ
																												* lBytesPerVoxel)
																				.order(ByteOrder.nativeOrder());

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final char lValue = (char) (256 * ((char) x ^ (char) y ^ (char) z));
					lByteBuffer.putChar(lValue);

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	lByteBuffer,
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
		}

		lClearVolumeRenderer.close();
	}
}
