package clearvolume.renderer.opencl.test;

import static org.junit.Assert.fail;

import org.junit.Test;

import clearvolume.renderer.opencl.OpenCLDevice;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLTests
{

	@Test
	public void test_creation()
	{
		try
		{
			OpenCLDevice dev = new OpenCLDevice();
			dev.initCL();
			dev.printInfo();

			final int N = 512;

			// create the buffer/image type we would need for the renderer

			CLBuffer<Float> clBufIn = dev.createInputFloatBuffer(N);
			CLBuffer<Integer> clBufOut = dev.createOutputIntBuffer(N);

			CLImage3D img = dev.createGenericImage3D(	N,
																								N,
																								N,
																								CLImageFormat.ChannelOrder.R,
																								CLImageFormat.ChannelDataType.SignedInt16);
		}
		catch (Throwable e)
		{

			fail();
			e.printStackTrace();
		}

	}

	@Test
	public void test_compile()
	{
		try
		{
			OpenCLDevice lOpenCLDevice = new OpenCLDevice();
			lOpenCLDevice.initCL();
			lOpenCLDevice.printInfo();

			CLKernel lCLKernel = lOpenCLDevice.compileKernel(	OpenCLTests.class.getResource("kernels/test.cl"),
																				"test_char");
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			fail();
		}

	}
}
