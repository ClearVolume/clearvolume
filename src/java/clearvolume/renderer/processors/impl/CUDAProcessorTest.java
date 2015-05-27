package clearvolume.renderer.processors.impl;

import static jcuda.driver.JCudaDriver.CU_TRSF_NORMALIZED_COORDINATES;

import java.io.File;
import java.io.IOException;

import jcuda.driver.CUaddress_mode;
import jcuda.driver.CUfilter_mode;
import clearcuda.CudaArray;
import clearcuda.CudaCompiler;
import clearcuda.CudaFunction;
import clearcuda.CudaModule;
import clearcuda.CudaTextureReference;
import clearvolume.renderer.processors.CUDAProcessor;

/**
 * 
 *
 * @author Loic Royer (2015)
 *
 */
public class CUDAProcessorTest extends CUDAProcessor<Double>
{
	private static final int cBlockSize = 32;

	private CudaFunction mSumFunction;

	private CudaTextureReference mVolumeDataCudaTexture;

	@Override
	public String getName()
	{
		return "cudatest";
	}

	public CUDAProcessorTest() throws IOException
	{
		super();

	}

	private void ensureCudaInitialized()
	{
		if (mSumFunction == null)
			try
			{
				final CudaCompiler lCudaCompiler = new CudaCompiler(getDevice(),
																														this.getClass()
																																.getSimpleName());

				final File lCUFile = lCudaCompiler.addFile(	this.getClass(),
																										"kernels/test.cu",
																										true);

				lCudaCompiler.addFiles(	CudaCompiler.class,
																true,
																"includes/helper_cuda.h",
																"includes/helper_math.h",
																"includes/helper_string.h");

				final File lPTXFile = lCudaCompiler.compile(lCUFile);

				final CudaModule lCudaModule = CudaModule.moduleFromPTX(lPTXFile);

				mVolumeDataCudaTexture = lCudaModule.getTexture("tex");
				mVolumeDataCudaTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
				mVolumeDataCudaTexture.setAddressMode(0,
																							CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
				mVolumeDataCudaTexture.setAddressMode(1,
																							CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
				mVolumeDataCudaTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);

				mSumFunction = lCudaModule.getFunction("test");
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
	}

	@Override
	public void applyToArray(CudaArray pCudaArray)
	{
		ensureCudaInitialized();
		mVolumeDataCudaTexture.setTo(pCudaArray);
	}

	@Override
	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels)
	{
		ensureCudaInitialized();
		mSumFunction.setGridDim((int) iDivUp(pWidthInVoxels, cBlockSize),
														(int) iDivUp(pHeightInVoxels, cBlockSize),
														1);// (int) iDivUp(pDepthInVoxels, cBlockSize));
		mSumFunction.setBlockDim(cBlockSize, cBlockSize, 1);
		mSumFunction.launch();
		getContext().synchronize();

		final double lThisValueShouldMakeSenseSomehow = 0;

		notifyListenersOfResult(lThisValueShouldMakeSenseSomehow);
	}

}
