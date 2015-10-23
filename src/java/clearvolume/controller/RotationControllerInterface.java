package clearvolume.controller;

import com.jogamp.opengl.math.Quaternion;

/**
 * Class RotationControllerInterface
 * 
 * @author Loic Royer 2014
 */
public interface RotationControllerInterface
{

	/**
	 * Sets the current Quaternion
	 * 
	 * @param pQuaternion
	 */
	void setQuaternion(final Quaternion pQuaternion);

	/**
	 * Returns the current Quaternion
	 * 
	 * @return current quaternion
	 */
	Quaternion getQuaternion();

	/**
	 * Sets controller active flag
	 * 
	 * @param pActive
	 *            true for active, false for inactive
	 */
	void setActive(boolean pActive);

	/**
	 * Is controller active?
	 * 
	 * @return true if controller is active.
	 */
	boolean isActive();

}
