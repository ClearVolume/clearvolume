package clearvolume.transferf;

import java.util.ArrayList;

public class CyclingTransferFunction extends TransferFunction1D	implements
																																CyclableTransferFunction
{

	private final ArrayList<TransferFunction1D> mListOfTransferFunctions = new ArrayList<TransferFunction1D>();

	private int mIndex = 0;

	public static CyclingTransferFunction getDefault()
	{
		final CyclingTransferFunction lCyclingTransferFunction = new CyclingTransferFunction();

		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getGrayLevel());
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getCoolWarm());
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getMatlabStyle());
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getHot());
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getGradientForColor(0));
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getGradientForColor(1));
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getGradientForColor(2));
		lCyclingTransferFunction.addTransferFunction(TransferFunctions.getGradientForColor(3));

		return lCyclingTransferFunction;
	}

	public void addTransferFunction(TransferFunction1D pTransferFunction)
	{
		mListOfTransferFunctions.add(pTransferFunction);
	}

	@Override
	public int getCurrent()
	{
		return mIndex;
	}

	@Override
	public void next()
	{
		mIndex = (mIndex + 1) % mListOfTransferFunctions.size();
	}

	@Override
	public float[] getArray()
	{
		return mListOfTransferFunctions.get(mIndex).getArray();
	}

	@Override
	public int getDimension()
	{
		return mListOfTransferFunctions.get(mIndex).getDimension();
	}

}
