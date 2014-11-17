package clearvolume.volume.sink.timeshift.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

import clearvolume.utils.ClearVolumeJFrame;
import clearvolume.volume.sink.timeshift.TimeShiftingSink;

public class TimeShiftingSinkJFrame	extends
																								ClearVolumeJFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JPanel mContentPane;

	/**
	 * Launch the application.
	 */
	public static void launch(TimeShiftingSink pTimeShiftingSink)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					TimeShiftingSinkJFrame frame = new TimeShiftingSinkJFrame(pTimeShiftingSink);
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
	 * @param pTimeShiftingSink
	 */
	public TimeShiftingSinkJFrame(TimeShiftingSink pTimeShiftingSink)
	{
		setTitle("TimeShift");
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setResizable(false);
		setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 651, 159);
		mContentPane = new JPanel();
		mContentPane.setBackground(Color.WHITE);
		mContentPane.setBorder(null);
		mContentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(mContentPane);

		TimeShiftingSinkJPanel lTimeShiftingSinkPanel = new TimeShiftingSinkJPanel(pTimeShiftingSink);
		mContentPane.add(lTimeShiftingSinkPanel);
	}

}
