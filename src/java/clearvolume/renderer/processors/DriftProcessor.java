package clearvolume.renderer.processors;

/**
 * Created by ulrik on 12/02/15.
 */
public interface DriftProcessor extends ProcessorResultListener<float[]> {
  public void notifyResult(Processor<float[]> pSource, float[] delta);
}
