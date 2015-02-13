package clearvolume.demo;

import java.util.ArrayList;

/**
 * Created by ulrik on 13/02/15.
 */
public class LevyFlightRandomizer implements FauxscopeRandomizer {
  float alpha, beta;

  ArrayList<Float> x = new ArrayList<>();
  ArrayList<Float> y = new ArrayList<>();
  ArrayList<Float> z = new ArrayList<>();

  int step = 0;

  @Override
  public float[] getNextPoint() {
    float[] result = new float[3];

    float theta = (float)(2.0f*Math.PI*Math.random());
    float phi = (float)(Math.PI*Math.random());
    float f = (float)(Math.pow(Math.random(), 1.0f / alpha));
    float g = (float)(Math.pow(Math.random(), 1.0f / beta));

    result[0] = x.get(step) + f * (float)Math.cos(phi) * g * (float)Math.sin(theta);
    result[1] = y.get(step) + f * (float)Math.sin(phi) * g * (float)Math.sin(theta);
    result[2] = z.get(step) + f * (float)Math.cos(theta);

    x.add(result[0]);
    y.add(result[1]);
    z.add(result[2]);

    step++;
    return result;
  }

  @Override
  public void init() {

  }

  public LevyFlightRandomizer(float alpha, float beta) {
    this.alpha = alpha;
    this.beta = beta;

    x.add(1.0f);
    y.add(1.0f);
    z.add(1.0f);
  }

  @Override
  public void reinitialize() {

  }
}
