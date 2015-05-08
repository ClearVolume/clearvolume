package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLHistogram extends OpenCLProcessor<FloatBuffer> {

	private CLKernel mKernelHist;
	private CLKernel mKernelClearCounts;

	static public final int N_BINS = 128;

	private CLBuffer<Integer> mBufCounts;

	float mMin = 0.f, mMax = 1.f;

	@Override
	public String getName() {
		return "opencl_histogram";
	}

	public void setRange(final float pMin, final float pMax) {

		mMax = pMax;
		mMin = pMin;

	}

	public void ensureOpenCLInitialized() {
		if (mKernelHist == null) {
			mKernelHist = getDevice().compileKernel(
					OpenCLHistogram.class.getResource("kernels/histogram.cl"),
					"histogram_naive");

			mKernelClearCounts = getDevice().compileKernel(
					OpenCLHistogram.class.getResource("kernels/histogram.cl"),
					"clear_counts");

		}

	}

	public void initBuffers() {

		// the buffer containing the counts
		mBufCounts = getDevice().createOutputIntBuffer(N_BINS);

	}

	@Override
	public void process(int pRenderLayerIndex, long pWidthInVoxels,
			long pHeightInVoxels, long pDepthInVoxels) {
		if (!isActive())
			return;

		long start = System.nanoTime();

		ensureOpenCLInitialized();

		if (mBufCounts == null) {
			System.out.println("setting up buffers");
			initBuffers();
		}

		// clear the count array
		mKernelClearCounts.setArgs(mBufCounts);
		getDevice().run(mKernelClearCounts, (int) N_BINS);

		// compute the histogram

		System.out.println(mMax);
		mKernelHist.setArgs(getVolumeBuffers()[pRenderLayerIndex], mBufCounts,
				mMin, mMax, N_BINS);

		getDevice().run(mKernelHist, (int) pWidthInVoxels,
				(int) pHeightInVoxels, (int) pDepthInVoxels);

		// fetch it from the gpu
		final IntBuffer out = getDevice().readIntBufferAsByte(mBufCounts)
				.asIntBuffer();

		long stop = System.nanoTime();

		if (false) {
			System.out.println(out.get(0));

			for (int i = 0; i < N_BINS; i++) {
				System.out.print(out.get(i) + " ");
			}

			System.out.printf("\n \n histogram: %.4f ms \n",
					(stop - start) / 1.e6f);

		}

		// FIXME
		final FloatBuffer out2 = FloatBuffer.allocate(N_BINS);
		final long lNumberOfPixels = pWidthInVoxels * pHeightInVoxels
				* pDepthInVoxels;

		for (int i = 0; i < N_BINS; i++) {
			out2.put(i, 1.f * out.get(i) / lNumberOfPixels);
		}

		notifyListenersOfResult(out2);

	}
}
