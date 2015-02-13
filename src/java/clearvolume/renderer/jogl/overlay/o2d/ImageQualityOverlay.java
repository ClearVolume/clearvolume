package clearvolume.renderer.jogl.overlay.o2d;

import clearvolume.renderer.jogl.overlay.OverlayForProcessors;
import clearvolume.renderer.jogl.overlay.SingleKeyToggable;
import clearvolume.renderer.processors.ProcessorResultListener;
import clearvolume.renderer.processors.impl.OpenCLTenengrad;

import com.jogamp.newt.event.KeyEvent;

public class ImageQualityOverlay extends OverlayForProcessors	implements
																															SingleKeyToggable
{

	public ImageQualityOverlay()
	{
		this(64);
	}

	@SuppressWarnings("unchecked")
	public ImageQualityOverlay(int pNumberOfPointsInGraph)
	{
		super(new GraphOverlay(pNumberOfPointsInGraph));

		final OpenCLTenengrad lOpenCLTenengrad = new OpenCLTenengrad();

		lOpenCLTenengrad.addResultListener((ProcessorResultListener<Double>) getDelegatedOverlay());
		addProcessor(lOpenCLTenengrad);
	}

	@Override
	public boolean toggleDisplay()
	{
		boolean lToggleDisplay = super.toggleDisplay();

		if (lToggleDisplay)
			((GraphOverlay) getDelegatedOverlay()).clear();
		return lToggleDisplay;
	}

	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_I;
	}

	@Override
	public int toggleKeyModifierMask()
	{
		return 0; // KeyEvent.CTRL_MASK;
	}

}
