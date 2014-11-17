package clearvolume.renderer;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ControlPanel extends JPanel
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final ClearVolumeRendererInterface mClearVolumeRendererInterface;

	public ControlPanel(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		super();
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;

		final JPanel controlPanel = new JPanel(new GridLayout(2, 2));
		JPanel panel = null;
		JSlider slider = null;

		// Brightness
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Brightness:"));
		slider = new JSlider(0, 100, 10);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				mClearVolumeRendererInterface.setBrightness(a * 10);
				mClearVolumeRendererInterface.requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Transfer offset
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Transfer Range Min:"));
		slider = new JSlider(0, 100, 55);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				mClearVolumeRendererInterface.setTransferFunctionRangeMin(a);
				mClearVolumeRendererInterface.requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Transfer scale
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Transfer Range Max:"));
		slider = new JSlider(0, 100, 10);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				mClearVolumeRendererInterface.setTransferFunctionRangeMax(a);
				mClearVolumeRendererInterface.requestDisplay();
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);
	}

}
