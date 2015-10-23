package clearvolume.volume.sink.timeshift.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;

import javax.swing.JPanel;
import javax.swing.WindowConstants;

import clearvolume.utils.ClearVolumeJFrame;
import clearvolume.volume.sink.timeshift.TimeShiftingSink;

public class TimeShiftingSinkJFrame extends ClearVolumeJFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final JPanel mContentPane;

	/**
	 * Launch the application.
	 * 
	 * @param pTimeShiftingSink
	 *            time shifting sink
	 */
	public static void launch(final TimeShiftingSink pTimeShiftingSink)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final TimeShiftingSinkJFrame frame = new TimeShiftingSinkJFrame(pTimeShiftingSink);
					frame.setVisible(true);
				}
				catch (final Exception e)
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
	 *            time shifting sink
	 */
	public TimeShiftingSinkJFrame(TimeShiftingSink pTimeShiftingSink)
	{
		setTitle("TimeShift");
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setResizable(false);
		setBackground(Color.WHITE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 651, 159);
		mContentPane = new JPanel();
		mContentPane.setBackground(Color.WHITE);
		mContentPane.setBorder(null);
		mContentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(mContentPane);

		final TimeShiftingSinkJPanel lTimeShiftingSinkPanel = new TimeShiftingSinkJPanel(pTimeShiftingSink);
		mContentPane.add(lTimeShiftingSinkPanel);
	}

}
