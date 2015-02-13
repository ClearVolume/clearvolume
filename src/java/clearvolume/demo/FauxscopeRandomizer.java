package clearvolume.demo;

/**
 * Created by ulrik on 13/02/15.
 */
public interface FauxscopeRandomizer {
  float[] getNextPoint();
  void init();
  void reinitialize();
}
