package clearvolume.volume.sink.filter.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;

import javax.swing.JPanel;
import javax.swing.WindowConstants;

import clearvolume.utils.ClearVolumeJFrame;
import clearvolume.volume.sink.filter.ChannelFilterSink;

public class ChannelFilterSinkJFrame extends ClearVolumeJFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final JPanel mContentPane;

	/**
	 * Launch the application.
	 * 
	 * @param pChannelFilterSink
	 *          channel filter sink
	 */
	public static void launch(final ChannelFilterSink pChannelFilterSink)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final ChannelFilterSinkJFrame frame = new ChannelFilterSinkJFrame(pChannelFilterSink);
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
	 * @param pChannelFilterSink
	 *          channel filter sink
	 */
	public ChannelFilterSinkJFrame(ChannelFilterSink pChannelFilterSink)
	{
		setResizable(false);
		setTitle("Channel Filter Selection");
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		setBackground(Color.WHITE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 275, 327);
		mContentPane = new JPanel();
		mContentPane.setBackground(Color.WHITE);
		mContentPane.setBorder(null);
		mContentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(mContentPane);

		if (pChannelFilterSink != null)
		{
			final ChannelFilterSinkJPanel lChannelFilterSinkJPanel = new ChannelFilterSinkJPanel(pChannelFilterSink);
			mContentPane.add(lChannelFilterSinkJPanel);
		}
	}

}
