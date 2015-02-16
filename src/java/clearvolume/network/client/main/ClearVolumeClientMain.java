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
import clearvolume.renderer.VolumeCaptureListener;
import clearvolume.utils.ClearVolumeJFrame;

public class ClearVolumeClientMain
{

	private JFrame mApplicationJFrame;
	private VolumeCaptureListener mVolumeCaptureListener;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		launchClientGUI(null, true);
	}

	public static void launchClientGUI(	final VolumeCaptureListener pVolumeCaptureListener,
																			final boolean pExitOnClose)
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
						final ClearVolumeClientMain lClearVolumeMain = new ClearVolumeClientMain(	pVolumeCaptureListener,
																																											pExitOnClose);
						lClearVolumeMain.mApplicationJFrame.setVisible(true);
					}
					catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
			});
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

	protected void setVolumeCaptureListener(VolumeCaptureListener pVolumeCaptureListener)
	{
		mVolumeCaptureListener = pVolumeCaptureListener;
	}

	public static void connect(	final String pHostName,
															final int pPortNumber,
															int pWindowSize,
															int pBytesPerVoxel,
															int pNumberOfLayers,
															boolean pTimeShiftMultiChannel,
															boolean pMultiColor)
	{

		final ClearVolumeTCPClientHelper lClearVolumeTCPClientHelper = new ClearVolumeTCPClientHelper()
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

		lClearVolumeTCPClientHelper.startClient(null,
																						pHostName,
																						pPortNumber,
																						pWindowSize,
																						pBytesPerVoxel,
																						pNumberOfLayers,
																						pTimeShiftMultiChannel,
																						pMultiColor);

	}

	/**
	 * Create the application.
	 * 
	 * @param pVolumeCaptureListener
	 */
	public ClearVolumeClientMain(	VolumeCaptureListener pVolumeCaptureListener,
																boolean pExitOnClose)
	{
		super();
		initialize(pVolumeCaptureListener, pExitOnClose);
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @param pVolumeCaptureListener
	 */
	private void initialize(VolumeCaptureListener pVolumeCaptureListener,
													boolean pExitOnClose)
	{
		mApplicationJFrame = new ClearVolumeJFrame();
		mApplicationJFrame.setResizable(false);
		mApplicationJFrame.getContentPane().setBackground(Color.WHITE);
		mApplicationJFrame.setBounds(100, 100, 529, 529);
		if (pExitOnClose)
			mApplicationJFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JMenuBar lMenuBar = new JMenuBar();
		mApplicationJFrame.setJMenuBar(lMenuBar);

		final JMenuItem lConnectToJMenuItem = new JMenuItem("About");
		lConnectToJMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(mApplicationJFrame,
																			"ClearVolume 1.0\nMPI-CBG\nAuthors: Loic Royer, Martin Weigert, Ulrik Guenther, Nicola Maghelli, Florian Jug, Ivo Sbalzarini, Eugene Myers.\n Contact: royer@mpi-cbg.de",
																			"About ClearVolume",
																			JOptionPane.INFORMATION_MESSAGE);
			}
		});
		lMenuBar.add(lConnectToJMenuItem);
		mApplicationJFrame.getContentPane().setLayout(null);

		final JLabel label = new JLabel("");
		label.setBounds(8, 5, 512, 157);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setIcon(new ImageIcon(ClearVolumeClientMain.class.getResource("images/ClearVolumeLogo_cropped.png")));
		mApplicationJFrame.getContentPane().add(label);

		final ConnectionPanel lConnectionPanel = new ConnectionPanel(pVolumeCaptureListener);
		lConnectionPanel.setBackground(Color.WHITE);
		lConnectionPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		lConnectionPanel.setBounds(8, 174, 512, 305);
		mApplicationJFrame.getContentPane().add(lConnectionPanel);

	}

	public JFrame getApplicationJFrame()
	{
		return mApplicationJFrame;
	}

}
