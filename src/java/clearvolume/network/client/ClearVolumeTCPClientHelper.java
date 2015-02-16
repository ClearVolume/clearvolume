package clearvolume.network.client;

import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.lang.exception.ExceptionUtils;

import clearvolume.renderer.VolumeCaptureListener;
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

	public void startClient(VolumeCaptureListener pVolumeCaptureListener,
													String pServerAddress,
													int pPortNumber,
													int pWindowSize,
													int pBytesPerVoxel,
													int pNumberOfLayers,
													boolean pTimeShift,
													boolean pChannelFilter)
	{
		final String lWindowTitle = "ClearVolume[" + pServerAddress
													+ ":"
													+ pPortNumber
													+ "]";

		try
		{

			try (ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(lWindowTitle,
																																													pWindowSize,
																																													pWindowSize,
																																													pBytesPerVoxel,
																																													pNumberOfLayers,
																																													cMaxMillisecondsToWaitForCopy,
																																													TimeUnit.MILLISECONDS,
																																													cMaxAvailableVolumes);)
			{

				lClearVolumeRendererSink.getClearVolumeRenderer()
																.addVolumeCaptureListener(pVolumeCaptureListener);

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

				final AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lSinkAfterAsynchronousVolumeSinkAdapter,
																																																					cMaxQueueLength,
																																																					cMaxMillisecondsToWait,
																																																					TimeUnit.MILLISECONDS);

				final ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lAsynchronousVolumeSinkAdapter);

				final SocketAddress lClientSocketAddress = new InetSocketAddress(	pServerAddress,
																																		pPortNumber);
				assertTrue(lClearVolumeTCPClient.open(lClientSocketAddress));

				assertTrue(lClearVolumeTCPClient.start());

				assertTrue(lAsynchronousVolumeSinkAdapter.start());

				lClearVolumeRendererSink.setVisible(true);

				while (lClearVolumeRendererSink.isShowing())
				{
					try
					{
						Thread.sleep(10);
					}
					catch (final InterruptedException e)
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
		}
		catch (final UnresolvedAddressException uae)
		{
			reportErrorWithPopUp(	uae,
														"Cannot find host: '" + pServerAddress
																+ "'");
		}
		catch (final Throwable e)
		{
			reportErrorWithPopUp(e, e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public void reportErrorWithPopUp(	Throwable pThrowable,
																		String pErrorMessage)
	{
		reportError(pThrowable, pErrorMessage);

		try
		{
			if (ExceptionUtils.getFullStackTrace(pThrowable)
												.contains("initSingleton"))
				JOptionPane.showMessageDialog(null,
																			"Sorry, but your OpenGL driver is not supported.",
																			"OpenGL error",
																			JOptionPane.ERROR_MESSAGE);
			else if (ExceptionUtils.getFullStackTrace(pThrowable)
															.contains("requires OpenCL version"))
				JOptionPane.showMessageDialog(null,
																			"Sorry, but your version of OpenCL is not supported (min 1.2).",
																			"Unsupported OpenCL version",
																			JOptionPane.ERROR_MESSAGE);
			else if (pThrowable.getClass()
													.toString()
													.toLowerCase()
													.contains("CudaException"))
				JOptionPane.showMessageDialog(null,
																			"Sorry, but your version of CUDA is not supported (min 6.5).",
																			"Unsupported OpenCL version",
																			JOptionPane.ERROR_MESSAGE);
			else if (pThrowable.getClass()
													.toString()
													.contains("UnresolvedAddressException"))
				JOptionPane.showMessageDialog(null,
																			"Sorry, but there is no ClearVolume server at that address.",
																			"Unknown host error",
																			JOptionPane.ERROR_MESSAGE);
			else if (pThrowable.getClass()
													.toString()
													.contains("ConnectException") && pThrowable.getLocalizedMessage() != null
								&& pThrowable.getLocalizedMessage()
															.toLowerCase()
															.contains("connection refused"))
				JOptionPane.showMessageDialog(null,
																			"Sorry, but there is no ClearVolume server listening on that machine/port",
																			"Connection refused error",
																			JOptionPane.ERROR_MESSAGE);
			else
				showEditableOptionPane(	ExceptionUtils.getFullStackTrace(pThrowable),
																"Unknown error, please copy and send to royer@mpi-cbg.de",
																JOptionPane.ERROR_MESSAGE);
		}
		catch (final Throwable e)
		{
			try
			{
				showEditableOptionPane(	ExceptionUtils.getFullStackTrace(e) + "\n"
																		+ ExceptionUtils.getFullStackTrace(pThrowable),
																"Unknown error, please copy and send to royer@mpi-cbg.de",
																JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			catch (final Throwable e1)
			{
				e1.printStackTrace();
			}
		}

	}

	private void showEditableOptionPane(String pText,
																			String pTitle,
																			int pMessageType)
	{
		final JTextArea ta = new JTextArea(48, 100);
		ta.setText(pText);
		ta.setWrapStyleWord(true);
		ta.setLineWrap(false);
		ta.setCaretPosition(0);
		ta.setEditable(false);

		JOptionPane.showMessageDialog(null,
																	new JScrollPane(ta),
																	pTitle,
																	pMessageType);
	}

	public abstract void reportError(Throwable e, String pErrorMessage);

}
