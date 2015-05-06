package clearvolume.controller;

import com.jogamp.opengl.math.Quaternion;

/**
 * Interface RotationControllerInterface
 * 
 * @author Loic Royer 2014
 */
public interface RotationControllerInterface extends
																						ControllerInterface
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
	 * @param pVolumeViewMatrix
	 */
	Quaternion getQuaternion();


}
