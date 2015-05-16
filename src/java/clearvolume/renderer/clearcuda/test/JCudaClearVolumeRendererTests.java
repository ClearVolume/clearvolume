package clearvolume.renderer.clearcuda.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transferf.TransferFunctions;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;

public class JCudaClearVolumeRendererTests
{
	@Test
	public void demoWith8BitGeneratedDataset() throws InterruptedException,
																						IOException
	{
		if (!CudaAvailability.isClearCudaOperational())
			return;

		try
		{
			final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																															768,
																																															768,
																																															NativeTypeEnum.UnsignedByte);

			lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());

			lClearVolumeRenderer.setVisible(true);

			final int lResolutionX = 256;
			final int lResolutionY = lResolutionX;
			final int lResolutionZ = lResolutionX;
			final int lLength = lResolutionX * lResolutionY * lResolutionZ;

			final byte[] lVolumeDataArray = new byte[lLength];

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = (x + lResolutionX * y + lResolutionX * lResolutionY
																												* z);
						lVolumeDataArray[lIndex] = (byte) (((byte) x ^ (byte) y ^ (byte) z));
					}

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);

			Thread.sleep(1000);

			final OffHeapMemory lOffHeapMemory = OffHeapMemory.allocateBytes(lLength);

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = (x + lResolutionX * y + lResolutionX * lResolutionY
																												* z);
						lOffHeapMemory.setByte(	lIndex,
																		(byte) (256 - (((byte) x ^ (byte) y ^ (byte) z))));
					}

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								lOffHeapMemory,
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);

			Thread.sleep(1000);

			lClearVolumeRenderer.close();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			System.err.println("IT SEEMS THAT CUDA IS NOT AVAILABLE!!");
		}
	}

	@Test
	public void demoWith16BitGeneratedDataset()	throws InterruptedException,
																							IOException
	{
		if (!CudaAvailability.isClearCudaOperational())
			return;

		try
		{
			final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																															768,
																																															768,
																																															NativeTypeEnum.UnsignedShort);
			lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
			lClearVolumeRenderer.setVisible(true);

			final int lResolutionX = 256;
			final int lResolutionY = 256;
			final int lResolutionZ = 256;

			final int lLength = lResolutionX * lResolutionY * lResolutionZ;

			final byte[] lVolumeDataArray = new byte[lLength * 2];

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
																														* z);
						lVolumeDataArray[lIndex + 1] = (byte) (((byte) x ^ (byte) y ^ (byte) z));
					}

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);

			Thread.sleep(1000);

			final OffHeapMemory lOffHeapMemory = OffHeapMemory.allocateShorts(lLength);

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = (x + lResolutionX * y + lResolutionX * lResolutionY
																												* z);
						lOffHeapMemory.setShort(lIndex,
																		(short) (256 - (((byte) x ^ (byte) y ^ (byte) z))));
					}

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								lOffHeapMemory,
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);

			Thread.sleep(1000);

			lClearVolumeRenderer.close();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			System.err.println("IT SEEMS THAT CUDA IS NOT AVAILABLE!!");
		}

	}

}
