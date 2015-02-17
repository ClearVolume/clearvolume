package clearvolume.demo;

import clearvolume.renderer.processors.ProcessorResultListener;
import io.scif.FormatException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ulrik on 17/02/15.
 */
public interface ScopeControl extends ProcessorResultListener<float[]> {
  public ByteBuffer queryNextVolume() throws IOException, FormatException;
  public int queryBytesPerPixel() throws FormatException, IOException;
  public float[] getResolution();
}
