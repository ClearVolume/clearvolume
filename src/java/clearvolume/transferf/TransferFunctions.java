package clearvolume.transferf;

import java.awt.Color;

/**
 * Class TransferFunctions
 * 
 * This class provides typical and ready-to-use transfer functions
 *
 * @author Loic Royer 2014
 *
 */
public class TransferFunctions
{

	/**
	 * @return
	 */
	public static TransferFunction getDefault()
	{
		return CyclingTransferFunction.getDefault();
		// return MatlabStyleTransferFunction.get();
		// return HotTransferFunction.get();
	}

	/**
	 * @param pColorIndex
	 * @return
	 */
	public static TransferFunction1D getGradientForColor(int pColorIndex)
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		switch (pColorIndex % 4)
		{
		case 0:
			lTransfertFunction.addPoint(0, 0, 0, 0);
			lTransfertFunction.addPoint(0.541, 0.482, 0.686, 1);
			break;

		case 1:
			lTransfertFunction.addPoint(0, 0, 0, 0);
			lTransfertFunction.addPoint(1, 0.698, 0.667, 1);
			break;

		case 2:
			lTransfertFunction.addPoint(0, 0, 0, 0);
			lTransfertFunction.addPoint(0.502, 0.757, 0.561, 1);
			break;

		case 3:
			lTransfertFunction.addPoint(0, 0, 0, 0);
			lTransfertFunction.addPoint(1, 0.949, 0.667, 1);
			break;

		}
		return lTransfertFunction;
	}

	/**
	 * Returns a simple transfer function that is a gradient from
	 * dark transparent to a given color. The color is given as a float vector in [0,1]^4.
	 * @param pColorRGBA color (R,G,B,A) with each R,G,B,A in [0,1]
	 * @return transfer function
	 */
	public static TransferFunction1D getGradientForColor(float... pColorRGBA)
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(pColorRGBA);
		return lTransfertFunction;
	}

	/**
	 * Returns a simple transfer function that is a gradient from
	 * dark transparent to a given color. The transparency of the given color is used.
	 * 
	 * @param pColor color
	 * @return 1D transfer function.
	 */
	public static TransferFunction1D getGradientForColor(Color pColor)
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		float lNormaFactor = (float) (1.0 / 255);
		lTransfertFunction.addPoint(lNormaFactor * pColor.getRed(),
									lNormaFactor * pColor.getGreen(),
									lNormaFactor * pColor.getBlue(),
									lNormaFactor * pColor.getAlpha());
		return lTransfertFunction;
	}

	/**
	 * Returns gray level transfer function.
	 * 
	 * @return gray level transfer function
	 */
	public static final TransferFunction1D getGrayLevel()
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns blue gradient transfer function.
	 * 
	 * @return blue gradient transfer function
	 */
	public static final TransferFunction1D getBlueGradient()
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(0, 0, 1, 0.333);
		lTransfertFunction.addPoint(0, 1, 1, 0.666);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns red gradient transfer function.
	 * 
	 * @return red gradient transfer function
	 */
	public static final TransferFunction1D getRedGradient()
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(1, 0, 0, 0.333);
		lTransfertFunction.addPoint(1, 1, 0, 0.666);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns green gradient transfer function.
	 * 
	 * @return green gradient transfer function
	 */
	public static final TransferFunction1D getGreenGradient()
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(0, 1, 0, 0.333);
		lTransfertFunction.addPoint(0, 1, 1, 0.666);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns rainbow transfer function.
	 * 
	 * @return rainbow transfer function
	 */
	public static final TransferFunction1D getRainbow()
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(0, 0, 1, 1);
		lTransfertFunction.addPoint(0, 1, 1, 1);
		lTransfertFunction.addPoint(0, 1, 0, 1);
		lTransfertFunction.addPoint(1, 1, 0, 1);
		lTransfertFunction.addPoint(1, 0, 0, 1);
		lTransfertFunction.addPoint(1, 0, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns rainbow variant transfer function.
	 * 
	 * @return rainbow variant transfer function
	 */
	public static final TransferFunction1D getRainbowSolid()
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 1, 1);
		lTransfertFunction.addPoint(0, 1, 1, 1);
		lTransfertFunction.addPoint(0, 1, 0, 1);
		lTransfertFunction.addPoint(1, 1, 0, 1);
		lTransfertFunction.addPoint(1, 0, 0, 1);
		lTransfertFunction.addPoint(1, 0, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns the 'CoolWarm' LUT.
	 * @return
	 */
	public static TransferFunction1D getCoolWarm()
	{
		return CoolWarmTransferFunction.get();
	}

	/**
	 * Returns the Hot LUT.
	 * @return
	 */
	public static TransferFunction1D getHot()
	{
		return HotTransferFunction.get();
	}

	/**
	 * Returns the standard Matlab style LUT.
	 * @return 
	 */
	public static TransferFunction1D getMatlabStyle()
	{
		return MatlabStyleTransferFunction.get();
	}

}
