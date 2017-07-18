package clearvolume.renderer.cleargl.recorder;

import static java.lang.Math.abs;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * Base class providing common fields and methods for all video recorder
 * implementations
 *
 * @author royer
 */
public abstract class VideoRecorderBase implements
                                        VideoRecorderInterface
{

  protected volatile long mLastImageTimePoint = 0;
  protected volatile boolean mFirstTime = true;

  protected volatile boolean mActive = false;
  protected volatile double mTargetFrameRate;

  /**
   * Instantiates a basic video recorder with target framerate set to 30 frames
   * per second.
   */
  public VideoRecorderBase()
  {
    super();
    mTargetFrameRate = 30;
  }

  /**
   * Instantiates a basic video recorder
   * 
   * @param pTargetFrameRate
   *          target frame rate
   */
  public VideoRecorderBase(double pTargetFrameRate)
  {
    super();
    mTargetFrameRate = pTargetFrameRate;
  }

  @Override
  public void toggleActive()
  {
    mActive = !mActive;
  }

  @Override
  public void setActive(boolean pActive)
  {
    mActive = pActive;
  }

  @Override
  public boolean isActive()
  {
    return mActive;
  }

  /**
   * Returns the target frame rate in FPS.
   * 
   * @return target frame rate in FPS.
   */
  @Override
  public double getTargetFrameRate()
  {
    return mTargetFrameRate;
  }

  /**
   * Sets target frame rate in FPS.
   * 
   * @param pTargetFrameRate
   *          target framerate in FPS.
   */
  @Override
  public void setTargetFrameRate(double pTargetFrameRate)
  {
    mTargetFrameRate = pTargetFrameRate;
  }

  /* Sub-classes should call this method todetermine whther it is time to take a snapshot
   */
  @Override
  public boolean screenshot(GLAutoDrawable pGLAutoDrawable,
                            boolean pAsynchronous)
  {
    return (mActive && !tooSoon());
  }

  private boolean tooSoon()
  {
    final long lCurrentTimePoint = System.nanoTime();
    final double lElpasedTimeInSeconds = 0.001 * 0.001
                                         * 0.001
                                         * (abs(lCurrentTimePoint
                                                - mLastImageTimePoint));
    final double lTargetPeriodInSeconds = 1 / getTargetFrameRate();
    if (lElpasedTimeInSeconds < lTargetPeriodInSeconds)
    {
      // System.out.println("too soon!");
      return true;
    }
    // System.out.println("go!");
    return false;
  }
}
