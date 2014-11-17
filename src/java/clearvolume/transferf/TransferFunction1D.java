package clearvolume.transferf;

import gnu.trove.list.array.TFloatArrayList;

/**
 * TransferFunction1D
 * 
 * Instances of this class represent 1D transfer functions.
 *
 * @author Loic Royer 2014
 *
 */
public class TransferFunction1D implements TransferFunction
{
	final TFloatArrayList mTransferFunctionList = new TFloatArrayList();

	/**
	 * Constructs an instance of the TransferFunction1D class
	 */
	public TransferFunction1D()
	{
		super();
	}

	/**
	 * Adds a point in this 1D transfer function. e.g: addPointComponent(1.0)
	 * 
	 * @param pComponent
	 *          red , blue, green or apha component
	 */
	public void addPointComponent(final double pComponent)
	{
		mTransferFunctionList.add((float) pComponent);
	}

	/**
	 * Adds a point in this 1D transfer function. e.g: addPoint(1.0,0.5,0.3,1)
	 * 
	 * @param pRGBA
	 */
	public void addPoint(final double... pRGBA)
	{
		if (pRGBA.length != 4)
			complain();

		for (final double value : pRGBA)
			mTransferFunctionList.add((float) value);
	}

	/**
	 * Adds a point in this 1D transfer function. e.g: addPoint(1.0f,0.5f,0.3f,1f)
	 * 
	 * @param pRGBA
	 */
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

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.transferf.TransferFunction#getArray()
	 */
	@Override
	public float[] getArray()
	{
		return mTransferFunctionList.toArray();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.transferf.TransferFunction#getDimension()
	 */
	@Override
	public int getDimension()
	{
		return 1;
	}

	@Override
	public String toString()
	{
		return String.format(	"TransferFunction1D [mTransferFunctionList=%s]",
													mTransferFunctionList);
	}

}
