package clearvolume.renderer.processors.impl;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDenoise extends OpenCLProcessor<Void> {

	private CLKernel mKernelBilateral;
	private CLKernel mKernelCopyBufToImg;

	private float sigSpace, sigValue;
	private int blockSize;

	private CLBuffer<Float> mBufScratch;

	@Override
	public String getName() {
		return "opencl_denosie_bilateral";
	}

	public void setParams(final int pBlockSize, final float pSigmaSpace,
			final float pSigmaValue) {

		sigSpace = pSigmaSpace;
		sigValue = pSigmaValue;
		blockSize = pBlockSize;

	}

	public void ensureOpenCLInitialized() {
		if (mKernelBilateral == null) {
			mKernelBilateral = getDevice()
					.compileKernel(
							OpenCLDenoise.class
									.getResource("kernels/denoise_bilat.cl"),
							"bilat");

			mKernelCopyBufToImg = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"copyBufToImg");

			setParams(2, 1.f, 1.f);
		}

	}

	public void initBuffers(final long Nx, final long Ny, final long Nz) {

		long bufSize = Nx * Ny * Nz;

		OpenCLDevice mDev = getDevice();
		mBufScratch = mDev.createOutputFloatBuffer(bufSize);

	}

	@Override
	public void process(int pRenderLayerIndex, long pWidthInVoxels,
			long pHeightInVoxels, long pDepthInVoxels) {
		if (!isActive())
			return;

		long start = System.nanoTime();

		ensureOpenCLInitialized();

		if (mBufScratch == null) {
			System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);
		}

		// bilateral filtering
		mKernelBilateral.setArgs(getVolumeBuffers()[pRenderLayerIndex],
				mBufScratch, (int) blockSize, (float) sigSpace,
				(float) sigValue);
		getDevice().run(mKernelBilateral, (int) pWidthInVoxels,
				(int) pHeightInVoxels, (int) pDepthInVoxels);

		// copy back
		mKernelCopyBufToImg.setArgs(mBufScratch,
				getVolumeBuffers()[pRenderLayerIndex]);
		getDevice().run(mKernelCopyBufToImg, (int) pWidthInVoxels,
				(int) pHeightInVoxels, (int) pDepthInVoxels);

		System.out
				.printf("denoising with blockSize, sigSpace, sigValue = %d,%.3f,%.3f\n",
						blockSize, sigSpace, sigValue);
		// notifyListenersOfResult(new Void);

	}

}
