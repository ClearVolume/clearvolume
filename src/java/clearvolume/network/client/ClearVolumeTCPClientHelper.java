package clearvolume.network.client;

import java.awt.Image;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.apple.eawt.Application;

import clearvolume.renderer.listeners.VolumeCaptureListener;
import clearvolume.volume.sink.AsynchronousVolumeSinkAdapter;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.filter.ChannelFilterSink;
import clearvolume.volume.sink.filter.gui.ChannelFilterSinkJFrame;
import clearvolume.volume.sink.relay.RelaySinkInterface;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;
import clearvolume.volume.sink.timeshift.TimeShiftingSink;
import clearvolume.volume.sink.timeshift.gui.TimeShiftingSinkJFrame;
import coremem.types.NativeTypeEnum;

public abstract class ClearVolumeTCPClientHelper
{
	private static final int cMaxAvailableVolumes = 20;
	private static final int cMaxQueueLength = 20;
	private static final long cMaxMillisecondsToWait = 10;
	private static final long cMaxMillisecondsToWaitForCopy = 10;
	private static final long cSoftHoryzon = 50;
	private static final long cHardHoryzon = 100;

	public void startClient(final VolumeCaptureListener pVolumeCaptureListener,
							final String pServerAddress,
							final int pPortNumber,
							final int pWindowSize,
							final int pBytesPerVoxel,
							final int pNumberOfLayers,
							final boolean pTimeShift,
							final boolean pChannelFilter,
							final Image appicon)
	{
		final String lWindowTitle = "ClearVolume[" + pServerAddress
									+ ":"
									+ pPortNumber
									+ "]";

		try
		{

			final NativeTypeEnum lNativeType = pBytesPerVoxel == 1	? NativeTypeEnum.UnsignedByte
																	: NativeTypeEnum.UnsignedShort;

			try (ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(lWindowTitle,
																								pWindowSize,
																								pWindowSize,
																								lNativeType,
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
				if (!lClearVolumeTCPClient.open(lClientSocketAddress))
				{
					throw new RuntimeException("Could not open connection to " + pServerAddress);
				}

				lClearVolumeTCPClient.start();

				lAsynchronousVolumeSinkAdapter.start();

				lClearVolumeRendererSink.setVisible(true);

				// Resteal app icon after JOGL stole it!
				setCurrentAppIcon(appicon);

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

				lAsynchronousVolumeSinkAdapter.stop();
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

				lClearVolumeTCPClient.stop();
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

	public void reportErrorWithPopUp(	final Throwable pThrowable,
										final String pErrorMessage)
	{
		reportError(pThrowable, pErrorMessage);

		try
		{
			if (ExceptionUtils.getFullStackTrace(pThrowable)
								.contains("initSingleton"))
				JOptionPane.showMessageDialog(	null,
												"Sorry, but your OpenGL driver is not supported.",
												"OpenGL error",
												JOptionPane.ERROR_MESSAGE);
			else if (ExceptionUtils.getFullStackTrace(pThrowable)
									.contains("requires OpenCL version"))
				JOptionPane.showMessageDialog(	null,
												"Sorry, but your version of OpenCL is not supported (min 1.2).",
												"Unsupported OpenCL version",
												JOptionPane.ERROR_MESSAGE);
			else if (pThrowable.getClass()
								.toString()
								.toLowerCase()
								.contains("CudaException"))
				JOptionPane.showMessageDialog(	null,
												"Sorry, but your version of CUDA is not supported (min 6.5).",
												"Unsupported OpenCL version",
												JOptionPane.ERROR_MESSAGE);
			else if (pThrowable.getClass()
								.toString()
								.contains("UnresolvedAddressException"))
				JOptionPane.showMessageDialog(	null,
												"Sorry, but there is no ClearVolume server at that address.",
												"Unknown host error",
												JOptionPane.ERROR_MESSAGE);
			else if (pThrowable.getClass()
								.toString()
								.contains("ConnectException") && pThrowable.getLocalizedMessage() != null
						&& pThrowable.getLocalizedMessage()
										.toLowerCase()
										.contains("connection refused"))
				JOptionPane.showMessageDialog(	null,
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

	private void showEditableOptionPane(final String pText,
										final String pTitle,
										final int pMessageType)
	{
		final JTextArea ta = new JTextArea(48, 100);
		ta.setText(pText);
		ta.setWrapStyleWord(true);
		ta.setLineWrap(false);
		ta.setCaretPosition(0);
		ta.setEditable(false);

		JOptionPane.showMessageDialog(	null,
										new JScrollPane(ta),
										pTitle,
										pMessageType);
	}

	public abstract void reportError(Throwable e, String pErrorMessage);

	/**
	 * Use this method to set the icon for this app. Best used after JOGL stole
	 * the application icon. Bad JOGL!
	 * 
	 * @param pFinalIcon
	 *            final icon
	 */
	public static void setCurrentAppIcon(final Image pFinalIcon)
	{
		if (pFinalIcon == null)
			return;

		final String os = System.getProperty("os.name").toLowerCase();

		SwingUtilities.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				if (os.indexOf("mac") >= 0)
				{
					Application.getApplication()
								.setDockIconImage(pFinalIcon);
				}
				/*else if (os.indexOf("win") >= 0)
				{
					// not yet clear
				}
				else
				{
					// not yet clear
				}/**/
			}
		});
	}
}
