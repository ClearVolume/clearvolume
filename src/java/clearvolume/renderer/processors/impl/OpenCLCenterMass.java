package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLCenterMass extends OpenCLProcessor<float[]> {

	private CLKernel mKernel;

	private CLBuffer<Float> mBufX, mBufY, mBufZ, mBufSum;

	private long mCurrentWidthInVoxels, mCurrentHeightInVoxels,
			mCurrentDepthInVoxels;

	private int mPaddedShapeX, mPaddedShapeY, mPaddedShapeZ;
	private int mLocalShapeX, mLocalShapeY, mLocalShapeZ;

	private final int mLocalSize = 8;
	private final int mDownSample = 2;

	@Override
	public String getName() {
		return "opencl_center_of_mass";
	}

	public void ensureOpenCLInitialized() {
		if (mKernel == null) {
			mKernel = getDevice()
					.compileKernel(
							OpenCLCenterMass.class
									.getResource("kernels/centermass.cl"),
							"center_of_mass_img");
		}
	}

	public void initBuffers(long pWidthInVoxels, long pHeightInVoxels,
			long pDepthInVoxels) {

		final int cutSize = mDownSample * mLocalSize;

		mPaddedShapeX = (int) (Math.ceil(1. * pWidthInVoxels / cutSize) * mLocalSize);
		mPaddedShapeY = (int) (Math.ceil(1. * pHeightInVoxels / cutSize) * mLocalSize);
		mPaddedShapeZ = (int) (Math.ceil(1. * pDepthInVoxels / cutSize) * mLocalSize);

		mLocalShapeX = mPaddedShapeX / mLocalSize;
		mLocalShapeY = mPaddedShapeY / mLocalSize;
		mLocalShapeZ = mPaddedShapeZ / mLocalSize;

		// System.out.println(mLocalShapeX);
		// System.out.println(mPaddedShapeX);

		final long lBinSize = mLocalShapeX * mLocalShapeY * mLocalShapeZ;
		// the buffer containing the counts
		mBufX = getDevice().createOutputFloatBuffer(lBinSize);
		mBufY = getDevice().createOutputFloatBuffer(lBinSize);
		mBufZ = getDevice().createOutputFloatBuffer(lBinSize);
		mBufSum = getDevice().createOutputFloatBuffer(lBinSize);

	}

	@Override
	public void process(int pRenderLayerIndex, long pWidthInVoxels,
			long pHeightInVoxels, long pDepthInVoxels) {
		if (!isActive())
			return;

		ensureOpenCLInitialized();

		if (mBufX == null || pWidthInVoxels != mCurrentWidthInVoxels
				|| pHeightInVoxels != mCurrentHeightInVoxels
				|| pDepthInVoxels != mCurrentDepthInVoxels) {
			// System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);
		}

		mKernel.setArgs(getVolumeBuffers()[0], mBufX, mBufY, mBufZ, mBufSum,
				mDownSample);

		boolean isdebug = true;

		long start = System.nanoTime();

		getDevice().run(mKernel, mPaddedShapeX, mPaddedShapeY, mPaddedShapeZ,
				mLocalSize, mLocalSize, mLocalSize);

		if (isdebug) {
			getDevice().mCLQueue.finish();
			long end = System.nanoTime();
			System.out.println("time to compute center of mass " + 1.e-6
					* (end - start) + " ms");
			// System.out.printf("time to compute center of mass: %.2f ms\n",
			// 1000000 * (start - end));
		}

		final FloatBuffer outX = getDevice().readFloatBuffer(mBufX);
		final FloatBuffer outY = getDevice().readFloatBuffer(mBufY);
		final FloatBuffer outZ = getDevice().readFloatBuffer(mBufZ);

		final FloatBuffer outSum = getDevice().readFloatBuffer(mBufSum);

		float resX = 0.f, resY = 0.f, resZ = 0.f, resSum = 0.f;

		for (int i = 0; i < outX.capacity(); i++) {
			resX += outX.get(i);
			resY += outY.get(i);
			resZ += outZ.get(i);
			resSum += outSum.get(i);
		}

		notifyListenersOfResult(new float[] { resX / resSum, resY / resSum,
				resZ / resSum, resSum });

	}
}
