package clearvolume.controller;

import com.jogamp.opengl.math.Matrix4;

/**
 * Class TranslationControllerInterface
 *
 * @author Ulrik Günther 2015
 */
public interface TranslationControllerInterface
{

  /**
   * Sets the current Quaternion
   *
   * @param pQuaternion
   */
  void setTranslation(final float x, final float y, final float z);

  /**
   * Returns the current translation vector as float array
   *
   * @return current translation vector as float[]
   */
  float[] getTranslationVector();

  /**
   * Returns the current translation matrix
   */
  Matrix4 getTranslationMatrix();

  /**
   * Sets controller active flag
   *
   * @param pActive
   *          true for active, false for inactive
   */
  void setActive(boolean pActive);

  float[] getEyeShift();

  /**
   * Is controller active?
   *
   * @return true if controller is active.
   */
  boolean isActive();

}
