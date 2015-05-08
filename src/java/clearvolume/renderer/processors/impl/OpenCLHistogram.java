package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLHistogram extends OpenCLProcessor<FloatBuffer>
{

	private CLKernel mKernelHist;
	private CLKernel mKernelClearCounts;

	private CLBuffer<Integer> mBufCounts;

	private volatile float mMin = 0.f, mMax = 1.f;

	private volatile int mNumberOfBins = 128;
	private FloatBuffer mOutputBuffer;

	public OpenCLHistogram()
	{
		super();

	}

	@Override
	public String getName()
	{
		return "opencl_histogram";
	}

	public void setRange(final float pMin, final float pMax)
	{
		mMax = pMax;
		mMin = pMin;
	}

	public void ensureOpenCLInitialized()
	{
		if (mKernelHist == null)
		{
			mKernelHist = getDevice().compileKernel(OpenCLHistogram.class.getResource("kernels/histogram.cl"),
																							"histogram_naive");

			mKernelClearCounts = getDevice().compileKernel(	OpenCLHistogram.class.getResource("kernels/histogram.cl"),
																											"clear_counts");

		}

	}

	public void initBuffers()
	{
		if (mBufCounts == null || mBufCounts.getElementCount() != mNumberOfBins)
			mBufCounts = getDevice().createOutputIntBuffer(mNumberOfBins);
	}

	@Override
	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels)
	{
		if (!isActive())
			return;

		final long start = System.nanoTime();

		ensureOpenCLInitialized();

		if (mBufCounts == null)
		{
			// System.out.println("setting up buffers");
			initBuffers();
		}

		// clear the count array:
		mKernelClearCounts.setArgs(mBufCounts);
		getDevice().run(mKernelClearCounts, mNumberOfBins);

		// compute the histogram

		// System.out.println(mMax);
		mKernelHist.setArgs(getVolumeBuffers()[0],
												mBufCounts,
												mMin,
												mMax,
												mNumberOfBins);

		getDevice().run(mKernelHist,
										(int) pWidthInVoxels,
										(int) pHeightInVoxels,
										(int) pDepthInVoxels);

		// fetch it from the gpu
		final IntBuffer out = getDevice().readIntBufferAsByte(mBufCounts)
																			.asIntBuffer();

		final long stop = System.nanoTime();

		if (false)
			debugOutput(start, out, stop);

		if (mOutputBuffer == null || mOutputBuffer.capacity() != mNumberOfBins)
			mOutputBuffer = FloatBuffer.allocate(mNumberOfBins);

		final long lNumberOfPixels = pWidthInVoxels * pHeightInVoxels
																	* pDepthInVoxels;

		for (int i = 0; i < mNumberOfBins; i++)
		{
			mOutputBuffer.put(i, 1.f * out.get(i) / lNumberOfPixels);
		}

		notifyListenersOfResult(mOutputBuffer);

	}

	private void debugOutput(	final long start,
														final IntBuffer out,
														final long stop)
	{
		{
			System.out.println(out.get(0));

			for (int i = 0; i < mNumberOfBins; i++)
			{
				System.out.print(out.get(i) + " ");
			}

			System.out.printf("\n \n histogram: %.4f ms \n",
												(stop - start) / 1.e6f);

		}
	}

}
