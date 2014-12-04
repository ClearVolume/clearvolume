package clearvolume.renderer;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

public class ControlJPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final int cPrecision = 1024 * 1024;

	private volatile ClearVolumeRendererInterface mClearVolumeRendererInterface;

	public ControlJPanel()
	{
		super();

		setLayout(new MigLayout("",
														"[225px,grow]",
														"[][][][51.00px,center]"));

		JSlider lGammaSlider = new JSlider(0, cPrecision, cPrecision / 2);
		lGammaSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float lGamma = 2f * (source.getValue()) / cPrecision;
				getClearVolumeRendererInterface().setGamma(lGamma);
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

		JLabel lblGamma = new JLabel("gamma");
		add(lblGamma, "cell 0 0");

		add(lGammaSlider, "cell 0 1,grow");

		JLabel lblMinMax = new JLabel("min & max");
		add(lblMinMax, "cell 0 2");

		JLayeredPane lLayeredPane = new JLayeredPane();
		lLayeredPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		add(lLayeredPane, "cell 0 3,grow");
		lLayeredPane.setLayout(null);

		final JSlider lMinSlider = new JSlider(0, cPrecision, 0);
		lMinSlider.setBounds(0, 23, 412, 20);
		lLayeredPane.add(lMinSlider);
		lMinSlider.setPaintTrack(false);

		final JSlider lMaxSlider = new JSlider(0, cPrecision, cPrecision);
		lMaxSlider.setBounds(0, 0, 412, 20);
		lMaxSlider.setPaintTrack(false);
		lLayeredPane.add(lMaxSlider);

		JButton separator = new JButton();
		separator.setHideActionText(true);
		separator.setBorder(new BevelBorder(BevelBorder.LOWERED,
																				Color.LIGHT_GRAY,
																				null,
																				null,
																				null));
		separator.setForeground(Color.DARK_GRAY);
		separator.setBounds(14, 19, 388, 4);
		lLayeredPane.add(separator);
		lMaxSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				if (lMinSlider.getValue() > lMaxSlider.getValue())
				{
					lMinSlider.setValue(lMaxSlider.getValue() - 1);
					// lMinSlider.repaint();
				}
				final float lMax = 1f * source.getValue() / cPrecision;
				getClearVolumeRendererInterface().setTransferFunctionRangeMax(lMax);
				getClearVolumeRendererInterface().requestDisplay();
			}
		});
		lMinSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				if (lMinSlider.getValue() > lMaxSlider.getValue())
				{
					lMaxSlider.setValue(lMinSlider.getValue() + 1);
					// lMaxSlider.repaint();
				}
				final float lMin = 1f * source.getValue() / cPrecision;
				getClearVolumeRendererInterface().setTransferFunctionRangeMin(lMin);
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

	}

	public ClearVolumeRendererInterface getClearVolumeRendererInterface()
	{
		return mClearVolumeRendererInterface;
	}

	public void setClearVolumeRendererInterface(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;
	}

}
