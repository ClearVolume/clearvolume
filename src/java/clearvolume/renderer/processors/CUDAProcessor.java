package clearvolume.renderer.processors;

import clearcuda.CudaArray;
import clearcuda.CudaContext;
import clearcuda.CudaDevice;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;

public abstract class CUDAProcessor<R> extends ProcessorBase<R>	implements
																																Processor<R>
{

	@Override
	public boolean isCompatibleRenderer(Class<?> pRendererClass)
	{
		return pRendererClass == JCudaClearVolumeRenderer.class;
	}

	public abstract void setDeviceAndContext(	CudaDevice pCudaDevice,
																						CudaContext pCudaContext);

	public abstract void applyToArray(CudaArray pCudaArray);

	@Override
	public abstract void process(	int pRenderLayerIndex,
																long pWidthInVoxels,
																long pHeightInVoxels,
																long pDepthInVoxels);




}
