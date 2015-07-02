package clearvolume.renderer.processors;

import clearcuda.CudaArray;
import clearcuda.CudaContext;
import clearcuda.CudaDevice;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;

public abstract class CUDAProcessor<R> extends ProcessorBase<R>	implements
																																ProcessorInterface<R>
{
	private CudaDevice mCudaDevice;
	private CudaContext mCudaContext;

	@Override
	public boolean isCompatibleProcessor(Class<?> pRendererClass)
	{
		return pRendererClass == JCudaClearVolumeRenderer.class;
	}

	public void setDeviceAndContext(CudaDevice pCudaDevice,
																	CudaContext pCudaContext)
	{
		mCudaDevice = pCudaDevice;
		mCudaContext = pCudaContext;
	}

	public CudaDevice getDevice()
	{
		return mCudaDevice;
	}

	public CudaContext getContext()
	{
		return mCudaContext;
	}

	public abstract void applyToArray(CudaArray pCudaArray);

	@Override
	public abstract void process(	int pRenderLayerIndex,
																long pWidthInVoxels,
																long pHeightInVoxels,
																long pDepthInVoxels);

}
