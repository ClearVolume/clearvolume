package clearvolume.transfertf;

public class TransfertFunctions
{

	public static final TransfertFunction getDefaultTransfertFunction()
	{
		final TransfertFunction lTransfertFunction = new TransfertFunction();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(1, 0, 0, 1);
		lTransfertFunction.addPoint(1, 0, 0.5, 1);
		lTransfertFunction.addPoint(1, 1, 0, 1);
		lTransfertFunction.addPoint(0, 1, 0, 1);
		lTransfertFunction.addPoint(0, 1, 1, 1);
		lTransfertFunction.addPoint(0, 0, 1, 1);
		lTransfertFunction.addPoint(1, 0, 1, 1);
		lTransfertFunction.addPoint(0, 0, 0, 0);

		return lTransfertFunction;
	}

	public static final TransfertFunction getGrayLevel()
	{
		final TransfertFunction lTransfertFunction = new TransfertFunction();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	public static final TransfertFunction getBlueGradient()
	{
		final TransfertFunction lTransfertFunction = new TransfertFunction();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(0, 0, 1, 0.333);
		lTransfertFunction.addPoint(0, 1, 1, 0.666);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	public static final TransfertFunction getRedGradient()
	{
		final TransfertFunction lTransfertFunction = new TransfertFunction();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(1, 0, 0, 0.333);
		lTransfertFunction.addPoint(1, 1, 0, 0.666);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}

	public static final TransfertFunction getGreenGradient()
	{
		final TransfertFunction lTransfertFunction = new TransfertFunction();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(0, 1, 0, 0.333);
		lTransfertFunction.addPoint(0, 1, 1, 0.666);
		lTransfertFunction.addPoint(1, 1, 1, 1);
		return lTransfertFunction;
	}


	
	public static final TransfertFunction getRainbow()
	{
		final TransfertFunction lTransfertFunction = new TransfertFunction();
		lTransfertFunction.addPoint(0, 0, 0, 0);
		lTransfertFunction.addPoint(0, 0, 1, 1);
		lTransfertFunction.addPoint(0, 1, 1, 1);
		lTransfertFunction.addPoint(0, 1, 0, 1);
		lTransfertFunction.addPoint(1, 1, 0, 1);
		lTransfertFunction.addPoint(1, 0, 0, 1);
		lTransfertFunction.addPoint(1, 0, 1, 1);
		return lTransfertFunction;
	}

}
