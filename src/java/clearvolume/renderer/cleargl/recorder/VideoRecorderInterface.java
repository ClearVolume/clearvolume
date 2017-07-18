package clearvolume.renderer.cleargl.recorder;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * Video recorders implement this interface
 *
 * @author royer
 */
public interface VideoRecorderInterface
{

  /**
   * Sets the target frame rate
   * 
   * @param pTargetFrameRate
   *          target frame rate
   */
  void setTargetFrameRate(double pTargetFrameRate);

  /**
   * Returns the target frame rate
   * 
   * @return target frame rate
   */
  double getTargetFrameRate();

  /**
   * Toggles this recorder state.
   */
  void toggleActive();

  /**
   * Sets this recorder active state.
   * 
   * @param pActive
   *          state: true records, false does not record
   */
  void setActive(boolean pActive);

  /**
   * Returns whether this recorder is active.
   * 
   * @return true if recorder active, false otherwise.
   */
  boolean isActive();

  /**
   * This method is called automatically from within a JOGL display method,
   * after all rendering that needs to be recorded has been done.
   * 
   * @param pDrawable
   *          JOGL GLAutoDrawable to be used to get pixel data from.
   * @param pAsynchronous
   *          true if call should be asynchronous
   * @return true if snapshot was taken (or _should_ be taken) false otherwise.
   */
  boolean screenshot(GLAutoDrawable pDrawable, boolean pAsynchronous);

}
