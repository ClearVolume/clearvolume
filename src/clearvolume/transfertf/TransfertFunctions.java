package clearvolume.transfertf;

/**
 * Class TransfertFunctions
 * 
 * This class provides typical and ready-to-use transfer functions
 *
 * @author Loic Royer 2014
 *
 */
public class TransfertFunctions
{

	/**
	 * Returns gray level transfer function.
	 * 
	 * @return gray level transfer function
	 */
	public static final TransfertFunction1D getGrayLevel()
	{
		final TransfertFunction1D lTransfertFunction = new TransfertFunction1D();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	/**
	 * Returns blue gradient transfer function.
	 * 
	 * @return blue gradient transfer function
	 */
	public static final TransfertFunction1D getBlueGradient()
	{
		final TransfertFunction1D lTransfertFunction = new TransfertFunction1D();
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
	public static final TransfertFunction1D getRedGradient()
	{
		final TransfertFunction1D lTransfertFunction = new TransfertFunction1D();
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
	public static final TransfertFunction1D getGreenGradient()
	{
		final TransfertFunction1D lTransfertFunction = new TransfertFunction1D();
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
	public static final TransfertFunction1D getRainbow()
	{
		final TransfertFunction1D lTransfertFunction = new TransfertFunction1D();
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
	public static final TransfertFunction1D getRainbowSolid()
	{
		final TransfertFunction1D lTransfertFunction = new TransfertFunction1D();
		lTransfertFunction.addPoint(0, 0, 1, 1);
		lTransfertFunction.addPoint(0, 1, 1, 1);
		lTransfertFunction.addPoint(0, 1, 0, 1);
		lTransfertFunction.addPoint(1, 1, 0, 1);
		lTransfertFunction.addPoint(1, 0, 0, 1);
		lTransfertFunction.addPoint(1, 0, 1, 1);
		return lTransfertFunction;
	}

}
