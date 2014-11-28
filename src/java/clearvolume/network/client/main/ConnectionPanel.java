package clearvolume.network.client.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
	private JTextField mBytesPerVoxelTextField;

	private JCheckBox mTimeShiftAndMultiChannelCheckBox;

	private JCheckBox mChannelFilterCheckBox;

	private JTextField mNumberOfColorsField;
	private JTextField mTCPPortTextField;

	public ConnectionPanel()
	{
		setBackground(Color.WHITE);
		ConnectionPanel lThis = this;
		setLayout(new MigLayout("",
														"[435.00px,grow][435.00px,grow]",
														"[16px][16px][29px][50.00px:n,grow][10px:n,grow]"));
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
		lServerAddressTextField.setText("clearvolume.mpi-cbg.de");
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

		JLabel lWindowSizeLabel = new JLabel("Window size");
		lWindowSizeLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lWindowSizeLabel.setBounds(6, 6, 119, 16);
		lOptionsPanel.add(lWindowSizeLabel);

		mWindowSizeField = new JTextField();
		mWindowSizeField.setHorizontalAlignment(SwingConstants.TRAILING);
		mWindowSizeField.setBounds(148, 6, 45, 16);
		mWindowSizeField.setText("512");
		mWindowSizeField.setBackground(new Color(220, 220, 220));
		mWindowSizeField.setBorder(new EmptyBorder(0, 0, 0, 0));
		lOptionsPanel.add(mWindowSizeField);
		mWindowSizeField.setColumns(10);

		JLabel lBytesPerVoxelLabel = new JLabel("Bytes-per-voxel");
		lBytesPerVoxelLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lBytesPerVoxelLabel.setBounds(6, 32, 119, 16);
		lOptionsPanel.add(lBytesPerVoxelLabel);

		mBytesPerVoxelTextField = new JTextField();
		mBytesPerVoxelTextField.setText("2");
		mBytesPerVoxelTextField.setHorizontalAlignment(SwingConstants.TRAILING);
		mBytesPerVoxelTextField.setColumns(10);
		mBytesPerVoxelTextField.setBorder(new EmptyBorder(0, 0, 0, 0));
		mBytesPerVoxelTextField.setBackground(new Color(220, 220, 220));
		mBytesPerVoxelTextField.setBounds(148, 32, 45, 16);
		lOptionsPanel.add(mBytesPerVoxelTextField);

		JLabel lTimeShiftAndMultiChannelLabel = new JLabel("TimeShift");
		lTimeShiftAndMultiChannelLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lTimeShiftAndMultiChannelLabel.setBounds(205, 32, 171, 16);
		lOptionsPanel.add(lTimeShiftAndMultiChannelLabel);

		mTimeShiftAndMultiChannelCheckBox = new JCheckBox("");
		mTimeShiftAndMultiChannelCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
		mTimeShiftAndMultiChannelCheckBox.setMinimumSize(new Dimension(	30,
																																		30));
		mTimeShiftAndMultiChannelCheckBox.setMaximumSize(new Dimension(	30,
																																		30));
		mTimeShiftAndMultiChannelCheckBox.setBounds(382, 29, 28, 20);
		lOptionsPanel.add(mTimeShiftAndMultiChannelCheckBox);

		JLabel lMultiColorLabel = new JLabel("ChannelFilter");
		lMultiColorLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lMultiColorLabel.setBounds(217, 58, 159, 16);
		lOptionsPanel.add(lMultiColorLabel);

		mChannelFilterCheckBox = new JCheckBox("");
		mChannelFilterCheckBox.setMinimumSize(new Dimension(30, 30));
		mChannelFilterCheckBox.setMaximumSize(new Dimension(30, 30));
		mChannelFilterCheckBox.setAlignmentX(0.5f);
		mChannelFilterCheckBox.setBounds(382, 56, 28, 20);
		lOptionsPanel.add(mChannelFilterCheckBox);

		JLabel lNumberOfColorsLabel = new JLabel("Number of colors");
		lNumberOfColorsLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lNumberOfColorsLabel.setBounds(6, 60, 119, 16);
		lOptionsPanel.add(lNumberOfColorsLabel);

		mNumberOfColorsField = new JTextField();
		mNumberOfColorsField.setText("1");
		mNumberOfColorsField.setHorizontalAlignment(SwingConstants.TRAILING);
		mNumberOfColorsField.setColumns(10);
		mNumberOfColorsField.setBorder(new EmptyBorder(0, 0, 0, 0));
		mNumberOfColorsField.setBackground(new Color(220, 220, 220));
		mNumberOfColorsField.setBounds(148, 60, 45, 16);
		lOptionsPanel.add(mNumberOfColorsField);

		mTCPPortTextField = new JTextField();
		mTCPPortTextField.setText("" + ClearVolumeSerialization.cStandardTCPPort);
		mTCPPortTextField.setHorizontalAlignment(SwingConstants.TRAILING);
		mTCPPortTextField.setColumns(10);
		mTCPPortTextField.setBorder(new EmptyBorder(0, 0, 0, 0));
		mTCPPortTextField.setBackground(new Color(220, 220, 220));
		mTCPPortTextField.setBounds(359, 6, 45, 16);
		lOptionsPanel.add(mTCPPortTextField);

		JLabel lTCPPortLabel = new JLabel("port");
		lTCPPortLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lTCPPortLabel.setBounds(228, 6, 119, 16);
		lOptionsPanel.add(lTCPPortLabel);

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
			final String lServerAddress = lServerAddressTextField.getText();
			final int lTCPPort = Integer.parseInt(mTCPPortTextField.getText());
			final int lWindowSize = Integer.parseInt(mWindowSizeField.getText());
			final int lBytesPerVoxel = Integer.parseInt(mBytesPerVoxelTextField.getText());
			final boolean lTimeShiftMultiChannel = mTimeShiftAndMultiChannelCheckBox.isSelected();
			final boolean lChannelFilter = mChannelFilterCheckBox.isSelected();
			final int lNumberOfLayers = Integer.parseInt(mNumberOfColorsField.getText());
			Runnable lStartClientRunnable = () -> mClearVolumeTCPClientHelper.startClient(lServerAddress,
																																										lTCPPort,
																																										lWindowSize,
																																										lBytesPerVoxel,
																																										lNumberOfLayers,
																																										lTimeShiftMultiChannel,
																																										lChannelFilter);
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
