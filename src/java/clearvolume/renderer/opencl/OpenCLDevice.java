package clearvolume.renderer.opencl;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import org.bridj.Pointer;

import clearvolume.ClearVolumeCloseable;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLImageFormat.ChannelDataType;
import com.nativelibs4java.opencl.CLImageFormat.ChannelOrder;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.IOUtils;

public class OpenCLDevice implements ClearVolumeCloseable
{

	public CLContext mCLContext;
	public CLProgram mCLProgram;
	public CLDevice mCLDevice;
	public CLQueue mCLQueue;
	public ByteOrder mCLContextByteOrder;

	public ArrayList<CLKernel> mCLKernelList = new ArrayList<CLKernel>();

	public boolean initCL()
	{
		return initCL(false);
	}

	public boolean initCL(final boolean useExistingOpenGLContext)
	{
		// initialize the platform and devices OpenCL will use
		// usually chooses the best, i.e. fastest, platform/device/context
		CLPlatform bestPlatform = null;
		CLDevice bestDevice = null;
		try
		{

			if (useExistingOpenGLContext)
			{
				// FIXME using existing OpenGL context does not work yet
				mCLContext = JavaCL.createContextFromCurrentGL();

			}
			else
			{
				final CLPlatform[] platforms = JavaCL.listPlatforms();

				long maxMemory = 0;

				for (final CLPlatform p : platforms)
				{
					final CLDevice bestDeviceInPlatform = getDeviceWithMostMemory(p.listGPUDevices(true));

					if (bestDeviceInPlatform.getGlobalMemSize() > maxMemory)
					{
						maxMemory = bestDeviceInPlatform.getGlobalMemSize();
						bestDevice = bestDeviceInPlatform;
						bestPlatform = p;
					}
				}

				// final CLDevice bestDeviceInPlatform = JavaCL.getBestDevice();//
				// bestPlatform.listGPUDevices(true)[1];
				// bestDevice = bestDeviceInPlatform;

				System.out.println("Using " + bestDevice.getName()
														+ " from platform "
														+ bestPlatform.getName());

				mCLContext = JavaCL.createContext(null, bestDevice);

			}
		}
		catch (final Exception e)
		{
			System.err.println("failed to create OpenCL context");
			return false;
		}

		try
		{
			mCLQueue = mCLContext.createDefaultQueue();

		}
		catch (final Exception e)
		{
			System.err.println("failed to create OpenCL context");
			return false;
		}

		try
		{
			mCLDevice = bestDevice;
		}
		catch (final Exception e)
		{
			System.err.println("could not get opencl device from context");
			e.printStackTrace();
			return false;
		}

		mCLContextByteOrder = mCLContext.getByteOrder();

		return (mCLContext != null && mCLContext != null && mCLQueue != null);

	}

	private CLDevice getDeviceWithMostMemory(CLDevice[] devices)
	{
		long globalMemSize = 0;
		CLDevice bestDevice = null;

		for (final CLDevice lCLDevice : devices)
		{
			final long tmp = lCLDevice.getGlobalMemSize();

			System.out.println(lCLDevice.getPlatform().getName() + "."
													+ lCLDevice.getName()
													+ " L"
													+ lCLDevice.getLocalMemSize()
													/ 1024
													+ "k/G"
													+ lCLDevice.getGlobalMemSize()
													/ 1024
													/ 1024
													+ "M mem with "
													+ lCLDevice.getMaxComputeUnits()
													+ " compute units");

			final boolean lIsKnownHighPerfCard = lCLDevice.getName()
																										.toLowerCase()
																										.contains("geforce") || lCLDevice.getName()
																																											.toLowerCase()
																																											.contains("nvidia")
																						|| lCLDevice.getName()
																												.toLowerCase()
																												.contains("quadro")
																						|| lCLDevice.getName()
																												.toLowerCase()
																												.contains("firepro");

			if (tmp > globalMemSize || (tmp == globalMemSize && lIsKnownHighPerfCard))
			{
				bestDevice = lCLDevice;
				globalMemSize = tmp;
			}

		}

		if (bestDevice == null)
		{
			bestDevice = devices[0];
		}

		System.out.println(bestDevice.getName() + " is best in platform "
												+ bestDevice.getPlatform().getName());
		return bestDevice;
	}

	public CLContext getContext()
	{
		return mCLContext;
	}

	public void printInfo()
	{

		System.out.printf("Device name: \t %s \n", mCLDevice);

	}

	public int getKernelIndex(CLKernel pCLKernel)
	{
		return mCLKernelList.indexOf(pCLKernel);
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
			mCLProgram.setFastRelaxedMath();
			mCLProgram.setFiniteMathOnly();
			mCLProgram.setMadEnable();
			// mCLProgram.setNoSignedZero();
			mCLProgram.setUnsafeMathOptimizations();/**/
			try
			{
				// mCLProgram.setNVOptimizationLevel(3);
			}
			catch (final Throwable e)
			{
			}
		}
		catch (final Exception e)
		{
			System.err.println("couldn't create program from " + src);
			e.printStackTrace();
			return null;
		}

