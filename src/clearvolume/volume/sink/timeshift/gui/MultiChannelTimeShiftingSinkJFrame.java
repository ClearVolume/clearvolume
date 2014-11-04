package clearvolume.volume.sink.timeshift.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import clearvolume.volume.sink.timeshift.MultiChannelTimeShiftingSink;

public class MultiChannelTimeShiftingSinkJFrame extends JFrame
{

	private JPanel mContentPane;

	/**
	 * Launch the application.
	 */
	public static void launch(MultiChannelTimeShiftingSink pMultiChannelTimeShiftingSink)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					MultiChannelTimeShiftingSinkJFrame frame = new MultiChannelTimeShiftingSinkJFrame(pMultiChannelTimeShiftingSink);
					frame.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * 
	 * @param pMultiChannelTimeShiftingSink
	 */
	public MultiChannelTimeShiftingSinkJFrame(MultiChannelTimeShiftingSink pMultiChannelTimeShiftingSink)
	{
		setTitle("TimeShift and MultiChannel");
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setResizable(false);
		setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 651, 118);
		mContentPane = new JPanel();
		mContentPane.setBackground(Color.WHITE);
		mContentPane.setBorder(null);
		mContentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(mContentPane);

		MultiChannelTimeShiftingSinkJPanel lMultiChannelTimeShiftingSinkPanel = new MultiChannelTimeShiftingSinkJPanel(pMultiChannelTimeShiftingSink);
		mContentPane.add(lMultiChannelTimeShiftingSinkPanel);
	}

}
