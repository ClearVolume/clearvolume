package clearvolume.renderer.opencl;

import static java.lang.Math.max;
import static java.lang.Math.pow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.bridj.Pointer;

import com.jogamp.opengl.GLEventListener;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearvolume.exceptions.ClearVolumeMemoryException;
import clearvolume.exceptions.ClearVolumeUnsupportdDataTypeException;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import coremem.ContiguousMemoryInterface;
import coremem.enums.NativeTypeEnum;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.offheap.OffHeapMemory;

public class OpenCLVolumeRenderer extends ClearGLVolumeRenderer
                                  implements GLEventListener
{
  private OpenCLDevice mCLDevice;

  private ClearCLBuffer[] mCLRenderBuffers;
  private ClearCLImage[] mCLVolumeImages;
  private ClearCLImage[] mCLTransferFunctionImages;

  private ClearCLBuffer mCLInvModelViewBuffer, mCLInvProjectionBuffer;

  private ClearCLKernel mCurrentRenderKernel,
      mMaxProjectionRenderKernel, mIsoSurfaceRenderKernel,
      mClearKernel;

  private OffHeapMemory mTransferBuffer;

  public OpenCLVolumeRenderer(final String pWindowName,
                              final int pWindowWidth,
                              final int pWindowHeight)
  {
    super("[OpenCL] " + pWindowName, pWindowWidth, pWindowHeight);

  }

  public OpenCLVolumeRenderer(final String pWindowName,
                              final int pWindowWidth,
                              final int pWindowHeight,
                              final NativeTypeEnum pNativeTypeEnum)
  {
    super("[OpenCL] " + pWindowName,
          pWindowWidth,
          pWindowHeight,
          pNativeTypeEnum);

  }

  public OpenCLVolumeRenderer(final String pWindowName,
                              final int pWindowWidth,
                              final int pWindowHeight,
                              final NativeTypeEnum pNativeTypeEnum,
                              final int pMaxTextureWidth,
                              final int pMaxTextureHeight)
  {
    super("[OpenCL] " + pWindowName,
          pWindowWidth,
          pWindowHeight,
          pNativeTypeEnum,
          pMaxTextureWidth,
          pMaxTextureHeight);

  }

  @SuppressWarnings("unchecked")
  public OpenCLVolumeRenderer(final String pWindowName,
                              final Integer pWindowWidth,
                              final Integer pWindowHeight,
                              final String pNativeTypeEnum,
                              final Integer pMaxTextureWidth,
                              final Integer pMaxTextureHeight,
                              final Integer pNumberOfRenderLayers,
                              final Boolean pUseInCanvas)
  {
    this(pWindowName,
         pWindowWidth,
         pWindowHeight,
         NativeTypeEnum.valueOf(pNativeTypeEnum),
         pMaxTextureWidth,
         pMaxTextureHeight,
         pNumberOfRenderLayers,
         pUseInCanvas);
  }

  @SuppressWarnings("unchecked")
  public OpenCLVolumeRenderer(final String pWindowName,
                              final Integer pWindowWidth,
                              final Integer pWindowHeight,
                              final NativeTypeEnum pNativeTypeEnum,
                              final Integer pMaxTextureWidth,
                              final Integer pMaxTextureHeight,
                              final Integer pNumberOfRenderLayers,
                              final Boolean useInCanvas)
  {

    super("[OpenCL] " + pWindowName,
          pWindowWidth,
          pWindowHeight,
          pNativeTypeEnum,
          pMaxTextureWidth,
          pMaxTextureHeight,
          pNumberOfRenderLayers,
          useInCanvas);

    mCLRenderBuffers = new ClearCLBuffer[pNumberOfRenderLayers];
    mCLVolumeImages = new ClearCLImage[pNumberOfRenderLayers];
    mCLTransferFunctionImages =
                              new ClearCLImage[pNumberOfRenderLayers];

  }

  @Override
  protected boolean initVolumeRenderer()
  {
    mCLDevice = new OpenCLDevice();
    mCLDevice.initCL(sBadTrack);
    
    if(mCLDevice.isCPU())
      mAdaptiveLODController.setNumberOfPasses(mAdaptiveLODController.getMaxNumberOfPasses());
    
    mCLDevice.printInfo();
    mMaxProjectionRenderKernel =
                               mCLDevice.compileKernel(OpenCLVolumeRenderer.class,
                                                       "kernels/VolumeRender.cl",
                                                       "maxproj_render");
    mClearKernel = mCLDevice.compileKernel(OpenCLVolumeRenderer.class,
                                           "kernels/VolumeRender.cl",
                                           "clearbuffer");

    mIsoSurfaceRenderKernel =
                            mCLDevice.compileKernel(OpenCLVolumeRenderer.class,
                                                    "kernels/VolumeRender.cl",
                                                    "isosurface_render");

    mCLInvModelViewBuffer = mCLDevice.createInputFloatBuffer(16);
    mCLInvProjectionBuffer = mCLDevice.createInputFloatBuffer(16);

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
      prepareVolumeDataArray(i, null);

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
      prepareTransferFunctionArray(i);

    return true;
  }

  @Override
  protected void notifyChangeOfTextureDimensions()
  {
    final int lRenderBufferSize =
                                getRenderHeight() * getRenderWidth();

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
    {
      if (mCLRenderBuffers[i] != null)
        mCLRenderBuffers[i].close();
      mCLRenderBuffers[i] =
                          mCLDevice.createInputOutputIntBuffer(lRenderBufferSize);
    }
  }

  private void prepareVolumeDataArray(final int pRenderLayerIndex,
                                      final FragmentedMemoryInterface pVolumeDataBuffer)
  {
    synchronized (getSetVolumeDataBufferLock(pRenderLayerIndex))
    {

      FragmentedMemoryInterface lVolumeDataBuffer = pVolumeDataBuffer;
      if (lVolumeDataBuffer == null)
        lVolumeDataBuffer = getVolumeDataBuffer(pRenderLayerIndex);
      if (lVolumeDataBuffer == null)
        return;

      final long lWidth = getVolumeSizeX(pRenderLayerIndex);
      final long lHeight = getVolumeSizeY(pRenderLayerIndex);
      final long lDepth = getVolumeSizeZ(pRenderLayerIndex);

      final long lBytePerVoxel = getBytesPerVoxel();

      if (lVolumeDataBuffer.getSizeInBytes() != (lWidth * lHeight
                                                 * lDepth
                                                 * lBytePerVoxel))
      {
        throw new ClearVolumeMemoryException("Volume buffer has wrong size!");
      }

      if (getNativeType() == NativeTypeEnum.UnsignedByte)

        mCLVolumeImages[pRenderLayerIndex] =
                                           mCLDevice.createGenericImage3D(lWidth,
                                                                          lHeight,
                                                                          lDepth,
                                                                          ImageChannelDataType.UnsignedNormalizedInt8);
      else if (getNativeType() == NativeTypeEnum.UnsignedShort)
        mCLVolumeImages[pRenderLayerIndex] =
                                           mCLDevice.createGenericImage3D(lWidth,
                                                                          lHeight,
                                                                          lDepth,
                                                                          ImageChannelDataType.UnsignedNormalizedInt16);
      else if (getNativeType() == NativeTypeEnum.Byte)
        mCLVolumeImages[pRenderLayerIndex] =
                                           mCLDevice.createGenericImage3D(lWidth,
                                                                          lHeight,
                                                                          lDepth,
                                                                          ImageChannelDataType.UnsignedNormalizedInt8);
      else if (getNativeType() == NativeTypeEnum.Short)
        mCLVolumeImages[pRenderLayerIndex] =
                                           mCLDevice.createGenericImage3D(lWidth,
                                                                          lHeight,
                                                                          lDepth,
                                                                          ImageChannelDataType.UnsignedNormalizedInt16);
      else
        throw new ClearVolumeUnsupportdDataTypeException("Received an unsupported data type: "
                                                         + getNativeType());

      fillWithByteBuffer(mCLVolumeImages[pRenderLayerIndex],
                         lVolumeDataBuffer);

    }
  }

  private void prepareTransferFunctionArray(final int pRenderLayerIndex)
  {

    final float[] lTransferFunctionArray =
                                         getTransferFunction(pRenderLayerIndex).getArray();

    /*
     * System.out.println("render layer %" + pRenderLayerIndex + " -> " +
     * Arrays.toString(lTransferFunctionArray));/*
     */

    final int lTransferFunctionArrayLength =
                                           lTransferFunctionArray.length;

    final int lNeededWidth = lTransferFunctionArrayLength / 4;
    if (mCLTransferFunctionImages[pRenderLayerIndex] == null
        || mCLTransferFunctionImages[pRenderLayerIndex].getWidth() != lNeededWidth)
    {
      if (mCLTransferFunctionImages[pRenderLayerIndex] != null)
        mCLTransferFunctionImages[pRenderLayerIndex].close();

      mCLTransferFunctionImages[pRenderLayerIndex] =
                                                   mCLDevice.createGenericImage2D(lNeededWidth,
                                                                                  1,
                                                                                  ImageChannelOrder.RGBA,
                                                                                  ImageChannelDataType.Float);
    }

    mCLDevice.writeImage(mCLTransferFunctionImages[pRenderLayerIndex],
                         FloatBuffer.wrap(lTransferFunctionArray));

  }

  @Override
  protected boolean[] renderVolume(final float[] pInvModelViewMatrix,
                                   final float[] pInvProjectionMatrix)
  {

    doCaptureBuffersIfNeeded();

    // System.out.println("render");
    try
    {
      mCLDevice.writeFloatBuffer(mCLInvModelViewBuffer,
                                 FloatBuffer.wrap(pInvModelViewMatrix));

      mCLDevice.writeFloatBuffer(mCLInvProjectionBuffer,
                                 FloatBuffer.wrap(pInvProjectionMatrix));

      return updateBufferAndRunKernel();
    }
    catch (final Throwable e)
    {
      System.err.println(e.getLocalizedMessage());
      return null;
    }

  }

  private void doCaptureBuffersIfNeeded()
  {
    if (mVolumeCaptureFlag)
    {
      for (int l = 0; l < getNumberOfRenderLayers(); l++)
      {
        final ByteBuffer lCaptureBuffer;

        synchronized (getSetVolumeDataBufferLock(l))
        {
          lCaptureBuffer = ByteBuffer
                                     .allocateDirect((int) (getBytesPerVoxel()
                                                            * getVolumeSizeX(l)
                                                            * getVolumeSizeY(l)
                                                            * getVolumeSizeZ(l)))
                                     .order(ByteOrder.nativeOrder());

          mCLVolumeImages[getCurrentRenderLayerIndex()].writeTo(lCaptureBuffer,
                                                                true);
        }

        notifyVolumeCaptureListeners(lCaptureBuffer,
                                     getNativeType(),
                                     getVolumeSizeX(l),
                                     getVolumeSizeY(l),
                                     getVolumeSizeZ(l),
                                     getVoxelSizeX(l),
                                     getVoxelSizeY(l),
                                     getVoxelSizeZ(l));
      }

      mVolumeCaptureFlag = false;
    }
  }

  private boolean[] updateBufferAndRunKernel()
  {
    final boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

    lUpdated[0] = true;

    boolean lAnyVolumeDataUpdated = false;

    if (isVolumeDataUpdateAllowed())
    {
      for (int lLayerIndex =
                           0; lLayerIndex < getNumberOfRenderLayers(); lLayerIndex++)
      {
        synchronized (getSetVolumeDataBufferLock(lLayerIndex))
        {
          final FragmentedMemoryInterface lVolumeDataBuffer =
                                                            getVolumeDataBuffer(lLayerIndex);

          if (lVolumeDataBuffer != null)
          {

            clearVolumeDataBufferReference(lLayerIndex);

            if (haveVolumeDimensionsChanged(lLayerIndex)
                || mCLVolumeImages[lLayerIndex] == null)
            {
              if (mCLVolumeImages[lLayerIndex] != null)
              {

                mCLVolumeImages[lLayerIndex].close();
              }

              prepareVolumeDataArray(lLayerIndex, lVolumeDataBuffer);
            }
            else
            {
              fillWithByteBuffer(mCLVolumeImages[lLayerIndex],
                                 lVolumeDataBuffer);

            }

            notifyCompletionOfDataBufferCopy(lLayerIndex);
            lAnyVolumeDataUpdated |= true;

          }

        }
      }

      clearVolumeDimensionsChanged();
    }

    if (lAnyVolumeDataUpdated
        || haveVolumeRenderingParametersChanged()
        || getAdaptiveLODController().isKernelRunNeeded())
    {
      for (int i = 0; i < getNumberOfRenderLayers(); i++)
      {
        if (mCLVolumeImages[i] != null)
        {
          runKernel(i);
          lUpdated[i] = true;
        }
      }
    }

    return lUpdated;
  }

  private void fillWithByteBuffer(final ClearCLImage clImage3D,
                                  final FragmentedMemoryInterface pVolumeDataBuffer)
  {
    if (pVolumeDataBuffer.getNumberOfFragments() == 1)
    {
      final ContiguousMemoryInterface lContiguousBuffer =
                                                        pVolumeDataBuffer.get(0);
      mCLDevice.writeImage(clImage3D, lContiguousBuffer);
    }
    else
      throw new UnsupportedOperationException("NOT SUPPORTED IN THIS VERSION");
  }

  private void runKernel(final int pRenderLayerIndex)
  {
    // System.out.println("kernel");
    // System.out.println(mCLVolumeImages[i].getHeight());
    if (isLayerVisible(pRenderLayerIndex))
    {
      prepareTransferFunctionArray(pRenderLayerIndex);

      final int lMaxNumberSteps = getMaxSteps(pRenderLayerIndex);

      final int lNumberOfPasses =
                                getAdaptiveLODController().getNumberOfPasses();

      final int lPassIndex =
                           getAdaptiveLODController().getPassIndex();
      final boolean lActive = getAdaptiveLODController().isActive();

      int lMaxSteps = lMaxNumberSteps;
      float lDithering = 0;
      float lPhase = 0;
      int lClear = 0;

      switch (getRenderAlgorithm(pRenderLayerIndex))
      {
      case MaxProjection:
        mCurrentRenderKernel = mMaxProjectionRenderKernel;
        lMaxSteps = max(16, lMaxNumberSteps / lNumberOfPasses);
        lDithering = getDithering(pRenderLayerIndex)
                     * (1.0f * (lNumberOfPasses - lPassIndex)
                        / lNumberOfPasses);
        lPhase = getAdaptiveLODController().getPhase();
        lClear = (lPassIndex == 0) ? 0 : 1;
        float[] lClipBox = getClipBox();

        mCLDevice.setArgs(mCurrentRenderKernel,
                          mCLRenderBuffers[pRenderLayerIndex],
                          getRenderWidth(),
                          getRenderHeight(),
                          (float) getBrightness(pRenderLayerIndex),
                          (float) getTransferRangeMin(pRenderLayerIndex),
                          (float) getTransferRangeMax(pRenderLayerIndex),
                          (float) getGamma(pRenderLayerIndex),
                          lMaxSteps,
                          lDithering,
                          lPhase,
                          lClear,
                          lClipBox[0],
                          lClipBox[1],
                          lClipBox[2],
                          lClipBox[3],
                          lClipBox[4],
                          lClipBox[5],
                          mCLTransferFunctionImages[pRenderLayerIndex],
                          mCLInvProjectionBuffer,
                          mCLInvModelViewBuffer,
                          mCLVolumeImages[pRenderLayerIndex]);
        break;
      case IsoSurface:
        mCurrentRenderKernel = mIsoSurfaceRenderKernel;

        lClipBox = getClipBox();

        lMaxSteps = max(16,
                        (lMaxNumberSteps * (1 + lPassIndex))
                            / (2 * lNumberOfPasses));
        lDithering = (float) pow(getDithering(pRenderLayerIndex)
                                 * (1.0f
                                    * (lNumberOfPasses - lPassIndex)
                                    / lNumberOfPasses),
                                 2);
        lPhase = getAdaptiveLODController().getPhase();
        lClear = (lPassIndex == lNumberOfPasses - 1)
                 || (lPassIndex == 0) ? 0 : 1;

        final float[] lLightVector = getLightVector();

        mCLDevice.setArgs(mCurrentRenderKernel,
                          mCLRenderBuffers[pRenderLayerIndex],
                          getRenderWidth(),
                          getRenderHeight(),
                          (float) getBrightness(pRenderLayerIndex),
                          (float) getTransferRangeMin(pRenderLayerIndex),
                          (float) getTransferRangeMax(pRenderLayerIndex),
                          (float) getGamma(pRenderLayerIndex),
                          lMaxSteps,
                          lDithering,
                          lPhase,
                          lClear,
                          lLightVector[0],
                          lLightVector[1],
                          lLightVector[2],
                          lClipBox[0],
                          lClipBox[1],
                          lClipBox[2],
                          lClipBox[3],
                          lClipBox[4],
                          lClipBox[5],
                          mCLTransferFunctionImages[pRenderLayerIndex],
                          mCLInvProjectionBuffer,
                          mCLInvModelViewBuffer,
                          mCLVolumeImages[pRenderLayerIndex]);
        break;
      }

      mCLDevice.run(mCurrentRenderKernel,
                    getRenderWidth(),
                    getRenderHeight());

    }
    else
    {
      mCLDevice.setArgs(mClearKernel,
                        mCLRenderBuffers[pRenderLayerIndex],
                        getRenderWidth(),
                        getRenderHeight());

      mCLDevice.run(mClearKernel,
                    getRenderWidth(),
                    getRenderHeight());

    }

    long lSizeInBytes =
                      mCLRenderBuffers[pRenderLayerIndex].getSizeInBytes();
    if (mTransferBuffer == null
        || mTransferBuffer.getSizeInBytes() != lSizeInBytes)
    {
      if (mTransferBuffer != null)
        mTransferBuffer.free();
      mTransferBuffer = OffHeapMemory.allocateBytes(lSizeInBytes);
    }

    mCLDevice.copyCLBufferToPointer(mCLRenderBuffers[pRenderLayerIndex],
                                    mTransferBuffer);
    copyBufferToTexture(pRenderLayerIndex,
                        mTransferBuffer.getByteBuffer());

  }

  @Override
  public void close()
  {
    mDisplayReentrantLock.lock();
    try
    {
      super.close();
      if (mCLDevice != null)
        mCLDevice.close();
    }
    finally
    {
      if (mDisplayReentrantLock.isHeldByCurrentThread())
        mDisplayReentrantLock.unlock();
    }

  }

}
