package clearvolume.renderer.cleargl.overlay.o2d;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.nio.FloatBuffer;

import javax.swing.JPanel;

import clearvolume.renderer.cleargl.overlay.o2d.panels.HistogramPanel;
import clearvolume.renderer.processors.ProcessorInterface;
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
	public void notifyResult(	ProcessorInterface<FloatBuffer> pSource,
														FloatBuffer pResult)
	{
		adjustMinMax(pResult);

		super.notifyResult(pSource, pResult);

	}

	public void setRange(float pMin, float pMax)
	{
		mHistoProcessor.setRange(pMin, pMax);
		setDisplayedRange(pMin, pMax);
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

	@Override
	public JPanel getPanel()
	{
		return new HistogramPanel(this);
	}

	private void adjustMinMax(FloatBuffer pResult)
	{
		final int lLength = pResult.capacity();

		float lMax = Float.MIN_VALUE;

		for (int i = 0; i < lLength; i++)
		{
			lMax = max(lMax, pResult.get(i));
		}

		final float lThreshold = 0.05f * lMax;

		final float lRange = mHistoProcessor.getRangeMax() - mHistoProcessor.getRangeMin();
		final float lStep = lRange / lLength;
		float lNewMin = 0;
		for (int i = 0; i < lLength && pResult.get(i) <= lThreshold; i++, lNewMin += lStep)
			;

		float lNewMax = 1;
		for (int i = lLength - 1; i >= 0 && pResult.get(i) <= lThreshold; i--, lNewMax -= lStep)
			;

		lNewMin = mHistoProcessor.getRangeMin() + max(0, lNewMin - 5
																											* lStep)
							* lRange;
		lNewMax = mHistoProcessor.getRangeMin() + min(1, lNewMax + 5
																											* lStep)
							* lRange;

		// for (int i = 0; i < lLength; i++)
		// System.out.println("->" + pResult.get(i));

		setRange(lNewMin, lNewMax);

		// System.out.format("min=%g\tmax=%g \n", lNewMin, lNewMax);

	}

}
