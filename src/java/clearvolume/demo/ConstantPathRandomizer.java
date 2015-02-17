package clearvolume.demo;

import java.util.ArrayList;

/**
 * Created by ulrik on 13/02/15.
 */
public class ConstantPathRandomizer implements FauxscopeRandomizer {
  float dirX, dirY, dirZ;

  ArrayList<Float> x = new ArrayList<>();
  ArrayList<Float> y = new ArrayList<>();
  ArrayList<Float> z = new ArrayList<>();

  int step = 0;

  @Override
  public float[] getNextPoint() {
    float[] result = new float[3];

    result[0] = x.get(step) + dirX;
    result[1] = y.get(step) + dirY;
    result[2] = z.get(step) + dirZ;

    x.add(result[0]);
    y.add(result[1]);
    z.add(result[2]);

    step++;
    return result;
  }

  @Override
  public void init() {

  }

  public ConstantPathRandomizer(float deltaX, float deltaY, float deltaZ) {
    dirX = deltaX;
    dirY = deltaY;
    dirZ = deltaZ;

    x.add(1.0f);
    y.add(1.0f);
    z.add(1.0f);
  }

  @Override
  public void reinitialize() {

  }
}
