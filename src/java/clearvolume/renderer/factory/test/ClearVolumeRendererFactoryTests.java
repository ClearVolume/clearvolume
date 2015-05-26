package clearvolume.renderer.factory.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.renderer.opencl.OpenCLAvailability;

import com.jogamp.opengl.GL;

import coremem.types.NativeTypeEnum;

public class ClearVolumeRendererFactoryTests
{

	@Test
	public void testBestRenderer8Bit()
	{
		if (!CudaAvailability.isClearCudaOperational() && !OpenCLAvailability.isOpenCLAvailable())
			return;

		try
		{
			final ClearVolumeRendererInterface lNewBestRenderer = ClearVolumeRendererFactory.newBestRenderer8Bit(	"Test",
																																																						128,
																																																						128,
																																																						false);
			System.out.println(lNewBestRenderer.getClass());
			assertNotNull(lNewBestRenderer);
		}
		catch (final Throwable e)
		{
			System.err.println("!!! COULD NOT BUILD ANY RENDERER NEITHER WITH CUDA OR OPENCL !!!");
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testBestRenderer16Bit()
	{
		if (!CudaAvailability.isClearCudaOperational() && !OpenCLAvailability.isOpenCLAvailable())
			return;

		try
		{
			final ClearVolumeRendererInterface lNewBestRenderer = ClearVolumeRendererFactory.newBestRenderer16Bit("Test",
																																																						128,
																																																						128,
																																																						false);
			System.out.println(lNewBestRenderer.getClass());
			assertNotNull(lNewBestRenderer);
		}
		catch (final Throwable e)
		{
			System.err.println("!!! COULD NOT BUILD ANY RENDERER NEITHER WITH CUDA OR OPENCL !!!");
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testIsolatedRenderer()
	{
		if (!CudaAvailability.isClearCudaOperational() && !OpenCLAvailability.isOpenCLAvailable())
			return;

		try
		{
			ClearVolumeRendererFactory.setClassLoaderIsolation(true);

			final String lJoglJar = GL.class.getProtectionDomain()
																			.getCodeSource()
																			.getLocation()
																			.toString()
																			.replaceAll("file:", "");

			final File lJoglJarFile = new File(lJoglJar);

			if (!lJoglJarFile.exists())
				System.out.println("JAR DOES NOT EXIST");

			System.out.println("lJoglJar='" + lJoglJar + "'");

			ClearVolumeRendererFactory.getPrivateJarList().add(lJoglJar);

			final ClearVolumeRendererInterface lNewBestRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"test",
																																																					128,
																																																					128,
																																																					NativeTypeEnum.UnsignedByte,
																																																					1024,
																																																					1024,
																																																					1,
																																																					false);
			ClearVolumeRendererFactory.setClassLoaderIsolation(false);

			lNewBestRenderer.setVisible(true);

			Thread.sleep(10000);

			System.out.println(lNewBestRenderer.getClass());
			assertNotNull(lNewBestRenderer);
		}
		catch (final Throwable e)
		{
			System.err.println("!!! COULD NOT BUILD ANY RENDERER NEITHER WITH CUDA OR OPENCL !!!");
			e.printStackTrace();
			fail();
		}
	}

}
