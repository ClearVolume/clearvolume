package clearvolume.renderer;

/**
 * Interface DisplayRequest
 * 
 * Classes that implement this interface can b requested to update their
 * 'display'.
 *
 * @author Loic Royer 2014
 *
 */
public interface DisplayRequestInterface
{
	/**
	 * Requests the update of the display in a fair manner.
	 */
	void requestDisplay();

	/**
	 * Requests the update of the display in an unfair manner - it will pass the
	 * line of waiting threads.
	 */
	// void requestDisplayUnfairly();
}
