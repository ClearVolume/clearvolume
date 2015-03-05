package clearvolume.renderer.factory.test;

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
			final ClearVolumeRendererInterface lNewBestRenderer = ClearVolumeRendererFactory.newBestRenderer8Bit(	"Test",
			                                                                                                     	128,
			                                                                                                     	128,
			                                                                                                     	false);
			System.out.println(lNewBestRenderer.getClass());
			// assertNotNull(lNewBestRenderer);
		}
		catch (final Throwable e)
		{
			System.err.println("!!! COULD NOT BUILD ANY RENDERER NEITHER WITH CUDA OR OPENCL !!!");
			e.printStackTrace();
			fail();
		}
	}

}
