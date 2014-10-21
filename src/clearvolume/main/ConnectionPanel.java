package clearvolume.main;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.transfertf.TransfertFunctions;
import clearvolume.volume.sink.ClearVolumeRendererSink;

public class ConnectionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private JTextArea mErrorTextArea;

	public ConnectionPanel()
	{
		setBackground(Color.WHITE);
		ConnectionPanel lThis = this;
		setLayout(new MigLayout("",
														"[435.00px,grow]",
														"[16px][16px][29px][grow]"));
		JLabel lblNewLabel = new JLabel("Enter IP address or hostname of ClearVolume server:");
		add(lblNewLabel, "cell 0 0,alignx left,aligny top");

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
		add(lServerAddressTextField, "cell 0 1,growx,aligny top");

		JButton lConnectButton = new JButton("Connect");
		lConnectButton.setBorder(null);
		lConnectButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				startClientAsync(lServerAddressTextField);
			}
		});
		add(lConnectButton, "cell 0 2,alignx right,aligny top");

		mErrorTextArea = new JTextArea();
		mErrorTextArea.setEditable(false);
		mErrorTextArea.setForeground(Color.RED);
		mErrorTextArea.setBackground(Color.WHITE);
		add(mErrorTextArea, "cell 0 3, grow");
	}

	private void startClientAsync(JTextField lServerAddressTextField)
	{
		mErrorTextArea.setText("");
		Runnable lStartClientRunnable = () -> startClient(lServerAddressTextField.getText());
		Thread lStartClientThread = new Thread(	lStartClientRunnable,
																						"StartClientThread" + lServerAddressTextField.getText());
		lStartClientThread.setDaemon(true);
		lStartClientThread.start();
	}

	private void startClient(String pServerAddress)
	{
		try (final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer("ClearVolumeTest",
																																																256,
																																																256))
		{
			try
			{
				lClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());

				ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(lClearVolumeRenderer);

				ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lClearVolumeRendererSink);

				SocketAddress lClientSocketAddress = new InetSocketAddress(	pServerAddress,
																																		ClearVolumeSerialization.cStandardTCPPort);
				assertTrue(lClearVolumeTCPClient.open(lClientSocketAddress));

				assertTrue(lClearVolumeTCPClient.start());

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

				assertTrue(lClearVolumeTCPClient.stop());
				lClearVolumeTCPClient.close();
			}
			catch (UnresolvedAddressException uae)
			{
				SwingUtilities.invokeLater(() -> {
					mErrorTextArea.setText("Cannot find host: '" + pServerAddress
																	+ "'");
				});
			}
			catch (Throwable e)
			{
				SwingUtilities.invokeLater(() -> {
					String lErrorMessage = e.getLocalizedMessage();
					mErrorTextArea.setText(e.getClass().getName() + (lErrorMessage == null ? ""
																																								: ": " + lErrorMessage));
				});
				e.printStackTrace();
			}
		}
	}

}
