package clearvolume.network.client.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import clearvolume.network.client.ClearVolumeTCPClientHelper;
import clearvolume.network.serialization.ClearVolumeSerialization;

public class ConnectionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private JTextArea mErrorTextArea;
	private JTextField mWindowSizeField;

	private ClearVolumeTCPClientHelper mClearVolumeTCPClientHelper;

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

		mClearVolumeTCPClientHelper = new ClearVolumeTCPClientHelper()
		{

			@Override
			public void reportError(Throwable pE, String pErrorMessage)
			{
				SwingUtilities.invokeLater(() -> {
					mErrorTextArea.setText(pE.getClass().getName() + (pErrorMessage == null	? ""
																																									: ": " + pErrorMessage));
					lThis.revalidate();
					lThis.repaint();
				});
			}
		};

	}

	private void startClientAsync(JTextField lServerAddressTextField)
	{
		try
		{
			mErrorTextArea.setText("");
			final int lWindowSize = Integer.parseInt(mWindowSizeField.getText());
			Runnable lStartClientRunnable = () -> mClearVolumeTCPClientHelper.startClient(lServerAddressTextField.getText(),
																																										ClearVolumeSerialization.cStandardTCPPort,
																																										lWindowSize,
																																										1);
			Thread lStartClientThread = new Thread(	lStartClientRunnable,
																							"StartClientThread" + lServerAddressTextField.getText());
			lStartClientThread.setDaemon(true);
			lStartClientThread.start();
		}
		catch (Throwable e)
		{
			mClearVolumeTCPClientHelper.reportError(e,
																							e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

}
