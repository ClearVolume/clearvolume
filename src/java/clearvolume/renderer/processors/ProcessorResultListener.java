package clearvolume.renderer.processors;

public interface ProcessorResultListener<R>
{
	public void notifyResult(Processor<R> pSource, R pResult);
}
