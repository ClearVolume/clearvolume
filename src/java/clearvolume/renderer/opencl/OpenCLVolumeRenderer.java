package clearvolume.renderer.opencl;

import static java.lang.Math.pow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GLEventListener;

import jcuda.CudaException;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.cleargl.overlay.o2d.BarGraphOverlay;
import clearvolume.renderer.cleargl.overlay.o2d.HistogramOverlay;
import clearvolume.renderer.processors.OpenCLProcessor;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.impl.OpenCLHistogram;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.types.NativeTypeEnum;

public class OpenCLVolumeRenderer extends ClearGLVolumeRenderer	implements
																																GLEventListener
{
	private OpenCLDevice mCLDevice;

	private CLBuffer<Integer>[] mCLRenderBuffers;
	private CLImage3D[] mCLVolumeImages;
	private CLImage2D[] mCLTransferFunctionImages;

	private CLBuffer<Float> mCLInvModelViewBuffer,
			mCLInvProjectionBuffer;

	private CLKernel mCurrentRenderKernel, mMaxProjectionRenderKernel,
			mIsoSurfaceRenderKernel, mClearKernel;

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
															final int pWindowWidth,
															final int pWindowHeight,
															final NativeTypeEnum pNativeTypeEnum,
															final int pMaxTextureWidth,
															final int pMaxTextureHeight,
															final int pNumberOfRenderLayers,
															final boolean useInCanvas)
	{

		super("[OpenCL] " + pWindowName,
					pWindowWidth,
					pWindowHeight,
					pNativeTypeEnum,
					pMaxTextureWidth,
					pMaxTextureHeight,
					pNumberOfRenderLayers,
					useInCanvas);

		mCLRenderBuffers = new CLBuffer[pNumberOfRenderLayers];
		mCLVolumeImages = new CLImage3D[pNumberOfRenderLayers];
		mCLTransferFunctionImages = new CLImage2D[pNumberOfRenderLayers];

		final OpenCLHistogram lHistoProcessor = new OpenCLHistogram();
		addProcessor(lHistoProcessor);

		final BarGraphOverlay lBarGraphOverlay = new HistogramOverlay(lHistoProcessor);
		addOverlay(lBarGraphOverlay);

		lBarGraphOverlay.setDisplayed(false);

	}

	@Override
	protected boolean initVolumeRenderer()
	{
		mCLDevice = new OpenCLDevice();

		mCLDevice.initCL();
		mCLDevice.printInfo();
		mMaxProjectionRenderKernel = mCLDevice.compileKernel(	OpenCLVolumeRenderer.class.getResource("kernels/VolumeRender.cl"),
																													"maxproj_render");
		mClearKernel = mCLDevice.compileKernel(	OpenCLVolumeRenderer.class.getResource("kernels/VolumeRender.cl"),
																						"clearbuffer");

		mIsoSurfaceRenderKernel = mCLDevice.compileKernel(OpenCLVolumeRenderer.class.getResource("kernels/VolumeRender.cl"),
																											"isosurface_render");

		for (final Processor<?> lProcessor : mProcessorsMap.values())
			if (lProcessor.isCompatibleProcessor(getClass()))
				if (lProcessor instanceof OpenCLProcessor)
				{
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
	protected void notifyChangeOfTextureDimensions()
	{
		final int lRenderBufferSize = getTextureHeight() * getTextureWidth();

		for (int i = 0; i < getNumberOfRenderLayers(); i++)
		{
			if (mCLRenderBuffers[i] != null)
				mCLRenderBuffers[i].release();
			mCLRenderBuffers[i] = mCLDevice.createInputOutputIntBuffer(lRenderBufferSize);
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

			final long lWidth = getVolumeSizeX();
			final long lHeight = getVolumeSizeY();
			final long lDepth = getVolumeSizeZ();

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

			fillWithByteBuffer(	mCLVolumeImages[pRenderLayerIndex],
													lVolumeDataBuffer);

		}
	}

	private void prepareTransferFunctionArray(final int pRenderLayerIndex)
	{

		final float[] lTransferFunctionArray = getTransferFunction(pRenderLayerIndex).getArray();

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
				synchronized (getSetVolumeDataBufferLock(i))
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
			}

			notifyVolumeCaptureListeners(	lCaptureBuffers,
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

	private boolean[] updateBufferAndRunKernel()
	{
		final boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

		lUpdated[0] = true;

		boolean lAnyVolumeDataUpdated = false;

		for (int lLayerIndex = 0; lLayerIndex < getNumberOfRenderLayers(); lLayerIndex++)
		{
			synchronized (getSetVolumeDataBufferLock(lLayerIndex))
			{
				final FragmentedMemoryInterface lVolumeDataBuffer = getVolumeDataBuffer(lLayerIndex);

				if (lVolumeDataBuffer != null)
				{

					clearVolumeDataBufferReference(lLayerIndex);

					if (haveVolumeDimensionsChanged() || mCLVolumeImages[lLayerIndex] == null)
					{
						if (mCLVolumeImages[lLayerIndex] != null)
						{

							mCLVolumeImages[lLayerIndex].release();
						}

						prepareVolumeDataArray(lLayerIndex, lVolumeDataBuffer);
						clearVolumeDimensionsChanged();
					}
					else
					{
						fillWithByteBuffer(	mCLVolumeImages[lLayerIndex],
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
		if (isLayerVisible(pRenderLayerIndex))
		{
			prepareTransferFunctionArray(pRenderLayerIndex);

			final int lMaxNumberSteps = getMaxSteps(pRenderLayerIndex);

			final int lNumberOfPasses = getAdaptiveLODController().getNumberOfPasses();

			final int lPassIndex = getAdaptiveLODController().getPassIndex();
			final boolean lActive = getAdaptiveLODController().isActive();

			int lMaxSteps = lMaxNumberSteps;
			float lDithering = 0;
			float lPhase = 0;
			int lClear = 0;

			switch (getRenderAlgorithm(pRenderLayerIndex))
			{
			case MaxProjection:
				mCurrentRenderKernel = mMaxProjectionRenderKernel;
				lMaxSteps = lMaxNumberSteps / lNumberOfPasses;
				lDithering = getDithering(pRenderLayerIndex) * (1.0f * (lNumberOfPasses - lPassIndex) / lNumberOfPasses);
				lPhase = getAdaptiveLODController().getPhase();
				lClear = getAdaptiveLODController().isBufferClearingNeeded() ? 0
																																		: 1;
				mCLDevice.setArgs(mCurrentRenderKernel,
													mCLRenderBuffers[pRenderLayerIndex],
													getTextureWidth(),
													getTextureHeight(),
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
				break;
			case IsoSurface:
				mCurrentRenderKernel = mIsoSurfaceRenderKernel;

				lMaxSteps = (lMaxNumberSteps * (1 + lPassIndex)) / (2 * lNumberOfPasses);
				lDithering = (float) pow(	getDithering(pRenderLayerIndex) * (1.0f * (lNumberOfPasses - lPassIndex) / lNumberOfPasses),
																	2);
				lClear = 0;
				lPhase = 0;

				final float[] lLightVector = getLightVector();

				mCLDevice.setArgs(mCurrentRenderKernel,
													mCLRenderBuffers[pRenderLayerIndex],
													getTextureWidth(),
													getTextureHeight(),
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
													mCLTransferFunctionImages[pRenderLayerIndex],
													mCLInvProjectionBuffer,
													mCLInvModelViewBuffer,
													mCLVolumeImages[pRenderLayerIndex]);
				break;
			}

			/*
			 * System.out.format("steps=%d, dith=%g, phase=%g, clear=%d \n",
			 * lMaxSteps, lDithering, lPhase, lClear);/*
			 */

			mCLDevice.run(mCurrentRenderKernel,
										getTextureWidth(),
										getTextureHeight());

		}
		else
		{
			mCLDevice.setArgs(mClearKernel,
												mCLRenderBuffers[pRenderLayerIndex],
												getTextureWidth(),
												getTextureHeight());

			mCLDevice.run(mClearKernel,
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
				synchronized (getSetVolumeDataBufferLock(pRenderLayerIndex))
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
					/*
					 * System.out.format("Elapsedtime in '%s' is %g ms \n",
					 * lOpenCLProcessor.getClass().getSimpleName(),
					 * lElapsedTimeInMilliseconds);/*
					 */
				}
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
