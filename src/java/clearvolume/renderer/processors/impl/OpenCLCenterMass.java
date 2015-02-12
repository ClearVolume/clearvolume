package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLCenterMass extends OpenCLProcessor<IntBuffer>
{

	private CLKernel mKernel;

	private CLBuffer<Float> mBufX,mBufY,mBufZ,mBufSum;
	
	@Override
	public String getName()
	{
		return "opencl_center_of_mass";
	}

	public void ensureOpenCLInitialized()
	{
		if (mKernel == null)
		{
			mKernel = getDevice().compileKernel(OpenCLCenterMass.class.getResource("kernels/center_of_mass.cl"),
																							"center_of_mass_img");
		}
	}

	public void initBuffers()
	{

		long lBinSize = 100;
		// the buffer containing the counts
		mBufX = getDevice().createOutputFloatBuffer(lBinSize);

		
	}

	@Override
	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels)
	{
		if (!isActive())
			return;

		ensureOpenCLInitialized();

		if (mBufBins == null)
		{
			System.out.println("setting up buffers");
			initBuffers();
		}

		// fill the bins
		getDevice().writeFloatBuffer(mBufBins, FloatBuffer.wrap(mBins));

		final FloatBuffer bins = FloatBuffer.wrap(mBins);

		mKernelHist.setArgs(getVolumeBuffers()[0],
												mBufBins,
												mBufCounts,
												mBins.length);

		getDevice().run(mKernelHist,
										(int) pWidthInVoxels,
										(int) pHeightInVoxels,
										(int) pDepthInVoxels);

		final IntBuffer out = getDevice().readIntBufferAsByte(mBufCounts)
																.asIntBuffer();

		for (int i = 0; i < out.capacity(); i++)
		{
			System.out.println(out.get(i));
		}

		notifyListenersOfResult(out);

	}
}
