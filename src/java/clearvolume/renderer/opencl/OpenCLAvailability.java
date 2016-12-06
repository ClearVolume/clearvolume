package clearvolume.renderer.opencl;

import clearcl.backend.ClearCLBackends;

public class OpenCLAvailability
{
  public static final boolean isOpenCLAvailable()
  {
    try
    {
      return ClearCLBackends.getBestBackend() != null;
    }
    catch (final Throwable e)
    {
      e.printStackTrace();
      return false;
    }
  }
}
