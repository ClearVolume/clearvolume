package clearvolume.renderer.jogl.overlay.o2d;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import javax.media.opengl.GL4;

import cleargl.ClearTextRenderer;
import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.OverlayForProcessors;
import clearvolume.renderer.jogl.overlay.SingleKeyToggable;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;
import clearvolume.renderer.processors.impl.OpenCLTenengrad;

import com.jogamp.newt.event.KeyEvent;

public class ImageQualityOverlay extends OverlayForProcessors	implements
																															SingleKeyToggable
{

	private OpenCLTenengrad mOpenCLTenengrad;

	private Font mFont;
	private ClearTextRenderer mClearTextRenderer;
	private volatile double mMeasure;

	public ImageQualityOverlay()
	{
		this(20);
	}

	@SuppressWarnings("unchecked")
	public ImageQualityOverlay(int pNumberOfPointsInGraph)
	{
		super(new PlainGraphOverlay(pNumberOfPointsInGraph));

		mOpenCLTenengrad = new OpenCLTenengrad();

		mOpenCLTenengrad.addResultListener((ProcessorResultListener<Double>) getDelegatedOverlay());

		mOpenCLTenengrad.addResultListener(new ProcessorResultListener<Double>()
		{

			@Override
			public void notifyResult(	Processor<Double> pSource,
																Double pResult)
			{
				mMeasure = pResult;
			}
		});

		addProcessor(mOpenCLTenengrad);
	}

	@Override
	public boolean toggleDisplay()
	{
		boolean lToggleDisplay = super.toggleDisplay();

		if (lToggleDisplay)
			getGraphOverlay().clear();
		return lToggleDisplay;
	}

	public PlainGraphOverlay getGraphOverlay()
	{
		return (PlainGraphOverlay) getDelegatedOverlay();
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

	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		super.init(pGL4, pDisplayRequestInterface);
		final String lFontPath = "/clearvolume/fonts/SourceCodeProLight.ttf";
		try
		{

			mFont = Font.createFont(Font.TRUETYPE_FONT,
															getClass().getResourceAsStream(lFontPath))
									.deriveFont(24.f);
		}
		catch (final FontFormatException | IOException e)
		{
			// use a fallback font in case the original couldn't be found or there has
			// been a problem
			// with the font format
			System.err.println("Could not use \"" + lFontPath
													+ "\" ("
													+ e.toString()
													+ "), falling back to Sans.");
			mFont = new Font("Sans", Font.PLAIN, 24);
		}

		mClearTextRenderer = new ClearTextRenderer(pGL4, true);
	}

	@Override
	public void render2D(GL4 pGL4, GLMatrix pProjectionMatrix)
	{
		super.render2D(pGL4, pProjectionMatrix);

		/*mClearTextRenderer.drawTextAtPosition(String.format("image quality metric: %g",
																												mMeasure),
																					10,
																					15,
																					mFont,
																					FloatBuffer.wrap(new float[]
																					{ 1.0f, 1.0f, 1.0f }),
																					true);/**/

	}

}
