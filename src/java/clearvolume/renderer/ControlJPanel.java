package clearvolume.renderer;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
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

		setLayout(new MigLayout("", "[][225px,grow]", "[][][]"));

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

		add(lGammaSlider, "cell 1 0,grow");

		final JSlider lMaxSlider = new JSlider(0, cPrecision, cPrecision);
		final JSlider lMinSlider = new JSlider(0, cPrecision, 0);
		lMinSlider.setPreferredSize(new Dimension(0, 0));

		JLabel lblMinMax = new JLabel("max");
		add(lblMinMax, "cell 0 1");

		add(lMaxSlider, "cell 1 1,grow");
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

		JLabel lblMax = new JLabel("min");
		add(lblMax, "cell 0 2");

		add(lMinSlider, "cell 1 2,grow");
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
