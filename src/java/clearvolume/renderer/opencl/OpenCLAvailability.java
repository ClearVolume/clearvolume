package clearvolume.renderer.opencl;

public class OpenCLAvailability
{
	public static final boolean isOpenCLAvailable()
	{
		try
		{
			return com.nativelibs4java.opencl.JavaCL.getBestDevice() != null;
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
