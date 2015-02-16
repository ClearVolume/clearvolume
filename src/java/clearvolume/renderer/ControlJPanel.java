package clearvolume.renderer;

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

		setLayout( new MigLayout( "", "[min!,right][:1200:][min!,right][:1200:]", "[][][]" ) );

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

		final JLabel lblGamma = new JLabel( String.format( "Ch.%d:   gamma", layerId ) );
		add( lblGamma );

		add( lGammaSlider, "span,grow,wrap" );

		final JSlider lMinSlider =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * mClearVolumeRendererInterface.getTransferRangeMin( layerId ) ) );
		final JSlider lMaxSlider =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * mClearVolumeRendererInterface.getTransferRangeMax( layerId ) ) );

		final JLabel lblMin = new JLabel( String.format( "min" ) );
		add( lblMin );

		add( lMinSlider, "grow" );
		lMinSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				if (lMinSlider.getValue() > lMaxSlider.getValue())
				{
					lMaxSlider.setValue( lMinSlider.getValue() + 1 );
					// lMaxSlider.repaint();
				}
				final float lMin = 1f * source.getValue() / cPrecision;
				getClearVolumeRendererInterface().setTransferFunctionRangeMin( layerId, lMin );
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

		final JLabel lblMax = new JLabel( String.format( "max" ) );
		add( lblMax );

		add( lMaxSlider, "grow,wrap" );
		lMaxSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				if (lMinSlider.getValue() > lMaxSlider.getValue())
				{
					lMinSlider.setValue( lMaxSlider.getValue() - 1 );
					// lMinSlider.repaint();
				}
				final float lMax = 1f * source.getValue() / cPrecision;
				getClearVolumeRendererInterface().setTransferFunctionRangeMax( layerId, lMax );
				getClearVolumeRendererInterface().requestDisplay();
			}
		});

		final JSlider lQuality =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * mClearVolumeRendererInterface.getQuality( layerId ) ) );

		final JLabel lblQuality = new JLabel( String.format( "quality" ) );
		add( lblQuality );

		add( lQuality, "grow" );
		lQuality.addChangeListener( new ChangeListener()
		{

			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final JSlider source = ( JSlider ) e.getSource();
				final float lQ = 1f * source.getValue() / cPrecision;
				mClearVolumeRendererInterface.setQuality( layerId, lQ );
				mClearVolumeRendererInterface.requestDisplay();
			}
		} );

		final JSlider lDithering =
				new JSlider( 0, cPrecision, ( int ) ( cPrecision * mClearVolumeRendererInterface.getDithering( layerId ) ) );

		final JLabel lblDithering = new JLabel( String.format( "dithering" ) );
		add( lblDithering );

		add( lDithering, "grow" );
		lDithering.addChangeListener( new ChangeListener()
		{

			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final JSlider source = ( JSlider ) e.getSource();
				final float lDit = 1f * source.getValue() / cPrecision;
				mClearVolumeRendererInterface.setDithering( layerId, lDit );
				mClearVolumeRendererInterface.requestDisplay();
			}
		} );
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
