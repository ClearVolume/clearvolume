package clearvolume.demo;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.processors.Processor;
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
import java.util.HashMap;


/**
 * Fauxscope.
 *
 * @author Ulrik Guenther (2015)
 *
 */

public class Fauxscope implements ScopeControl {

  protected final ClearVolumeRendererInterface mRenderer;
  protected final SCIFIO scifio;
  protected ArrayList<Reader> readers = new ArrayList<>();
  protected int currentReaderIndex = 0;
  protected boolean mRandomDrift;
  protected boolean mCorrectDrift;
  protected FauxscopeRandomizer r;

  protected long resolutionX;
  protected long resolutionY;
  protected long resolutionZ;
  protected int timepoints;

  protected ArrayList<Float> oldCenterOfMass = new ArrayList<>();
  protected ArrayList<Float> centerOfMass = new ArrayList<>();

  protected ByteBuffer lVolumeDataArray;

  public Fauxscope(boolean randomDrift, boolean correctDrift, ClearVolumeRendererInterface renderer, FauxscopeRandomizer randomizer) {
    mRandomDrift = randomDrift;
    mCorrectDrift = correctDrift;
    mRenderer = renderer;
    scifio = new SCIFIO();

    System.out.println("Fauxscope: Using " + randomizer.getClass().getName() + " randomizer.");
    r = randomizer;

    centerOfMass.add(0.0f);
    centerOfMass.add(0.0f);
    centerOfMass.add(0.0f);
  }

  public void use4DStack(String filename) {

  }

  public float[] getResolution() {
    return new float[] {resolutionX, resolutionY, resolutionZ, timepoints};
  }

