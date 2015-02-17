package clearvolume.renderer.opencl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
	private CLKernel mClearKernel;

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
		mCLDevice = new OpenCLDevice();

		// FIXME using existing OpenGL context does not work yet
		// mCLDevice.initCL(true);

		mCLDevice.initCL(false);
		mCLDevice.printInfo();
		mRenderKernel = mCLDevice.compileKernel(OpenCLVolumeRenderer.class.getResource("kernels/VolumeRenderPerspective.cl"),
																						"volumerender");
		mClearKernel = mCLDevice.compileKernel(	OpenCLVolumeRenderer.class.getResource("kernels/VolumeRenderPerspective.cl"),
																						"clearbuffer");

		for (final Processor<?> lProcessor : mProcessorsMap.values())
			if (lProcessor.isCompatibleProcessor(getClass()))
				if (lProcessor instanceof OpenCLProcessor)
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
		}

		for (int i = 0; i < getNumberOfRenderLayers(); i++)
			prepareVolumeDataArray(i, null);

		for (int i = 0; i < getNumberOfRenderLayers(); i++)
			prepareTransferFunctionArray(i);

		return true;
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

		final float[] lTransferFunctionArray = getTransferFunction(pRenderLayerIndex).getArray();

		/*System.out.println("render layer %" + pRenderLayerIndex
												+ " -> "
												+ Arrays.toString(lTransferFunctionArray));/**/

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

		/*final float[] color4 = new float[]
		{ lTransferFunctionArray[lTransferFunctionArrayLength - 4],
			lTransferFunctionArray[lTransferFunctionArrayLength - 3],
			lTransferFunctionArray[lTransferFunctionArrayLength - 2],
			lTransferFunctionArray[lTransferFunctionArrayLength - 1] };

		// System.out.println("prepare+ " + pRenderLayerIndex
		// + Arrays.toString(color4));

		mCLDevice.writeFloatBuffer(	mCLTransferColorBuffers[pRenderLayerIndex],
																FloatBuffer.wrap(color4));/**/

		mCLDevice.writeFloatImage2D(mCLTransferFunctionImages[pRenderLayerIndex],
																FloatBuffer.wrap(lTransferFunctionArray));

	}

	@Override
	protected boolean[] renderVolume(	final float[] pInvModelViewMatrix,
																		final float[] pInvProjectionMatrix)
	{

		doCaptureBuffersIfNeeded();

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

	private void doCaptureBuffersIfNeeded()
	{
		if (mVolumeCaptureFlag)
		{
			final ByteBuffer[] lCaptureBuffers = new ByteBuffer[getNumberOfRenderLayers()];

			for (int i = 0; i < getNumberOfRenderLayers(); i++)
			{
				lCaptureBuffers[i] = ByteBuffer.allocateDirect((int) (getBytesPerVoxel() * getVolumeSizeX()
																															* getVolumeSizeY() * getVolumeSizeZ()))
																				.order(ByteOrder.nativeOrder());

				mCLVolumeImages[getCurrentRenderLayerIndex()].read(	mCLDevice.getQueue(),
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

			notifyVolumeCaptureListeners(	lCaptureBuffers,
																		false,
																		getBytesPerVoxel(),
																		getVolumeSizeX(),
																		getVolumeSizeY(),
																		getVolumeSizeZ(),
																		getVoxelSizeX(),
																		getVoxelSizeY(),
																		getVoxelSizeZ());

			mVolumeCaptureFlag = false;
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

		if (lAnyVolumeDataUpdated || haveVolumeRenderingParametersChanged()
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
		if (isLayerVisible(pRenderLayerIndex))
		{
			prepareTransferFunctionArray(pRenderLayerIndex);

			final int lMaxNumberSteps = getMaxSteps(pRenderLayerIndex);
			getAdaptiveLODController().notifyMaxNumberOfSteps(lMaxNumberSteps);
			final int lMaxSteps = lMaxNumberSteps / getAdaptiveLODController().getNumberOfPasses();
			final float lPhase = getAdaptiveLODController().getPhase();
			final int lClear = getAdaptiveLODController().isBufferClearingNeeded() ? 0
																																						: 1;

			mCLDevice.setArgs(mRenderKernel,
												mCLRenderBuffers[pRenderLayerIndex],
												getTextureWidth(),
												getTextureHeight(),
												(float) getBrightness(pRenderLayerIndex),
												(float) getTransferRangeMin(pRenderLayerIndex),
												(float) getTransferRangeMax(pRenderLayerIndex),
												(float) getGamma(pRenderLayerIndex),
												lMaxSteps,
												getDithering(pRenderLayerIndex),
												lPhase,
												lClear,
												mCLTransferFunctionImages[pRenderLayerIndex],
												mCLInvProjectionBuffer,
												mCLInvModelViewBuffer,
												mCLVolumeImages[pRenderLayerIndex]);

			mCLDevice.run(mRenderKernel,
										getTextureWidth(),
										getTextureHeight());

		}
		else
		{
			mCLDevice.setArgs(mRenderKernel,
												mCLRenderBuffers[pRenderLayerIndex],
												getTextureWidth(),
												getTextureHeight());

			mCLDevice.run(mRenderKernel,
										getTextureWidth(),
										getTextureHeight());

		}

		final ByteBuffer lRenderedImageBuffer = mCLDevice.readIntBufferAsByte(mCLRenderBuffers[pRenderLayerIndex]);
		copyBufferToTexture(pRenderLayerIndex, lRenderedImageBuffer);

	}

	private void runProcessorHook(final int pRenderLayerIndex)
	{
		for (final Processor<?> lProcessor : mProcessorsMap.values())
			if (lProcessor.isCompatibleProcessor(getClass()))
			{
				if (lProcessor instanceof OpenCLProcessor)
				{
					final OpenCLProcessor<?> lOpenCLProcessor = (OpenCLProcessor<?>) lProcessor;
					lOpenCLProcessor.setVolumeBuffers(mCLVolumeImages[pRenderLayerIndex]);
				}

				final long lStartTimeNs = System.nanoTime();
				lProcessor.process(	pRenderLayerIndex,
														getVolumeSizeX(),
														getVolumeSizeY(),
														getVolumeSizeZ());
				final long lStopTimeNs = System.nanoTime();
				final double lElapsedTimeInMilliseconds = 0.001 * 0.001 * (lStopTimeNs - lStartTimeNs);
				/*System.out.format("Elapsedtime in '%s' is %g ms \n",
													lOpenCLProcessor.getClass().getSimpleName(),
													lElapsedTimeInMilliseconds);/**/
			}
	}

	@Override
	public void close()
	{
		mDisplayReentrantLock.lock();
		try
		{
			super.close();
			mCLDevice.close();
		}
		finally
		{
			if (mDisplayReentrantLock.isHeldByCurrentThread())
				mDisplayReentrantLock.unlock();
		}

	}

}
