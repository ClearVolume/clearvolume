package clearvolume.controller;

import cleargl.GLMatrix;

/**
 * Class RotationControllerInterface
 * 
 * @author Loic Royer 2014
 */
public interface RotationControllerInterface
{

	/**
	 * Performs the model-view transform.
	 * 
	 * @param pVolumeViewMatrix
	 */
	void rotate(GLMatrix pVolumeViewMatrix);

	/**
	 * Is controller active?
	 * 
	 * @return
	 */
	boolean isActive();

}
