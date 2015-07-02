package clearvolume.renderer.processors.impl;

import javax.swing.JPanel;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.panels.HasGUIPanel;
import clearvolume.renderer.processors.OpenCLProcessor;
import clearvolume.renderer.processors.impl.panels.DenoisePanel;

import com.jogamp.newt.event.KeyEvent;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDenoise extends OpenCLProcessor<Boolean>	implements
																														HasGUIPanel
{

	public enum DenoiseAlgorithm
	{
		BilateralFiltering, LocalMeans
	}

	private DenoiseAlgorithm mDenoiseAlgorithm = DenoiseAlgorithm.LocalMeans;

	private CLKernel mKernelBilateral;
	private CLKernel mKernelNLM_dist;
	private CLKernel mKernelNLM_convolve;
	private CLKernel mKernelNLM_comp_plus;
	private CLKernel mKernelNLM_comp_minus;
	private CLKernel mKernelNLM_assemble;
	private CLKernel mKernelNLM_Simple;
	private CLKernel mKernelNLM_startup;
	private CLKernel mKernelCopyBufToImg;

	private volatile int mBlockSize = 1;
	private volatile float mSigma = .1f;
	private volatile int mNLM_BlockSearchSize = 1; // Search size
	private volatile float mBF_SigmaSpace = 1.5f;



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

	public DenoiseAlgorithm getDenoiseAlgorithm()
	{
		return mDenoiseAlgorithm;
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


	public void setSearchSize(final int pSearchSize)
	{
		mNLM_BlockSearchSize = pSearchSize;
	}

	public int getSearchSize()
	{
		return mNLM_BlockSearchSize;
	}

	public void setBlockSize(final int pBlockSize)
	{
		mBlockSize = pBlockSize;
	}

	public int getBlockSize()
	{
		return mBlockSize;
	}

	public void setSigmaSpace(final float pSigmaSpace)
	{
		mBF_SigmaSpace = pSigmaSpace;
	}

	public float getSigmaSpace()
	{
		return mBF_SigmaSpace;
	}

	public void setSigma(final float pSigma)
	{
		mSigma = pSigma;
	}

	public float getSigma()
	{
		return mSigma;
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

			mKernelNLM_startup = getDevice().compileKernel(	OpenCLDenoise.class.getResource("kernels/denoise_nlm_fast.cl"),
																											"startup");

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

	public void initBuffersBF(final long Nx,
														final long Ny,
														final long Nz)
	{
		final long lLength = Nx * Ny * Nz;
		final OpenCLDevice lDev = getDevice();

		if (mBufScratch == null || mBufScratch.getElementCount() != lLength)
		{
			mBufScratch = lDev.createOutputFloatBuffer(lLength);
			mBufScratch2 = lDev.createOutputFloatBuffer(lLength);
		}

	}

	public void initBuffersNLM(	final long Nx,
															final long Ny,
															final long Nz)
	{
		final long lLength = Nx * Ny * Nz;
		final OpenCLDevice lDev = getDevice();
		
		initBuffersBF(Nx, Ny, Nz);

		if (mBuf_NLM_acc == null || mBuf_NLM_acc.getElementCount() != lLength)
		{
			mBuf_NLM_acc = lDev.createOutputFloatBuffer(lLength);
			mBuf_NLM_weight = lDev.createOutputFloatBuffer(lLength);
			mBuf_NLM_dist = lDev.createOutputFloatBuffer(lLength);
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
		case BilateralFiltering:
			process_bilateral(pRenderLayerIndex,
												pWidthInVoxels,
												pHeightInVoxels,
												pDepthInVoxels);
			break;
		case LocalMeans:
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
			initBuffersBF(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

			// bilateral filtering
			mKernelBilateral.setArgs(	getVolumeBuffers()[pRenderLayerIndex],
																mBufScratch,
																mBlockSize,
																mBF_SigmaSpace,
																mSigma);
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
			 * "denoising with mBF_BlockSize, sigSpace, mBF_SigmaValue = %d,%.3f,%.3f\n"
			 * , mBF_BlockSize, sigSpace, mBF_SigmaValue);/*
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
			initBuffersNLM(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

			// set all to zero
			mKernelNLM_startup.setArgs(	mBuf_NLM_acc,
 mBuf_NLM_weight);

			getDevice().run(mKernelNLM_startup,
											(int) pWidthInVoxels,
											(int) pHeightInVoxels,
											(int) pDepthInVoxels);

			for (int dx = 0; dx < mNLM_BlockSearchSize + 1; dx++)
				for (int dy = -mNLM_BlockSearchSize; dy < mNLM_BlockSearchSize + 1; dy++)
					for (int dz = -mNLM_BlockSearchSize; dz < mNLM_BlockSearchSize + 1; dz++)
					{

						mKernelNLM_dist.setArgs(getVolumeBuffers()[pRenderLayerIndex],
																		mBuf_NLM_dist,
																		dx,
																		dy,
																		dz,
																		(int) (2 * mBlockSize + 1) * (2 * mBlockSize + 1));

						getDevice().run(mKernelNLM_dist,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_convolve.setArgs(mBuf_NLM_dist,
																				mBufScratch,
																				(int) mBlockSize,
																				1);
						System.out.println(mBlockSize);

						getDevice().run(mKernelNLM_convolve,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_convolve.setArgs(mBufScratch,
																				mBufScratch2,
																				(int) mBlockSize,
																				2);
						getDevice().run(mKernelNLM_convolve,
														(int) pWidthInVoxels,
														(int) pHeightInVoxels,
														(int) pDepthInVoxels);

						mKernelNLM_convolve.setArgs(mBufScratch2,
																				mBufScratch,
																				(int) mBlockSize,
																				4);
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
																					mSigma);

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
																						(mSigma));
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
			 * "denoising with mBF_BlockSize, sigSpace, mBF_SigmaValue = %d,%.3f,%.3f\n"
			 * , mBF_BlockSize, sigSpace, mBF_SigmaValue);/*
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
			initBuffersNLM(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

			mKernelNLM_Simple.setArgs(getVolumeBuffers()[pRenderLayerIndex],
																mBufScratch,
																mBlockSize,
																mNLM_BlockSearchSize,
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
			 * "denoising with mBF_BlockSize, sigSpace, mBF_SigmaValue = %d,%.3f,%.3f\n"
			 * , mBF_BlockSize, sigSpace, mBF_SigmaValue);/*
			 */
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			notifyListenersOfResult(new Boolean(false));
		}

		notifyListenersOfResult(new Boolean(true));

	}

	@Override
	public JPanel getPanel()
	{
		final DenoisePanel lDenoisePanel = new DenoisePanel(this);

		return lDenoisePanel;
	}

}
