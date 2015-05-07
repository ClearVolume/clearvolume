package clearvolume.renderer.processors.impl;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDeconv extends OpenCLProcessor<Void> {

	private CLKernel mKernelBlur;
	private CLKernel mKernelMul;
	private CLKernel mKernelCopyImgToBuf;
	private CLKernel mKernelCopyBufToImg;
	private CLKernel mKernelDiv;

	private CLKernel mKernelTest;

	CLBuffer<Float> mInput;
	CLBuffer<Float> mTmp;
	CLBuffer<Float> mTmp2;
	CLBuffer<Float> mScratch;
	CLBuffer<Float> mOut;

	private float sigX, sigY, sigZ;
	private int NhX, NhY, NhZ;

	private static int Niter = 5;

	@Override
	public String getName() {
		return "opencl_deconv_rl";
	}

	public void setSigmas(final float pSigmaX, final float pSigmaY,
			final float pSigmaZ) {

		sigX = pSigmaX;
		sigY = pSigmaY;
		sigZ = pSigmaZ;

		NhX = (int) (4 * sigX + 1);
		NhY = (int) (4 * sigY + 1);
		NhZ = (int) (4 * sigZ + 1);

	}

	public void ensureOpenCLInitialized() {
		if (mKernelBlur == null) {
			mKernelBlur = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"blur_sep");

			mKernelCopyImgToBuf = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"copyImgToBuf");

			mKernelCopyBufToImg = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"copyBufToImg");

			mKernelMul = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"multiply");

			mKernelDiv = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"divide");

			mKernelTest = getDevice()
					.compileKernel(
							OpenCLDeconv.class.getResource("kernels/deconv.cl"),
							"test");

			setSigmas(1.f, 1.f, 1.f);
		}

	}

	public void initBuffers(final long Nx, final long Ny, final long Nz) {

		long bufSize = Nx * Ny * Nz;

		OpenCLDevice mDev = getDevice();
		mInput = mDev.createInputFloatBuffer(bufSize);
		mTmp = mDev.createOutputFloatBuffer(bufSize);
		mTmp2 = mDev.createOutputFloatBuffer(bufSize);
		mScratch = mDev.createOutputFloatBuffer(bufSize);
		mOut = mDev.createOutputFloatBuffer(bufSize);

	}

	@Override
	public void process(int pRenderLayerIndex, long pWidthInVoxels,
			long pHeightInVoxels, long pDepthInVoxels) {
		if (!isActive())
			return;

		long start = System.nanoTime();

		ensureOpenCLInitialized();

		if (mInput == null) {
			System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);
		}

		copyImgToBuf(pRenderLayerIndex, mInput, pWidthInVoxels,
				pHeightInVoxels, pDepthInVoxels);

		copyImgToBuf(pRenderLayerIndex, mOut, pWidthInVoxels, pHeightInVoxels,
				pDepthInVoxels);

		for (int i = 0; i < Niter; i++) {

			blur(mOut, mTmp, mScratch, pWidthInVoxels, pHeightInVoxels,
					pDepthInVoxels);
			divide(mInput, mTmp, mTmp2, pWidthInVoxels, pHeightInVoxels,
					pDepthInVoxels);

			blur(mTmp2, mTmp, mScratch, pWidthInVoxels, pHeightInVoxels,
					pDepthInVoxels);

			multiply(mTmp, mOut, pWidthInVoxels, pHeightInVoxels,
					pDepthInVoxels);

		}
		copyBufToImg(mOut, pRenderLayerIndex, pWidthInVoxels, pHeightInVoxels,
				pDepthInVoxels);

		System.out.println("deconv");
		// notifyListenersOfResult(new Void);

	}

	private void divide(CLBuffer<Float> pOut, CLBuffer<Float> pTmp,
			CLBuffer<Float> pTmp2, long Nx, long Ny, long Nz) {

		mKernelDiv.setArgs(pOut, pTmp, pTmp2);
		getDevice().run(mKernelDiv, (int) (Nx * Ny * Nz), 1, 1);

	}

	private void multiply(CLBuffer<Float> pOut, CLBuffer<Float> pTmp, long Nx,
			long Ny, long Nz) {

		mKernelMul.setArgs(pOut, pTmp);
		getDevice().run(mKernelMul, (int) (Nx * Ny * Nz), 1, 1);

	}

	private void copyImgToBuf(int pRenderLayerIndex, CLBuffer<Float> bufIn,
			final long Nx, final long Ny, final long Nz) {

		mKernelCopyImgToBuf.setArgs(getVolumeBuffers()[0], bufIn);
		getDevice().run(mKernelCopyImgToBuf, (int) Nx, (int) Ny, (int) Nz);

	}

	private void copyBufToImg(CLBuffer<Float> bufIn, int pRenderLayerIndex,
			final long Nx, final long Ny, final long Nz) {

		mKernelCopyBufToImg.setArgs(bufIn, getVolumeBuffers()[0]);
		getDevice().run(mKernelCopyBufToImg, (int) Nx, (int) Ny, (int) Nz);

	}

	private void blur(CLBuffer<Float> bufIn, CLBuffer<Float> bufOut,
			CLBuffer<Float> bufScratch, final long Nx, final long Ny,
			final long Nz) {

		mKernelBlur.setArgs(bufIn, bufOut, (float) sigX, (int) (NhX), (int) 1);
		getDevice().run(mKernelBlur, (int) Nx, (int) Ny, (int) Nz);

		mKernelBlur.setArgs(bufOut, bufScratch, (float) sigY, (int) (NhY),
				(int) 2);
		getDevice().run(mKernelBlur, (int) Nx, (int) Ny, (int) Nz);

		mKernelBlur.setArgs(bufScratch, bufOut, (float) sigZ, (int) (NhZ),
				(int) 4);
		getDevice().run(mKernelBlur, (int) Nx, (int) Ny, (int) Nz);

	}

}
