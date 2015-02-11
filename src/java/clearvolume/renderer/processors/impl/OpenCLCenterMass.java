package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLCenterMass extends OpenCLProcessor<IntBuffer>
{

	private CLKernel mKernelHist;

	private CLBuffer<Float> mBufBins;
	private CLBuffer<Integer> mBufCounts;
	private float[] mBins;

	@Override
	public String getName()
	{
		return "opencl_histogram";
	}

	public void setBins(final float[] pBins)
	{
		mBins = pBins;

	}

	public void ensureOpenCLInitialized()
	{
		if (mKernelHist == null)
		{
			mKernelHist = getDevice().compileKernel(OpenCLCenterMass.class.getResource("kernels/histogram.cl"),
																							"histogram");
		}

		final float[] bins = new float[10];
		for (int i = 0; i < bins.length; i++)
		{
			bins[i] = 1.f * i / bins.length;
		}
		setBins(bins);

	}

	public void initBuffers()
	{

		final int lBinSize = mBins.length;

		// the buffer containing the counts
		mBufCounts = getDevice().createOutputIntBuffer(lBinSize);

		// the buffer containing the bins values
		mBufBins = getDevice().createInputFloatBuffer(lBinSize);

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
