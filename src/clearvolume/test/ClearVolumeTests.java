package clearvolume.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import jssc.SerialPortException;

import org.junit.Test;

import clearvolume.controller.Egg3DController;
import clearvolume.jcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.ProjectionAlgorithm;
import clearvolume.transfertf.TransfertFunctions;

public class ClearVolumeTests
{

	private static JCudaClearVolumeRenderer mJCudaClearVolumeRenderer;

	@Test
	public void testWithGeneratedDataset() throws InterruptedException,
																				IOException
	{
		final JCudaClearVolumeRenderer lJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																														768,
																																														768);
		lJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGreenGradient());
		lJCudaClearVolumeRenderer.setVisible(true);

		lJCudaClearVolumeRenderer.start();

		for (int i = 0; lJCudaClearVolumeRenderer.isShowing(); i++)
		{
			Thread.sleep(500);
			final int lResolutionX = 128 + (i % 256);
			final int lResolutionY = 256 + (i % 256);
			final int lResolutionZ = 128 + (i % 256);

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
		lJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getRainbow());
		lJCudaClearVolumeRenderer.setVisible(true);

		lJCudaClearVolumeRenderer.start();

		final int lResolutionX = 128;
		final int lResolutionY = 128;
		final int lResolutionZ = 128;

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
					lVolumeDataArray[lIndex] = (byte) (0);
					lVolumeDataArray[lIndex + 1] = (byte) ((x + y + z) % 4);// (byte) ((x
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
																									IOException,
																									SerialPortException
	{
		final Egg3DController lEgg3DController = new Egg3DController();

		final JCudaClearVolumeRenderer lJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																														768,
																																														768);
		lJCudaClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
		lJCudaClearVolumeRenderer.setVisible(true);
		lJCudaClearVolumeRenderer.setProjectionAlgorythm(ProjectionAlgorithm.MaxProjection);

		lJCudaClearVolumeRenderer.setQuaternionController(lEgg3DController);

		assertTrue(lEgg3DController.connect());
		lJCudaClearVolumeRenderer.start();

		final int lResolutionX = 256;
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
		lEgg3DController.close();

	}

	@Test
	public void testWithFileDatasets()
	{

		try
		{
			// startSample("./data/Bucky.raw", 32, 32, 32);

			// Other input files may be obtained from http://www.volvis.org
			startSample("./databig/vertebra8.raw", 512, 512, 512);
			// startSample("./data/Bucky.raw", 32, 32, 32);
			// startSample("./databig/test1024^3.raw", 1024, 1024, 1024);
			// startSample("./data/test2048^3.raw", 2048, 2048, 2048);

		}
		catch (final Throwable e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void startSample(final String pRessourceName,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException,
																									InterruptedException
	{
		final InputStream lResourceAsStream = ClearVolumeTests.class.getResourceAsStream(pRessourceName);
		startSample(lResourceAsStream, sizeX, sizeY, sizeZ);
	}

	private static void startSample(final InputStream pInputStream,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException,
																									InterruptedException
	{

		final byte[] data = loadData(pInputStream, sizeX, sizeY, sizeZ);

		// Start the sample with the data that was read from the file
		final ByteBuffer lVolumeData = ByteBuffer.wrap(data);
		final GLProfile profile = GLProfile.getMaxFixedFunc(true);
		final GLCapabilities capabilities = new GLCapabilities(profile);

		mJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																															768,
																															768);

		final Egg3DController lEgg3DController = new Egg3DController();
		mJCudaClearVolumeRenderer.setQuaternionController(lEgg3DController);
		assertTrue(lEgg3DController.connect());

		mJCudaClearVolumeRenderer.setVolumeDataBuffer(ByteBuffer.wrap(data),
																									sizeX,
																									sizeY,
																									sizeZ);
		mJCudaClearVolumeRenderer.setVisible(true);

		mJCudaClearVolumeRenderer.start();

		while (mJCudaClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

		mJCudaClearVolumeRenderer.stop();
	}

	//
	private static byte[] loadData(	final String pRessourceName,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException
	{
		final InputStream lResourceAsStream = ClearVolumeTests.class.getResourceAsStream(pRessourceName);

		return loadData(lResourceAsStream, sizeX, sizeY, sizeZ);
	}

	private static byte[] loadData(	final InputStream pInputStream,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException
	{
		// Try to read the specified file
		byte data[] = null;
		final InputStream fis = pInputStream;
		try
		{
			final int size = sizeX * sizeY * sizeZ;
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
