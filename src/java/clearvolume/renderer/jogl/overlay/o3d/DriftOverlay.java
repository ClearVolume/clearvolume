package clearvolume.renderer.jogl.overlay.o3d;

import cleargl.ClearTextRenderer;
import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.Overlay2D;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.media.opengl.GL4;
import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * Drift Path Overlay.
 *
 * @author Ulrik Guenther (2015)
 *
 */
public class DriftOverlay extends PathOverlay implements ProcessorResultListener<FloatBuffer>, Overlay2D {
  protected FloatBuffer mStartColor = FloatBuffer.wrap(new float[]{0.0f, 0.0f, 1.0f, 1.0f});
  protected FloatBuffer mEndColor = FloatBuffer.wrap(new float[]{1.0f, 0.0f, 0.0f, 1.0f});
  protected DescriptiveStatistics stats;

  protected ClearTextRenderer textRenderer;

  /* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#getName()
	 */
  @Override
  public void init(	GL4 pGL4,
                     DisplayRequestInterface pDisplayRequestInterface) {
    super.init(pGL4, pDisplayRequestInterface);

    textRenderer = new ClearTextRenderer(pGL4);
  }

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

  @Override
  public boolean hasChanged2D() {
    return false;
  }

  @Override
  public void render2D(GL4 pGL4, GLMatrix pProjectionMatrix, GLMatrix pInvVolumeMatrix) {
    Font font = null;
    stats = new DescriptiveStatistics();

    try {
      font = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("fonts/SourceCodeProLight.ttf")).deriveFont(12.0f);
    } catch (FontFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    int i = 0;
    for(; i < getPathPoints().capacity(); i = i + 3) {
      float x = getPathPoints().get(i);
      float y = getPathPoints().get(i+1);
      float z = getPathPoints().get(i+2);
      float dist = (float)Math.sqrt(x*x + y*y + z*z);

      stats.addValue(dist);
    }

    textRenderer.drawTextAtPosition("drift stats: n=" + i/3 + " avg=" + String.format("%.3f", stats.getMean()) + " stddev=" + String.format("%.3f", stats.getStandardDeviation()) + " min=" + String.format("%.3f", stats.getMin()) + " max=" + String.format("%.3f", stats.getMax()), 10, 15, font, FloatBuffer.wrap(new float[]{1.0f, 1.0f, 1.0f}), false);
  }
}


