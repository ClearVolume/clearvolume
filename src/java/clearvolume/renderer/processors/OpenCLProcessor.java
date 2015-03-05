package clearvolume.renderer.processors;

import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.opencl.OpenCLVolumeRenderer;

public abstract class OpenCLProcessor<R> extends ProcessorBase<R>	implements
																																	Processor<R>
{

	private OpenCLDevice mOpenCLDevice;
	private Object[] mVolumeBuffers;

	@Override
	public boolean isCompatibleProcessor(Class<?> pRendererClass)
	{
		return pRendererClass == OpenCLVolumeRenderer.class;
	}

	public void setDevice(OpenCLDevice pOpenCLDevice)
	{
		mOpenCLDevice = pOpenCLDevice;
	}

	public OpenCLDevice getDevice()
	{
		return mOpenCLDevice;
	}

	public void setVolumeBuffers(Object... pArgs)
	{
		mVolumeBuffers = pArgs;
	}

	public Object[] getVolumeBuffers()
	{
		return mVolumeBuffers;
	}

	@Override
	public abstract void process(	int pRenderLayerIndex,
																long pWidthInVoxels,
																long pHeightInVoxels,
																long pDepthInVoxels);

}
