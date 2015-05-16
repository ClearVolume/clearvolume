package clearvolume.renderer.processors;

import clearvolume.renderer.SingleKeyToggable;

public interface Processor<R> extends SingleKeyToggable
{

	public void addResultListener(ProcessorResultListener<R> pProcessorResultListener);

	public void removeResultListener(ProcessorResultListener<R> pProcessorResultListener);

	public boolean isCompatibleProcessor(Class<?> pRendererClass);

	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels);

	public String getName();

	@Override
	public boolean toggle();

	public boolean isActive();

	public void setActive(boolean pActive);

}
