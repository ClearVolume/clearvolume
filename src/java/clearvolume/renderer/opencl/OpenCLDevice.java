package clearvolume.renderer.opencl;

import clearvolume.ClearVolumeCloseable;
import clearvolume.renderer.opencl.utils.JavaCLUtils;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLImageFormat.ChannelDataType;
import com.nativelibs4java.opencl.CLImageFormat.ChannelOrder;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.IOUtils;
import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemoryInterface;
import org.bridj.Pointer;

import java.io.File;
import java.net.URL;
import java.nio.*;
import java.util.ArrayList;

public class OpenCLDevice implements ClearVolumeCloseable {

  public CLContext mCLContext;
  public CLProgram mCLProgram;
  public CLDevice mCLDevice;
  public CLQueue mCLQueue;
  public ByteOrder mCLContextByteOrder;

  public ArrayList<CLKernel> mCLKernelList = new ArrayList<CLKernel>();

  public boolean initCL() {
    return initCL(false);
  }

  public boolean initCL(final boolean useExistingOpenGLContext) {
    // initialize the platform and devices OpenCL will use
    // usually chooses the best, i.e. fastest, platform/device/context

    CLDevice lBestDevice = getBestDevice(true);
    if (lBestDevice == null)
      lBestDevice = getBestDevice(false);
    if (lBestDevice == null)
      lBestDevice = JavaCL.getBestDevice();

    if (lBestDevice == null) {
      System.err.println("Could not find best OpenCL device!");
      return false;
    }

    mCLDevice = lBestDevice;

    try {
      mCLContext = JavaCL.createContext(null, lBestDevice);
    } catch (final Throwable e) {
      System.err.println("failed to create OpenCL context");
      e.printStackTrace();
      return false;
    }

    try {
      mCLQueue = mCLContext.createDefaultQueue();
    } catch (final Throwable e) {
      System.err.println("failed to create default queue");
      e.printStackTrace();
      return false;
    }

    mCLContextByteOrder = mCLContext.getByteOrder();

    return (mCLDevice != null && mCLContext != null && mCLQueue != null);

  }

  private CLDevice getBestDevice(boolean pGPUOnly) {
    CLDevice lBestDevice = null;
    try {
      final CLPlatform[] lCLPlatforms = JavaCL.listPlatforms();

      printDevicesInfo(lCLPlatforms);

      long lMaxMemory = 0;

      CLPlatform lBestPlatform = null;

      for (final CLPlatform lCLPlatform : lCLPlatforms) {
        final CLDevice lBestCLDeviceForPlateform = getDeviceWithMostMemory(
                pGPUOnly, lCLPlatform);

        if (lBestCLDeviceForPlateform != null)
          if (lBestCLDeviceForPlateform.getGlobalMemSize() > lMaxMemory) {
            try {
              lMaxMemory = lBestCLDeviceForPlateform
                      .getGlobalMemSize();
              lBestDevice = lBestCLDeviceForPlateform;
              lBestPlatform = lCLPlatform;
            } catch (final Throwable e) {
              e.printStackTrace();
            }
          }
      }

      try {
        String lPreferredPlatform = System.getenv("CV_OPENCL_DEVICE");

        if (lPreferredPlatform == null) {
          lPreferredPlatform = System
                  .getProperty("ClearVolume.OpenCLDevice");
        }

        if (lPreferredPlatform == null)
          return lBestDevice;

        final String[] lPreferred = lPreferredPlatform.split(",");

        final int platformId = Integer.parseInt(lPreferred[0]);
        final int deviceId = Integer.parseInt(lPreferred[1]);

        if (lPreferred.length == 2
                && platformId < lCLPlatforms.length
                && deviceId < lCLPlatforms[platformId]
                .listAllDevices(true).length) {
          System.out.println("Overriding device selection:");
          lBestDevice = lCLPlatforms[platformId].listAllDevices(true)[deviceId];
          lBestPlatform = lCLPlatforms[platformId];
        }
      } catch (final NumberFormatException
              | ArrayIndexOutOfBoundsException e) {
        System.err
                .println("Invalid specification for device and platform IDs. Please set as CV_OPENCL_DEVICE=platformId,deviceId");
      }

      if (lBestPlatform != null && lBestDevice != null)
        System.out.println("Found best device to be: "
                + lBestDevice.getName() + " from platform "
                + lBestPlatform.getName());

    } catch (final Throwable e) {
      System.err.println("failed to find best device");
      e.printStackTrace();
      return null;
    }
    return lBestDevice;
  }

