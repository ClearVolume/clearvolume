package clearvolume.renderer.cleargl.overlay.o2d;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.nio.FloatBuffer;

import javax.swing.JPanel;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL;

import cleargl.GLMatrix;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.processors.ProcessorInterface;
import clearvolume.renderer.processors.impl.OpenCLHistogram;

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
		return null; // new HistogramPanel(this);
	}

	private void adjustMinMax(FloatBuffer pResult)
	{
		final int lLength = pResult.capacity();

		float lMax = Float.MIN_VALUE;

		for (int i = 0; i < lLength; i++)
		{
			lMax = max(lMax, pResult.get(i));
		}

		final float lThreshold = 0.01f * lMax;

		final float lRange = mHistoProcessor.getRangeMax() - mHistoProcessor.getRangeMin();
		final float lStep = lRange / lLength;
		float lNewMin = 0;
		for (int i = 0; i < lLength && pResult.get(i) <= lThreshold; i++, lNewMin += lStep)
			;

		float lNewMax = 1;
		for (int i = lLength - 1; i >= 0 && pResult.get(i) <= lThreshold; i--, lNewMax -= lStep)
			;

		lNewMin = max(	0,
						mHistoProcessor.getRangeMin() + (lNewMin - 5 * lStep)
								* lRange);
		lNewMax = min(	1,
						mHistoProcessor.getRangeMin() + (lNewMax + 5 * lStep)
								* lRange);

		if (abs(lNewMax - lNewMin) < (1.0f / 128))
		{
			lNewMin = lNewMin - 0.5f * 128 * abs(lNewMax - lNewMin);
			lNewMax = lNewMax + 0.5f * 128 * abs(lNewMax - lNewMin);
		}

		lNewMin = 0.9f * lNewMin
					+ 0.1f
					* mHistoProcessor.getRangeMin();
		lNewMax = 0.9f * lNewMax
					+ 0.1f
					* mHistoProcessor.getRangeMax();

		// for (int i = 0; i < lLength; i++)
		// System.out.println("->" + pResult.get(i));

		setRange(lNewMin, lNewMax);

		// System.out.format("min=%g\tmax=%g \n", lNewMin, lNewMax);

	}

	@Override
	public void render2D(	ClearGLVolumeRenderer pClearGLVolumeRenderer,
							GL pGL,
							int pWidth,
							int pHeight,
							GLMatrix pProjectionMatrix)
	{
		if (isDisplayed())
		{
			// Not implemented yet (WHY??? It was there )
		}
	}
}
