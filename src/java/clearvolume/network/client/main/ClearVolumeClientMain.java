package clearvolume.network.client.main;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import clearvolume.main.CheckRequirements;
import clearvolume.network.client.ClearVolumeTCPClientHelper;
import clearvolume.utils.ClearVolumeJFrame;

public class ClearVolumeClientMain
{

	private JFrame mApplicationJFrame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		try
		{
			EventQueue.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						CheckRequirements.check();
						ClearVolumeClientMain lClearVolumeMain = new ClearVolumeClientMain();
						lClearVolumeMain.mApplicationJFrame.setVisible(true);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	public static void connect(	final String pHostName,
															final int pPortNumber,
															int pWindowSize,
															int pBytesPerVoxel,
															int pNumberOfLayers,
															boolean pTimeShiftMultiChannel,
															boolean pMultiColor)
	{

		ClearVolumeTCPClientHelper lClearVolumeTCPClientHelper = new ClearVolumeTCPClientHelper()
		{
			@Override
			public void reportError(Throwable pE, String pErrorMessage)
			{
				System.err.println("Cannot connect to host: '" + pHostName
														+ ":"
														+ pPortNumber
														+ "'");
				System.err.println("Error: '" + pErrorMessage + "'");
				pE.printStackTrace();
			}
		};

		lClearVolumeTCPClientHelper.startClient(pHostName,
																						pPortNumber,
																						pWindowSize,
																						pBytesPerVoxel,
																						pNumberOfLayers,
																						pTimeShiftMultiChannel,
																						pMultiColor);

	}

	/**
	 * Create the application.
	 */
	public ClearVolumeClientMain()
	{
		super();
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		mApplicationJFrame = new ClearVolumeJFrame();
		mApplicationJFrame.setResizable(false);
		mApplicationJFrame.getContentPane().setBackground(Color.WHITE);
		mApplicationJFrame.setBounds(100, 100, 529, 529);
		mApplicationJFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar lMenuBar = new JMenuBar();
		mApplicationJFrame.setJMenuBar(lMenuBar);

		JMenuItem lConnectToJMenuItem = new JMenuItem("About");
		lConnectToJMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(mApplicationJFrame,
																			"ClearVolume 1.0\nMPI-CBG\nAuthors: Loic Royer, Martin Weigert, Ulrik Günther.\n Contact: royer@mpi-cbg.de",
																			"About ClearVolume",
																			JOptionPane.INFORMATION_MESSAGE);
			}
		});
		lMenuBar.add(lConnectToJMenuItem);
		mApplicationJFrame.getContentPane().setLayout(null);

		JLabel label = new JLabel("");
		label.setBounds(8, 5, 512, 157);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setIcon(new ImageIcon(ClearVolumeClientMain.class.getResource("images/ClearVolumeLogo_cropped.png")));
		mApplicationJFrame.getContentPane().add(label);

		ConnectionPanel connectionPanel = new ConnectionPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		connectionPanel.setBounds(8, 174, 512, 305);
		mApplicationJFrame.getContentPane().add(connectionPanel);

	}
}
