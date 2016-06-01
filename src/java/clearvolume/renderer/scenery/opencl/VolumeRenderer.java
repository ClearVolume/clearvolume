package clearvolume.renderer.scenery.opencl;

import clearvolume.exceptions.ClearVolumeMemoryException;
import clearvolume.exceptions.ClearVolumeUnsupportdDataTypeException;
import clearvolume.renderer.scenery.VolumeNode;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.nativelibs4java.opencl.*;
import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemory;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.types.NativeTypeEnum;
import org.bridj.Pointer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class VolumeRenderer implements GLEventListener
{
  private OpenCLDevice mCLDevice;

  private CLBuffer<Integer>[] mCLRenderBuffers;
  private CLImage3D[] mCLVolumeImages;
  private CLImage2D[] mCLTransferFunctionImages;

  private CLBuffer<Float> mCLInvModelViewBuffer,
          mCLInvProjectionBuffer;

  private CLKernel mCurrentRenderKernel, mMaxProjectionRenderKernel,
          mIsoSurfaceRenderKernel, mClearKernel;

  private Pointer<Integer> mTransferBuffer;
  private NativeTypeEnum nativeType;
  private Integer numberOfLayers;

  private Integer textureWidth;
  private Integer textureHeight;

  private VolumeNode currentNode = null;
  private boolean volumeCaptureFlag;
  private ArrayList<Integer> lastDimensions;

  @SuppressWarnings("unchecked")
  public VolumeRenderer(final String nodeName,
                              final NativeTypeEnum pNativeTypeEnum,
                              final Integer pMaxTextureWidth,
                              final Integer pMaxTextureHeight,
                              final Integer pNumberOfRenderLayers) {

    mCLRenderBuffers = new CLBuffer[pNumberOfRenderLayers];
    mCLVolumeImages = new CLImage3D[pNumberOfRenderLayers];
    mCLTransferFunctionImages = new CLImage2D[pNumberOfRenderLayers];

    nativeType = pNativeTypeEnum;
    numberOfLayers = pNumberOfRenderLayers;

    textureWidth = pMaxTextureWidth;
    textureHeight = pMaxTextureHeight;
  }

  protected Integer getNumberOfRenderLayers() {
    return numberOfLayers;
  }

  protected boolean initVolumeRenderer()
  {
    mCLDevice = new OpenCLDevice();

    mCLDevice.initCL();
    mCLDevice.printInfo();
    mMaxProjectionRenderKernel = mCLDevice.compileKernel(	VolumeRenderer.class.getResource("kernels/VolumeRenderer.cl"),
            "maxproj_render");
    mClearKernel = mCLDevice.compileKernel(	VolumeRenderer.class.getResource("kernels/VolumeRenderer.cl"),
            "clearbuffer");

    mIsoSurfaceRenderKernel = mCLDevice.compileKernel(VolumeRenderer.class.getResource("kernels/VolumeRenderer.cl"),
            "isosurface_render");

    mCLInvModelViewBuffer = mCLDevice.createInputFloatBuffer(16);
    mCLInvProjectionBuffer = mCLDevice.createInputFloatBuffer(16);

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
      prepareVolumeDataArray(i, null);

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
      prepareTransferFunctionArray(i);

    return true;
  }

  protected void notifyChangeOfTextureDimensions()
  {
    final int lRenderBufferSize = textureWidth * textureHeight;

    for (int i = 0; i < getNumberOfRenderLayers(); i++)
    {
      if (mCLRenderBuffers[i] != null)
        mCLRenderBuffers[i].release();
      mCLRenderBuffers[i] = mCLDevice.createInputOutputIntBuffer(lRenderBufferSize);
    }
  }

  private NativeTypeEnum getNativeType() {
    return currentNode.getVolumeType();
  }

  private long getBytesPerVoxel() {
    return 2;//(long)currentNode.getVolumeType();
  }

  private void prepareVolumeDataArray(final int pRenderLayerIndex,
                                      final FragmentedMemoryInterface pVolumeDataBuffer)
  {
    synchronized (currentNode.getVolumeData())
    {

      FragmentedMemoryInterface lVolumeDataBuffer = pVolumeDataBuffer;
      if (lVolumeDataBuffer == null)
        lVolumeDataBuffer = FragmentedMemory.wrap(currentNode.getVolumeData().get(pRenderLayerIndex));
      if (lVolumeDataBuffer == null)
        return;

      final long lWidth = currentNode.getVolumeDimensions().get(0);
      final long lHeight = currentNode.getVolumeDimensions().get(1);
      final long lDepth = currentNode.getVolumeDimensions().get(2);

      final long lBytePerVoxel = getBytesPerVoxel();

      if (lVolumeDataBuffer.getSizeInBytes() != (lWidth * lHeight
              * lDepth * lBytePerVoxel))
      {
        throw new ClearVolumeMemoryException("Volume buffer has wrong size!");
      }

      if (getNativeType() == NativeTypeEnum.UnsignedByte)

        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt8);
      else if (getNativeType() == NativeTypeEnum.UnsignedShort)
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt16);
      else if (getNativeType() == NativeTypeEnum.Byte)
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt8);
      else if (getNativeType() == NativeTypeEnum.Short)
        mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(lWidth,
                lHeight,
                lDepth,
                CLImageFormat.ChannelOrder.R,
                CLImageFormat.ChannelDataType.UNormInt16);
      else
        throw new ClearVolumeUnsupportdDataTypeException("Received an unsupported data type: " + getNativeType());

      fillWithByteBuffer(	mCLVolumeImages[pRenderLayerIndex],
              lVolumeDataBuffer);

    }
  }

  private void prepareTransferFunctionArray(final int pRenderLayerIndex)
  {

    final float[] lTransferFunctionArray = currentNode.getTransferFunctions().get(pRenderLayerIndex).getArray();

		/*
		 * System.out.println("render layer %" + pRenderLayerIndex + " -> " +
		 * Arrays.toString(lTransferFunctionArray));/*
		 */

    final int lTransferFunctionArrayLength = lTransferFunctionArray.length;

    final int lNeededWidth = lTransferFunctionArrayLength / 4;
    if (mCLTransferFunctionImages[pRenderLayerIndex] == null || mCLTransferFunctionImages[pRenderLayerIndex].getWidth() != lNeededWidth)
    {
      if (mCLTransferFunctionImages[pRenderLayerIndex] != null)
        mCLTransferFunctionImages[pRenderLayerIndex].release();

      mCLTransferFunctionImages[pRenderLayerIndex] = mCLDevice.createGenericImage2D(lNeededWidth,
              1,
              CLImageFormat.ChannelOrder.RGBA,
              CLImageFormat.ChannelDataType.Float);
    }

    mCLDevice.writeImage(	mCLTransferFunctionImages[pRenderLayerIndex],
            FloatBuffer.wrap(lTransferFunctionArray));

  }

  protected boolean[] renderVolume(	final float[] pInvModelViewMatrix,
                                     final float[] pInvProjectionMatrix)
  {

    //doCaptureBuffersIfNeeded();

    // System.out.println("render");
    try
    {
      mCLDevice.writeFloatBuffer(	mCLInvModelViewBuffer,
              FloatBuffer.wrap(pInvModelViewMatrix));

      mCLDevice.writeFloatBuffer(	mCLInvProjectionBuffer,
              FloatBuffer.wrap(pInvProjectionMatrix));

      return updateBufferAndRunKernel();
    }
    catch (final Exception e)
    {
      System.err.println(e.getLocalizedMessage());
      return null;
    }

  }

  /*private void doCaptureBuffersIfNeeded()
  {
    if (volumeCaptureFlag)
    {
      for (int l = 0; l < getNumberOfRenderLayers(); l++)
      {
        final ByteBuffer lCaptureBuffer;

        synchronized (currentNode.getVolumeData().get(l))
        {
          lCaptureBuffer = ByteBuffer.allocateDirect((int) (getBytesPerVoxel() * getVolumeSizeX(l)
                  * getVolumeSizeY(l) * getVolumeSizeZ(l)))
                  .order(ByteOrder.nativeOrder());

          mCLVolumeImages[getCurrentRenderLayerIndex()].read(	mCLDevice.getQueue(),
                  0,
                  0,
                  0,
                  getVolumeSizeX(l),
                  getVolumeSizeY(l),
                  getVolumeSizeZ(l),
                  0,
                  0,
                  lCaptureBuffer,
                  true);
        }

        notifyVolumeCaptureListeners(	lCaptureBuffer,
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
  }*/

  private boolean isVolumeDataUpdateAllowed() {
    return true;
  }

  private boolean[] updateBufferAndRunKernel()
  {
    lastDimensions = (ArrayList<Integer>)currentNode.getVolumeDimensions().clone();
    final boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

    lUpdated[0] = true;

    boolean lAnyVolumeDataUpdated = false;

    if (isVolumeDataUpdateAllowed())
    {
      for (int lLayerIndex = 0; lLayerIndex < getNumberOfRenderLayers(); lLayerIndex++)
      {
        synchronized (currentNode.getVolumeData().get(lLayerIndex))
        {
          final FragmentedMemoryInterface lVolumeDataBuffer = FragmentedMemory.wrap(currentNode.getVolumeData().get(lLayerIndex));

          if (lVolumeDataBuffer != null)
          {

//            clearVolumeDataBufferReference(lLayerIndex);

            if (lastDimensions == null || lastDimensions != currentNode.getVolumeDimensions() || mCLVolumeImages[lLayerIndex] == null)
            {
              if (mCLVolumeImages[lLayerIndex] != null)
              {

                mCLVolumeImages[lLayerIndex].release();
              }

              prepareVolumeDataArray(lLayerIndex, lVolumeDataBuffer);
            }
            else
            {
              fillWithByteBuffer(	mCLVolumeImages[lLayerIndex],
                      lVolumeDataBuffer);

            }

//            notifyCompletionOfDataBufferCopy(lLayerIndex);
            lAnyVolumeDataUpdated |= true;
          }

        }
      }

//      clearVolumeDimensionsChanged();
    }

    if (lAnyVolumeDataUpdated /*|| haveVolumeRenderingParametersChanged()
            || getAdaptiveLODController().isKernelRunNeeded()*/)
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

  private void fillWithByteBuffer(final CLImage3D clImage3D,
                                  final FragmentedMemoryInterface pVolumeDataBuffer)
  {
    if (pVolumeDataBuffer.getNumberOfFragments() == 1)
    {
      final ContiguousMemoryInterface lContiguousBuffer = pVolumeDataBuffer.get(0);
      mCLDevice.writeImage(clImage3D, lContiguousBuffer);
    }
    else
    {
      mCLDevice.writeImagePerPlane(clImage3D, pVolumeDataBuffer);
    }
  }

  private void runKernel(final int pRenderLayerIndex)
  {
    // System.out.println("kernel");
    // System.out.println(mCLVolumeImages[i].getHeight());
//    if (isLayerVisible(pRenderLayerIndex))
    if(true)
    {
      prepareTransferFunctionArray(pRenderLayerIndex);

      final int lMaxNumberSteps = 16;//getMaxSteps(pRenderLayerIndex);

      final int lNumberOfPasses = 16;//getAdaptiveLODController().getNumberOfPasses();

      final int lPassIndex = 1;//getAdaptiveLODController().getPassIndex();
//      final boolean lActive = getAdaptiveLODController().isActive();

      int lMaxSteps = lMaxNumberSteps;
      float lDithering = 0;
      float lPhase = 0;
      int lClear = 0;


      mCurrentRenderKernel = mMaxProjectionRenderKernel;
      lMaxSteps = Math.max(16, lMaxNumberSteps / lNumberOfPasses);
      lDithering = currentNode.getSettings().getProperty("render.Dithering", Float.class);//getDithering(pRenderLayerIndex) * (1.0f * (lNumberOfPasses - lPassIndex) / lNumberOfPasses);
      lPhase = 0.0f;//getAdaptiveLODController().getPhase();
      lClear = (lPassIndex == 0) ? 0 : 1;
      float[] lClipBox = currentNode.getROI();

      mCLDevice.setArgs(mCurrentRenderKernel,
              mCLRenderBuffers[pRenderLayerIndex],
              textureWidth,
              textureHeight,
              currentNode.getSettings().getProperty("render.Brightness", Float.class),
              currentNode.getSettings().getProperty("render.TransferMin", Float.class),
              currentNode.getSettings().getProperty("render.TransferMax", Float.class),
              currentNode.getSettings().getProperty("render.Gamma", Float.class),
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


      mCLDevice.run(mCurrentRenderKernel,
              textureWidth,
              textureHeight);

    }
    else
    {
      mCLDevice.setArgs(mClearKernel,
              mCLRenderBuffers[pRenderLayerIndex],
              textureWidth,
              textureHeight);

      mCLDevice.run(mClearKernel, textureWidth, textureHeight);

    }

    if (mTransferBuffer == null || mTransferBuffer.getValidBytes() != mCLRenderBuffers[pRenderLayerIndex].getByteCount())
    {
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

  public void copyBufferToTexture(final int pRenderLayerIndex,
                                  final ByteBuffer pByteBuffer)
  {
    pByteBuffer.rewind();

    currentNode.getVolumeTextures().get(pRenderLayerIndex).copyFrom(pByteBuffer);
    currentNode.getVolumeTextures().get(pRenderLayerIndex).updateMipMaps();
  }

  public void close()
  {
      if (mCLDevice != null)
        mCLDevice.close();
  }

  @Override
  public void init(GLAutoDrawable glAutoDrawable) {

  }

  @Override
  public void dispose(GLAutoDrawable glAutoDrawable) {

  }

  @Override
  public void display(GLAutoDrawable glAutoDrawable) {

  }

  @Override
  public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

  }
}
