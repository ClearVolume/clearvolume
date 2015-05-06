package clearvolume.controller;

/**
 * Interface TranslationControllerInterface
 * 
 * @author Loic Royer 2014
 */
public interface TranslationControllerInterface	extends
																								ControllerInterface
{

	/**
	 * Sets the current Quaternion
	 * 
	 * @param pQuaternion
	 */
	void setVector(float[] pVector);

	/**
	 * Returns the current Quaternion
	 * 
	 * @param pVolumeViewMatrix
	 */
	float[] getVector();

}
