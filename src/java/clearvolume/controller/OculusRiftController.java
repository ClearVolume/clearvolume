package clearvolume.controller;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import clearvolume.renderer.DisplayRequestInterface;

import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.*;
import static com.oculusvr.capi.OvrLibrary.ovrHmdType.*;
import static com.oculusvr.capi.OvrLibrary.ovrRenderAPIType.*;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.*;

import com.oculusvr.capi.*;
import com.oculusvr.capi.OvrLibrary.ovrHmdCaps;
import com.oculusvr.capi.OvrLibrary.ovrTrackingCaps;
import com.sun.jna.Pointer;

import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Matrix4;

/**
 * Class OculusRiftController
 *
 * @author Ulrik Günther, 2015
 *
 */
public class OculusRiftController	extends
        TranslationRotationControllerBase implements
        TranslationRotationControllerInterface,
        Closeable,
        Runnable
{

  /**
   * Default Egg3D TCP port
   */
  // Quaternion and locking object.
  private final Quaternion mQuaternion = new Quaternion();
  private final Object mQuaternionUpdateLock = new Object();

  // Translation and locking
  private final Matrix4 mTranslation = new Matrix4();
  private final Object mTranslationUpdateLock = new Object();

  private final int mHmdNum;
  private final Hmd hmd;

  private Thread mReceptionThread;

  /**
   * DisplayRequest object that has to be called when requesting a display
   * update.
   */
  private final DisplayRequestInterface mDisplayRequest;

  /**
   * Opens the HMD with the given number and returns a hmd object.
   * If opening the HMD fails, the function will return a fake HMD.
   *
   * @return Hmd object
   */
  private static Hmd openHmd(int num) {
    Hmd hmd = Hmd.create(num);
    if(null == hmd) {
      return Hmd.createDebug(OvrLibrary.ovrHmdType.ovrHmd_DK2);
    }
    return hmd;
  }

  /**
   * Constructs an instance of the ExternalRotationController class
   *
   * @param pPortNumber
   *          port number
   * @param pDisplayRequest
   *          display request
   * @throws UnknownHostException
   *           thrown when host unknown
   * @throws IOException
   *           thrown when IO error
   */
  public OculusRiftController(final int hmdNum,
                                    DisplayRequestInterface pDisplayRequest)
  {
    super();
    mHmdNum = hmdNum;

    mQuaternion.setX(1);
    mQuaternion.normalize();
    mTranslation.loadIdentity();
    mDisplayRequest = pDisplayRequest;

    // initialize Oculus Rift API and open given HMD
    Hmd.initialize();
    hmd = openHmd(mHmdNum);

    if (0 == hmd.configureTracking(
            ovrTrackingCap_Orientation |
                    ovrTrackingCap_Position, 0)) {
      throw new IllegalStateException("Unable to start Oculus Rift HMD tracking sensors.");
    }

    System.out.println("Using Rift HMD: " + hmd.Type + ", sn=" + hmd.SerialNumber.toString() + ", res " + hmd.Resolution.w + "x" + hmd.Resolution.h);
    setActive(true);
  }

  /**
   * Starts a thread that asynchronously attempts to connect to the TCP server
   * using connect().
   */
  public void connectAsynchronouslyOrWait()
  {
    final Runnable lConnectionRunnable = new Runnable()
    {
      @Override
      public void run()
      {
        while (!connect())
          try
          {
            Thread.sleep(500);
          }
          catch (final InterruptedException e)
          {
          }
      }
    };
    final Thread lConnectionThread = new Thread(lConnectionRunnable,
            "ClearVolume-TRControllerConnectionThread");
    lConnectionThread.setDaemon(true);
    lConnectionThread.start();
  }

  /**
   * Makes one attempt at connecting to the TCP server and proceeds to start the
   * reception thread.
   *
   * @return true if connection succeeded.
   */
  public boolean connect()
  {
    try
    {
      mReceptionThread = new Thread(this,
              "ClearVolume-TRExternalController");
      mReceptionThread.setDaemon(true);
      mReceptionThread.start();

      return true;
    }
    catch (final Throwable e)
    {
      return false;
    }
  }

  /**
   * Interface method implementation
   *
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() throws IOException
  {
  }

  /**
   * Interface method implementation
   *
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    TrackingState s;
    Quaternion q = new Quaternion();
    OvrQuaternionf oq;

    while(true) {
      s = hmd.getSensorState(0.0);
      oq = s.HeadPose.Pose.Orientation;

      q.set(oq.x, oq.y, oq.z, oq.w);
      setQuaternion(q);
    }
  }

  /**
   * Interface method implementation
   *
   * @see clearvolume.controller.RotationControllerInterface#isActive()
   */
  @Override
  public boolean isActive()
  {
    return true;
  }

}
