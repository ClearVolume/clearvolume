package clearvolume.demo;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.renderer.opencl.OpenCLVolumeRenderer;
import clearvolume.transferf.TransferFunctions;
import coremem.ContiguousMemoryInterface;
import coremem.buffers.ContiguousBuffer;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;

public class ClearVolumeMultiChannelDemos
{

	public static void main(final String[] argv) throws ClassNotFoundException
	{
		if (argv.length == 0)
		{
			final Class<?> c = Class.forName("clearvolume.demo.ClearVolumeMultiChannelDemos");

			System.out.println("Give one of the following method names as parameter:");

			for (final Member m : c.getMethods())
			{
				final String name = ((Method) m).getName();

				if (name.substring(0, 4).equals("demo"))
				{
					System.out.println("Demo: " + ((Method) m).getName());
				}
			}
		}
		else
		{
			final ClearVolumeMultiChannelDemos cvdemo = new ClearVolumeMultiChannelDemos();
			Method m;

			try
			{
				m = cvdemo.getClass().getMethod(argv[0]);
			}
			catch (final Exception e)
			{
				System.out.println("Could not launch " + argv[0]
									+ " because ...");
				e.printStackTrace();

				return;
			}

			try
			{
				System.out.println("Running " + argv[0] + "()...");
				m.invoke(cvdemo);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	@Test
	public void demoWith8BitGeneratedDataset2LayersJCuda()	throws InterruptedException,
															IOException

	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																								512,
																								512,
																								NativeTypeEnum.UnsignedByte,
																								512,
																								512,
																								2,
																								false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
													ByteBuffer.wrap(lVolumeDataArray0),
													lResolutionX,
													lResolutionY,
													lResolutionZ);

		lClearVolumeRenderer.requestDisplay();
		Thread.sleep(2000);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
													ByteBuffer.wrap(lVolumeDataArray1),
													lResolutionX,
													lResolutionY,
													lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			lClearVolumeRenderer.setLayerVisible(	i % 2,
													!lClearVolumeRenderer.isLayerVisible(i % 2));

			lClearVolumeRenderer.requestDisplay();
			i++;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith8BitGeneratedDataset2LayersOpenCL()	throws InterruptedException,
															IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																							512,
																							512,
																							NativeTypeEnum.UnsignedByte,
																							512,
																							512,
																							2,
																							false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
													ByteBuffer.wrap(lVolumeDataArray0),
													lResolutionX,
													lResolutionY,
													lResolutionZ);

		lClearVolumeRenderer.requestDisplay();
		Thread.sleep(2000);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
													ByteBuffer.wrap(lVolumeDataArray1),
													lResolutionX,
													lResolutionY,
													lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			/*lClearVolumeRenderer.setLayerVisible(	i % 2,
																						!lClearVolumeRenderer.isLayerVisible(i % 2));/**/

			// lClearVolumeRenderer.requestDisplay();
			i++;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith8BitGeneratedDataset3LayersOpenCL()	throws InterruptedException,
															IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																							// new
																							// JCudaClearVolumeRenderer(
																							// "ClearVolumeTest",
																							1024,
																							1024,
																							NativeTypeEnum.UnsignedByte,
																							1024,
																							1024,
																							3,
																							false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
													ByteBuffer.wrap(lVolumeDataArray0),
													lResolutionX,
													lResolutionY,
													lResolutionZ);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
													ByteBuffer.wrap(lVolumeDataArray1),
													lResolutionX,
													lResolutionY,
													lResolutionZ);/**/

		final byte[] lVolumeDataArray2 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = lResolutionX / 2
										+ x
										+ lResolutionX
										* (lResolutionY / 2 + y)
										+ lResolutionX
										* lResolutionY
										* (lResolutionZ / 2 + z);
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y + z) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray2[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	2,
													ByteBuffer.wrap(lVolumeDataArray2),
													lResolutionX,
													lResolutionY,
													lResolutionZ);/**/

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			lClearVolumeRenderer.setLayerVisible(	i % 3,
													!lClearVolumeRenderer.isLayerVisible(i % 3));/**/

			i++;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith8BitGeneratedDataset3LayersCuda()	throws InterruptedException,
															IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																								1024,
																								1024,
																								NativeTypeEnum.UnsignedByte,
																								1024,
																								1024,
																								3,
																								false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
													ByteBuffer.wrap(lVolumeDataArray0),
													lResolutionX,
													lResolutionY,
													lResolutionZ);

		lClearVolumeRenderer.requestDisplay();
		Thread.sleep(2000);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
										* y
										+ lResolutionX
										* lResolutionY
										* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
													ByteBuffer.wrap(lVolumeDataArray1),
													lResolutionX,
													lResolutionY,
													lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		final byte[] lVolumeDataArray2 = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = lResolutionX / 2
										+ x
										+ lResolutionX
										* (lResolutionY / 2 + y)
										+ lResolutionX
										* lResolutionY
										* (lResolutionZ / 2 + z);
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y + z) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray2[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	2,
													ByteBuffer.wrap(lVolumeDataArray2),
													lResolutionX,
													lResolutionY,
													lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			lClearVolumeRenderer.setLayerVisible(	i % 3,
													!lClearVolumeRenderer.isLayerVisible(i % 3));/**/

			lClearVolumeRenderer.requestDisplay();
			i++;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith8BitGeneratedDatasetRandomStackSizesContiguousMemoryMultiChannel()	throws InterruptedException,
																							IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																												1024,
																												1024,
																												NativeTypeEnum.UnsignedByte,
																												1024,
																												1024,
																												2,
																												false);
		lClearVolumeRenderer.setTransferFunction(	0,
													TransferFunctions.getHot());
		lClearVolumeRenderer.setTransferFunction(	1,
													TransferFunctions.getGreenGradient());

		lClearVolumeRenderer.setLayerVisible(0, true);
		lClearVolumeRenderer.setLayerVisible(1, true);

		lClearVolumeRenderer.setVisible(true);

		while (lClearVolumeRenderer.isShowing())
		{
			{
				final int lSize = 128;

				final int lResolutionX = (int) (lSize + Math.random() * lSize
														/ 2);
				final int lResolutionY = (int) (lSize + Math.random() * lSize
														/ 2);
				final int lResolutionZ = (int) (lSize + Math.random() * lSize
														/ 2);/**/

				/*
				 * final int lResolutionX = 2 * 256; final int lResolutionY =
				 * lResolutionX; final int lResolutionZ = lResolutionX;/*
				 */

				lClearVolumeRenderer.setVolumeDataUpdateAllowed(false);

				for (int c = 0; c < 2; c++)
				{
					System.out.println(c);
					final ContiguousMemoryInterface lBuffer = OffHeapMemory.allocateBytes(lResolutionX * lResolutionY
																							* lResolutionZ);
					final ContiguousBuffer lContiguousBuffer = new ContiguousBuffer(lBuffer);

					for (int z = 0; z < lResolutionZ; z++)
						for (int y = 0; y < lResolutionY; y++)
							for (int x = 0; x < lResolutionX; x++)
							{
								int lCharValue = (((byte) x ^ (byte) y
													^ (byte) z ^ (byte) (17 * c)));
								if (lCharValue < 12)
									lCharValue = 0;
								lContiguousBuffer.writeByte((byte) lCharValue);
							}

					lClearVolumeRenderer.setVolumeDataBuffer(	0,
																TimeUnit.SECONDS,
																c,
																lBuffer,
																lResolutionX,
																lResolutionY,
																lResolutionZ);
				}

				lClearVolumeRenderer.setVolumeDataUpdateAllowed(true);

			}
			System.gc();

			Thread.sleep(100);
		}

		lClearVolumeRenderer.close();

	}

}