  private void printDevicesInfo(final CLPlatform[] lCLPlatforms) {
    for (final CLPlatform lCLPlatform : lCLPlatforms) {
      System.out.format("Platform: %s \n", lCLPlatform);
      for (final CLDevice lCLDevice : lCLPlatform.listAllDevices(true)) {
        try {
          System.out.format("	#device: %s \n", lCLDevice.toString());
          System.out.format("		*opencl version: %s \n",
                  lCLDevice.getOpenCLCVersion());

          System.out.format("		*driver version: %s \n",
                  lCLDevice.getDriverVersion());

          System.out.format("		*max mem alloc size: %d \n",
                  lCLDevice.getMaxMemAllocSize());
          System.out.format("		*global mem size: %d \n",
                  lCLDevice.getGlobalMemSize());

          System.out.format("		*max compute units: %d \n",
                  lCLDevice.getMaxComputeUnits());
          System.out.format("		*max clock freq: %d \n",
                  lCLDevice.getMaxClockFrequency());

          System.out.format("		*3d volume max width: %d \n",
                  lCLDevice.getImage3DMaxWidth());
          System.out.format("		*3d volume max height: %d \n",
                  lCLDevice.getImage3DMaxHeight());
          System.out.format("		*3d volume max depth: %d \n",
                  lCLDevice.getImage3DMaxDepth());

          System.out.format("		*isHostUnifiedMemory: %s \n",
                  lCLDevice.isHostUnifiedMemory() ? "true" : "false");
        } catch (final Throwable e) {
          e.printStackTrace();
        }

      }
    }
  }

  private CLDevice getDeviceWithMostMemory(boolean pGPUOnly,
                                           CLPlatform pCLPlatform) {

    final CLDevice[] lDevices = pGPUOnly ? pCLPlatform.listGPUDevices(true)
            : pCLPlatform.listCPUDevices(true);

    if (lDevices.length == 0)
      return null;

    long lBestDeviceGlobalMemSize = 0;
    CLDevice lBestDevice = null;

    try {
      for (final CLDevice lCLDevice : lDevices) {
        final long lDeviceGlobalMemSize = lCLDevice.getGlobalMemSize();

        System.out.println(lCLDevice.getPlatform().getName() + "."
                + lCLDevice.getName() + " L"
                + lCLDevice.getLocalMemSize() / 1024 + "k/G "
                + lCLDevice.getGlobalMemSize() / 1024 / 1024
                + "M mem with " + lCLDevice.getMaxComputeUnits()
                + " compute units");

        final boolean lIsKnownHighPerfCard = lCLDevice.getName()
                .toLowerCase().contains("geforce")
                || lCLDevice.getName().toLowerCase().contains("nvidia")
                || lCLDevice.getName().toLowerCase().contains("quadro")
                || lCLDevice.getName().toLowerCase()
                .contains("firepro");

        if (lDeviceGlobalMemSize > lBestDeviceGlobalMemSize
                || (lDeviceGlobalMemSize >= lBestDeviceGlobalMemSize && lIsKnownHighPerfCard)) {
          lBestDevice = lCLDevice;
          lBestDeviceGlobalMemSize = lDeviceGlobalMemSize;
        }

      }

      if (lBestDevice == null && lDevices.length >= 1) {
        lBestDevice = lDevices[0];
      }

    } catch (final Throwable e) {
      e.printStackTrace();
    }

    if (lBestDevice != null)
      System.out.println(lBestDevice.getName() + " is best in platform "
              + lBestDevice.getPlatform().getName());
    return lBestDevice;
  }

