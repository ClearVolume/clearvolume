package clearvolume.controller;

import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Matrix4;

/**
 * Class TranslationRotationControllerBase
 *
 * Example implementation of a TranslationControllerInterface plus a RotationControllerInterface using quaternions.
 *
 * @author Ulrik Günther, 2015
 *
 */
public class TranslationRotationControllerBase	implements
       TranslationRotationControllerInterface
{

  private volatile boolean mActive = true;
  private final Quaternion mQuaternion = new Quaternion();
  private final Matrix4 mTranslation = new Matrix4();

  protected final Object mQuaternionUpdateLock = new Object();
  protected final Object mTranslationUpdateLock = new Object();

  /**
   * Constructs an instance of the QuaternionRotationControllerBase class
   */
  public TranslationRotationControllerBase()
  {
    super();
    mQuaternion.setIdentity();
    mTranslation.loadIdentity();
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
   * Sets the quaternion.
   *
   * @param pQuaternion
   *          quaternion
   */
  @Override
  public void setQuaternion(final Quaternion pQuaternion)
  {
    synchronized (mQuaternionUpdateLock)
    {
      mQuaternion.setX(pQuaternion.getX());
      mQuaternion.setY(pQuaternion.getY());
      mQuaternion.setZ(pQuaternion.getZ());
      mQuaternion.setW(pQuaternion.getW());
    }
  }

  @Override
  public void setTranslation(final float x, final float y, final float z) {
    synchronized (mTranslationUpdateLock) {
      mTranslation.translate(x, y, z);
    }
  }

  /**
   * Returns a copy of the currently used quaternion.
   *
   * @return quaternion
   */
  @Override
  public Quaternion getQuaternion()
  {
    return mQuaternion;
  }

  @Override
  public float[] getTranslationVector()
  {
    return new float[]{mTranslation.getMatrix()[3], mTranslation.getMatrix()[7], mTranslation.getMatrix()[11]};
  }

  @Override
  public Matrix4 getTranslationMatrix() {
    return mTranslation;
  }

  public float[] getEyeShift() {
    return new float[]{0.0f, 0.0f};
  }

}
