package clearvolume.renderer.processors;

/**
 * Created by ulrik on 12/02/15.
 */
public interface DriftProcessor	extends
																ProcessorResultListener<float[]>
{
	@Override
	public void notifyResult(ProcessorInterface<float[]> pSource, float[] delta);
}
