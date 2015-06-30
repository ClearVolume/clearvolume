package clearvolume.renderer.processors.impl;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.processors.OpenCLProcessor;

import com.jogamp.newt.event.KeyEvent;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDenoise extends OpenCLProcessor<Boolean>
{

	public enum DenoiseAlgorithm
	{
		BILATERAL, NLM
	}

	private DenoiseAlgorithm mDenoiseAlgorithm = DenoiseAlgorithm.NLM;

	private CLKernel mKernelBilateral;

	private CLKernel mKernelNLM_dist;
	private CLKernel mKernelNLM_convolve;
	private CLKernel mKernelNLM_comp_plus;
	private CLKernel mKernelNLM_comp_minus;
	private CLKernel mKernelNLM_assemble;

	private CLKernel mKernelNLM_Simple;

	private final int NLM_BS = 1; // Search size
	private final int NLM_FS = 1; // Patch size
	private final float NLM_SIGMA = .1f;
	private CLKernel mKernelCopyBufToImg;

	private float mSigmaSpace, mSigmaValue;
	private int mBlockSize;

	private CLBuffer<Float> mBufScratch;
	private CLBuffer<Float> mBufScratch2;

	private CLBuffer<Float> mBuf_NLM_dist, mBuf_NLM_acc,
			mBuf_NLM_weight;

	public OpenCLDenoise()
	{
		super();
		setActive(false);
	}

	public void setDenoiseAlgorithm(final DenoiseAlgorithm pDenoiseAlgorithm)
	{
		mDenoiseAlgorithm = pDenoiseAlgorithm;
	}

	@Override
	public String getName()
	{
		return "opencl_denoise";
	}

	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_D;
	}

	public void setBlockSize(final int pBlockSize)
	{
		mBlockSize = pBlockSize;
	}

	public void setSigmaSpace(final float pSigmaSpace)
	{
		mSigmaSpace = pSigmaSpace;
	}

	public void setSigmaValue(final float pSigmaValue)
	{
		mSigmaValue = pSigmaValue;
	}

	public void ensureOpenCLInitialized()
	{
		if (mKernelBilateral == null)
		{
			mKernelBilateral = getDevice().compileKernel(	OpenCLDenoise.class.getResource("kernels/denoise_bilat.cl"),
																										"bilat");

			mKernelCopyBufToImg = getDevice().compileKernel(OpenCLDeconvolutionLR.class.getResource("kernels/deconv.cl"),
																											"copyBufToImg");

			mKernelNLM_dist = getDevice().compileKernel(OpenCLDenoise.class.getResource("kernels/denoise_nlm_fast.cl"),
																									"dist");

			mKernelNLM_convolve = getDevice().compileKernel(OpenCLDenoise.class.getResource("kernels/denoise_nlm_fast.cl"),
																											"convolve");

			mKernelNLM_comp_plus = getDevice().compileKernel(	OpenCLDenoise.class.getResource("kernels/denoise_nlm_fast.cl"),
																												"computePlus");

			mKernelNLM_comp_minus = getDevice().compileKernel(OpenCLDenoise.class.getResource("kernels/denoise_nlm_fast.cl"),
																												"computeMinus");

			mKernelNLM_assemble = getDevice().compileKernel(OpenCLDenoise.class.getResource("kernels/denoise_nlm_fast.cl"),
																											"assemble");

			mKernelNLM_Simple = getDevice().compileKernel(OpenCLDenoise.class.getResource("kernels/denoise_nlm.cl"),
																										"nlm");

		}

	}

	public void initBuffers(final long Nx, final long Ny, final long Nz)
	{
		final long lLength = Nx * Ny * Nz;

		if (mBufScratch == null || mBufScratch.getElementCount() != lLength)
		{
			final OpenCLDevice mDev = getDevice();
			mBufScratch = mDev.createOutputFloatBuffer(lLength);
			mBufScratch2 = mDev.createOutputFloatBuffer(lLength);

			mBuf_NLM_acc = mDev.createOutputFloatBuffer(lLength);
			mBuf_NLM_weight = mDev.createOutputFloatBuffer(lLength);
			mBuf_NLM_dist = mDev.createOutputFloatBuffer(lLength);

		}
	}

	@Override
	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels)
	{
		if (!isActive())
			return;

		switch (mDenoiseAlgorithm)
		{
		case BILATERAL:
			process_bilateral(pRenderLayerIndex,
												pWidthInVoxels,
												pHeightInVoxels,
												pDepthInVoxels);
			break;
		case NLM:
			process_nlm_fast(	pRenderLayerIndex,
												pWidthInVoxels,
												pHeightInVoxels,
												pDepthInVoxels);

			break;

		}

	}

	private void process_bilateral(	int pRenderLayerIndex,
																	long pWidthInVoxels,
																	long pHeightInVoxels,
																	long pDepthInVoxels)
	{

		try
		{
			// final long start = System.nanoTime();

			ensureOpenCLInitialized();

			// System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

			// bilateral filtering
			mKernelBilateral.setArgs(	getVolumeBuffers()[pRenderLayerIndex],
																mBufScratch,
																mBlockSize,
																mSigmaSpace,
																mSigmaValue);
			getDevice().run(mKernelBilateral,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			// copy back
			mKernelCopyBufToImg.setArgs(mBufScratch,
																	getVolumeBuffers()[pRenderLayerIndex]);
			getDevice().run(mKernelCopyBufToImg,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			/*
			 * System.out.printf(
			 * "denoising with mBlockSize, sigSpace, mSigmaValue = %d,%.3f,%.3f\n"
			 * , mBlockSize, sigSpace, mSigmaValue);/*
			 */
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			notifyListenersOfResult(new Boolean(false));
		}

		notifyListenersOfResult(new Boolean(true));

	}

	private void process_nlm_fast(int pRenderLayerIndex,
																long pWidthInVoxels,
																long pHeightInVoxels,
																long pDepthInVoxels)
	{

		try
		{
			// final long start = System.nanoTime();

			ensureOpenCLInitialized();

			// System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

			for (int dx = 0; dx < NLM_BS + 1; dx++)
				for (int dy = -NLM_BS; dy < NLM_BS + 1; dy++)
					for (int dz = -NLM_BS; dz < NLM_BS + 1; dz++)
					{

						mKernelNLM_dist.setArgs(getVolumeBuffers()[pRenderLayerIndex],
																		mBuf_NLM_dist,
																		dx,
																		dy,
																		dz);
						getDevice().run(mKernelNLM_dist,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_convolve.setArgs(mBuf_NLM_dist, mBufScratch, 1);
						getDevice().run(mKernelNLM_convolve,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_convolve.setArgs(mBufScratch, mBufScratch2, 2);
						getDevice().run(mKernelNLM_convolve,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_convolve.setArgs(mBufScratch2, mBufScratch, 4);
						getDevice().run(mKernelNLM_convolve,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_comp_plus.setArgs(	getVolumeBuffers()[pRenderLayerIndex],
																					mBufScratch,
																					mBuf_NLM_acc,
																					mBuf_NLM_weight,
																					dx,
																					dy,
																					dz,
																					(NLM_SIGMA));
						getDevice().run(mKernelNLM_comp_plus,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						if (dx != 0 || dy != 0 || dz != 0)
						{
							mKernelNLM_comp_minus.setArgs(getVolumeBuffers()[pRenderLayerIndex],
																						mBufScratch,
																						mBuf_NLM_acc,
																						mBuf_NLM_weight,
																						dx,
																						dy,
																						dz,
																						(NLM_SIGMA));
							getDevice().run(mKernelNLM_comp_minus,
															(int) pWidthInVoxels,
															(int) pHeightInVoxels,
															(int) pDepthInVoxels);
						}

					}
			// assemble
			mKernelNLM_assemble.setArgs(mBuf_NLM_acc,
																	mBuf_NLM_weight,
																	mBufScratch);
			getDevice().run(mKernelNLM_assemble,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			// copy back
			mKernelCopyBufToImg.setArgs(mBufScratch,
																	getVolumeBuffers()[pRenderLayerIndex]);
			getDevice().run(mKernelCopyBufToImg,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			/*
			 * System.out.printf(
			 * "denoising with mBlockSize, sigSpace, mSigmaValue = %d,%.3f,%.3f\n"
			 * , mBlockSize, sigSpace, mSigmaValue);/*
			 */
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			notifyListenersOfResult(new Boolean(false));
		}

		notifyListenersOfResult(new Boolean(true));

	}

	private void process_nlm(	int pRenderLayerIndex,
														long pWidthInVoxels,
														long pHeightInVoxels,
														long pDepthInVoxels)
	{

		try
		{
			// final long start = System.nanoTime();

			ensureOpenCLInitialized();

			// System.out.println("setting up buffers");
			initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

			mKernelNLM_Simple.setArgs(getVolumeBuffers()[pRenderLayerIndex],
																mBufScratch,
																NLM_FS,
																NLM_BS,
																(float) (.1));
			getDevice().run(mKernelNLM_Simple,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			// copy back
			mKernelCopyBufToImg.setArgs(mBufScratch,
																	getVolumeBuffers()[pRenderLayerIndex]);
			getDevice().run(mKernelCopyBufToImg,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			/*
			 * System.out.printf(
			 * "denoising with mBlockSize, sigSpace, mSigmaValue = %d,%.3f,%.3f\n"
			 * , mBlockSize, sigSpace, mSigmaValue);/*
			 */
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			notifyListenersOfResult(new Boolean(false));
		}

		notifyListenersOfResult(new Boolean(true));

	}
}
