package clearvolume.renderer.processors.impl;

import static java.lang.Math.log1p;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.JPanel;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

import clearvolume.renderer.cleargl.overlay.o2d.panels.HistogramPanel;
import clearvolume.renderer.panels.HasGUIPanel;
import clearvolume.renderer.processors.OpenCLProcessor;

public class OpenCLHistogram extends OpenCLProcessor<FloatBuffer>	implements
																	HasGUIPanel
{

	private CLKernel mKernelHist;
	private CLKernel mKernelClearCounts;

	private CLBuffer<Integer> mBufCounts;

	private volatile float mMin = 0.f, mMax = 1.f;
	private volatile boolean mLogarithm = false;
	private volatile int mNumberOfBins = 64;
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

	public void setLogarithm(boolean pLogarithm)
	{
		mLogarithm = pLogarithm;
	}

	public void setRange(final float pMin, final float pMax)
	{
		mMax = pMax;
		mMin = pMin;
	}

	public float getRangeMin()
	{
		return mMin;
	}

	public float getRangeMax()
	{
		return mMax;
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

		final long lNumberOfVoxels = pWidthInVoxels * pHeightInVoxels
										* pDepthInVoxels;

		for (int i = 0; i < mNumberOfBins; i++)
		{
			float lValue = 1.f * out.get(i) / lNumberOfVoxels;
			if (mLogarithm)
				lValue = (float) log1p(lValue);
			mOutputBuffer.put(i, lValue);
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

			System.out.printf(	"\n \n histogram: %.4f ms \n",
								(stop - start) / 1.e6f);

		}
	}

	@Override
	public JPanel getPanel()
	{
		final HistogramPanel lJPanel = new HistogramPanel(this);

		return null;
	}

}
