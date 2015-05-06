package clearvolume.controller;



/**
 * Interface ControllerInterface
 * 
 * @author Loic Royer 2014
 */
public interface ControllerInterface
{

	/**
	 * Sets controller active flag
	 * 
	 * @param pActive
	 *          true for active, false for inactive
	 */
	void setActive(boolean pActive);

	/**
	 * Is controller active?
	 * 
	 * @return true if controller is active.
	 */
	boolean isActive();
	

}
