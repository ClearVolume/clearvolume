package clearvolume.controller;

import javax.media.opengl.GL2;

/**
 * Class RotationControllerInterface
 * 
 * @author Loic Royer 2014
 */
public interface RotationControllerInterface
{

	/**
	 * Sets the values of the model-view matrix corresponding to this rotation
	 * controller state to the float array pModelViewMatrix.
	 * 
	 * @param pModelViewMatrix
	 *          float array into which the model-view matrix is written.
	 */
	void putModelViewMatrixIn(float[] pModelViewMatrix);

	/**
	 * Performs the model-view transform. TODO: do we need to have a reference to
	 * JOGL here? can we be mmore general?
	 * 
	 * @param pGl
	 */
	void rotateGL(GL2 pGl);

	/**
	 * Is controller active?
	 * 
	 * @return
	 */
	boolean isActive();

}
