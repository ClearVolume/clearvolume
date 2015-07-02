package clearvolume.renderer.processors.impl;

import javax.swing.JPanel;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.panels.HasGUIPanel;
import clearvolume.renderer.processors.OpenCLProcessor;
import clearvolume.renderer.processors.impl.panels.DeconvolvePanel;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.KeyEvent;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDeconvolutionLR extends OpenCLProcessor<Boolean>	implements
																																		HasGUIPanel
{

	private CLKernel mKernelBlur;
	private CLKernel mKernelMul;
	private CLKernel mKernelCopyImgToBuf;
	private CLKernel mKernelCopyBufToImg;
	private CLKernel mKernelDiv;

	CLBuffer<Float> mInput;
	CLBuffer<Float> mTmp;
	CLBuffer<Float> mTmp2;
	CLBuffer<Float> mScratch;
	CLBuffer<Float> mOut;

	private volatile float sigX, sigY, sigZ;
	private volatile int NhX, NhY, NhZ;
	private volatile int mNumberOfIterations = 5;

	public OpenCLDeconvolutionLR()
	{
		super();
		setActive(false);
		setSigmas(1.f, 1.f, 1.f);
	}

	@Override
	public String getName()
	{
		return "opencl_deconv_rl";
	}

	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_D;
	}

	@Override
	public int toggleKeyModifierMask()
	{
		return InputEvent.SHIFT_MASK;
	};

	public void setNumberOfIterations(int pNumberOfIterations)
	{
		mNumberOfIterations = pNumberOfIterations;
	}

	public int getNumberOfIterations()
	{
		return mNumberOfIterations;
	}

	public void setSigmas(final float pSigmaX,
												final float pSigmaY,
												final float pSigmaZ)
	{
		setSigmaX(pSigmaX);
		setSigmaY(pSigmaY);
		setSigmaZ(pSigmaZ);
	}

	public void setSigmaX(final float pSigmaX)
	{
		sigX = pSigmaX;
		NhX = (int) (4 * sigX + 1);
	}

	public float getSigmaX()
	{
		return sigX;
	}

	public void setSigmaY(final float pSigmaY)
	{
		sigY = pSigmaY;
		NhY = (int) (4 * sigY + 1);
	}

	public float getSigmaY()
	{
		return sigY;
	}

	public void setSigmaZ(final float pSigmaZ)
	{
		sigZ = pSigmaZ;
		NhZ = (int) (4 * sigZ + 1);
	}

	public float getSigmaZ()
	{
		return sigZ;
	}

	public void ensureOpenCLInitialized()
	{
		if (mKernelBlur == null)
		{
			mKernelBlur = getDevice().compileKernel(OpenCLDeconvolutionLR.class.getResource("kernels/deconv.cl"),
																							"blur_sep");

			mKernelCopyImgToBuf = getDevice().compileKernel(OpenCLDeconvolutionLR.class.getResource("kernels/deconv.cl"),
																											"copyImgToBuf");

			mKernelCopyBufToImg = getDevice().compileKernel(OpenCLDeconvolutionLR.class.getResource("kernels/deconv.cl"),
																											"copyBufToImg");

			mKernelMul = getDevice().compileKernel(	OpenCLDeconvolutionLR.class.getResource("kernels/deconv.cl"),
																							"multiply");

			mKernelDiv = getDevice().compileKernel(	OpenCLDeconvolutionLR.class.getResource("kernels/deconv.cl"),
																							"divide");

		}

	}

	public void initBuffers(final long Nx, final long Ny, final long Nz)
	{
		final long lBufferSize = Nx * Ny * Nz;
		final OpenCLDevice lDevice = getDevice();

		if (mInput == null || mInput.getElementCount() != lBufferSize)
		{
			if (mInput != null)
				mInput.release();
			if (mTmp != null)
				mTmp.release();
			if (mTmp2 != null)
				mTmp2.release();
			if (mTmp2 != null)
				mTmp2.release();
			if (mScratch != null)
				mScratch.release();
			if (mOut != null)
				mOut.release();

			mInput = lDevice.createInputFloatBuffer(lBufferSize);
			mTmp = lDevice.createOutputFloatBuffer(lBufferSize);
			mTmp2 = lDevice.createOutputFloatBuffer(lBufferSize);
			mScratch = lDevice.createOutputFloatBuffer(lBufferSize);
			mOut = lDevice.createOutputFloatBuffer(lBufferSize);
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

		try
		{
			final long start = System.nanoTime();

			ensureOpenCLInitialized();

			if (mInput == null)
			{
				System.out.println("setting up buffers");
				initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);
			}

			copyImgToBuf(	pRenderLayerIndex,
										mInput,
										pWidthInVoxels,
										pHeightInVoxels,
										pDepthInVoxels);

			copyImgToBuf(	pRenderLayerIndex,
										mOut,
										pWidthInVoxels,
										pHeightInVoxels,
										pDepthInVoxels);

			for (int i = 0; i < mNumberOfIterations; i++)
			{

				blur(	mOut,
							mTmp,
							mScratch,
							pWidthInVoxels,
							pHeightInVoxels,
							pDepthInVoxels);
				divide(	mInput,
								mTmp,
								mTmp2,
								pWidthInVoxels,
								pHeightInVoxels,
								pDepthInVoxels);

				blur(	mTmp2,
							mTmp,
							mScratch,
							pWidthInVoxels,
							pHeightInVoxels,
							pDepthInVoxels);

				multiply(	mTmp,
									mOut,
									pWidthInVoxels,
									pHeightInVoxels,
									pDepthInVoxels);

			}
			copyBufToImg(	mOut,
										pRenderLayerIndex,
										pWidthInVoxels,
										pHeightInVoxels,
										pDepthInVoxels);

			/*System.out.printf("deconv with sigX, sigY, sigZ = %.3f,%.3f,%.3f\n",
												sigX,
												sigY,
												sigZ);/**/
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			notifyListenersOfResult(new Boolean(false));
		}

		notifyListenersOfResult(new Boolean(true));
	}

	private void divide(CLBuffer<Float> pOut,
											CLBuffer<Float> pTmp,
											CLBuffer<Float> pTmp2,
											long Nx,
											long Ny,
											long Nz)
	{
		mKernelDiv.setArgs(pOut, pTmp, pTmp2);
		getDevice().run(mKernelDiv, (int) (Nx * Ny * Nz), 1, 1);
	}

	private void multiply(CLBuffer<Float> pOut,
												CLBuffer<Float> pTmp,
												long Nx,
												long Ny,
												long Nz)
	{
		mKernelMul.setArgs(pOut, pTmp);
		getDevice().run(mKernelMul, (int) (Nx * Ny * Nz), 1, 1);
	}

	private void copyImgToBuf(int pRenderLayerIndex,
														CLBuffer<Float> bufIn,
														final long Nx,
														final long Ny,
														final long Nz)
	{
		mKernelCopyImgToBuf.setArgs(getVolumeBuffers()[pRenderLayerIndex],
																bufIn);
		getDevice().run(mKernelCopyImgToBuf, (int) Nx, (int) Ny, (int) Nz);
	}

	private void copyBufToImg(CLBuffer<Float> bufIn,
														int pRenderLayerIndex,
														final long Nx,
														final long Ny,
														final long Nz)
	{
		mKernelCopyBufToImg.setArgs(bufIn,
																getVolumeBuffers()[pRenderLayerIndex]);
		getDevice().run(mKernelCopyBufToImg, (int) Nx, (int) Ny, (int) Nz);
	}

	private void blur(CLBuffer<Float> bufIn,
										CLBuffer<Float> bufOut,
										CLBuffer<Float> bufScratch,
										final long Nx,
										final long Ny,
										final long Nz)
	{
		mKernelBlur.setArgs(bufIn, bufOut, sigX, (NhX), 1);
		getDevice().run(mKernelBlur, (int) Nx, (int) Ny, (int) Nz);

		mKernelBlur.setArgs(bufOut, bufScratch, sigY, (NhY), 2);
		getDevice().run(mKernelBlur, (int) Nx, (int) Ny, (int) Nz);

		mKernelBlur.setArgs(bufScratch, bufOut, sigZ, (NhZ), 4);
		getDevice().run(mKernelBlur, (int) Nx, (int) Ny, (int) Nz);
	}

	@Override
	public JPanel getPanel()
	{
		final DeconvolvePanel lDeconvolvePanel = new DeconvolvePanel(this);

		return lDeconvolvePanel;
	}

}
