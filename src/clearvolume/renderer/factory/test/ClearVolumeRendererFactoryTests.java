package clearvolume.renderer.factory.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;

public class ClearVolumeRendererFactoryTests
{

	@Test
	public void test()
	{
		try
		{
			ClearVolumeRendererInterface lNewBestRenderer = ClearVolumeRendererFactory.newBestRenderer(	"Test",
																																																	128,
																																																	128);
			System.out.println(lNewBestRenderer.getClass());
			assertNotNull(lNewBestRenderer);
		}
		catch (Throwable e)
		{
			System.err.println("!!! COULD NOT BUILD ANY RENDERER NEITHER WITH CUDA OR OPENCL !!!");
			e.printStackTrace();
			fail();
		}
	}

}
