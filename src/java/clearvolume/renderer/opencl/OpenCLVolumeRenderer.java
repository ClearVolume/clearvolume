package clearvolume.renderer.opencl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GLEventListener;

import jcuda.CudaException;
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.processors.OpenCLProcessor;
import clearvolume.renderer.processors.Processor;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

public class OpenCLVolumeRenderer extends JOGLClearVolumeRenderer	implements
GLEventListener
{
	private OpenCLDevice mCLDevice;

	private CLBuffer<Integer>[] mCLRenderBuffers;
	private CLImage3D[] mCLVolumeImages;
	private CLImage2D[] mCLTransferFunctionImages;

	private ByteBuffer RenderBuffers;

	private CLBuffer<Float> mCLInvModelViewBuffer,
	mCLInvProjectionBuffer;

	private CLBuffer<Float>[] mCLTransferColorBuffers;

	private CLKernel mRenderKernel;

	public OpenCLVolumeRenderer(final String pWindowName,
	                            final int pWindowWidth,
	                            final int pWindowHeight)
	{
		super("[OpenCL] " + pWindowName, pWindowWidth, pWindowHeight);

	}

	public OpenCLVolumeRenderer(final String pWindowName,
	                            final int pWindowWidth,
	                            final int pWindowHeight,
	                            final int pBytesPerVoxel)
	{
		super("[OpenCL] " + pWindowName,
		      pWindowWidth,
		      pWindowHeight,
		      pBytesPerVoxel);

	}

	public OpenCLVolumeRenderer(final String pWindowName,
	                            final int pWindowWidth,
	                            final int pWindowHeight,
	                            final int pBytesPerVoxel,
	                            final int pMaxTextureWidth,
	                            final int pMaxTextureHeight)
	{
		super("[OpenCL] " + pWindowName,
		      pWindowWidth,
		      pWindowHeight,
		      pBytesPerVoxel,
		      pMaxTextureWidth,
		      pMaxTextureHeight);

	}

	@SuppressWarnings("unchecked")
	public OpenCLVolumeRenderer(final String pWindowName,
	                            final int pWindowWidth,
	                            final int pWindowHeight,
	                            final int pBytesPerVoxel,
	                            final int pMaxTextureWidth,
	                            final int pMaxTextureHeight,
	                            final int pNumberOfRenderLayers,
	                            final boolean useInCanvas)
	{

		super("[OpenCL] " + pWindowName,
		      pWindowWidth,
		      pWindowHeight,
		      pBytesPerVoxel,
		      pMaxTextureWidth,
		      pMaxTextureHeight,
		      pNumberOfRenderLayers,
		      useInCanvas);

		mCLRenderBuffers = new CLBuffer[pNumberOfRenderLayers];
		mCLVolumeImages = new CLImage3D[pNumberOfRenderLayers];
		mCLTransferFunctionImages = new CLImage2D[pNumberOfRenderLayers];
		mCLTransferColorBuffers = new CLBuffer[pNumberOfRenderLayers];
	}

	@Override
	protected boolean initVolumeRenderer()
	{
		mUsePBOs = false;

		mCLDevice = new OpenCLDevice();

		// FIXME using existing OpenGL context does not work yet
		// mCLDevice.initCL(true);

		mCLDevice.initCL(false);
		mCLDevice.printInfo();
		mRenderKernel = mCLDevice.compileKernel(OpenCLVolumeRenderer.class.getResource("kernels/VolumeRenderPerspective.cl"),
				"volumerender");

		for (final Processor<?> lProcessor : mProcessorsMap.values())
			if (lProcessor.isCompatibleRenderer(getClass()))
			{
				final OpenCLProcessor<?> lOpenCLProcessor = (OpenCLProcessor<?>) lProcessor;
				lOpenCLProcessor.setDevice(mCLDevice);
			}

		mCLInvModelViewBuffer = mCLDevice.createInputFloatBuffer(16);
		mCLInvProjectionBuffer = mCLDevice.createInputFloatBuffer(16);

		final int lRenderBufferSize = getTextureHeight() * getTextureWidth();

		// setting up the OpenCL Renderbuffer we will write the render result into
		for (int i = 0; i < getNumberOfRenderLayers(); i++)
		{
			mCLRenderBuffers[i] = mCLDevice.createOutputIntBuffer(lRenderBufferSize);
			mCLTransferColorBuffers[i] = mCLDevice.createInputFloatBuffer(4);
		}

		for (int i = 0; i < getNumberOfRenderLayers(); i++)
			prepareVolumeDataArray(i, null);

		for (int i = 0; i < getNumberOfRenderLayers(); i++)
			prepareTransferFunctionArray(i);

		return true;
	}

	@Override
	protected void registerPBO(	final int pRenderLayerIndex,
	                           	final int pPixelBufferObjectId)
	{
		// no need to do anything here, as we're not using PBOs
	}

	@Override
	protected void unregisterPBO(	final int pRenderLayerIndex,
	                             	final int pPixelBufferObjectId)
	{
		// no need to do anything here, as we're not using PBOs
	}

	private void prepareVolumeDataArray(final int pRenderLayerIndex,
	                                    final ByteBuffer pByteBuffer)
	{
		synchronized (getSetVolumeDataBufferLock(pRenderLayerIndex))
		{

			ByteBuffer lVolumeDataBuffer = pByteBuffer;
			if (lVolumeDataBuffer == null)
				lVolumeDataBuffer = getVolumeDataBuffer(pRenderLayerIndex);
			if (lVolumeDataBuffer == null)
				return;

			final long lWidth = getVolumeSizeX();
			final long lHeight = getVolumeSizeY();
			final long lDepth = getVolumeSizeZ();

			if (getBytesPerVoxel() == 1)
				mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(lWidth,
				                                                                    lHeight,
				                                                                    lDepth,
				                                                                    CLImageFormat.ChannelOrder.R,
				                                                                    CLImageFormat.ChannelDataType.UNormInt8);
			else if (getBytesPerVoxel() == 2)
				mCLVolumeImages[pRenderLayerIndex] = mCLDevice.createGenericImage3D(lWidth,
				                                                                    lHeight,
				                                                                    lDepth,
				                                                                    CLImageFormat.ChannelOrder.R,
				                                                                    CLImageFormat.ChannelDataType.UNormInt16);

			lVolumeDataBuffer.rewind();

			fillWithByteBufferAsShort(mCLVolumeImages[pRenderLayerIndex],
			                          lVolumeDataBuffer);

		}
	}

	private void prepareTransferFunctionArray(final int pRenderLayerIndex)
	{

		final float[] lTransferFunctionArray = getTransfertFunction(pRenderLayerIndex).getArray();

		final int lTransferFunctionArrayLength = lTransferFunctionArray.length;

		mCLTransferFunctionImages[pRenderLayerIndex] = mCLDevice.createGenericImage2D(lTransferFunctionArrayLength / 4,
		                                                                              1,
		                                                                              CLImageFormat.ChannelOrder.RGBA,
		                                                                              CLImageFormat.ChannelDataType.Float);

		final float[] color4 = new float[]
				{ lTransferFunctionArray[lTransferFunctionArrayLength - 4],
				  lTransferFunctionArray[lTransferFunctionArrayLength - 3],
				  lTransferFunctionArray[lTransferFunctionArrayLength - 2],
				  lTransferFunctionArray[lTransferFunctionArrayLength - 1] };

		// System.out.println("prepare+ " + pRenderLayerIndex
		// + Arrays.toString(color4));

		mCLDevice.writeFloatBuffer(	mCLTransferColorBuffers[pRenderLayerIndex],
		                           	FloatBuffer.wrap(color4));

		mCLDevice.writeFloatImage2D(mCLTransferFunctionImages[pRenderLayerIndex],
		                            FloatBuffer.wrap(lTransferFunctionArray));

	}

	@Override
	protected boolean[] renderVolume(	final float[] pInvModelViewMatrix,
	                                 	final float[] pInvProjectionMatrix)
	{

		// System.out.println("render");
		try
		{

			mCLDevice.writeFloatBuffer(	mCLInvModelViewBuffer,
			                           	FloatBuffer.wrap(pInvModelViewMatrix));

			mCLDevice.writeFloatBuffer(	mCLInvProjectionBuffer,
			                           	FloatBuffer.wrap(pInvProjectionMatrix));

			return updateBufferAndRunKernel();
		}
		catch (final CudaException e)
		{
			System.err.println(e.getLocalizedMessage());
			return null;
		}

	}

	private boolean[] updateBufferAndRunKernel()
	{
		final boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

		lUpdated[0] = true;

		boolean lAnyVolumeDataUpdated = false;

		for (int lLayerIndex = 0; lLayerIndex < getNumberOfRenderLayers(); lLayerIndex++)
		{
			final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer(lLayerIndex);

			if (lVolumeDataBuffer != null)
			{
				synchronized (getSetVolumeDataBufferLock(lLayerIndex))
				{
					clearVolumeDataBufferReference(lLayerIndex);

					if (haveVolumeDimensionsChanged() || mCLVolumeImages[lLayerIndex] == null)
					{
						if (mCLVolumeImages[lLayerIndex] != null)
						{

							mCLVolumeImages[lLayerIndex].release();
						}

						prepareVolumeDataArray(lLayerIndex, lVolumeDataBuffer);
					}
					else
					{
						lVolumeDataBuffer.rewind();

						fillWithByteBufferAsShort(mCLVolumeImages[lLayerIndex],
						                          lVolumeDataBuffer);

					}

					notifyCompletionOfDataBufferCopy(lLayerIndex);
					lAnyVolumeDataUpdated |= true;

					runProcessorHook(lLayerIndex);
				}

			}
		}

		if (lAnyVolumeDataUpdated || getIsUpdateVolumeRenderingParameters())
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
		clearIsUpdateVolumeParameters();
		clearVolumeDimensionsChanged();

		return lUpdated;
	}

	private void fillWithByteBufferAsShort(	final CLImage3D clImage3D,
	                                       	final ByteBuffer lVolumeDataBuffer)
	{
		lVolumeDataBuffer.rewind();
		mCLDevice.writeImage(clImage3D, lVolumeDataBuffer);
	}

	private void runKernel(final int pRenderLayerIndex)
	{
		// System.out.println("kernel");
		// System.out.println(mCLVolumeImages[i].getHeight());

		prepareTransferFunctionArray(pRenderLayerIndex);

		mCLDevice.setArgs(mRenderKernel,
		                  mCLRenderBuffers[pRenderLayerIndex],
		                  getTextureWidth(),
		                  getTextureHeight(),
		                  (float) getBrightness(pRenderLayerIndex),
		                  (float) getTransferRangeMin(pRenderLayerIndex),
		                  (float) getTransferRangeMax(pRenderLayerIndex),
		                  (float) getGamma(pRenderLayerIndex),
		                  // mCLTranferFunctionImages[pRenderLayerIndex],
		                  mCLTransferColorBuffers[pRenderLayerIndex],
		                  mCLInvProjectionBuffer,
		                  mCLInvModelViewBuffer,
		                  mCLVolumeImages[pRenderLayerIndex]);

		// long startTime = System.nanoTime();

		mCLDevice.run(mRenderKernel,
		              getTextureWidth(),
		              getTextureHeight());

		if(isLayerVisible(pRenderLayerIndex))
			copyBufferToTexture(pRenderLayerIndex,
			                    mCLDevice.readIntBufferAsByte(mCLRenderBuffers[pRenderLayerIndex]));
		else
			clearTexture(pRenderLayerIndex);


		// long endTime = System.nanoTime();

		// System.out.println("time to render: " + (endTime - startTime)
		// / 1000000.
		// + " ms");

	}

	private void runProcessorHook(int pRenderLayerIndex)
	{
		for (final Processor<?> lProcessor : mProcessorsMap.values())
			if (lProcessor.isCompatibleRenderer(getClass()))
			{
				final OpenCLProcessor<?> lOpenCLProcessor = (OpenCLProcessor<?>) lProcessor;
				lOpenCLProcessor.setVolumeBuffers(mCLVolumeImages[pRenderLayerIndex]);
				lOpenCLProcessor.process(	pRenderLayerIndex,
				                         	getVolumeSizeX(),
				                         	getVolumeSizeY(),
				                         	getVolumeSizeZ());
			}
	}
}
