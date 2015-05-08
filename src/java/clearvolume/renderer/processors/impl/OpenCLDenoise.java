package clearvolume.renderer.processors.impl;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.processors.OpenCLProcessor;

import com.jogamp.newt.event.KeyEvent;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLDenoise extends OpenCLProcessor<Boolean>
{

	private CLKernel mKernelBilateral;
	private CLKernel mKernelCopyBufToImg;

	private float sigSpace, sigValue;
	private int blockSize;

	private CLBuffer<Float> mBufScratch;

	public OpenCLDenoise()
	{
		super();
		setActive(false);
	}

	@Override
	public String getName()
	{
		return "opencl_denoise_bilateral";
	}

	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_D;
	}

	public void setParams(final int pBlockSize,
												final float pSigmaSpace,
												final float pSigmaValue)
	{
		sigSpace = pSigmaSpace;
		sigValue = pSigmaValue;
		blockSize = pBlockSize;
	}

	public void ensureOpenCLInitialized()
	{
		if (mKernelBilateral == null)
		{
			mKernelBilateral = getDevice().compileKernel(	OpenCLDenoise.class.getResource("kernels/denoise_bilat.cl"),
																										"bilat");

			mKernelCopyBufToImg = getDevice().compileKernel(OpenCLDeconv.class.getResource("kernels/deconv.cl"),
																											"copyBufToImg");

			setParams(2, 1.f, 1.f);
		}

	}

	public void initBuffers(final long Nx, final long Ny, final long Nz)
	{
		final long lLength = Nx * Ny * Nz;

		if (mBufScratch == null || mBufScratch.getElementCount() != lLength)
		{
			final OpenCLDevice mDev = getDevice();
			mBufScratch = mDev.createOutputFloatBuffer(lLength);
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

		// final long start = System.nanoTime();

		ensureOpenCLInitialized();

		// System.out.println("setting up buffers");
		initBuffers(pWidthInVoxels, pHeightInVoxels, pDepthInVoxels);

		// bilateral filtering
		mKernelBilateral.setArgs(	getVolumeBuffers()[pRenderLayerIndex],
															mBufScratch,
															blockSize,
															sigSpace,
															sigValue);
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

		/*System.out.printf("denoising with blockSize, sigSpace, sigValue = %d,%.3f,%.3f\n",
											blockSize,
											sigSpace,
											sigValue);/**/
		
		notifyListenersOfResult(new Boolean(true));

	}

}
