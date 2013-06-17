package clearvolume.transfertf;

import gnu.trove.list.array.TFloatArrayList;

public class TransfertFunction
{
	final TFloatArrayList mTransferFunctionList = new TFloatArrayList();

	public TransfertFunction()
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
}

/*
 * // The RGBA components of the transfer function texture
		final float transferFunc[] = new float[]
		{ 0.0f,
			0.0f,
			0.0f,
			0.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			1.0f,
			0.5f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			1.0f,
			0.0f,
			1.0f,
			0.0f,
			1.0f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			1.0f,
			1.0f,
			0.0f,
			0.0f,
			0.0f,
			0.0f };*/
