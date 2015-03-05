package clearvolume.renderer.opencl;

import com.nativelibs4java.opencl.JavaCL;

public class OpenCLAvailability
{
	public static final boolean isOpenCLAvailable()
	{
		try
		{
			return JavaCL.getBestDevice() != null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
