package clearvolume.network.client;

import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeUnit;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.sink.AsynchronousVolumeSinkAdapter;
import clearvolume.volume.sink.ClearVolumeRendererSink;

public abstract class ClearVolumeTCPClientHelper
{
	private static final int cMaxAvailableVolumes = 20;
	private static final int cMaxQueueLength = 20;
	private static final long cMaxMillisecondsToWait = 10;
	private static final long cMaxMillisecondsToWaitForCopy = 10;

	public void startClient(String pServerAddress,
													int pPortNumber,
													int pWindowSize,
													int pBytesPerVoxel)
	{
		try (final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer("ClearVolume[" + pServerAddress
																																																									+ ":"
																																																									+ pPortNumber
																																																									+ "]",
																																																							pWindowSize,
																																																							pWindowSize,
																																																							pBytesPerVoxel))
		{
			try
			{
				lClearVolumeRenderer.setTransfertFunction(TransferFunctions.getGrayLevel());

				ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																												lClearVolumeRenderer.createCompatibleVolumeManager(cMaxAvailableVolumes),
																																												cMaxMillisecondsToWaitForCopy,
																																												TimeUnit.MILLISECONDS);

				AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lClearVolumeRendererSink,
																																																					cMaxQueueLength,
																																																					cMaxMillisecondsToWait,
																																																					TimeUnit.MILLISECONDS);

				ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lAsynchronousVolumeSinkAdapter);

				SocketAddress lClientSocketAddress = new InetSocketAddress(	pServerAddress,
																																		pPortNumber);
				assertTrue(lClearVolumeTCPClient.open(lClientSocketAddress));

				assertTrue(lClearVolumeTCPClient.start());

				assertTrue(lAsynchronousVolumeSinkAdapter.start());

				lClearVolumeRenderer.setVisible(true);

				while (lClearVolumeRenderer.isShowing())
				{
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				assertTrue(lAsynchronousVolumeSinkAdapter.stop());
				assertTrue(lClearVolumeTCPClient.stop());
				lClearVolumeTCPClient.close();
			}
			catch (UnresolvedAddressException uae)
			{
				reportError(uae, "Cannot find host: '" + pServerAddress + "'");
			}
			catch (Throwable e)
			{
				reportError(e, e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	public abstract void reportError(Throwable e, String pErrorMessage);

}
