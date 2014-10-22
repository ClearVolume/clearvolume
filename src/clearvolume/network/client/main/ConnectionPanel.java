package clearvolume.network.client.main;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.TransfertFunctions;
import clearvolume.volume.sink.AsynchronousVolumeSinkAdapter;
import clearvolume.volume.sink.ClearVolumeRendererSink;

public class ConnectionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final int cMaxQueueLength = 20;
	private static final long cMaxMillisecondsToWait = 10;
	private static final long cMaxMillisecondsToWaitForCopy = 10;

	private JTextArea mErrorTextArea;
	private JTextField mWindowSizeField;

	public ConnectionPanel()
	{
		setBackground(Color.WHITE);
		ConnectionPanel lThis = this;
		setLayout(new MigLayout("",
														"[435.00px,grow][435.00px,grow]",
														"[16px][16px][29px][10px:n,grow][10px:n,grow]"));
		JLabel lblNewLabel = new JLabel("Enter IP address or hostname of ClearVolume server:");
		add(lblNewLabel, "cell 0 0 2 1,alignx left,aligny top");

		JTextField lServerAddressTextField = new JTextField();
		lServerAddressTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				startClientAsync(lServerAddressTextField);
			}
		});
		lServerAddressTextField.setBorder(null);
		lServerAddressTextField.setText("localhost");
		lServerAddressTextField.setBackground(new Color(220, 220, 220));
		add(lServerAddressTextField, "cell 0 1 2 1,growx,aligny top");

		JButton lConnectButton = new JButton("connect");
		lConnectButton.setBorder(null);
		lConnectButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				startClientAsync(lServerAddressTextField);
			}
		});

		JButton lAdvancedButton = new JButton("advanced...");
		lAdvancedButton.setForeground(Color.GRAY);
		lAdvancedButton.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
		lAdvancedButton.setVerticalAlignment(SwingConstants.TOP);

		lAdvancedButton.setBorder(null);
		add(lAdvancedButton, "cell 0 2,aligny top");
		add(lConnectButton, "cell 1 2,alignx right,aligny top");

		JPanel lOptionsPanel = new JPanel();
		lOptionsPanel.setVisible(false);
		lOptionsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		lOptionsPanel.setBackground(Color.WHITE);
		add(lOptionsPanel, "cell 0 3 2 1,grow");
		lAdvancedButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				lOptionsPanel.setVisible(!lOptionsPanel.isVisible());
			}
		});
		lOptionsPanel.setLayout(null);

		JLabel lWindowSizeLabel = new JLabel("Window size:");
		lWindowSizeLabel.setBounds(6, 6, 82, 16);
		lOptionsPanel.add(lWindowSizeLabel);

		mWindowSizeField = new JTextField();
		mWindowSizeField.setHorizontalAlignment(SwingConstants.TRAILING);
		mWindowSizeField.setBounds(96, 6, 45, 16);
		mWindowSizeField.setText("512");
		mWindowSizeField.setBackground(new Color(220, 220, 220));
		mWindowSizeField.setBorder(new EmptyBorder(0, 0, 0, 0));
		lOptionsPanel.add(mWindowSizeField);
		mWindowSizeField.setColumns(10);

		mErrorTextArea = new JTextArea();
		mErrorTextArea.setEditable(false);
		mErrorTextArea.setForeground(Color.RED);
		mErrorTextArea.setBackground(Color.WHITE);
		add(mErrorTextArea, "cell 0 4 2 1,grow");

	}

	private void startClientAsync(JTextField lServerAddressTextField)
	{
		try
		{
			mErrorTextArea.setText("");
			final int lWindowSize = Integer.parseInt(mWindowSizeField.getText());
			Runnable lStartClientRunnable = () -> startClient(lServerAddressTextField.getText(),
																												lWindowSize,
																												1);
			Thread lStartClientThread = new Thread(	lStartClientRunnable,
																							"StartClientThread" + lServerAddressTextField.getText());
			lStartClientThread.setDaemon(true);
			lStartClientThread.start();
		}
		catch (Throwable e)
		{
			reportError(e, e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private void startClient(	String pServerAddress,
														int pWindowSize,
														int pBytesPerVoxel)
	{
		try (final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																																pWindowSize,
																																																pWindowSize,
																																																pBytesPerVoxel))
		{
			try
			{
				lClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());

				ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																												cMaxMillisecondsToWaitForCopy,
																																												TimeUnit.MILLISECONDS);

				AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lClearVolumeRendererSink,
																																																					cMaxQueueLength,
																																																					cMaxMillisecondsToWait,
																																																					TimeUnit.MILLISECONDS);

				ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lAsynchronousVolumeSinkAdapter);

				SocketAddress lClientSocketAddress = new InetSocketAddress(	pServerAddress,
																																		ClearVolumeSerialization.cStandardTCPPort);
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

	private void reportError(Throwable e, String pErrorMessage)
	{
		SwingUtilities.invokeLater(() -> {
			mErrorTextArea.setText(e.getClass().getName() + (pErrorMessage == null ? ""
																																						: ": " + pErrorMessage));
			this.revalidate();
			this.repaint();
		});
	}

}
