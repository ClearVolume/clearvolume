package clearvolume.renderer.processors;


public interface Processor<R>
{

	public void addResultListener(ProcessorResultListener<R> pProcessorResultListener);

	public void removeResultListener(ProcessorResultListener<R> pProcessorResultListener);

	public boolean isCompatibleProcessor(Class<?> pRendererClass);

	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels);

	public String getName();

	public boolean toggleActive();

	public boolean isActive();

	public void setActive(boolean pActive);

}