  @Override
  public HashMap<String, Object> getComputedData() {
    return null;
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

    //System.out.println("Loading volume from " + thisReader.getCurrentFile() + " (" + bytesPerPixel*resolutionX*resolutionY*resolutionZ/1024/1024 + "M)");

    lVolumeDataArray = ByteBuffer.allocate(bytesPerPixel*(int)resolutionX * (int)resolutionY * (int)resolutionZ);
    if(isLittleEndian) {
      lVolumeDataArray.order(ByteOrder.LITTLE_ENDIAN);
    } else {
      lVolumeDataArray.order(ByteOrder.BIG_ENDIAN);
    }

    int currentZ = 0;
    int zStart = 0;
    int zEnd = (int)resolutionZ;

    int maxZShift = (int)resolutionZ/8-1;
    int minZShift = -(int)resolutionZ/8+1;
    int shiftZ = 0;

    int maxYShift = (int)resolutionY/8-1;
    int minYShift = -(int)resolutionY/8-1;
    int shiftY = 0;

    int maxXShift = (int)resolutionX/8-1;
    int minXShift = -(int)resolutionX/8-1;
    int shiftX = 0;

    int zReadStart = 0;
    int zReadEnd = (int)resolutionZ;

    try {
      if(mRandomDrift) {
        //Random rand = new Random();
        float[] shift = new float[3];
        shift = r.getNextPoint();
        shiftX = (int)Math.floor((maxXShift - minXShift) * (shift[0] - 1.0) + minXShift);
        shiftY = (int)Math.floor((maxYShift - minYShift) * (shift[1] - 1.0) + minYShift);
        shiftZ = (int)Math.floor((maxZShift - minZShift) * (shift[2] - 1.0) + minZShift);
        System.out.format("Shaking volume: ∂x=%d ∂y=%d ∂z=%d\n", shiftX, shiftY, shiftZ);

        if(mCorrectDrift) {
          if(oldCenterOfMass.size() == 3 && centerOfMass.size() == 3) {
//            float[] fDeltas = new float[]{antiDrift.get(antiDrift.size()-6) - antiDrift.get(antiDrift.size()-3),
//                    antiDrift.get(antiDrift.size()-5)-antiDrift.get(antiDrift.size()-2),
//                    antiDrift.get(antiDrift.size()-4)-antiDrift.get(antiDrift.size()-1)};

//            float[] fDeltas = new float[]{oldCenterOfMass.get(0) - centerOfMass.get(0),
//                    oldCenterOfMass.get(1) - centerOfMass.get(1),
//                    oldCenterOfMass.get(2) - centerOfMass.get(2)};
//
//            int[] deltas = rescaleFromLocalVoxelInterval(fDeltas[0], fDeltas[1], fDeltas[2]);

            //int[] oldC = rescaleFromLocalVoxelInterval(oldCenterOfMass.get(0), oldCenterOfMass.get(1), oldCenterOfMass.get(2));
            //int[] newC = rescaleFromLocalVoxelInterval(centerOfMass.get(0), centerOfMass.get(1), centerOfMass.get(2));

            //int[] deltas = new int[]{oldC[0]-newC[0], oldC[1]-newC[1], oldC[2]-newC[2]};
            int[] deltas = new int[]{(int)Math.floor(Math.abs(centerOfMass.get(0) - oldCenterOfMass.get(0))),
                    (int)Math.floor(Math.abs(centerOfMass.get(1) - oldCenterOfMass.get(1))),
                    (int)Math.floor(Math.abs(centerOfMass.get(2) - oldCenterOfMass.get(2)))};

            System.out.format("Correcting drift with %d,%d,%d \n", deltas[0], deltas[1], deltas[2]);
            System.out.format("Deltas  volume: x=%d y=%d z=%d\n", Math.abs(deltas[0]-shiftX), Math.abs(deltas[1]-shiftY), Math.abs(deltas[2]-shiftZ));
            shiftX -= deltas[0];
            shiftY -= deltas[1];
            shiftZ -= deltas[2];
            System.out.format("Faaking volume: ∂x=%d ∂y=%d ∂z=%d\n", shiftX, shiftY, shiftZ);
            System.out.format("Deltas  volume: x=%d y=%d z=%d\n", Math.abs(deltas[0]-shiftX), Math.abs(deltas[1]-shiftY), Math.abs(deltas[2]-shiftZ));
          }
        }

        zStart += shiftZ;
        zEnd += shiftZ;
      }

      if(shiftZ > 0) {
        // pad volume before, as it is shifted inwards
        System.out.println("Padding before");
        lVolumeDataArray.put(new byte[bytesPerPixel*(int)resolutionX * (int)resolutionY * shiftZ]);
      }

      if(zStart < 0) {
        zReadStart = 0;
        zReadEnd = zReadEnd + shiftZ;
      }
      if(zEnd > resolutionZ) {
        zReadStart = zReadStart + shiftZ;
        zReadEnd = (int)resolutionZ;
      }

      System.out.format("rs: %d re: %d ∂z: %d\n", zReadStart, zReadEnd, shiftZ);

      for (currentZ = zReadStart; currentZ < zReadEnd; currentZ++) {
        Plane p;

        if(currentZ % 10 == 0) {
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

    if(shiftZ < 0) {
      System.out.println("Padding after");
      lVolumeDataArray.put(new byte[bytesPerPixel*(int)resolutionX * (int)resolutionY * Math.abs(shiftZ)]);
    }

    //System.out.println("Read z=[0," + currentZ + " from " + thisReader.getCurrentFile() + ", returning volume." );
    currentReaderIndex++;

    return lVolumeDataArray;
  }

  private int[] rescaleFromLocalVoxelInterval(float x, float y, float z) {

//    int new_x = (int) Math.ceil((x - (-1.0f)) * (resolutionX/4 - (-resolutionX / 4)) / (1.0f - (-1.0f)) + (-resolutionX / 4));
//    int new_y = (int) Math.ceil((y - (-1.0f)) * (resolutionY/4 - (-resolutionY / 4)) / (1.0f - (-1.0f)) + (-resolutionY / 4));
//    int new_z = (int) Math.ceil((z - (-1.0f)) * (resolutionZ/4 - (-resolutionZ/4))/(1.0f - (-1.0f)) + (-resolutionZ/4));

    int new_x = (int)Math.ceil(((resolutionX - 0) * (x - (-1.0f)) / (1.0f - (-1.0f))) + 0);
    int new_y = (int)Math.ceil(((resolutionY - 0) * (x - (-1.0f)) / (1.0f - (-1.0f))) + 0);
    int new_z = (int)Math.ceil(((resolutionZ - 0) * (x - (-1.0f)) / (1.0f - (-1.0f))) + 0);

    return new int[] {new_x, new_y, new_z};
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

  @Override
  public void notifyResult(Processor<float[]> pSource, float[] pResult) {

    float[] tmp = new float[]{centerOfMass.get(0), centerOfMass.get(1), centerOfMass.get(2)};

      System.out.format("Setting correction target %g,%g,%g\n", centerOfMass.get(0), centerOfMass.get(1), centerOfMass.get(2));
      oldCenterOfMass.clear();
      oldCenterOfMass.add(centerOfMass.get(0));
      oldCenterOfMass.add(centerOfMass.get(1));
      oldCenterOfMass.add(centerOfMass.get(2));

    centerOfMass.clear();

    centerOfMass.add(pResult[3]);
    centerOfMass.add(pResult[4]);
    centerOfMass.add(pResult[5]);


  }
}