		CLKernel lNewKernel = null;
		try
		{
			lNewKernel = mCLProgram.createKernel(kernelName);
			mCLKernelList.add(lNewKernel);
		}
		catch (final Exception e)
		{
			System.err.println("couldn't create kernel '" + kernelName
													+ "'");
			e.printStackTrace();
		}

		return lNewKernel;
	}

	public void setArgs(final CLKernel pCLKernel, Object... args)
	{
		pCLKernel.setArgs(args);
	}

	public void setArgs(final int pKernelIndex, Object... args)
	{
		mCLKernelList.get(pKernelIndex).setArgs(args);
	}

	public CLEvent run(	final int pKernelIndex,
											final int mNx,
											final int mNy)
	{
		final CLEvent evt = mCLKernelList.get(pKernelIndex)
																			.enqueueNDRange(mCLQueue,
																											new int[]
																											{ mNx, mNy });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(	final int pKernelIndex,
											final int mNx,
											final int mNy,
											final int mNz)
	{
		final CLEvent evt = mCLKernelList.get(pKernelIndex)
																			.enqueueNDRange(mCLQueue,
																											new int[]
																											{ mNx, mNy, mNz });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(	final int pKernelIndex,
											final int mNx,
											final int mNy,
											final int mNz,
											final int mNxLoc,
											final int mNyLoc,
											final int mNzLoc)

	{
		final CLEvent evt = mCLKernelList.get(pKernelIndex)
																			.enqueueNDRange(mCLQueue,
																											new int[]
																											{ mNx, mNy, mNz },
																											new int[]
																											{ mNxLoc,
																												mNyLoc,
																												mNzLoc });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(final CLKernel pCLKernel, final int mNx)
	{
		final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]
		{ mNx });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(	final CLKernel pCLKernel,
											final int mNx,
											final int mNy)
	{
		final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]
		{ mNx, mNy });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(	final CLKernel pCLKernel,
											final int mNx,
											final int mNy,
											final int mNz)
	{
		final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]
		{ mNx, mNy, mNz });
		evt.waitFor();
		return evt;
	}

	public CLEvent run(	final CLKernel pCLKernel,
											final int mNx,
											final int mNy,
											final int mNz,
											final int mNxLoc,
											final int mNyLoc,
											final int mNzLoc)

	{
		final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]
		{ mNx, mNy, mNz }, new int[]
		{ mNxLoc, mNyLoc, mNzLoc });
		evt.waitFor();
		return evt;
	}

	public CLImage2D createGenericImage2D(final long Nx,
																				final long Ny,
																				ChannelOrder pChannelOrder,
																				ChannelDataType pChannelDataType)
	{

		final CLImageFormat fmt = new CLImageFormat(pChannelOrder,
																								pChannelDataType);

		return mCLContext.createImage2D(Usage.Input, fmt, Nx, Ny);

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

	public CLImage3D createGenericImage3D(final long Nx,
																				final long Ny,
																				final long Nz,
																				ChannelOrder pChannelOrder,
																				ChannelDataType pChannelDataType)
	{

		final CLImageFormat fmt = new CLImageFormat(pChannelOrder,
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

		final Pointer<Float> ptr = Pointer.pointerToFloats(pBuffer);

		return pCLBuffer.write(mCLQueue, ptr, true);

	}

	public CLEvent writeShortBuffer(final CLBuffer<Short> pCLBuffer,
																	final ShortBuffer pBuffer)
	{

		final Pointer<Short> ptr = Pointer.pointerToShorts(pBuffer);

		return pCLBuffer.write(mCLQueue, ptr, true);

	}

	public CLEvent writeByteBuffer(	final CLBuffer<Byte> pCLBuffer,
																	final ByteBuffer pBuffer)
	{

		final Pointer<Byte> ptr = Pointer.pointerToBytes(pBuffer);

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

	public CLEvent writeImage(final CLImage3D img,
														final ByteBuffer pByteBuffer)
	{
		return img.write(	mCLQueue,
											0,
											0,
											0,
											img.getWidth(),
											img.getHeight(),
											img.getDepth(),
											0,
											0,
											pByteBuffer,
											true);
	}

	public CLEvent writeImage(final CLImage2D img,
														final ByteBuffer pByteBuffer)
	{
		return img.write(	mCLQueue,
											0,
											0,
											img.getWidth(),
											img.getHeight(),
											0,
											pByteBuffer,
											true);
	}

	public CLEvent writeFloatImage2D(	final CLImage2D img,
																		final FloatBuffer pFloatBuffer)

	{

		return img.write(	mCLQueue,
											0,
											0,
											img.getWidth(),
											img.getHeight(),
											0,
											pFloatBuffer,
											true);

	}

	@Override
	public void close()
	{
		try
		{

			if (mCLKernelList != null)
				for (CLKernel lCLKernel : mCLKernelList)
				{
					lCLKernel.release();
					lCLKernel = null;
				}

			if (mCLProgram != null)
			{
				mCLProgram.release();
				mCLProgram = null;
			}

			if (mCLQueue != null)
			{
				mCLQueue.release();
				mCLQueue = null;
			}

			if (mCLContext != null)
			{
				mCLContext.release();
				mCLContext = null;
			}

			if (mCLDevice != null)
			{
				mCLDevice.release();
				mCLDevice = null;
			}

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}
}
