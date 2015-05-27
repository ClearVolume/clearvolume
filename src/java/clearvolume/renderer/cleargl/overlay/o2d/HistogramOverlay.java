package clearvolume.renderer.cleargl.overlay.o2d;

import clearvolume.renderer.processors.impl.OpenCLHistogram;

import com.jogamp.newt.event.KeyEvent;

public class HistogramOverlay extends BarGraphOverlay
{

	private final OpenCLHistogram mHistoProcessor;

	public HistogramOverlay(OpenCLHistogram pHistoProcessor)
	{
		mHistoProcessor = pHistoProcessor;
		mHistoProcessor.addResultListener(this);
		mHistoProcessor.setActive(isDisplayed());
	}

	@Override
	public void setDisplayed(boolean pDisplay)
	{
		mHistoProcessor.setActive(pDisplay);
		super.setDisplayed(pDisplay);
	}

	@Override
	public boolean toggle()
	{
		setDisplayed(!isDisplayed());
		return isDisplayed();
	}

	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_H;
	}

	@Override
	public int toggleKeyModifierMask()
	{
		return 0;
	}

}
