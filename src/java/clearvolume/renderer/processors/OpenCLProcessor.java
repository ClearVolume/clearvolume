package clearvolume.renderer.processors;

import clearvolume.renderer.opencl.OpenCLVolumeRenderer;

public abstract class OpenCLProcessor<R> extends ProcessorBase<R>	implements
																																	Processor<R>
{

	@Override
	public boolean isCompatibleRenderer(Class<?> pRendererClass)
	{
		return pRendererClass == OpenCLVolumeRenderer.class;
	}

	@Override
	public abstract void process(	int pRenderLayerIndex,
																long pWidthInVoxels,
																long pHeightInVoxels,
																long pDepthInVoxels);

}