  public CLContext getContext() {
    return mCLContext;
  }

  public CLQueue getQueue() {
    return mCLQueue;
  }

  public void printInfo() {

    System.out.printf("Device name: \t %s \n", mCLDevice);

  }

  public int getKernelIndex(CLKernel pCLKernel) {
    return mCLKernelList.indexOf(pCLKernel);
  }

  public CLKernel compileKernel(final URL url, final String kernelName) {

    // Read the program sources and compile them :
    String src = "";
    try {
      src = IOUtils.readText(url);
    } catch (final Exception e) {
      System.err.println("couldn't read program source ");
      e.printStackTrace();
      return null;
    }

    try {
      mCLProgram = mCLContext.createProgram(src);
      mCLProgram.addInclude(url.getPath().substring(0, url.getPath().lastIndexOf(File.separator)));
      mCLProgram.setFastRelaxedMath();
      mCLProgram.setFiniteMathOnly();
      mCLProgram.setMadEnable();
      // mCLProgram.setNVVerbose();
      // mCLProgram.setNoSignedZero();
      mCLProgram.setUnsafeMathOptimizations();/**/
      try {
        // mCLProgram.setNVOptimizationLevel(2);
      } catch (final Throwable e) {
      }
    } catch (final Exception e) {
      System.err.println("couldn't create program from " + src);
      e.printStackTrace();
      return null;
    }

    CLKernel lNewKernel = null;
    try {
      lNewKernel = mCLProgram.createKernel(kernelName);
      mCLKernelList.add(lNewKernel);
    } catch (final Exception e) {
      System.err.println("couldn't create kernel '" + kernelName + "'");
      e.printStackTrace();
    }

    return lNewKernel;
  }

