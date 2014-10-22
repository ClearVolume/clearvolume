package clearvolume.demo;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.TransfertFunctions;

public class BoxTest
{

	@Test
	public void demoWithBox() throws InterruptedException, IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														512,
																																														512);
		lClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
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
					char lCharValue = (char) (((byte) y));
					if (lCharValue < 12)
						lCharValue = 0;
					// float lFloatValue = (float) log1p(lByteValue);
					lVolumeDataArray[lIndex] = (byte) lCharValue;
					/*(byte) ((x / 3) ^ (y)
																					| (y / 3)
																					^ (z) | (z / 3) ^ (x));/**/
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		for (int i = 0; lClearVolumeRenderer.isShowing(); i++)
		{
			Thread.sleep(500);
			// lJCudaClearVolumeRenderer.requestDisplay();

		}

		lClearVolumeRenderer.close();
		// Thread.sleep(000);

	}

}
