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
import clearvolume.volume.sink.filter.ChannelFilterSink;
import clearvolume.volume.sink.filter.gui.ChannelFilterSinkJFrame;
import clearvolume.volume.sink.relay.RelaySinkInterface;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;
import clearvolume.volume.sink.timeshift.TimeShiftingSink;
import clearvolume.volume.sink.timeshift.gui.TimeShiftingSinkJFrame;

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
													boolean pTimeShift,
													boolean pChannelFilter,
													int pNumberOfLayers)
	{
		try (final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer("ClearVolume[" + pServerAddress
																																																									+ ":"
																																																									+ pPortNumber
																																																									+ "]",
																																																							pWindowSize,
																																																							pWindowSize,
																																																							pBytesPerVoxel,
																																																							(int) (pWindowSize * 1.33),
																																																							(int) (pWindowSize * 1.33),
																																																							pNumberOfLayers))
		{
			try
			{
				lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());

				ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																												lClearVolumeRenderer.createCompatibleVolumeManager(cMaxAvailableVolumes),
																																												cMaxMillisecondsToWaitForCopy,
																																												TimeUnit.MILLISECONDS);

				RelaySinkInterface lSinkAfterAsynchronousVolumeSinkAdapter = lClearVolumeRendererSink;

				ChannelFilterSink lChannelFilterSink = null;
				ChannelFilterSinkJFrame lChannelFilterSinkJFrame = null;
				if (pChannelFilter)
				{
					lChannelFilterSink = new ChannelFilterSink();

					lChannelFilterSinkJFrame = new ChannelFilterSinkJFrame(lChannelFilterSink);
					lChannelFilterSinkJFrame.setVisible(true);

					lChannelFilterSink.setRelaySink(lClearVolumeRendererSink);

					lClearVolumeRendererSink.setRelaySink(new NullVolumeSink());

					lSinkAfterAsynchronousVolumeSinkAdapter = lChannelFilterSink;
				}

				TimeShiftingSink lTimeShiftingSink = null;
				TimeShiftingSinkJFrame lTimeShiftingSinkJFrame = null;
				if (pTimeShift)
				{
					lTimeShiftingSink = new TimeShiftingSink(	cSoftHoryzon,
																										cHardHoryzon);

					lTimeShiftingSinkJFrame = new TimeShiftingSinkJFrame(lTimeShiftingSink);
					lTimeShiftingSinkJFrame.setVisible(true);

					lTimeShiftingSink.setRelaySink(lSinkAfterAsynchronousVolumeSinkAdapter);

					lClearVolumeRendererSink.setRelaySink(new NullVolumeSink());

					lSinkAfterAsynchronousVolumeSinkAdapter = lTimeShiftingSink;
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
				if (lTimeShiftingSink != null)
				{
					lTimeShiftingSinkJFrame.setVisible(false);
					lTimeShiftingSinkJFrame.dispose();
					lTimeShiftingSink.close();
				}
				if (lChannelFilterSink != null)
				{
					lChannelFilterSinkJFrame.setVisible(false);
					lChannelFilterSinkJFrame.dispose();
					lChannelFilterSink.close();
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
