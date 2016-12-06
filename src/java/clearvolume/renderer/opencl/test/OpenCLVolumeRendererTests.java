package clearvolume.renderer.opencl.test;

import static java.lang.Math.toIntExact;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.opencl.OpenCLAvailability;
import clearvolume.renderer.opencl.OpenCLVolumeRenderer;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;
import coremem.util.Size;

public class OpenCLVolumeRendererTests
{

	@Test
	public void demoWith8BitGeneratedDataset() throws InterruptedException,
																						IOException
	{
		if (!OpenCLAvailability.isOpenCLAvailable())
			return;

		final NativeTypeEnum lNativeType = NativeTypeEnum.UnsignedByte;
		final long lBytesPerVoxel = Size.of(lNativeType);

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																																												512,
																																												512,
																																												lNativeType,
																																												512,
																																												512,
																																												1,
																																												false);

		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getBlueGradient());

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final int lLength = lResolutionX * lResolutionY * lResolutionZ;

		final ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(toIntExact(lLength * lBytesPerVoxel))
																							.order(ByteOrder.nativeOrder());

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final byte lValue = (byte) ((byte) x ^ (byte) y ^ (byte) z);
					lByteBuffer.put(lValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							lByteBuffer,
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

	@Test
	public void demoWith16BitGeneratedDataset()	throws InterruptedException,
																							IOException
	{
		if (!OpenCLAvailability.isOpenCLAvailable())
			return;

		final NativeTypeEnum lNativeType = NativeTypeEnum.UnsignedShort;
		final long lBytesPerVoxel = Size.of(lNativeType);

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																																												512,
																																												512,
																																												lNativeType,
																																												512,
																																												512,
																																												1,
																																												false);

		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		// lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getBlueGradient());

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final int lLength = lResolutionX * lResolutionY * lResolutionZ;

		final ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(toIntExact(lLength * lBytesPerVoxel))
																							.order(ByteOrder.nativeOrder());

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final char lValue = (char) (256 * ((char) x ^ (char) y ^ (char) z));
					lByteBuffer.putChar(lValue);

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							lByteBuffer,
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
}
