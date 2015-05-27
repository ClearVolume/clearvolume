package clearvolume.transferf;

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

	public static TransferFunction getDefault()
	{
		return CoolWarmTransferFunction.get();
		// return MatlabStyleTransferFunction.get();
		// return HotTransferFunction.get();
	}

	public static TransferFunction getGradientForColor(int pColorIndex)
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

	public static TransferFunction getGradientForColor(float... pColorRGBA)
	{
		final TransferFunction1D lTransfertFunction = new TransferFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(pColorRGBA);
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

	public static TransferFunction getCoolWarm()
	{
		return CoolWarmTransferFunction.get();
	}

}
