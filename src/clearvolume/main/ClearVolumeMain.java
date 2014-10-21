package clearvolume.main;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ClearVolumeMain
{

	private JFrame mApplicationJFrame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ClearVolumeMain lClearVolumeMain = new ClearVolumeMain();
					lClearVolumeMain.mApplicationJFrame.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ClearVolumeMain()
	{
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		mApplicationJFrame = new JFrame();
		mApplicationJFrame.getContentPane().setBackground(Color.WHITE);
		mApplicationJFrame.setBounds(100, 100, 529, 372);
		mApplicationJFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar lMenuBar = new JMenuBar();
		mApplicationJFrame.setJMenuBar(lMenuBar);

		JMenuItem lConnectToJMenuItem = new JMenuItem("About");
		lMenuBar.add(lConnectToJMenuItem);
		mApplicationJFrame.getContentPane().setLayout(null);

		JLabel label = new JLabel("");
		label.setBounds(8, 5, 512, 157);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setIcon(new ImageIcon(ClearVolumeMain.class.getResource("/clearvolume/main/images/Logo.inverted.cropped.png")));
		mApplicationJFrame.getContentPane().add(label);

		ConnectionPanel connectionPanel = new ConnectionPanel();
		connectionPanel.setBackground(Color.WHITE);
		connectionPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		connectionPanel.setBounds(8, 174, 512, 157);
		mApplicationJFrame.getContentPane().add(connectionPanel);

	}
}
