package clearvolume.transfertf;

import gnu.trove.list.array.TFloatArrayList;

public class TransfertFunction1D implements TransfertFunction
{
	final TFloatArrayList mTransferFunctionList = new TFloatArrayList();

	public TransfertFunction1D()
	{
		super();
	}

	public void addPoint(final double... pRGBA)
	{
		if (pRGBA.length != 4)
			complain();

		for (final double value : pRGBA)
			mTransferFunctionList.add((float) value);
	}

	public void addPoint(final float... pRGBA)
	{
		if (pRGBA.length != 4)
			complain();

		mTransferFunctionList.add(pRGBA);
	}

	private void complain()
	{
		throw new IllegalArgumentException("Invalid parameter for addPoint: should be 4 floats (RGBA)");
	}

	public float[] getArray()
	{
		return mTransferFunctionList.toArray();
	}

	@Override
	public int getDimension()
	{
		return 1;
	}
}
