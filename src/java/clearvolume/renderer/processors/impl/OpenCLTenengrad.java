package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;
import java.util.Arrays;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLTenengrad extends OpenCLProcessor<Double>
{

	private CLKernel mKernelDownsample;
	private CLKernel mKernelDiff;
	private CLKernel mKernelSmooth;
	private CLKernel mKernelSum;
	private CLKernel mKernelBlur;
	private CLKernel mKernelCopy;

	private CLBuffer<Float> mBufDownSampled;
	private CLBuffer<Float> mBufGx, mBufGy, mBufGz, mBufScratch,
			mBufRes;

	private long mCurrentWidthInVoxels, mCurrentHeightInVoxels,
			mCurrentDepthInVoxels;

	private final int[] mDownShape = new int[]
	{ 64, 64, 64 };

	private int mDownSize;

	private final int NDownSample = 3;
	volatile private double mSigma = 0;

	@Override
	public String getName()
	{
		return "opencl_tenengrad";
	}

	public void setSigma(final double pSigma)
	{
		System.out.println("setting sigma to " + pSigma);
		mSigma = pSigma;
	}

	public void ensureOpenCLInitialized()
	{
		if (mKernelDownsample == null)
		{
			mKernelDownsample = getDevice().compileKernel(OpenCLTenengrad.class.getResource("kernels/tenengrad.cl"),
																										"downsample");

			mKernelDiff = getDevice().compileKernel(OpenCLTenengrad.class.getResource("kernels/tenengrad.cl"),
																							"convolve_diff");

			mKernelSmooth = getDevice().compileKernel(OpenCLTenengrad.class.getResource("kernels/tenengrad.cl"),
																								"convolve_smooth");
			mKernelSum = getDevice().compileKernel(	OpenCLTenengrad.class.getResource("kernels/tenengrad.cl"),
																							"sum");

			mKernelBlur = getDevice().compileKernel(OpenCLTenengrad.class.getResource("kernels/tenengrad.cl"),
																							"blur");

			mKernelCopy = getDevice().compileKernel(OpenCLTenengrad.class.getResource("kernels/tenengrad.cl"),
																							"copy");

			setSigma(0.);
		}

	}

	public void initBuffers(long pWidthInVoxels,
													long pHeightInVoxels,
													long pDepthInVoxels)
	{
		mCurrentWidthInVoxels = pWidthInVoxels;
		mCurrentHeightInVoxels = pHeightInVoxels;
		mCurrentDepthInVoxels = pDepthInVoxels;

		// the downsampled volume shape

		mDownShape[0] = (int) Math.ceil(1. * pWidthInVoxels / NDownSample);
		mDownShape[1] = (int) Math.ceil(1. * pHeightInVoxels
																		/ NDownSample);
		mDownShape[2] = (int) Math.ceil(1. * pDepthInVoxels / NDownSample);

		System.out.println("downsampled shape: " + Arrays.toString(mDownShape));

		mDownSize = mDownShape[0] * mDownShape[1] * mDownShape[2];

		// Release previosuly allocated buffer of wrong size...
		if (mBufDownSampled != null)
		{
			mBufDownSampled.release();
			mBufGx.release();
			mBufGy.release();
			mBufGz.release();
			mBufRes.release();
			mBufScratch.release();
		}

		// the buffer for the downsampled volume
		mBufDownSampled = getDevice().createOutputFloatBuffer(mDownSize);

		// the buffers for the sobel responses
		mBufGx = getDevice().createOutputFloatBuffer(mDownSize);
		mBufGy = getDevice().createOutputFloatBuffer(mDownSize);
		mBufGz = getDevice().createOutputFloatBuffer(mDownSize);
		mBufRes = getDevice().createOutputFloatBuffer(mDownSize);
		mBufScratch = getDevice().createOutputFloatBuffer(mDownSize);

	}

	// the finite difference step (out of place)
	private void diff_step(	CLBuffer<Float> pBufIn,
													CLBuffer<Float> pBufOut,
													int flag)
	{
		// flag is 1 (in x direction), 2 (y) or 4 (z)

		mKernelDiff.setArgs(pBufIn,
												pBufOut,
												mDownShape[0],
												mDownShape[1],
												mDownShape[2],
												flag);

		getDevice().run(mKernelDiff,
										mDownShape[0],
										mDownShape[1],
										mDownShape[2]);

	}

	// the smoothing step (done in place)
	private void smooth_step(	CLBuffer<Float> pBufIn,
														CLBuffer<Float> pBufOut,
														int flag)
	{
		// flag is 1 (in x direction), 2 (y) or 4 (z)

		mKernelSmooth.setArgs(pBufIn,
													pBufOut,
													mDownShape[0],
													mDownShape[1],
													mDownShape[2],
													flag);

		getDevice().run(mKernelSmooth,
										mDownShape[0],
										mDownShape[1],
										mDownShape[2]);

	}

	// copy buffer
	private void copy_step(	CLBuffer<Float> pBufIn,
													CLBuffer<Float> pBufOut)
	{

		mKernelCopy.setArgs(pBufIn, pBufOut, mDownSize);

		getDevice().run(mKernelCopy, mDownSize);

	}

	// the blurring step (done in place)
	private void blur_step(	CLBuffer<Float> pBufIn,
													CLBuffer<Float> pBufOut,
													int flag)
	{
		// flag is 1 (in x direction), 2 (y) or 4 (z)

		mKernelBlur.setArgs(pBufIn,
												pBufOut,
												(float) mSigma,
												mDownShape[0],
												mDownShape[1],
												mDownShape[2],
												flag);

		getDevice().run(mKernelBlur,
										mDownShape[0],
										mDownShape[1],
										mDownShape[2]);

	}

	private void downsample()
	{
		mKernelDownsample.setArgs(getVolumeBuffers()[0],
															mBufDownSampled,
															mDownShape[0],
															mDownShape[1],
															mDownShape[2],
															NDownSample);

		getDevice().run(mKernelDownsample,
										mDownShape[0],
										mDownShape[1],
										mDownShape[2]);

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

		if (mBufDownSampled == null || pWidthInVoxels != mCurrentWidthInVoxels
				|| pHeightInVoxels != mCurrentHeightInVoxels
				|| pDepthInVoxels != mCurrentDepthInVoxels)
		{
			// System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);
		}

		// downsample
		downsample();

		if (mSigma > 0)
		{
			System.out.println("blurring with sigma = " + mSigma);
			blur_step(mBufDownSampled, mBufScratch, 1);
			blur_step(mBufScratch, mBufDownSampled, 2);
			blur_step(mBufDownSampled, mBufScratch, 4);
			copy_step(mBufScratch, mBufDownSampled);
		}
		// convolve with the sobels

		// Gx
		diff_step(mBufDownSampled, mBufScratch, 1);
		smooth_step(mBufScratch, mBufGx, 2);
		smooth_step(mBufGx, mBufScratch, 4);
		copy_step(mBufScratch, mBufGx);

		// Gy
		diff_step(mBufDownSampled, mBufScratch, 2);
		smooth_step(mBufScratch, mBufGz, 1);
		smooth_step(mBufGz, mBufScratch, 4);
		copy_step(mBufScratch, mBufGz);

		// Gz
		diff_step(mBufDownSampled, mBufScratch, 4);
		smooth_step(mBufScratch, mBufGz, 2);
		smooth_step(mBufGz, mBufScratch, 1);
		copy_step(mBufScratch, mBufGz);

		// adding all up

		mKernelSum.setArgs(mBufGx, mBufGy, mBufGz, mBufScratch, mDownSize);
		getDevice().run(mKernelSum, mDownSize);

		final FloatBuffer out = getDevice().readFloatBuffer(mBufScratch);

		double meanValue = 0;
		for (int i = 0; i < out.capacity(); i++)
			meanValue += out.get(i);

		meanValue *= 1. / out.capacity();

		// FloatBuffer out = getDevice().readFloatBuffer(mBufRes);
		// final int N = mDownShape[0];
		//
		// for (int i = 0; i < 4; i++)
		// {
		// System.out.println(out.get(10 + (10 + i) * N + 10 * N * N));
		// }

		// System.out.println("OUTPUT:     " + meanValue);

		// meanValue = out.get(100);

		// meanValue = out.get(16 + 16 * 42 + 16 * 42 * 42);

		System.out.format("Tenengrad measure = %g \n", meanValue);

		notifyListenersOfResult(meanValue);

	}
}
