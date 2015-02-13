package clearvolume.demo;

import clearvolume.renderer.ClearVolumeRendererInterface;
import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImageRegion;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;


/**
 * Created by ulrik on 12/02/15.
 */
public class Fauxscope {

  protected final ClearVolumeRendererInterface mRenderer;
  protected final SCIFIO scifio;
  protected ArrayList<Reader> readers = new ArrayList<>();
  protected int currentReaderIndex = 0;
  protected boolean mRandomDrift;

  protected long resolutionX;
  protected long resolutionY;
  protected long resolutionZ;
  protected int timepoints;

  protected ByteBuffer lVolumeDataArray;

  public Fauxscope(boolean randomDrift, ClearVolumeRendererInterface renderer) {
    mRandomDrift = randomDrift;
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

  public int queryBytesPerPixel() throws FormatException, IOException {
    if(readers.size() > 0) {
      int bpp = readers.get(0).openPlane(0,1).getImageMetadata().getBitsPerPixel()/8;
      return bpp;
    } else {
      throw new IOException("No readers registered. Did you click cancel in the file selection dialog?");
    }
  }

  public ByteBuffer queryNextVolume() throws IOException, FormatException{
    lVolumeDataArray = null;

    if(currentReaderIndex + 1 > readers.size()) {
      currentReaderIndex = 0;
    }

    Reader thisReader = readers.get(currentReaderIndex);
    Plane firstPlane = thisReader.openPlane(0,1);
    int bytesPerPixel = firstPlane.getImageMetadata().getBitsPerPixel()/8;
    boolean isLittleEndian = firstPlane.getImageMetadata().isLittleEndian();

    System.out.println("Loading volume from " + thisReader.getCurrentFile() + " (" + bytesPerPixel*resolutionX*resolutionY*resolutionZ/1024/1024 + "M)");

    lVolumeDataArray = ByteBuffer.allocate(bytesPerPixel*(int)resolutionX * (int)resolutionY * (int)resolutionZ);
    if(isLittleEndian) {
      lVolumeDataArray.order(ByteOrder.LITTLE_ENDIAN);
    } else {
      lVolumeDataArray.order(ByteOrder.BIG_ENDIAN);
    }

    int currentZ = 0;
    int zStart = 0;
    int zEnd = (int)resolutionZ;

    int maxZShift = 50;
    int minZShift = -50;
    int shiftZ = 0;

    int maxYShift = 600;
    int minYShift = -600;
    int shiftY = 0;

    int zReadStart = 0;
    int zReadEnd = (int)resolutionZ;

    try {
      if(mRandomDrift) {
        Random rand = new Random();
        shiftY = rand.nextInt((maxYShift - minYShift) + 1) + minYShift;
        shiftZ = rand.nextInt((maxZShift - minZShift) + 1) + minZShift;
        System.out.format("Shaking volume: ∂y=%d ∂z=%d\n", shiftY, shiftZ);

        zStart += shiftZ;
        zEnd += shiftZ;
      }

      if(zEnd > resolutionZ) {
        // pad volume before, as it is shifted inwards
        lVolumeDataArray.put(new byte[bytesPerPixel*(int)resolutionX * (int)resolutionY * (zEnd - (int)resolutionZ)]);
      }

      if(zStart < 0) {
        zReadStart = 0;
        zReadEnd = zReadEnd + shiftZ;
      }
      if(zEnd > resolutionZ) {
        zReadStart = zReadStart + shiftZ;
        zReadEnd = (int)resolutionZ;
      }

      for (currentZ = zReadStart; currentZ < zReadEnd; currentZ++) {
        Plane p;

        if(currentZ % 10 == 0) {
          //System.out.println("offsetting!");
          SCIFIOConfig c = new SCIFIOConfig();
          AxisType[] a = new AxisType[]{Axes.X, Axes.Y};
          c.imgOpenerSetRegion(new ImageRegion(a, new String[]{"500-1920","500-1000"}));

          p = thisReader.openPlane(0, currentZ, c);
        } else {
          p = thisReader.openPlane(0, currentZ);
        }
        if(mRandomDrift) {
          lVolumeDataArray.put(cutRowsFromPlane(p, shiftY).array());
        } else {
          lVolumeDataArray.put(p.getBytes());
        }
      }
    } catch(IOException | io.scif.FormatException e) {
      System.out.println("Warning: Exception during reading data. Stack trace follows, returning volume until z=" + currentZ);
      e.printStackTrace();
      return lVolumeDataArray;
    }

    if(zStart < 0) {
      lVolumeDataArray.put(new byte[bytesPerPixel*(int)resolutionX * (int)resolutionY * Math.abs(zStart)]);
    }

    System.out.println("Read z=[0," + currentZ + " from " + thisReader.getCurrentFile() + ", returning volume." );
    currentReaderIndex++;

    return lVolumeDataArray;
  }

  public ByteBuffer cutRowsFromPlane(Plane p, int shiftRows) {
    // allocate padding buffer
    byte[] padBuffer = new byte[(int)(p.getLengths()[0])*p.getImageMetadata().getBitsPerPixel()/8*Math.abs(shiftRows)];
    byte[] original = p.getBytes();

    ByteBuffer newBuffer = ByteBuffer.allocate((int)(p.getLengths()[0])*(int)(p.getLengths()[1])*p.getImageMetadata().getBitsPerPixel()/8);

    if(shiftRows > 0) {
      newBuffer.put(padBuffer);
      newBuffer.put(original, 0, original.length-padBuffer.length);
    } else if(shiftRows < 0) {
      newBuffer.put(original, padBuffer.length, original.length-padBuffer.length);
      newBuffer.put(padBuffer);
    } else {
      return ByteBuffer.wrap(original);
    }

    return newBuffer;
  }
}
