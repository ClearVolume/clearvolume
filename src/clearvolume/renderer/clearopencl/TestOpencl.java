package clearvolume.renderer.clearopencl;

import org.junit.Test;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

public class TestOpencl
{

	@Test
	public void test_creation()
	{
		OpenCLDevice dev = new OpenCLDevice();
		dev.initCL();
		dev.printInfo();
		
		final int N = 100;

		// create the buffer/image type we would need for the renderer

		CLBuffer<Float> clBufIn = dev.createInputFloatBuffer(N);
		CLBuffer<Integer> clBufOut = dev.createOutputIntBuffer(N);

		CLImage3D img = dev.createGenericImage3D(	N,
																							N,
																							N,
																							CLImageFormat.ChannelOrder.R,
																							CLImageFormat.ChannelDataType.SignedInt16);

	}

	@Test
	public void test_compile()
	{
		OpenCLDevice dev = new OpenCLDevice();
		dev.initCL();
		dev.printInfo();

		CLKernel kern = dev.compileKernel(TestOpencl.class.getResource("kernels/volume_render.cl"),
											"max_project");

	}
}
