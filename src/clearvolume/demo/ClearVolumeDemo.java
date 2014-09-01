package clearvolume.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.controller.ExternalController;
import clearvolume.jcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.ProjectionAlgorithm;
import clearvolume.transfertf.TransfertFunctions;

public class ClearVolumeTests
{

	private static JCudaClearVolumeRenderer mJCudaClearVolumeRenderer;

	@Test
	public void testWith8BitGeneratedDataset() throws InterruptedException,
																						IOException
	{
		final JCudaClearVolumeRenderer lJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																														768,
																																														768);
		lJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGreenGradient());
		lJCudaClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 64;
		final int lResolutionY = 64;
		final int lResolutionZ = 64;

		// System.out.println(i);

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
					/*(byte) ((x / 3) ^ (y)
																					| (y / 3)
																					^ (z) | (z / 3) ^ (x));/**/
				}

		lJCudaClearVolumeRenderer.setVolumeDataBuffer(ByteBuffer.wrap(lVolumeDataArray),
																									lResolutionX,
																									lResolutionY,
																									lResolutionZ);

		for (int i = 0; lJCudaClearVolumeRenderer.isShowing(); i++)
		{
			Thread.sleep(500);
			// lJCudaClearVolumeRenderer.requestDisplay();

		}

		lJCudaClearVolumeRenderer.close();
		// Thread.sleep(000);

	}

	@Test
	public void testWith16BitGeneratedDataset()	throws InterruptedException,
																							IOException
	{
		final JCudaClearVolumeRenderer lJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																														768,
																																														768,
																																														2);
		lJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
		lJCudaClearVolumeRenderer.setVisible(true);
		// lJCudaClearVolumeRenderer.start();

		final int lResolutionX = 256;
		final int lResolutionY = 256;
		final int lResolutionZ = 256;

		// System.out.println(i);

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ
																							* 2];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
																													* z);
					// lVolumeDataArray[lIndex] = (byte) (0);
					lVolumeDataArray[lIndex + 1] = (byte) ((x ^ y ^ z));// (byte) ((x
																															// ^ y ^
					// z)/16);
					/*(byte) ((x / 3) ^ (y)
																					| (y / 3)
																					^ (z) | (z / 3) ^ (x));/**/
				}

		lJCudaClearVolumeRenderer.setVolumeDataBuffer(ByteBuffer.wrap(lVolumeDataArray),
																									lResolutionX,
																									lResolutionY,
																									lResolutionZ);

		while (lJCudaClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

		lJCudaClearVolumeRenderer.close();
		// Thread.sleep(000);

	}

	@Test
	public void testWithGeneratedDatasetWithEgg3D()	throws InterruptedException,
																									IOException
	{

		final JCudaClearVolumeRenderer lJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																														512,
																																														512);
		lJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
		lJCudaClearVolumeRenderer.setVisible(true);
		lJCudaClearVolumeRenderer.setProjectionAlgorythm(ProjectionAlgorithm.MaxProjection);

		ExternalController lEgg3DController = null;
		try
		{
			lEgg3DController = new ExternalController(ExternalController.Egg3DTCPport,
																								lJCudaClearVolumeRenderer);
			lJCudaClearVolumeRenderer.setQuaternionController(lEgg3DController);
			lEgg3DController.connectAsynchronouslyOrWait();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}

		final int lResolutionX = 128;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		// System.out.println(i);

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
		lJCudaClearVolumeRenderer.setVolumeDataBuffer(lWrappedArray,
																									lResolutionX,
																									lResolutionY,
																									lResolutionZ);

		for (int i = 0; lJCudaClearVolumeRenderer.isShowing(); i++)
		{
			Thread.sleep(100);
		}

		lJCudaClearVolumeRenderer.close();
		// Thread.sleep(000);
		if (lEgg3DController != null)
			lEgg3DController.close();

	}

	@Test
	public void testWithFileDatasets()
	{

		try
		{
			// startSample("./data/Bucky.raw", 32, 32, 32);

			// Other input files may be obtained from http://www.volvis.org
			// startSample("./databig/celegans.raw", 512, 512, 512);
			// startSample("./data/Bucky.raw", 1,32, 32, 32);
			startSample("./databig/backpack16.raw", 2, 512, 512, 373);
			// startSample("./databig/test1024^3.raw", 1, 1024, 1024, 1024);
			// startSample("./data/test2048^3.raw", 2048, 2048, 2048);

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

	}

	private static void startSample(final String pRessourceName,
																	final int pBytesPerVoxel,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ)	throws IOException,
																										InterruptedException
	{
		final InputStream lResourceAsStream = ClearVolumeTests.class.getResourceAsStream(pRessourceName);
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

		// Start the sample with the data that was read from the file
		final ByteBuffer lVolumeData = ByteBuffer.wrap(data);

		mJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																															512,
																															512,
																															pBytesPerVoxel);
		mJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getRainbow());
		// mJCudaClearVolumeRenderer.setupControlFrame();

		/*final Egg3DController lEgg3DController = new Egg3DController();
		mJCudaClearVolumeRenderer.setQuaternionController(lEgg3DController);
		assertTrue(lEgg3DController.connect());/**/

		mJCudaClearVolumeRenderer.setVolumeDataBuffer(ByteBuffer.wrap(data),
																									pSizeX,
																									pSizeY,
																									pSizeZ);
		mJCudaClearVolumeRenderer.setVisible(true);

		while (mJCudaClearVolumeRenderer.isShowing())
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
		final InputStream lResourceAsStream = ClearVolumeTests.class.getResourceAsStream(pRessourceName);

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
