package clearvolume.renderer.processors;

public interface ProcessorResultListener<R>
{
	public void notifyResult(ProcessorInterface<R> pSource, R pResult);
}
