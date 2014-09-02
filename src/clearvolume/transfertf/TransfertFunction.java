package clearvolume.transfertf;

/**
 * Interface TransfertFunction
 * 
 * Classes implementing this interface implement transfer functions of a given
 * dimension and provide the underlying data as a float array of row-major
 * ordering.
 *
 * @author Loic Royer 2014
 *
 */
public interface TransfertFunction
{
	/**
	 * Returns the dimension of the transfer function.
	 * 
	 * @return dimension
	 */
	int getDimension();
	
	/**
	 * Returns the data of the transfer function in the form of a float array in
	 * row major order.
	 * 
	 * @return
	 */
	float[] getArray();

}
