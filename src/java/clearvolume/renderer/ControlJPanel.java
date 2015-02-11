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

	private final int layerId;

	private static final int cPrecision = 1024 * 1024;

	private volatile ClearVolumeRendererInterface mClearVolumeRendererInterface;

	public ControlJPanel(
			final int layerToBeControlled,
			final ClearVolumeRendererInterface pClearVolumeRendererInterface )
	{
		super();

		this.layerId = layerToBeControlled;
		this.mClearVolumeRendererInterface = pClearVolumeRendererInterface;

		setLayout(new MigLayout("", "[][225px,grow]", "[][][]"));

		final JSlider lGammaSlider =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * .5 * mClearVolumeRendererInterface.getGamma( layerId ) ) );
		lGammaSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float lGamma = 2f * (source.getValue()) / cPrecision;
				getClearVolumeRendererInterface().setGamma( layerId, lGamma );
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

		final JLabel lblGamma = new JLabel( String.format( "Ch.%d: gamma", layerId ) );
		add(lblGamma, "cell 0 0");

		add(lGammaSlider, "cell 1 0,grow");

		final JSlider lMaxSlider =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * mClearVolumeRendererInterface.getTransferRangeMax( layerId ) ) );
		final JSlider lMinSlider =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * mClearVolumeRendererInterface.getTransferRangeMin( layerId ) ) );
		lMinSlider.setPreferredSize(new Dimension(0, 0));

		final JLabel lblMinMax = new JLabel( String.format( "Ch.%d: max", layerId ) );
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
				getClearVolumeRendererInterface().setTransferFunctionRangeMax( layerId, lMax );
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

		final JLabel lblMax = new JLabel( String.format( "Ch.%d: min", layerId ) );
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
				getClearVolumeRendererInterface().setTransferFunctionRangeMin( layerId, lMin );
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

	}

	public ClearVolumeRendererInterface getClearVolumeRendererInterface()
	{
		return mClearVolumeRendererInterface;
	}

	public void setClearVolumeRendererInterface(final ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;
	}

}
