package clearvolume.volume.sink.timeshift.gui;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import clearvolume.volume.sink.timeshift.MultiChannelTimeShiftingSink;

public class MultiChannelTimeShiftingSinkJPanel extends JPanel
{

	private MultiChannelTimeShiftingSink mMultiChannelTimeShiftingSink;

	public static final void createJFrame(MultiChannelTimeShiftingSink pMultiChannelTimeShiftingSink)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					JFrame lJFrame = new JFrame();

					MultiChannelTimeShiftingSinkJPanel lMultiChannelTimeShiftingSinkPanel = new MultiChannelTimeShiftingSinkJPanel(pMultiChannelTimeShiftingSink);
					lJFrame.getContentPane()
									.add(lMultiChannelTimeShiftingSinkPanel);
					lJFrame.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the panel.
	 */
	public MultiChannelTimeShiftingSinkJPanel(final MultiChannelTimeShiftingSink pMultiChannelTimeShiftingSink)
	{
		setBackground(Color.WHITE);
		setLayout(new MigLayout("",
														"[grow,fill][grow,fill]",
														"[26px:26px,grow,center][]"));

		JLayeredPane lJLayeredPane = new JLayeredPane();
		lJLayeredPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		add(lJLayeredPane, "cell 0 0 2 1,grow");
		lJLayeredPane.setLayout(null);

		JProgressBar lPlayBar = new JProgressBar();
		lPlayBar.setBorder(new EmptyBorder(0, 0, 0, 0));
		lPlayBar.setMinimum(0);
		lPlayBar.setMaximum(Integer.MAX_VALUE);
		lPlayBar.setValue(Integer.MAX_VALUE);

		lPlayBar.setBounds(6, 6, 605, 11);
		// lPlayBar.setBackground(new Color(0, 0, 0, 0));
		lJLayeredPane.add(lPlayBar);

		JSlider lTimeShiftSlider = new JSlider();
		lTimeShiftSlider.setMaximum(Integer.MAX_VALUE);
		lTimeShiftSlider.setValue(Integer.MAX_VALUE);
		lTimeShiftSlider.setBorder(new EmptyBorder(0, 0, 0, 0));
		lJLayeredPane.setLayer(lTimeShiftSlider, 1);
		lTimeShiftSlider.setPaintTrack(false);
		lTimeShiftSlider.setBounds(6, -3, 616, 29);
		lTimeShiftSlider.addChangeListener(e -> {
			final double lNormalizedTimeShift = (Integer.MAX_VALUE - 1.0 * lTimeShiftSlider.getValue())
																					/ Integer.MAX_VALUE;
			pMultiChannelTimeShiftingSink.setTimeShiftNormalized(lNormalizedTimeShift);
		});
		lJLayeredPane.add(lTimeShiftSlider);

		JButton lPreviousChannelButton = new JButton("previous channel");
		lPreviousChannelButton.addActionListener(e -> pMultiChannelTimeShiftingSink.previousChannel());
		add(lPreviousChannelButton, "cell 0 1");

		JButton lNextChannelButton = new JButton("next channel");
		lNextChannelButton.addActionListener(e -> pMultiChannelTimeShiftingSink.nextChannel());
		add(lNextChannelButton, "cell 1 1");

		mMultiChannelTimeShiftingSink = pMultiChannelTimeShiftingSink;

	}

}
