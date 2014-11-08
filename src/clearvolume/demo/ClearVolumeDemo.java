package clearvolume.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.controller.ExternalRotationController;
import clearvolume.projections.ProjectionAlgorithm;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.transferf.TransferFunctions;

public class ClearVolumeDemo
{

	private static ClearVolumeRendererInterface mClearVolumeRenderer;

	@Test
	public void demoWith8BitGeneratedDataset() throws InterruptedException,
																						IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														1024,
																																														1024,
																																														1,
																																														512,
																																														512,
																																														1);
		lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
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
					lVolumeDataArray[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setCurrentRenderLayer(0);
		lClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(lVolumeDataArray),
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
	public void demoAspectRatio()	throws InterruptedException,
																IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														512,
																																														512,
																																														1,
																																														512,
																																														512,
																																														1);
		lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 400;
		final int lResolutionY = 100;
		final int lResolutionZ = 200;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
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

					lVolumeDataArray[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setCurrentRenderLayer(0);
		lClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(lVolumeDataArray),
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
		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					768,
																																																					768,
																																																					2);
		lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = 256;
		final int lResolutionZ = 256;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ
																							* 2];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
																													* z);
					lVolumeDataArray[lIndex + 1] = (byte) (((byte) x ^ (byte) y ^ (byte) z));
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

		lClearVolumeRenderer.close();

	}

	@Test
	public void demoWithGeneratedDatasetWithEgg3D()	throws InterruptedException,
																									IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					512,
																																																					512);
		lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);
		lClearVolumeRenderer.setProjectionAlgorythm(ProjectionAlgorithm.MaxProjection);

		ExternalRotationController lEgg3DController = null;
		try
		{
			lEgg3DController = new ExternalRotationController(ExternalRotationController.cDefaultEgg3DTCPport,
																												lClearVolumeRenderer);
			lClearVolumeRenderer.setQuaternionController(lEgg3DController);
			lEgg3DController.connectAsynchronouslyOrWait();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}

		final int lResolutionX = 128;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
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
					lVolumeDataArray[lIndex] = (byte) (x ^ y ^ z);
				}

		final ByteBuffer lWrappedArray = ByteBuffer.wrap(lVolumeDataArray);
		lClearVolumeRenderer.setVolumeDataBuffer(	lWrappedArray,
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

		lClearVolumeRenderer.close();

		if (lEgg3DController != null)
			lEgg3DController.close();

	}

	@Test
	public void demoWithFileDatasets()
	{

		try
		{
			startSample("./data/Bucky.raw", 1, 32, 32, 32);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

	}

	@Test
	public void demoWith8BitGeneratedDataset2Layers()	throws InterruptedException,
																										IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														512,
																																														512,
																																														1,
																																														512,
																																														512,
																																														2);

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

		lClearVolumeRenderer.setCurrentRenderLayer(0);
		lClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(lVolumeDataArray0),
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

		lClearVolumeRenderer.setCurrentRenderLayer(1);
		lClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(lVolumeDataArray1),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
		}

		lClearVolumeRenderer.close();
	}

	private static void startSample(final String pRessourceName,
																	final int pBytesPerVoxel,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ)	throws IOException,
																										InterruptedException
	{
		final InputStream lResourceAsStream = ClearVolumeDemo.class.getResourceAsStream(pRessourceName);
		startSample(lResourceAsStream,
								pBytesPerVoxel,
								pSizeX,
								pSizeY,
								pSizeZ);
	}

	private static void startSample(final InputStream pInputStream,
																	final int pBytesPerVoxel,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ)	throws IOException,
																										InterruptedException
	{

		final byte[] data = loadData(	pInputStream,
																	pBytesPerVoxel,
																	pSizeX,
																	pSizeY,
																	pSizeZ);

		mClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer("ClearVolumeTest",
																																			512,
																																			512,
																																			pBytesPerVoxel);

		mClearVolumeRenderer.setTransfertFunction(TransferFunctions.getRainbow());
		mClearVolumeRenderer.setVisible(true);

		mClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(data),
																							pSizeX,
																							pSizeY,
																							pSizeZ);

		mClearVolumeRenderer.requestDisplay();

		while (mClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

	}

	private static byte[] loadData(	final String pRessourceName,
																	final int pBytesPerVoxel,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException
	{
		final InputStream lResourceAsStream = ClearVolumeDemo.class.getResourceAsStream(pRessourceName);

		return loadData(lResourceAsStream,
										pBytesPerVoxel,
										sizeX,
										sizeY,
										sizeZ);
	}

	private static byte[] loadData(	final InputStream pInputStream,
																	final int pBytesPerVoxel,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException
	{
		// Try to read the specified file
		byte data[] = null;
		final InputStream fis = pInputStream;
		try
		{
			final int size = pBytesPerVoxel * sizeX * sizeY * sizeZ;
			data = new byte[size];
			fis.read(data);
		}
		catch (final IOException e)
		{
			System.err.println("Could not load input file");
			e.printStackTrace();
			return null;
		}
		fis.close();
		return data;
	}

}
