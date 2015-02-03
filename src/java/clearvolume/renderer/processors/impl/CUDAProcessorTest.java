package clearvolume.renderer.processors.impl;

import static jcuda.driver.JCudaDriver.CU_TRSF_NORMALIZED_COORDINATES;

import java.io.File;
import java.io.IOException;

import jcuda.driver.CUaddress_mode;
import jcuda.driver.CUfilter_mode;
import clearcuda.CudaArray;
import clearcuda.CudaCompiler;
import clearcuda.CudaContext;
import clearcuda.CudaDevice;
import clearcuda.CudaFunction;
import clearcuda.CudaModule;
import clearcuda.CudaTextureReference;
import clearvolume.renderer.processors.CUDAProcessor;

public class CUDAProcessorTest extends CUDAProcessor<Double>
{
	private static final int cBlockSize = 32;

	private CudaDevice mCudaDevice;
	private CudaContext mCudaContext;
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

	@Override
	public void setDeviceAndContext(CudaDevice pCudaDevice,
																	CudaContext pCudaContext)
	{
		mCudaDevice = pCudaDevice;
		mCudaContext = pCudaContext;
	}

	private void ensureCudaInitialized()
	{
		if (mSumFunction == null)
			try
			{
				final CudaCompiler lCudaCompiler = new CudaCompiler(mCudaDevice,
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

				File lPTXFile = lCudaCompiler.compile(lCUFile);

				CudaModule lCudaModule = CudaModule.moduleFromPTX(lPTXFile);

				mVolumeDataCudaTexture = lCudaModule.getTexture("tex");
				mVolumeDataCudaTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
				mVolumeDataCudaTexture.setAddressMode(0,
																							CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
				mVolumeDataCudaTexture.setAddressMode(1,
																							CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
				mVolumeDataCudaTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);

				mSumFunction = lCudaModule.getFunction("test");
			}
			catch (IOException e)
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
		mCudaContext.synchronize();
	}

}
