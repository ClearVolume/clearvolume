package clearvolume.renderer.clearopencl;

import java.nio.ShortBuffer;

import org.junit.Test;

import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLKernel;

public class TestOpencl
{

	// @Test
	// public void test()
	// {
	// OpenCLDevice dev = new OpenCLDevice();
	// dev.initCL();
	// dev.printInfo();
	// CLKernel kern =
	// dev.compileKernel(TestOpencl.class.getResource("kernels/test.cl"),
	// "test_int");
	//
	// final int N = 100;
	// CLBuffer<Integer> clBufOut = dev.createOutputIntBuffer(N * N);
	//
	// dev.setArgs(clBufOut, N);
	//
	// dev.run(N, N);
	//
	// ByteBuffer outBuf = ByteBuffer.allocate(4 * N * N);
	//
	// outBuf = dev.readIntBufferAsByte(clBufOut);
	//
	// // System.out.println(Arrays.toString(fBuf2.array()));
	// System.out.println(outBuf.get(3));
	//
	// }

	@Test
	public void test()
	{
		OpenCLDevice dev = new OpenCLDevice();
		dev.initCL();
		dev.printInfo();
		CLKernel kern = dev.compileKernel(TestOpencl.class.getResource("kernels/test.cl"),
																			"test_int");

		final int N = 10;

		ShortBuffer pBuf = ShortBuffer.allocate(N * N * N);

		// CLBuffer<Integer> clBufOut = dev.createOutputIntBuffer(N * N * N);

		CLImage3D clImg = dev.createShortImage3D(N, N, N);

		dev.writeShortImage(clImg, pBuf);

		System.out.println("hallo");

		// dev.setArgs(clBufOut, N, clImg);
		//
		// dev.run(N, N);
		//
		// ByteBuffer outBuf = ByteBuffer.allocate(4 * N * N);
		//
		// outBuf = dev.readIntBufferAsByte(clBufOut);
		//
		// // System.out.println(Arrays.toString(fBuf2.array()));
		// System.out.println(outBuf.get(3));

	}
}
