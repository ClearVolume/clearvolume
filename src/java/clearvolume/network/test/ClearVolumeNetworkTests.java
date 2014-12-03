package clearvolume.network.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.network.server.ClearVolumeTCPServerSink;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.VolumeSinkAdapter;
import clearvolume.volume.sink.VolumeSinkInterface;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;

public class ClearVolumeNetworkTests
{
	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 127 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor;
	private static final int cDepth = 129 * cSizeMultFactor;

	private static final int cNumberOfAvailableVolumes = 10;
	private static final int cVolumeQueueLength = 11;

	private static final int cNumberOfVolumesToSend = 22;

	@Test
	public void testConsole() throws IOException, InterruptedException
	{
		final VolumeSinkInterface lVolumeSink = new VolumeSinkAdapter(cNumberOfAvailableVolumes)
		{
			@Override
			public void sendVolume(final Volume<?> pVolume)
			{
				// System.out.println("Received volume:" + pVolume);
			}
		};
		networkConduit(lVolumeSink, cNumberOfVolumesToSend, true);
	}

	// @Test
	public void testLive() throws IOException, InterruptedException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														256,
																																														256);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																													lClearVolumeRenderer.createCompatibleVolumeManager(cVolumeQueueLength),
																																													100,
																																													TimeUnit.MILLISECONDS);
		networkConduit(	lClearVolumeRendererSink,
										cNumberOfVolumesToSend,
										false);

		lClearVolumeRenderer.close();

	}

	private void networkConduit(final VolumeSinkInterface lVolumeSink,
															final int pNumberOfVolumes,
															final boolean pClose)	throws IOException,
																										InterruptedException
	{
		final int lPortRandomizer = (int) (Math.random() * 100);

		final VolumeManager lVolumeManager = new VolumeManager(cNumberOfAvailableVolumes);
		final ClearVolumeTCPServerSink lClearVolumeTCPServerSink = new ClearVolumeTCPServerSink(cVolumeQueueLength);
		lClearVolumeTCPServerSink.setRelaySink(new NullVolumeSink(lVolumeManager));

		final SocketAddress lServerSocketAddress = new InetSocketAddress(ClearVolumeSerialization.cStandardTCPPort + lPortRandomizer);
		assertTrue(lClearVolumeTCPServerSink.open(lServerSocketAddress));
		assertTrue(lClearVolumeTCPServerSink.start());

		final ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lVolumeSink);

		final SocketAddress lClientSocketAddress = new InetSocketAddress(	"localhost",
																																			ClearVolumeSerialization.cStandardTCPPort + lPortRandomizer);
		assertTrue(lClearVolumeTCPClient.open(lClientSocketAddress));

		assertTrue(lClearVolumeTCPClient.start());

		final Volume<Byte> lVolume = lClearVolumeTCPServerSink.getManager()
																													.requestAndWaitForVolume(	1,
																																										TimeUnit.MILLISECONDS,
																																										Byte.class,
																																										1,
																																										cWidth,
																																										cHeight,
																																										cDepth);
		for (int i = 0; i < pNumberOfVolumes; i++)
		{
			final ByteBuffer lVolumeData = lVolume.getDataBuffer();

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

			lClearVolumeTCPServerSink.sendVolume(lVolume);
			Thread.sleep(10);
		}
		Thread.sleep(500);
		if (pClose)
		{
			lClearVolumeTCPClient.stop();

			lClearVolumeTCPClient.close();

			lClearVolumeTCPServerSink.stop();
			lClearVolumeTCPServerSink.close();
		}/**/
	}
}
