package clearvolume.network.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.network.server.ClearVolumeTCPServer;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.TransfertFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.sink.ClearVolumeRendererSink;
import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeNetworkTests
{
	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 127 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor;
	private static final int cDepth = 129 * cSizeMultFactor;

	@Test
	public void testConsole() throws IOException, InterruptedException
	{
		VolumeSinkInterface lVolumeSink = new VolumeSinkInterface()
		{
			@Override
			public void sendVolume(Volume<?> pVolume)
			{
				System.out.println("Received volume:" + pVolume);
			}
		};
		networkConduit(lVolumeSink, 10, true);
	}

	@Test
	public void testLive() throws IOException, InterruptedException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														256,
																																														256);
		lClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(lClearVolumeRenderer);
		networkConduit(lClearVolumeRendererSink, 10, false);

		lClearVolumeRenderer.close();
	}

	private void networkConduit(VolumeSinkInterface lVolumeSink,
															int pNumberOfVolumes,
															boolean pClose)	throws IOException,
																							InterruptedException
	{
		int lPortRandomizer = (int) (Math.random() * 100);

		ClearVolumeTCPServer lClearVolumeTCPServer = new ClearVolumeTCPServer(10);

		SocketAddress lServerSocketAddress = new InetSocketAddress(ClearVolumeSerialization.cStandardTCPPort + lPortRandomizer);
		assertTrue(lClearVolumeTCPServer.open(lServerSocketAddress));
		assertTrue(lClearVolumeTCPServer.start());

		ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lVolumeSink);

		SocketAddress lClientSocketAddress = new InetSocketAddress(	"localhost",
																																ClearVolumeSerialization.cStandardTCPPort + lPortRandomizer);
		assertTrue(lClearVolumeTCPClient.open(lClientSocketAddress));

		assertTrue(lClearVolumeTCPClient.start());

		Volume<Byte> lVolume = new Volume<Byte>(Byte.class,
																						1,
																						cWidth,
																						cHeight,
																						cDepth);
		for (int i = 0; i < pNumberOfVolumes; i++)
		{
			ByteBuffer lVolumeData = lVolume.getVolumeData();

			lVolumeData.rewind();
			for (int z = 0; z < cWidth; z++)
				for (int y = 0; y < cHeight; y++)
					for (int x = 0; x < cDepth; x++)
					{
						byte lCharValue = (byte) ((byte) x * i ^ (byte) y ^ (byte) z);
						if (lCharValue < 12)
							lCharValue = 0;

						lVolumeData.put(lCharValue);
					}

			lClearVolumeTCPServer.sendVolume(lVolume);
			Thread.sleep(10);
		}
		Thread.sleep(500);

		/*while (true)
			;/**/

		if (pClose)
		{
			lClearVolumeTCPClient.stop();

			lClearVolumeTCPClient.close();

			lClearVolumeTCPServer.stop();
			lClearVolumeTCPServer.close();/**/
		}
	}
}
