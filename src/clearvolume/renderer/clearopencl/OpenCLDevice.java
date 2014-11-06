package clearvolume.renderer.clearopencl;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLImageFormat.ChannelDataType;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.IOUtils;

public class OpenCLDevice
{

	public CLContext mCLContext;
	public CLProgram mCLProgram;
	public CLDevice mCLDevice;
	public CLQueue mCLQueue;
	public ByteOrder mCLContextByteOrder;

	public CLKernel mCLKernel;

	public boolean initCL()
	{
		return initCL(false);
	}

	public boolean initCL(final boolean useExistingOpenGLContext)
	{
		// initialize the platform and devices OpenCL will use
		// usually chooses the best, i.e. fastest, platform/device/context
		try
		{
			if (useExistingOpenGLContext)
			{
				// FIXME using existing OpenGL context does not work yet
				mCLContext = JavaCL.createContextFromCurrentGL();

			}
			else
			{
				mCLContext = JavaCL.createBestContext();
			}
		}
		catch (Exception e)
		{
			System.err.println("failed to create OpenCL context");
			return false;
		}

		try
		{
			mCLQueue = mCLContext.createDefaultQueue();

		}
		catch (Exception e)
		{
			System.err.println("failed to create OpenCL context");
			return false;
		}

		try
		{
			mCLDevice = mCLContext.getDevices()[0];
		}
		catch (Exception e)
		{
			System.err.println("could not get opencl device from context");
			e.printStackTrace();
			return false;
		}

		mCLContextByteOrder = mCLContext.getByteOrder();

		return (mCLContext != null && mCLContext != null && mCLQueue != null);

	}

	public CLContext getContext()
	{
		return mCLContext;
	}

	public void printInfo()
	{

		System.out.printf("Device name:    \t %s \n", mCLDevice);

	}

	public CLKernel compileKernel(final URL url, final String kernelName)
	{

		// Read the program sources and compile them :
		String src = "";
		try
		{
			src = IOUtils.readText(url);
		}
		catch (final Exception e)
		{
			System.err.println("couldn't read program source ");
			e.printStackTrace();
			return null;
		}

		try
		{
			mCLProgram = mCLContext.createProgram(src);
		}
		catch (final Exception e)
		{
			System.err.println("couldn't create program from " + src);
			e.printStackTrace();
			return null;
		}

		try
		{
			mCLKernel = mCLProgram.createKernel(kernelName);
		}
		catch (final Exception e)
		{
			System.err.println("couldn't create kernel '" + kernelName
													+ "'");
			e.printStackTrace();
		}

		return mCLKernel;
	}

	public void setArgs(Object... args)
	{

		mCLKernel.setArgs(args);

	}

	public CLEvent run(final int mNx, final int mNy)
	{
		final CLEvent evt = mCLKernel.enqueueNDRange(mCLQueue, new int[]
		{ mNx, mNy });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(final int mNx, final int mNy, final int mNz)
	{
		final CLEvent evt = mCLKernel.enqueueNDRange(mCLQueue, new int[]
		{ mNx, mNy, mNz });
		evt.waitFor();
		return evt;
	}

	public CLImage2D createImage2D(final int Nx, final int Ny)
	{

		final CLImageFormat fmt = new CLImageFormat(CLImageFormat.ChannelOrder.R,
																								CLImageFormat.ChannelDataType.SignedInt16);

		return mCLContext.createImage2D(Usage.Input, fmt, Nx, Ny);

	}

	public CLImage3D createShortImage3D(final long Nx,
																			final long Ny,
																			final long Nz)
	{

		final CLImageFormat fmt = new CLImageFormat(CLImageFormat.ChannelOrder.R,
																								CLImageFormat.ChannelDataType.SignedInt16);

		return mCLContext.createImage3D(Usage.Input, fmt, Nx, Ny, Nz);

	}

	public CLImage3D createImage3D(	final long Nx,
																	final long Ny,
																	final long Nz,
																	ChannelDataType pChannelDataType)
	{

		final CLImageFormat fmt = new CLImageFormat(CLImageFormat.ChannelOrder.R,
																								pChannelDataType);

		return mCLContext.createImage3D(Usage.Input, fmt, Nx, Ny, Nz);

	}

	public CLBuffer<Float> createInputFloatBuffer(final long N)
	{
		return mCLContext.createFloatBuffer(Usage.Input, N);
	}

	public CLBuffer<Short> createInputShortBuffer(final long N)
	{
		return mCLContext.createShortBuffer(Usage.Input, N);
	}

	public CLBuffer<Float> createOutputFloatBuffer(final long N)
	{
		return mCLContext.createFloatBuffer(Usage.Output, N);
	}

	public CLBuffer<Short> createOutputShortBuffer(final long N)
	{
		return mCLContext.createShortBuffer(Usage.Output, N);
	}

	public CLBuffer<Integer> createOutputIntBuffer(final long N)
	{

		return mCLContext.createIntBuffer(Usage.Output, N);
	}

	public CLBuffer<Byte> createOutputByteBuffer(final long N)
	{
		return mCLContext.createByteBuffer(Usage.Output, N);
	}

	public CLEvent writeFloatBuffer(final CLBuffer<Float> pCLBuffer,
																	final FloatBuffer pBuffer)
	{

		Pointer<Float> ptr = Pointer.pointerToFloats(pBuffer);

		return pCLBuffer.write(mCLQueue, ptr, true);

	}

	public CLEvent writeShortBuffer(final CLBuffer<Short> pCLBuffer,
																	final ShortBuffer pBuffer)
	{

		Pointer<Short> ptr = Pointer.pointerToShorts(pBuffer);

		return pCLBuffer.write(mCLQueue, ptr, true);

	}

	public CLEvent writeByteBuffer(	final CLBuffer<Byte> pCLBuffer,
																	final ByteBuffer pBuffer)
	{

		Pointer<Byte> ptr = Pointer.pointerToBytes(pBuffer);

		return pCLBuffer.write(mCLQueue, ptr, true);

	}

	public FloatBuffer readFloatBuffer(final CLBuffer<Float> pCLBuffer)
	{

		return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
										.getFloatBuffer();

	}

	public ShortBuffer readShortBuffer(final CLBuffer<Short> pCLBuffer)
	{

		return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
										.getShortBuffer();

	}

	public ByteBuffer readByteBuffer(final CLBuffer<Byte> pCLBuffer)
	{

		return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
										.getByteBuffer();

	}

	public ByteBuffer readIntBufferAsByte(final CLBuffer<Integer> pCLBuffer)
	{

		return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
										.getByteBuffer();

	}

	public CLEvent writeShortImage(	final CLImage3D img,
																	final ShortBuffer pShortBuffer)
	{
		if (img.getWidth() * img.getHeight() * img.getDepth() != pShortBuffer.capacity())
		{

			System.err.println("image and buffer sizes dont align!");
			return null;
		}

		return img.write(	mCLQueue,
											0,
											0,
											0,
											img.getWidth(),
											img.getHeight(),
											img.getDepth(),
											0,
											0,
											pShortBuffer,
											true);

	}

}
