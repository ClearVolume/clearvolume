package clearvolume.renderer.scenery;

import clearvolume.transferf.TransferFunction;
import coremem.ContiguousMemoryInterface;
import coremem.types.NativeTypeEnum;
import scenery.Material;
import scenery.Mesh;
import scenery.Settings;
import scenery.rendermodules.opengl.OpenGLShaderPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * <Description>
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
public class VolumeNode extends Mesh {

  protected NativeTypeEnum volumeType;
  protected ArrayList<ContiguousMemoryInterface> volumeData;
  protected ArrayList<Long> volumeDimensions;
  protected ArrayList<TransferFunction> transferFunctions;
  protected float[] roi;
  protected Settings settings;

  public VolumeNode() {
    Material m = new Material();
    m.setDoubleSided(false);
//    m.setTransparent(true);
    this.setMaterial(m);

    volumeData = new ArrayList<>();
    volumeDimensions = new ArrayList<>();
    transferFunctions = new ArrayList<>();

    settings = getDefaultSettings();

    roi = new float[]{-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f};

    float side2 = 1.0f;
    setVertices(new float[]{
            // Front
            -side2, -side2, side2,
            side2, -side2, side2,
            side2, side2, side2,
            -side2, side2, side2
    });

    setNormals(new float[]{
            // Front
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f
    });

    setIndices(new int[]{
            0, 1, 2, 0, 2, 3
    });

    setTexcoords(new float[]{
      0.0f, 0.0f,
              1.0f, 0.0f,
              1.0f, 1.0f,
              0.0f, 1.0f,
              0.0f, 0.0f,
              1.0f, 0.0f
    });

    setBillboard(true);

    this.getMetadata().put(
            "ShaderPreference",
            new OpenGLShaderPreference(
                    Arrays.asList("shaders/VolumeNode.vert", "shaders/VolumeNode.frag"),
                    new HashMap<String, String>(),
                    Arrays.asList("DeferredShadingRenderer")));
  }

  protected Settings getDefaultSettings() {
    Settings ds = new Settings();
    ds.set("render.Dithering", 0.0f);
    ds.set("render.TransferMin", 0.0018f);
    ds.set("render.TransferMax", 0.88f);
    ds.set("render.Gamma", 0.21f);
    ds.set("render.Brightness", 1.0f);

    return ds;
  }

  public void addVolume(ContiguousMemoryInterface volume, long volumeSizeX, long volumeSizeY, long volumeSizeZ, int textureSizeX, int textureSizeY, NativeTypeEnum type) {
    volumeData.add(volume);

    volumeDimensions.add(volumeSizeX);
    volumeDimensions.add(volumeSizeY);
    volumeDimensions.add(volumeSizeZ);
  }

  public void setVolumeData(ArrayList<ContiguousMemoryInterface> volumeData) {
    this.volumeData = volumeData;
  }

  public Settings getSettings() {
    return settings;
  }

  public void freeVolumes(int offset, int count) {
    for(int i = offset+1; i < offset + 1 + count; i++) {
      if(i < volumeData.size()) {
        this.volumeData.get(i).free();
      }
    }
  }

  @Override
  public void preDraw() {
  }

  public NativeTypeEnum getVolumeType() {
    return volumeType;
  }

  public void setVolumeType(NativeTypeEnum volumeType) {
    this.volumeType = volumeType;
  }

  public ArrayList<ContiguousMemoryInterface> getVolumeData() {
    return volumeData;
  }

  public ArrayList<Long> getVolumeDimensions() {
    return volumeDimensions;
  }

  public void setVolumeDimensions(ArrayList<Long> volumeDimensions) {
    this.volumeDimensions = volumeDimensions;
  }

  public ArrayList<TransferFunction> getTransferFunctions() {
    return transferFunctions;
  }

  public void setTransferFunctions(ArrayList<TransferFunction> transferFunctions) {
    this.transferFunctions = transferFunctions;
  }

  public float[] getROI() {
    return roi;
  }

  public void setROI(float[] roi) {
    this.roi = roi;
  }
}
