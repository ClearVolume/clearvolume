package clearvolume.renderer.processors.impl;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDeconv extends OpenCLProcessor<Void> {

	private CLKernel mKernelBlur;
	private CLKernel mKernelMul;
	private CLKernel mKernelCopy;
	private CLKernel mKernelDiv;

	CLBuffer<Float> mInput;
	CLBuffer<Float> mTmp;
	CLBuffer<Float> mTmp2;
	CLBuffer<Float> mScratch;
	CLBuffer<Float> mOut;

	private float sigX, sigY, sigZ;
	private int NhX, NhY, NhZ;

	private static int Niter = 3;

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

			mKernelCopy = getDevice()
					.compileKernel(
							OpenCLDeconv.class.getResource("kernels/deconv.cl"),
							"copy");

			mKernelMul = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"multiply");

			mKernelDiv = getDevice().compileKernel(
					OpenCLDeconv.class.getResource("kernels/deconv.cl"),
					"divide");

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

		copy(pRenderLayerIndex, pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

		// notifyListenersOfResult(new Void);

	}

	private void copy(int pRenderLayerIndex, final long Nx, final long Ny,
			final long Nz) {

		mKernelCopy.setArgs(getVolumeBuffers()[0], mInput);
		getDevice().run(mKernelCopy, (int) Nx, (int) Ny, (int) Nz);

	}

	private void blur(CLBuffer<Float> bufIn, CLBuffer<Float> bufOut,
			CLBuffer<Float> bufScratch, final int Nx, final int Ny, final int Nz) {

		mKernelBlur.setArgs(bufIn, bufOut, (float) sigX, (int) (NhX), (int) 1);
		getDevice().run(mKernelBlur, Nx, Ny, Nz);

		mKernelBlur.setArgs(bufOut, bufScratch, (float) sigY, (int) (NhY),
				(int) 2);
		getDevice().run(mKernelBlur, Nx, Ny, Nz);

		mKernelBlur.setArgs(bufScratch, bufOut, (float) sigZ, (int) (NhZ),
				(int) 4);
		getDevice().run(mKernelBlur, Nx, Ny, Nz);

	}
}
