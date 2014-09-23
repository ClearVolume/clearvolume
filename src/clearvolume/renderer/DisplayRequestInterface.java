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
	 * Requests the update of the display.
	 */
	void requestDisplay();
}
