package clearvolume.renderer.opencl;

import java.io.File;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Scanner;

import clearcl.ClearCL;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.ClearCLQueue;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearvolume.ClearVolumeCloseable;
import coremem.ContiguousMemoryInterface;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;

public class OpenCLDevice implements ClearVolumeCloseable
{
  private ClearCL mClearCL;
  public ClearCLContext mCLContext;
  public ClearCLProgram mCLProgram;
  public ClearCLDevice mCLDevice;
  public ClearCLQueue mCLQueue;
  public ByteOrder mCLContextByteOrder;

  public ArrayList<ClearCLKernel> mCLKernelList = new ArrayList<>();

  public boolean initCL()
  {
    return initCL(false);
  }

  public boolean initCL(final boolean useExistingOpenGLContext)
  {
    mClearCL = new ClearCL(ClearCLBackends.getBestBackend());

    mCLDevice = mClearCL.getFastestGPUDevice();

    mCLContext = mCLDevice.createContext();

    mCLQueue = mCLContext.getDefaultQueue();

    return (mCLDevice != null && mCLContext != null
            && mCLQueue != null);
  }

  public ClearCLContext getContext()
  {
    return mCLContext;
  }

  public ClearCLQueue getQueue()
  {
    return mCLQueue;
  }

  public void printInfo()
  {

    System.out.printf("Device name: \t %s \n", mCLDevice);

  }

  public int getKernelIndex(ClearCLKernel pCLKernel)
  {
    return mCLKernelList.indexOf(pCLKernel);
  }

  public ClearCLKernel compileKernel(final Class<?> pRootClass,
                                     final String pRessourceName,
                                     final String pKernelName)
  {

    try
    {
      mCLProgram = mCLContext.createProgram(pRootClass,pRessourceName);
      mCLProgram.addBuildOptionAllMathOpt();
      mCLProgram.buildAndLog();
    }
    catch (final Exception e)
    {
      System.err.println("couldn't create program from " + pRessourceName);
      e.printStackTrace();
      return null;
    }

    ClearCLKernel lNewKernel = null;
    try
    {
      lNewKernel = mCLProgram.getKernel(pKernelName);
      mCLKernelList.add(lNewKernel);
    }
    catch (final Exception e)
    {
      System.err.println("couldn't create kernel '" + pKernelName
                         + "'");
      e.printStackTrace();
    }

    return lNewKernel;
  }

  private static String getText(URL url) throws Exception
  {
    File f = new File(url.getFile());
    Scanner lScanner = new Scanner(f);
    lScanner.useDelimiter("\\Z");
    String content = lScanner.next();
    lScanner.close();
    return content;
  }

  public void setArgs(final ClearCLKernel pCLKernel, Object... args)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLKernel.setArguments(args);
  }

  public void setArgs(final int pKernelIndex, Object... args)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    mCLKernelList.get(pKernelIndex).setArguments(args);
  }

  public void run(final int pKernelIndex,
                  final int mNx,
                  final int mNy)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    ClearCLKernel lKernel = mCLKernelList.get(pKernelIndex);
    lKernel.setGlobalSizes(mNx, mNy);
    lKernel.run();
  }

  public void run(final int pKernelIndex,
                  final int mNx,
                  final int mNy,
                  final int mNz)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    ClearCLKernel lKernel = mCLKernelList.get(pKernelIndex);
    lKernel.setGlobalSizes(mNx, mNy, mNz);
  }

  public void run(final int pKernelIndex,
                  final int mNx,
                  final int mNy,
                  final int mNz,
                  final int mNxLoc,
                  final int mNyLoc,
                  final int mNzLoc)

  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    ClearCLKernel lKernel = mCLKernelList.get(pKernelIndex);
    lKernel.setGlobalSizes(mNx, mNy, mNz);
    lKernel.setLocalSizes(mNxLoc, mNyLoc, mNzLoc);

  }

  public void run(final ClearCLKernel pCLKernel, final int mNx)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLKernel.setGlobalSizes(mNx);
    pCLKernel.run();
  }

  public void run(final ClearCLKernel pCLKernel,
                  final int mNx,
                  final int mNy)
  {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLKernel.setGlobalSizes(mNx, mNy);
    pCLKernel.run();
  }

  public void run(final ClearCLKernel pCLKernel,
                  final int mNx,
                  final int mNy,
                  final int mNz)
  {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLKernel.setGlobalSizes(mNx, mNy, mNz);

  }

  public void run(final ClearCLKernel pCLKernel,
                  final int mNx,
                  final int mNy,
                  final int mNz,
                  final int mNxLoc,
                  final int mNyLoc,
                  final int mNzLoc)

  {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLKernel.setGlobalSizes(mNx, mNy, mNz);
    pCLKernel.setLocalSizes(mNxLoc, mNyLoc, mNzLoc);

  }

  public ClearCLImage createGenericImage2D(final long Nx,
                                           final long Ny,
                                           ImageChannelOrder pChannelOrder,
                                           ImageChannelDataType pChannelDataType)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createImage(pChannelOrder,
                                  pChannelDataType,
                                  Nx,
                                  Ny);

  }

  public ClearCLImage createGenericImage3D(final long Nx,
                                           final long Ny,
                                           final long Nz,
                                           ImageChannelOrder pChannelOrder,
                                           ImageChannelDataType pChannelDataType)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createImage(pChannelOrder,
                                  pChannelDataType,
                                  Nx,
                                  Ny,
                                  Nz);

  }

  public ClearCLBuffer createInputFloatBuffer(final long N)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createBuffer(NativeTypeEnum.Float, N);
  }

  public ClearCLBuffer createOutputFloatBuffer(final long N)
  {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;
    return mCLContext.createBuffer(NativeTypeEnum.Float, N);
  }

  public ClearCLBuffer createOutputIntBuffer(final long N)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createBuffer(NativeTypeEnum.Int, N);
  }

  public ClearCLBuffer createInputOutputIntBuffer(final long N)
  {

    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return null;

    return mCLContext.createBuffer(NativeTypeEnum.Int, N);
  }

  public void writeFloatBuffer(final ClearCLBuffer pCLBuffer,
                               final FloatBuffer pBuffer)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLBuffer.readFrom(pBuffer, true);
  }

  public void copyCLBufferToPointer(final ClearCLBuffer pCLBuffer,
                                    OffHeapMemory pPointer)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    pCLBuffer.writeTo(pPointer, true);

  }

  public void writeImage(final ClearCLImage img,
                         final ContiguousMemoryInterface pContiguousMemoryInterface)
  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    img.readFrom(pContiguousMemoryInterface, true);
  }

  public void writeImage(final ClearCLImage img, final Buffer pBuffer)

  {
    if (mCLDevice == null || mCLContext == null || mCLQueue == null)
      return;

    img.readFrom(pBuffer, true);
  }

  @Override
  public void close()
  {
    try
    {

      if (mCLKernelList != null)
        for (ClearCLKernel lCLKernel : mCLKernelList)
        {
          lCLKernel.close();
          lCLKernel = null;
        }

      if (mCLProgram != null)
      {
        mCLProgram.close();
        mCLProgram = null;
      }

      if (mCLQueue != null)
      {
        mCLQueue.close();
        mCLQueue = null;
      }

      if (mCLContext != null)
      {
        mCLContext.close();
        mCLContext = null;
      }

      if (mCLDevice != null)
      {
        mCLDevice.close();
        mCLDevice = null;
      }

    }
    catch (final Throwable e)
    {
      e.printStackTrace();
    }
  }

}
