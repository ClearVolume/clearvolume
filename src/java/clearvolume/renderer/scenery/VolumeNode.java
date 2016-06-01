package clearvolume.renderer.scenery;

import cleargl.GLTexture;
import clearvolume.transferf.TransferFunction;
import coremem.ContiguousMemoryInterface;
import coremem.types.NativeTypeEnum;
import scenery.Material;
import scenery.Mesh;
import scenery.Settings;

import java.util.ArrayList;

/**
 * <Description>
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
public class VolumeNode extends Mesh {

  protected NativeTypeEnum volumeType;
  protected ArrayList<ContiguousMemoryInterface> volumeData;
  protected ArrayList<Integer> volumeDimensions;
  protected ArrayList<TransferFunction> transferFunctions;
  protected ArrayList<GLTexture> volumeTextures;
  protected float[] roi;
  protected Settings settings;

  public VolumeNode() {
    Material m = new Material();
    this.setMaterial(m);

    volumeData = new ArrayList<>();
    volumeDimensions = new ArrayList<>();
    transferFunctions = new ArrayList<>();
    volumeTextures = new ArrayList<>();
  }

  protected Settings getDefaultSettings() {
    Settings ds = new Settings();
    ds.set("render.dithering", 0.0f);
    ds.set("render.TransferMin", 0.0f);
    ds.set("render.TransferMax", 1.0f);
    ds.set("render.Gamma", 2.2f);
    ds.set("render.Brightness", 1.0f);

    return ds;
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

  public NativeTypeEnum getVolumeType() {
    return volumeType;
  }

  public void setVolumeType(NativeTypeEnum volumeType) {
    this.volumeType = volumeType;
  }

  public ArrayList<ContiguousMemoryInterface> getVolumeData() {
    return volumeData;
  }

  public ArrayList<Integer> getVolumeDimensions() {
    return volumeDimensions;
  }

  public void setVolumeDimensions(ArrayList<Integer> volumeDimensions) {
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


  public ArrayList<GLTexture> getVolumeTextures() {
    return volumeTextures;
  }

  public void setVolumeTextures(ArrayList<GLTexture> volumeTextures) {
    this.volumeTextures = volumeTextures;
  }

}
