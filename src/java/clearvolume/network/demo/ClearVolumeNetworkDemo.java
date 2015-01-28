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
import clearvolume.network.server.ClearVolumeTCPServerSink;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;

public class ClearVolumeNetworkDemo
{
	private static final int cSizeMultFactor = 1;
	private static final int cWidth = 128 * cSizeMultFactor;
	private static final int cHeight = 128 * cSizeMultFactor + 1;
	private static final int cDepth = 128 * cSizeMultFactor + 3;
	private static final int cNumberOfVolumes = 10000000;
	private static final double cSecondsPerTimePoint = 0.1;

	private static final int cNumberOfAvailableVolumes = 10;
	private static final int cVolumeQueueLength = 11;

	@Test
	public void startServerOneChannel()
	{
		startServer(1);
	}

	@Test
	public void startServerTwoChannels()
	{
		startServer(2);
	}

	public void startServer(long pNumberOfChannels)
	{
		try
		{
			VolumeManager lVolumeManager = new VolumeManager(cNumberOfAvailableVolumes);

			ClearVolumeTCPServerSink lClearVolumeTCPServerSink = new ClearVolumeTCPServerSink(cNumberOfAvailableVolumes);
			lClearVolumeTCPServerSink.setRelaySink(new NullVolumeSink(lVolumeManager));

			SocketAddress lServerSocketAddress = new InetSocketAddress(ClearVolumeSerialization.cStandardTCPPort);
			assertTrue(lClearVolumeTCPServerSink.open(lServerSocketAddress));
			assertTrue(lClearVolumeTCPServerSink.start());

			for (long i = 0; i < cNumberOfVolumes; i++)
			{
				try
				{
					if (i % 1000 == 0)
						System.out.println("sending volume with index=" + i);
					Volume<Character> lVolume = lVolumeManager.requestAndWaitForVolume(	1,
							TimeUnit.MILLISECONDS,
							Character.class,
							1,
							cWidth,
							cHeight,
							cDepth);

					final long lTimePoint = i / pNumberOfChannels;
					final int lChannleID = (int) (i % pNumberOfChannels);

					lVolume.setTimeIndex(lTimePoint);
					lVolume.setTimeInSeconds(lTimePoint * cSecondsPerTimePoint);
					lVolume.setChannelID(lChannleID);
					lVolume.setChannelName("channel " + lChannleID);

					ByteBuffer lVolumeData = lVolume.getDataBuffer();

					lVolumeData.rewind();
					for (int j = 0; j < cDepth * cHeight * cWidth * 2; j++)
						lVolumeData.put((byte) 0);

					lVolumeData.rewind();
					for (int z = 0; z < (i + 10) % cDepth; z++)
						for (int y = 0; y < cHeight / (1 + lChannleID); y++)
							for (int x = cWidth / 2 * (lChannleID); x < cWidth; x++)
							{
								char lValue = (char) (512 * ((char) x ^ (char) y ^ (char) z));

								lVolumeData.putChar(lValue);
							}

					lClearVolumeTCPServerSink.sendVolume(lVolume);
					Thread.sleep(100);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
			Thread.sleep(1000);

			assertTrue(lClearVolumeTCPServerSink.stop());
			lClearVolumeTCPServerSink.close();
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
				256,
				2,
				256,
				256,
				2,
				false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
				lClearVolumeRenderer.createCompatibleVolumeManager(cNumberOfAvailableVolumes),
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
