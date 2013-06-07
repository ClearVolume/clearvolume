package clearvolume.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.swing.SwingUtilities;

import org.junit.Test;

import clearvolume.jcuda.JCudaClearVolumeRenderer;

public class ClearVolumeTests
{

	private static JCudaClearVolumeRenderer mJCudaClearVolumeRenderer;

	@Test
	public void test()
	{

		try
		{
			// startSample("./data/Bucky.raw", 32, 32, 32);

			// Other input files may be obtained from http://www.volvis.org
			// startSample("mri_ventricles.raw", 256, 256, 124);
			startSample("./data/Bucky.raw", 32, 32, 32);
			// startSample("vertebra8.raw", 512, 512, 512);

			// startSample("foot.raw", 256, 256, 256);
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
		SwingUtilities.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				mJCudaClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																	768,
																																	768,
																																	capabilities,
																																	lVolumeData,
																																	sizeX,
																																	sizeY,
																																	sizeZ);
			}
		});

		Thread.sleep(1000);

		/**/

		final byte[] lLoadData = loadData("./data/Bucky.raw",
																			512,
																			512,
																			512);
		final ByteBuffer lWrap = ByteBuffer.wrap(lLoadData);

		while (true)
		{

			for (int i = 0; i < lLoadData.length - 1; i++)
			{
				lLoadData[i] = lLoadData[i + 1];
			}

			lLoadData[lLoadData.length - 1] = lLoadData[0];

			mJCudaClearVolumeRenderer.setVolumeDataBuffer(lWrap);
			System.out.println("update");
		}

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
