package clearvolume.network.demo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.network.server.ClearVolumeTCPServer;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.TransfertFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.ClearVolumeRendererSink;

public class ClearVolumeNetworkDemo
{
	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 128 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor + 1;
	private static final int cDepth = 128 * cSizeMultFactor + 3;
	private static final int cNumberOfVolumes = 10000000;

	VolumeManager mVolumeManager = new VolumeManager(20);

	@Test
	public void startServer()
	{
		try
		{
			ClearVolumeTCPServer lClearVolumeTCPServer = new ClearVolumeTCPServer(10);

			SocketAddress lServerSocketAddress = new InetSocketAddress(ClearVolumeSerialization.cStandardTCPPort);
			assertTrue(lClearVolumeTCPServer.open(lServerSocketAddress));
			assertTrue(lClearVolumeTCPServer.start());

			Volume<Byte> lVolume = mVolumeManager.requestAndWaitForVolume(1,
																																		TimeUnit.MILLISECONDS,
																																		Byte.class,
																																		1,
																																		cWidth,
																																		cHeight,
																																		cDepth);
			ByteBuffer lVolumeData = lVolume.getVolumeData();

			lVolumeData.rewind();

			for (int i = 0; i < cWidth * cHeight * cDepth; i++)
				lVolumeData.put((byte) 32);

			for (long i = 0; i < cNumberOfVolumes; i++)
			{
				try
				{
					if (i % 1000 == 0)
						System.out.println("sending volume with index=" + i);
					lVolumeData = lVolume.getVolumeData();
					lVolumeData.rewind();
					for (int z = 0; z < cDepth; z++)
						for (int y = 0; y < cHeight; y++)
							for (int x = 0; x < cWidth; x++)
							{
								final int lIndex = x + cWidth
																		* y
																		+ cWidth
																		* cHeight
																		* z;

								byte lByteValue = (byte) ((byte) i ^ (byte) x
																					^ (byte) y ^ (byte) z);
								if (lByteValue < 3)
									lByteValue = 0;

								lVolumeData.put(lIndex, lByteValue);
							}/**/

					lClearVolumeTCPServer.sendVolume(lVolume);
					Thread.sleep(1);
				}
				catch (Throwable e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Thread.sleep(500);

			assertTrue(lClearVolumeTCPServer.stop());
			lClearVolumeTCPServer.close();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

	}

	@Test
	public void startClient() throws IOException, InterruptedException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														256,
																																														256);
		lClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																										10,
																																										TimeUnit.MILLISECONDS);

		ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lClearVolumeRendererSink);

		SocketAddress lClientSocketAddress = new InetSocketAddress(	"localhost",
																																ClearVolumeSerialization.cStandardTCPPort);
		assertTrue(lClearVolumeTCPClient.open(lClientSocketAddress));

		assertTrue(lClearVolumeTCPClient.start());

		while (lClearVolumeRenderer.isShowing())
			Thread.sleep(10);

		assertTrue(lClearVolumeTCPClient.stop());

		lClearVolumeTCPClient.close();

		lClearVolumeRenderer.close();

	}
}
