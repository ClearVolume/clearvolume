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
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.VolumeSinkInterface;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;
import clearvolume.volume.sink.timeshift.MultiChannelTimeShiftingSink;
import clearvolume.volume.sink.timeshift.gui.MultiChannelTimeShiftingSinkJFrame;

public abstract class ClearVolumeTCPClientHelper
{
	private static final int cMaxAvailableVolumes = 20;
	private static final int cMaxQueueLength = 20;
	private static final long cMaxMillisecondsToWait = 10;
	private static final long cMaxMillisecondsToWaitForCopy = 10;
	private static final long cSoftHoryzon = 50;
	private static final long cHardHoryzon = 100;

	public void startClient(String pServerAddress,
													int pPortNumber,
													int pWindowSize,
													int pBytesPerVoxel,
													boolean pTimeShiftMultiChannel,
													boolean pMultiColor)
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

				VolumeSinkInterface lSinkAfterAsynchronousVolumeSinkAdapter = lClearVolumeRendererSink;

				MultiChannelTimeShiftingSink lMultiChannelTimeShiftingSink = null;
				MultiChannelTimeShiftingSinkJFrame lMultiChannelTimeShiftingSinkJFrame = null;
				if (pTimeShiftMultiChannel)
				{
					lMultiChannelTimeShiftingSink = new MultiChannelTimeShiftingSink(	cSoftHoryzon,
																																						cHardHoryzon);

					lMultiChannelTimeShiftingSinkJFrame = new MultiChannelTimeShiftingSinkJFrame(lMultiChannelTimeShiftingSink);
					lMultiChannelTimeShiftingSinkJFrame.setVisible(true);

					lMultiChannelTimeShiftingSink.setRelaySink(lClearVolumeRendererSink);

					lClearVolumeRendererSink.setRelaySink(new NullVolumeSink());

					lSinkAfterAsynchronousVolumeSinkAdapter = lMultiChannelTimeShiftingSink;
				}

				AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lSinkAfterAsynchronousVolumeSinkAdapter,
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
				if (lMultiChannelTimeShiftingSink != null)
				{
					lMultiChannelTimeShiftingSinkJFrame.setVisible(false);
					lMultiChannelTimeShiftingSinkJFrame.dispose();
					lMultiChannelTimeShiftingSink.close();
				}
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
