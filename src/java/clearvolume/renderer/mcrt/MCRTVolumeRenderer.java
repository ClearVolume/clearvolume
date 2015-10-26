package clearvolume.renderer.mcrt;

import clearvolume.exceptions.ClearVolumeUnsupportdDataTypeException;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.cleargl.overlay.o2d.HistogramOverlay;
import clearvolume.renderer.cleargl.overlay.o3d.DriftOverlay;
import clearvolume.renderer.opencl.OpenCLDevice;
import clearvolume.renderer.processors.OpenCLProcessor;
import clearvolume.renderer.processors.ProcessorInterface;
import clearvolume.renderer.processors.impl.OpenCLCenterMass;
import clearvolume.renderer.processors.impl.OpenCLDeconvolutionLR;
import clearvolume.renderer.processors.impl.OpenCLDenoise;
import clearvolume.renderer.processors.impl.OpenCLHistogram;
import com.jogamp.opengl.GLEventListener;
import com.nativelibs4java.opencl.*;
import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.types.NativeTypeEnum;
import org.bridj.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static java.lang.Math.max;

public class MCRTVolumeRenderer extends ClearGLVolumeRenderer implements
        GLEventListener {
  private OpenCLDevice mCLDevice;

  private CLBuffer<Integer>[] mCLRenderBuffers;
  private CLBuffer<Float>[] mHDRBuffers;
  private CLImage3D[] mCLVolumeImages;
  private CLImage2D[] mCLTransferFunctionImages;

  private CLBuffer<Float> mCLInvModelViewBuffer,
          mCLInvProjectionBuffer;

  private CLKernel mCurrentRenderKernel, mMCRTRenderKernel,
          mClearKernel, mToneMappingKernel, mAvgLKernel;

  private Pointer<Integer> mTransferBuffer;

  public MCRTVolumeRenderer(final String pWindowName,
                            final int pWindowWidth,
                            final int pWindowHeight) {
    super("[OpenCL] " + pWindowName, pWindowWidth, pWindowHeight);

  }

  public MCRTVolumeRenderer(final String pWindowName,
                            final int pWindowWidth,
                            final int pWindowHeight,
                            final NativeTypeEnum pNativeTypeEnum) {
    super("[OpenCL] " + pWindowName,
            pWindowWidth,
            pWindowHeight,
            pNativeTypeEnum);

  }

  public MCRTVolumeRenderer(final String pWindowName,
                            final int pWindowWidth,
                            final int pWindowHeight,
                            final NativeTypeEnum pNativeTypeEnum,
                            final int pMaxTextureWidth,
                            final int pMaxTextureHeight) {
    super("[OpenCL] " + pWindowName,
            pWindowWidth,
            pWindowHeight,
            pNativeTypeEnum,
            pMaxTextureWidth,
            pMaxTextureHeight);

  }

  @SuppressWarnings("unchecked")
  public MCRTVolumeRenderer(final String pWindowName,
                            final Integer pWindowWidth,
                            final Integer pWindowHeight,
                            final String pNativeTypeEnum,
                            final Integer pMaxTextureWidth,
                            final Integer pMaxTextureHeight,
                            final Integer pNumberOfRenderLayers,
                            final Boolean pUseInCanvas) {
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
  public MCRTVolumeRenderer(final String pWindowName,
                            final Integer pWindowWidth,
                            final Integer pWindowHeight,
                            final NativeTypeEnum pNativeTypeEnum,
                            final Integer pMaxTextureWidth,
                            final Integer pMaxTextureHeight,
                            final Integer pNumberOfRenderLayers,
                            final Boolean useInCanvas) {

    super("[OpenCL] " + pWindowName,
            pWindowWidth,
            pWindowHeight,
            pNativeTypeEnum,
            pMaxTextureWidth,
            pMaxTextureHeight,
            pNumberOfRenderLayers,
            useInCanvas);

    mCLRenderBuffers = new CLBuffer[pNumberOfRenderLayers];
    mHDRBuffers = new CLBuffer[pNumberOfRenderLayers];
    mCLVolumeImages = new CLImage3D[pNumberOfRenderLayers];
    mCLTransferFunctionImages = new CLImage2D[pNumberOfRenderLayers];

    final OpenCLHistogram lHistoProcessor = new OpenCLHistogram();
    addProcessor(lHistoProcessor);

    final HistogramOverlay lHistogramOverlay = new HistogramOverlay(lHistoProcessor);
    addOverlay(lHistogramOverlay);

    lHistogramOverlay.setDisplayed(false);

    final OpenCLDenoise lOpenCLDenoise = new OpenCLDenoise();
    addProcessor(lOpenCLDenoise);

    final OpenCLDeconvolutionLR lOpenCLDeconvolutionLR = new OpenCLDeconvolutionLR();
    addProcessor(lOpenCLDeconvolutionLR);

    final DriftOverlay lDriftOverlay = new DriftOverlay();
    addOverlay(lDriftOverlay);
    final OpenCLCenterMass lOpenCLCenterMass = new OpenCLCenterMass();
    addProcessor(lOpenCLCenterMass);
    lOpenCLCenterMass.addResultListener(lDriftOverlay);
    lDriftOverlay.setDisplayed(false);
    lOpenCLCenterMass.setActive(false);

  }

  @Override
  protected boolean initVolumeRenderer() {
    mCLDevice = new OpenCLDevice();

    mCLDevice.initCL();
    mCLDevice.printInfo();
    mMCRTRenderKernel = mCLDevice.compileKernel(
            MCRTVolumeRenderer.class.getResource("kernels/MCRTRender.cl"),
            "mcrt_render");

    mClearKernel = mCLDevice.compileKernel(
            MCRTVolumeRenderer.class.getResource("kernels/MCRTRender.cl"),
            "clearbuffer");

    mToneMappingKernel = mCLDevice.compileKernel(
            MCRTVolumeRenderer.class.getResource("kernels/MCRTRender.cl"),
            "tonemapping");

    mAvgLKernel = mCLDevice.compileKernel(
            MCRTVolumeRenderer.class.getResource("kernels/MCRTRender.cl"),
            "AvgLuminance");

    for (final ProcessorInterface<?> lProcessor : mProcessorInterfacesMap.values())
      if (lProcessor.isCompatibleProcessor(getClass()))
        if (lProcessor instanceof OpenCLProcessor) {
          final OpenCLProcessor<?> lOpenCLProcessor = (OpenCLProcessor<?>) lProcessor;
          lOpenCLProcessor.setDevice(mCLDevice);
        }

    mCLInvModelViewBuffer = mCLDevice.createInputFloatBuffer(16);
    mCLInvProjectionBuffer = mCLDevice.createInputFloatBuffer(16);

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
      prepareVolumeDataArray(i, null);

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
      prepareTransferFunctionArray(i);

    return true;
  }

  @Override
  protected void notifyChangeOfTextureDimensions() {
    final int lRenderBufferSize = getRenderHeight() * getRenderWidth();

    for (int i = 0; i < getNumberOfRenderLayers(); i++) {
      if (mCLRenderBuffers[i] != null) {
        mCLRenderBuffers[i].release();
      }

      mCLRenderBuffers[i] = mCLDevice.createInputOutputIntBuffer(lRenderBufferSize * 4);

      if (mHDRBuffers[i] != null) {
        mHDRBuffers[i].release();
      }

      mHDRBuffers[i] = mCLDevice.createOutputFloatBuffer(lRenderBufferSize * 4);
    }
  }

  private void prepareVolumeDataArray(final int pRenderLayerIndex,
                                      final FragmentedMemoryInterface pVolumeDataBuffer) {
    synchronized (getSetVolumeDataBufferLock(pRenderLayerIndex)) {

      FragmentedMemoryInterface lVolumeDataBuffer = pVolumeDataBuffer;
      if (lVolumeDataBuffer == null)
        lVolumeDataBuffer = getVolumeDataBuffer(pRenderLayerIndex);
      if (lVolumeDataBuffer == null)
        return;

      final long lWidth = getVolumeSizeX();
      final long lHeight = getVolumeSizeY();
      final long lDepth = getVolumeSizeZ();

      if (getNativeType() == NativeTypeEnum.UnsignedByte) {
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(
                lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt8);
      }
      else if (getNativeType() == NativeTypeEnum.UnsignedShort) {
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(
                lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt16);
      }
      else if (getNativeType() == NativeTypeEnum.Byte) {
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(
                lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt8);
      }
      else if (getNativeType() == NativeTypeEnum.Short) {
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(
                lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt16);
      }
      else {
        throw new ClearVolumeUnsupportdDataTypeException("Received an unsupported data type: " + getNativeType());
      }

      fillWithByteBuffer(mCLVolumeImages[pRenderLayerIndex],
              lVolumeDataBuffer);

    }
  }

  private void prepareTransferFunctionArray(final int pRenderLayerIndex) {

    final float[] lTransferFunctionArray = getTransferFunction(pRenderLayerIndex).getArray();

    /*
     * System.out.println("render layer %" + pRenderLayerIndex + " -> " +
     * Arrays.toString(lTransferFunctionArray));/*
     */

    final int lTransferFunctionArrayLength = lTransferFunctionArray.length;

    final int lNeededWidth = lTransferFunctionArrayLength / 4;
    if (mCLTransferFunctionImages[pRenderLayerIndex] == null || mCLTransferFunctionImages[pRenderLayerIndex].getWidth() != lNeededWidth) {
      if (mCLTransferFunctionImages[pRenderLayerIndex] != null)
        mCLTransferFunctionImages[pRenderLayerIndex].release();

      mCLTransferFunctionImages[pRenderLayerIndex] = mCLDevice.createGenericImage2D(lNeededWidth,
              1,
              CLImageFormat.ChannelOrder.RGBA,
              CLImageFormat.ChannelDataType.Float);
    }

    mCLDevice.writeImage(mCLTransferFunctionImages[pRenderLayerIndex],
            FloatBuffer.wrap(lTransferFunctionArray));

  }

  @Override
  protected boolean[] renderVolume(final float[] pInvModelViewMatrix,
                                   final float[] pInvProjectionMatrix) {

    doCaptureBuffersIfNeeded();

    // System.out.println("render");
    mCLDevice.writeFloatBuffer(mCLInvModelViewBuffer,
            FloatBuffer.wrap(pInvModelViewMatrix));

    mCLDevice.writeFloatBuffer(mCLInvProjectionBuffer,
            FloatBuffer.wrap(pInvProjectionMatrix));

    return updateBufferAndRunKernel();
  }

  private void doCaptureBuffersIfNeeded() {
    if (mVolumeCaptureFlag) {

      final ByteBuffer[] lCaptureBuffers = new ByteBuffer[getNumberOfRenderLayers()];

      for (int i = 0; i < getNumberOfRenderLayers(); i++) {
        synchronized (getSetVolumeDataBufferLock(i)) {
          lCaptureBuffers[i] = ByteBuffer.allocateDirect(
                  (int) (getBytesPerVoxel() * getVolumeSizeX()
                  * getVolumeSizeY() * getVolumeSizeZ()))
                  .order(ByteOrder.nativeOrder());

          mCLVolumeImages[getCurrentRenderLayerIndex()].read(
                  mCLDevice.getQueue(),
                  0,
                  0,
                  0,
                  getVolumeSizeX(),
                  getVolumeSizeY(),
                  getVolumeSizeZ(),
                  0,
                  0,
                  lCaptureBuffers[i],
                  true);
        }
      }

      notifyVolumeCaptureListeners(lCaptureBuffers,
              getNativeType(),
              getVolumeSizeX(),
              getVolumeSizeY(),
              getVolumeSizeZ(),
              getVoxelSizeX(),
              getVoxelSizeY(),
              getVoxelSizeZ());

      mVolumeCaptureFlag = false;
    }
  }

  private boolean[] updateBufferAndRunKernel() {
    final boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

    lUpdated[0] = true;

    boolean lAnyVolumeDataUpdated = false;


    for (int lLayerIndex = 0; lLayerIndex < getNumberOfRenderLayers(); lLayerIndex++) {
      synchronized (getSetVolumeDataBufferLock(lLayerIndex)) {
        final FragmentedMemoryInterface lVolumeDataBuffer = getVolumeDataBuffer(lLayerIndex);

        if (lVolumeDataBuffer != null) {
          clearVolumeDataBufferReference(lLayerIndex);

          if (haveVolumeDimensionsChanged() || mCLVolumeImages[lLayerIndex] == null) {
            if (mCLVolumeImages[lLayerIndex] != null) {
              mCLVolumeImages[lLayerIndex].release();
            }

            prepareVolumeDataArray(lLayerIndex, lVolumeDataBuffer);
          } else {
            fillWithByteBuffer(
                    mCLVolumeImages[lLayerIndex],
                    lVolumeDataBuffer);
          }

          notifyCompletionOfDataBufferCopy(lLayerIndex);
          lAnyVolumeDataUpdated |= true;

          runProcessorHook(lLayerIndex);
        }

      }
    }

    clearVolumeDimensionsChanged();

    if (lAnyVolumeDataUpdated || haveVolumeRenderingParametersChanged()
            || getAdaptiveLODController().isKernelRunNeeded()) {
      for (int i = 0; i < getNumberOfRenderLayers(); i++) {
        if (mCLVolumeImages[i] != null) {
          runKernel(i);
          lUpdated[i] = true;
        }
      }
    }

    return lUpdated;
  }

  private void fillWithByteBuffer(final CLImage3D clImage3D,
                                  final FragmentedMemoryInterface pVolumeDataBuffer) {
    if (pVolumeDataBuffer.getNumberOfFragments() == 1) {
      final ContiguousMemoryInterface lContiguousBuffer = pVolumeDataBuffer.get(0);
      if (mCLDevice.writeImage(clImage3D, lContiguousBuffer) == null) {
        // TODO: figure out what the null return value actually means
      }
    } else {
      if (mCLDevice.writeImagePerPlane(clImage3D, pVolumeDataBuffer) == null) {
        // TODO: figure out what the null return value actually means
      }
    }
  }

  private void runKernel(final int pRenderLayerIndex) {
    if (isLayerVisible(pRenderLayerIndex)) {
      prepareTransferFunctionArray(pRenderLayerIndex);

      final int lMaxNumberSteps = getMaxSteps(pRenderLayerIndex);

      final int lNumberOfPasses = getAdaptiveLODController().getNumberOfPasses();

      final int lPassIndex = getAdaptiveLODController().getPassIndex();
      final boolean lActive = getAdaptiveLODController().isActive();

      int lMaxSteps = lMaxNumberSteps;
      float lDithering = 0;
      float lPhase = 0;
      int lClear = 0;

      mCurrentRenderKernel = mMCRTRenderKernel;
      lMaxSteps = max(16, lMaxNumberSteps / lNumberOfPasses);
      lDithering = getDithering(pRenderLayerIndex) * (1.0f * (lNumberOfPasses - lPassIndex) / lNumberOfPasses);
      lPhase = getAdaptiveLODController().getPhase();
      lClear = (lPassIndex == 0) ? 0 : 1;

      float avgL = 0.0f;

      mCLDevice.setArgs(mCurrentRenderKernel,
              mHDRBuffers[pRenderLayerIndex],
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
              mCLTransferFunctionImages[pRenderLayerIndex],
              mCLInvProjectionBuffer,
              mCLInvModelViewBuffer,
              mCLVolumeImages[pRenderLayerIndex]);

      mCLDevice.runSubdivisions(mCurrentRenderKernel,
              getRenderWidth(),
              getRenderHeight(), 16);

      /*// calculate average luminance
      Pointer<Float> fdata = mHDRBuffers[pRenderLayerIndex].read(mCLDevice.getQueue());
      FloatBuffer data = fdata.getFloatBuffer();
      data.rewind();

      for (int i = 0; i < getRenderHeight() * getRenderWidth(); i++) {
        float lum = data.get(i * 4 + 0) * 0.2126f
                + data.get(i * 4 + 1) * 0.7152f
                + data.get(i * 4 + 2) * 0.0722f;

        avgL += Math.log(lum + 0.00001f);
      }

      avgL = (float) Math.exp(avgL / (getRenderWidth() * getRenderHeight()));
      //System.out.println(avgL);*/

      mCLDevice.setArgs(mToneMappingKernel,
              mHDRBuffers[pRenderLayerIndex],
              mCLRenderBuffers[pRenderLayerIndex],
              getRenderWidth(),
              getRenderHeight(),
              0,
              avgL);

      mCLDevice.runSubdivisions(mToneMappingKernel,
              getRenderWidth(), getRenderHeight(), 16);

    } else {
      mCLDevice.setArgs(mClearKernel,
              mCLRenderBuffers[pRenderLayerIndex],
              getRenderWidth(),
              getRenderHeight());

      mCLDevice.run(mClearKernel, getRenderWidth(), getRenderHeight());

    }

    if (mTransferBuffer == null || mTransferBuffer.getValidBytes() != mCLRenderBuffers[pRenderLayerIndex].getByteCount()) {
      if (mTransferBuffer != null)
        mTransferBuffer.release();
      mTransferBuffer = mCLRenderBuffers[pRenderLayerIndex].allocateCompatibleMemory(mCLDevice.mCLDevice);
      // System.out.println("####### allocating");
    }

    mCLDevice.copyCLBufferToPointer(mCLRenderBuffers[pRenderLayerIndex],
            mTransferBuffer);
    copyBufferToTexture(pRenderLayerIndex,
            mTransferBuffer.getByteBuffer());

  }

  private void runProcessorHook(final int pRenderLayerIndex) {

    for (final ProcessorInterface<?> lProcessor : mProcessorInterfacesMap.values())
      if (lProcessor.isCompatibleProcessor(getClass())) {
        synchronized (getSetVolumeDataBufferLock(pRenderLayerIndex)) {
          if (lProcessor instanceof OpenCLProcessor) {
            final OpenCLProcessor<?> lOpenCLProcessor = (OpenCLProcessor<?>) lProcessor;
            lOpenCLProcessor.setVolumeBuffers(mCLVolumeImages[pRenderLayerIndex]);
          }

          final long lStartTimeNs = System.nanoTime();
          lProcessor.process(pRenderLayerIndex,
                  getVolumeSizeX(),
                  getVolumeSizeY(),
                  getVolumeSizeZ());
          final long lStopTimeNs = System.nanoTime();
          final double lElapsedTimeInMilliseconds = 0.001 * 0.001 * (lStopTimeNs - lStartTimeNs);
          /*
           * System.out.format("Elapsedtime in '%s' is %g ms \n",
           * lOpenCLProcessor.getClass().getSimpleName(),
           * lElapsedTimeInMilliseconds);/*
           */
        }
      }
  }

  @Override
  public void close() {
    mDisplayReentrantLock.lock();
    try {
      super.close();
      mCLDevice.close();
    } finally {
      if (mDisplayReentrantLock.isHeldByCurrentThread())
        mDisplayReentrantLock.unlock();
    }

  }

}
