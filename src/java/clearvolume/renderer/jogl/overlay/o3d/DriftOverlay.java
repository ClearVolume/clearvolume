package clearvolume.renderer.jogl.overlay.o3d;

import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;

import java.nio.FloatBuffer;

/**
 * Drift Path Overlay.
 *
 * @author Ulrik Guenther (2015)
 *
 */
public class DriftOverlay extends PathOverlay implements ProcessorResultListener<FloatBuffer> {
  private FloatBuffer mStartColor = FloatBuffer.wrap(new float[]{0.0f, 0.0f, 1.0f, 1.0f});
  private FloatBuffer mEndColor = FloatBuffer.wrap(new float[]{1.0f, 0.0f, 0.0f, 1.0f});

  /* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#getName()
	 */
  @Override
  public String getName() {
    return "drift-path";
  }

  public void addNewCenterOfMass(float x, float y, float z) {
    mPathPoints.add(x, y, z);
  }

  @Override
  public void notifyResult(Processor<FloatBuffer> pSource, FloatBuffer pResult) {
    addNewCenterOfMass(pResult.get(0), pResult.get(1), pResult.get(2));
  }
}


