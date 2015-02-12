package clearvolume.demo;

import clearvolume.renderer.ClearVolumeRendererInterface;
import io.scif.Reader;
import io.scif.SCIFIO;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;


/**
 * Created by ulrik on 12/02/15.
 */
public class Fauxscope {

  protected final ClearVolumeRendererInterface mRenderer;
  protected final SCIFIO scifio;
  protected ArrayList<Reader> readers = new ArrayList<>();
  protected int currentReaderIndex = 0;

  protected long resolutionX;
  protected long resolutionY;
  protected long resolutionZ;
  protected int timepoints;

  protected ByteBuffer lVolumeDataArray;

  public Fauxscope(boolean randomDrift, ClearVolumeRendererInterface renderer) {
    mRenderer = renderer;
    scifio = new SCIFIO();
  }

  public void use4DStack(String filename) {

  }

  public float[] getResolution() {
    return new float[] {resolutionX, resolutionY, resolutionZ, timepoints};
  }

  public void use4DStacksFromDirectory() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int returnVal = chooser.showOpenDialog(null);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
      System.out.println("You chose to open this file: " +
              chooser.getSelectedFile().getName());
    } else {
      return;
    }

    final String imageDirectory = chooser.getSelectedFile().getAbsolutePath();
    File folder = new File(imageDirectory);
    File[] filesInFolder = folder.listFiles();
    ArrayList<String> tiffFiles = new ArrayList<>();

    for(File f: filesInFolder) {
      if(f.exists() && f.isFile() && f.canRead() && (f.getName().endsWith(".tiff") || f.getName().endsWith(".tif"))) {
        System.out.println("Found TIFF: " + f.getName());
        tiffFiles.add(f.getAbsolutePath());
      }
    }

    timepoints = tiffFiles.size();

    try {
      for (String tiff : tiffFiles) {

        Reader reader = scifio.initializer().initializeReader(tiff);

        resolutionX = reader.openPlane(0, 0).getLengths()[0];
        resolutionY = reader.openPlane(0, 0).getLengths()[1];
        resolutionZ = reader.getPlaneCount(0);

        System.out.format("Added reader for " + tiff.substring(tiff.lastIndexOf("/")) + ", %d/%d/%d\n", resolutionX, resolutionY, resolutionZ);

        readers.add(reader);
      }
    } catch (io.scif.FormatException | IOException e) {
      e.printStackTrace();
    }

  }

  public ByteBuffer queryNextVolume() {
    lVolumeDataArray = null;

    if(currentReaderIndex + 1 > readers.size()) {
      currentReaderIndex = 0;
    }

    Reader thisReader = readers.get(currentReaderIndex);
    System.out.println("Loading volume from " + thisReader.getCurrentFile() + " (" + resolutionX*resolutionY*resolutionZ/1024/1024 + "M)");
    lVolumeDataArray = ByteBuffer.allocate((int)resolutionX * (int)resolutionY * (int)resolutionZ);
    lVolumeDataArray.order(ByteOrder.LITTLE_ENDIAN);

    try {
      for (int z = 0; z < resolutionZ; z++) {
        System.out.println(thisReader.openPlane(0, 1).getImageMetadata().isLittleEndian());
        lVolumeDataArray.put(thisReader.openPlane(0, z).getBytes());
      }
      System.out.println("\n");
    } catch(IOException | io.scif.FormatException e) {
      e.printStackTrace();
      return null;
    }

    currentReaderIndex++;

    //lVolumeDataArray.order(ByteOrder.BIG_ENDIAN);
    return lVolumeDataArray;
  }

  public void sendVolume() {

  }
}