  public void setArgs(final CLKernel pCLKernel, Object... args) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLKernel.setArgs(args);
  }

  public void setArgs(final int pKernelIndex, Object... args) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    mCLKernelList.get(pKernelIndex).setArgs(args);
  }

  public CLEvent run(final int pKernelIndex, final int mNx, final int mNy) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = mCLKernelList.get(pKernelIndex).enqueueNDRange(
            mCLQueue, new int[]{mNx, mNy});
    evt.waitFor();
    return evt;
  }

  public CLEvent[] runSubdivisions(final CLKernel pKernel, final int mNx, final int mNy, int subdivisions) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    CLEvent[] evt = new CLEvent[subdivisions * subdivisions];
    final long sizeX = mNx / subdivisions;
    final long sizeY = mNy / subdivisions;

    for (int i = 0; i < subdivisions; i++) {
      for (int j = 0; j < subdivisions; j++) {
        evt[i + subdivisions * j] = pKernel.enqueueNDRange(
                mCLQueue, new long[]{i * sizeX, j * sizeY}, new long[]{sizeX, sizeY}, null);
      }
    }

    for (int i = 0; i < subdivisions * subdivisions; i++)
      mCLQueue.enqueueWaitForEvents(evt[i]);

    return evt;
  }

  public CLEvent run(final int pKernelIndex, final int mNx, final int mNy,
                     final int mNz) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = mCLKernelList.get(pKernelIndex).enqueueNDRange(
            mCLQueue, new int[]{mNx, mNy, mNz});
    evt.waitFor();
    return evt;
  }

  public CLEvent run(final int pKernelIndex, final int mNx, final int mNy,
                     final int mNz, final int mNxLoc, final int mNyLoc, final int mNzLoc)

  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = mCLKernelList.get(pKernelIndex).enqueueNDRange(
            mCLQueue, new int[]{mNx, mNy, mNz},
            new int[]{mNxLoc, mNyLoc, mNzLoc});
    evt.waitFor();
    return evt;
  }

  public CLEvent run(final CLKernel pCLKernel, final int mNx) {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue,
            new int[]{mNx});
    evt.waitFor();
    return evt;
  }

  public CLEvent run(final CLKernel pCLKernel, final int mNx, final int mNy) {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]{mNx,
            mNy});
    evt.waitFor();
    return evt;
  }

  public CLEvent run(final CLKernel pCLKernel, final int mNx, final int mNy,
                     final int mNz) {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]{mNx,
            mNy, mNz});
    evt.waitFor();
    return evt;
  }

  public CLEvent run(final CLKernel pCLKernel, final int mNx, final int mNy,
                     final int mNz, final int mNxLoc, final int mNyLoc, final int mNzLoc)

  {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLEvent evt = pCLKernel.enqueueNDRange(mCLQueue, new int[]{mNx,
            mNy, mNz}, new int[]{mNxLoc, mNyLoc, mNzLoc});
    evt.waitFor();
    return evt;
  }

  public CLImage2D createGenericImage2D(final long Nx, final long Ny,
                                        ChannelOrder pChannelOrder, ChannelDataType pChannelDataType) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLImageFormat fmt = new CLImageFormat(pChannelOrder,
            pChannelDataType);

    return mCLContext.createImage2D(Usage.Input, fmt, Nx, Ny);

  }

  public CLImage2D createImage2D(final int Nx, final int Ny) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLImageFormat fmt = new CLImageFormat(
            CLImageFormat.ChannelOrder.R,
            CLImageFormat.ChannelDataType.SignedInt16);

    return mCLContext.createImage2D(Usage.Input, fmt, Nx, Ny);

  }

  public CLImage3D createShortImage3D(final long Nx, final long Ny,
                                      final long Nz) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLImageFormat fmt = new CLImageFormat(
            CLImageFormat.ChannelOrder.R,
            CLImageFormat.ChannelDataType.SignedInt16);

    return mCLContext.createImage3D(Usage.InputOutput, fmt, Nx, Ny, Nz);

  }

  public CLImage3D createGenericImage3D(final long Nx, final long Ny,
                                        final long Nz, ChannelOrder pChannelOrder,
                                        ChannelDataType pChannelDataType) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final CLImageFormat fmt = new CLImageFormat(pChannelOrder,
            pChannelDataType);

    return mCLContext.createImage3D(Usage.InputOutput, fmt, Nx, Ny, Nz);

  }

  public CLBuffer<Float> createInputFloatBuffer(final long N) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createFloatBuffer(Usage.InputOutput, N);
  }

  public CLBuffer<Short> createInputShortBuffer(final long N) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createShortBuffer(Usage.Input, N);
  }

  public CLBuffer<Float> createOutputFloatBuffer(final long N) {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;
    return mCLContext.createFloatBuffer(Usage.InputOutput, N);
  }

  public CLBuffer<Short> createOutputShortBuffer(final long N) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createShortBuffer(Usage.Output, N);
  }

  public CLBuffer<Integer> createOutputIntBuffer(final long N) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createIntBuffer(Usage.Output, N);
  }

  public CLBuffer<Integer> createInputOutputIntBuffer(final long N) {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createIntBuffer(Usage.InputOutput, N);
  }

  public CLBuffer<Byte> createOutputByteBuffer(final long N) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createByteBuffer(Usage.Output, N);
  }

  public CLBuffer<Byte> createInputOutputByteBuffer(final long N) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createByteBuffer(Usage.InputOutput, N);
  }

  public CLEvent writeFloatBuffer(final CLBuffer<Float> pCLBuffer,
                                  final FloatBuffer pBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final Pointer<Float> ptr = Pointer.pointerToFloats(pBuffer);

    return pCLBuffer.write(mCLQueue, ptr, true);

  }

  public CLEvent writeShortBuffer(final CLBuffer<Short> pCLBuffer,
                                  final ShortBuffer pBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final Pointer<Short> ptr = Pointer.pointerToShorts(pBuffer);

    return pCLBuffer.write(mCLQueue, ptr, true);

  }

  public CLEvent writeByteBuffer(final CLBuffer<Byte> pCLBuffer,
                                 final ByteBuffer pBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final Pointer<Byte> ptr = Pointer.pointerToBytes(pBuffer);

    return pCLBuffer.write(mCLQueue, ptr, true);

  }

  public FloatBuffer readFloatBuffer(final CLBuffer<Float> pCLBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
            .getFloatBuffer();

  }

  public ShortBuffer readShortBuffer(final CLBuffer<Short> pCLBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
            .getShortBuffer();

  }

  public ByteBuffer readByteBuffer(final CLBuffer<Byte> pCLBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
            .getByteBuffer();

  }

  public ByteBuffer readIntBufferAsByte(final CLBuffer<Integer> pCLBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return pCLBuffer.read(mCLQueue, 0, pCLBuffer.getElementCount())
            .getByteBuffer();

  }

  public void copyCLBufferToPointer(final CLBuffer<Integer> pCLBuffer,
                                    Pointer<Integer> pPointer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLBuffer
            .read(mCLQueue, 0, pCLBuffer.getElementCount(), pPointer, true);

  }

  public CLEvent writeShortImage(final CLImage3D img,
                                 final ShortBuffer pShortBuffer) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    if (img.getWidth() * img.getHeight() * img.getDepth() != pShortBuffer
            .capacity()) {

      System.err.println("image and buffer sizes dont align!");
      return null;
    }

    return img.write(mCLQueue, 0, 0, 0, img.getWidth(), img.getHeight(),
            img.getDepth(), 0, 0, pShortBuffer, true);

  }

  public CLEvent writeImage(final CLImage3D pCLImage3D,
                            final ByteBuffer pByteBuffer) {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return pCLImage3D.write(mCLQueue,

            0, 0, 0, pCLImage3D.getWidth(), pCLImage3D.getHeight(),
            pCLImage3D.getDepth(), 0, 0, pByteBuffer, true);
  }

  public ArrayList<CLEvent> writeImagePerPlane(final CLImage3D img,
                                               final FragmentedMemoryInterface pFragmentedMemoryInterface) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final ArrayList<CLEvent> lEventList = new ArrayList<CLEvent>();

    int i = 0;
    for (final ContiguousMemoryInterface lMemory : pFragmentedMemoryInterface) {
      final Pointer<Byte> lBridJPointer = lMemory
              .getBridJPointer(Byte.class);
      final CLEvent lEvent = JavaCLUtils.writeImage3D(img, mCLQueue,
              lBridJPointer, 0, 0, i++, img.getWidth(), img.getHeight(),
              1, false);
      lEventList.add(lEvent);
    }

    for (final CLEvent lEvent : lEventList)
      lEvent.waitFor();

    return lEventList;
  }

  public CLEvent writeImage(final CLImage3D img,
                            final ContiguousMemoryInterface pContiguousMemoryInterface) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final Pointer<Byte> lBridJPointer = pContiguousMemoryInterface
            .getBridJPointer(Byte.class);
    return JavaCLUtils.writeImage3D(img, mCLQueue, lBridJPointer, 0, 0, 0,
            img.getWidth(), img.getHeight(), img.getDepth(), true);

  }

  public CLEvent writeImage(final CLImage2D img, final Buffer pBuffer)

  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return img.write(mCLQueue, 0, 0, img.getWidth(), img.getHeight(), 0,
            pBuffer, true);
  }

  public CLEvent writeImage(final CLImage2D img,
                            final ContiguousMemoryInterface pContiguousMemoryInterface) {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    final Pointer<Byte> lBridJPointer = pContiguousMemoryInterface
            .getBridJPointer(Byte.class);
    return img.write(mCLQueue, 0, 0, img.getWidth(), img.getHeight(), 0,
            lBridJPointer, true);
  }

  @Override
  public void close() {
    try {

      if (mCLKernelList != null)
        for (CLKernel lCLKernel : mCLKernelList) {
          lCLKernel.release();
          lCLKernel = null;
        }

      if (mCLProgram != null) {
        mCLProgram.release();
        mCLProgram = null;
      }

      if (mCLQueue != null) {
        mCLQueue.release();
        mCLQueue = null;
      }

      if (mCLContext != null) {
        mCLContext.release();
        mCLContext = null;
      }

      if (mCLDevice != null) {
        mCLDevice.release();
        mCLDevice = null;
      }

    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

}
