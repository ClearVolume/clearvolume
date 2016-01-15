package clearvolume.renderer.opencl;

import static java.lang.Math.max;
import static java.lang.Math.pow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cleargl.RendererInterface;
import org.bridj.Pointer;

import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GL;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

import clearvolume.exceptions.ClearVolumeMemoryException;
import clearvolume.exceptions.ClearVolumeUnsupportdDataTypeException;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.cleargl.overlay.o2d.HistogramOverlay;
import clearvolume.renderer.cleargl.overlay.o3d.DriftOverlay;
import clearvolume.renderer.processors.OpenCLProcessor;
import clearvolume.renderer.processors.ProcessorInterface;
import clearvolume.renderer.processors.impl.OpenCLCenterMass;
import clearvolume.renderer.processors.impl.OpenCLDeconvolutionLR;
import clearvolume.renderer.processors.impl.OpenCLDenoise;
import clearvolume.renderer.processors.impl.OpenCLHistogram;
import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.types.NativeTypeEnum;
import jcuda.CudaException;
import cleargl.scenegraph.Scene;

public class OpenCLVolumeRenderer extends ClearGLVolumeRenderer	implements
																GLEventListener, RendererInterface
{
	private OpenCLDevice mCLDevice;

	private CLBuffer<Integer>[] mCLRenderBuffers;
	private CLImage3D[] mCLVolumeImages;
	private CLImage2D[] mCLTransferFunctionImages;

	private float[] invMv;

	private CLBuffer<Float> mCLInvModelViewBuffer,
			mCLInvProjectionBuffer;

	private CLKernel mCurrentRenderKernel,
			mMaxProjectionRenderKernel, mIsoSurfaceRenderKernel,
			mClearKernel;

	private Pointer<Integer> mTransferBuffer;

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
		super(	"[OpenCL] " + pWindowName,
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
		super(	"[OpenCL] " + pWindowName,
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
		this(	pWindowName,
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

		super(	"[OpenCL] " + pWindowName,
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

		final HistogramOverlay lHistogramOverlay = new HistogramOverlay(lHistoProcessor);
		addOverlay(lHistogramOverlay);

		lHistogramOverlay.setDisplayed(false);

		final OpenCLDenoise lOpenCLDenoise = new OpenCLDenoise();
		addProcessor(lOpenCLDenoise);

		final OpenCLDeconvolutionLR lOpenCLDeconvolutionLR = new OpenCLDeconvolutionLR();
		addProcessor(lOpenCLDeconvolutionLR);

		final DriftOverlay lDriftOverlay = new DriftOverlay(this);
		addOverlay(lDriftOverlay);
		final OpenCLCenterMass lOpenCLCenterMass = new OpenCLCenterMass();
		addProcessor(lOpenCLCenterMass);
		lOpenCLCenterMass.addResultListener(lDriftOverlay);
		lDriftOverlay.setDisplayed(false);
		lOpenCLCenterMass.setActive(false);

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

		mIsoSurfaceRenderKernel = mCLDevice.compileKernel(	OpenCLVolumeRenderer.class.getResource("kernels/VolumeRender.cl"),
															"isosurface_render");

		for (final ProcessorInterface<?> lProcessor : mProcessorInterfacesMap.values())
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
		final int lRenderBufferSize = getRenderHeight() * getRenderWidth();

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

			final long lWidth = getVolumeSizeX(pRenderLayerIndex);
			final long lHeight = getVolumeSizeY(pRenderLayerIndex);
			final long lDepth = getVolumeSizeZ(pRenderLayerIndex);

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

			mCLTransferFunctionImages[pRenderLayerIndex] = mCLDevice.createGenericImage2D(	lNeededWidth,
																							1,
																							CLImageFormat.ChannelOrder.RGBA,
																							CLImageFormat.ChannelDataType.Float);
		}

		mCLDevice.writeImage(	mCLTransferFunctionImages[pRenderLayerIndex],
								FloatBuffer.wrap(lTransferFunctionArray));

	}

	@Override
	public boolean[] renderVolume(	final float[] pInvModelViewMatrix,
																	final float[] pInvProjectionMatrix) {
		invMv = pInvModelViewMatrix;

		boolean[] changed = new boolean[getNumberOfRenderLayers()];

		for(int i = 0; i < getNumberOfRenderLayers(); i++) {
			changed = renderVolume(pInvModelViewMatrix, pInvProjectionMatrix, i);
		}

		return changed;
	}

	public boolean[] renderVolume(	final float[] pInvModelViewMatrix,
										final float[] pInvProjectionMatrix, final int layerNum)
	{
		invMv = pInvModelViewMatrix;
		doCaptureBuffersIfNeeded();

		// System.out.println("render");
		try
		{
			mCLDevice.writeFloatBuffer(	mCLInvModelViewBuffer,
										FloatBuffer.wrap(pInvModelViewMatrix));

			mCLDevice.writeFloatBuffer(	mCLInvProjectionBuffer,
										FloatBuffer.wrap(pInvProjectionMatrix));

			return updateBufferAndRunKernel(layerNum);
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
			for (int l = 0; l < getNumberOfRenderLayers(); l++)
			{
				final ByteBuffer lCaptureBuffer;

				synchronized (getSetVolumeDataBufferLock(l))
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
	}

	private boolean[] updateBufferAndRunKernel() {
		boolean[] updated = new boolean[getNumberOfRenderLayers()];

		for(int i = 0; i < getNumberOfRenderLayers(); i++) {
			updated = updateBufferAndRunKernel(i);
		}

		return updated;
	}

	private boolean[] updateBufferAndRunKernel(int layerNum)
	{
		final boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

		lUpdated[0] = true;

		boolean lAnyVolumeDataUpdated = false;

		if (isVolumeDataUpdateAllowed())
		{
			final int lLayerIndex = layerNum;
			{
				synchronized (getSetVolumeDataBufferLock(lLayerIndex))
				{
					final FragmentedMemoryInterface lVolumeDataBuffer = getVolumeDataBuffer(lLayerIndex);

					if (lVolumeDataBuffer != null)
					{

						clearVolumeDataBufferReference(lLayerIndex);

						if (haveVolumeDimensionsChanged(lLayerIndex) || mCLVolumeImages[lLayerIndex] == null)
						{
							if (mCLVolumeImages[lLayerIndex] != null)
							{

								mCLVolumeImages[lLayerIndex].release();
							}

							prepareVolumeDataArray(	lLayerIndex,
													lVolumeDataBuffer);
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

			clearVolumeDimensionsChanged();
		}

		if (lAnyVolumeDataUpdated || haveVolumeRenderingParametersChanged()
			|| getAdaptiveLODController().isKernelRunNeeded())
		{

				if (mCLVolumeImages[layerNum] != null)
				{
					runKernel(layerNum);
					lUpdated[layerNum] = true;
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
				lMaxSteps = max(16, lMaxNumberSteps / lNumberOfPasses);
				lDithering = getDithering(pRenderLayerIndex) * (1.0f * (lNumberOfPasses - lPassIndex) / lNumberOfPasses);
				lPhase = getAdaptiveLODController().getPhase();
				lClear = (lPassIndex == 0) ? 0 : 1;
				float[] lClipBox = getClipBox();

				mCLDevice.setArgs(	mCurrentRenderKernel,
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
								(lMaxNumberSteps * (1 + lPassIndex)) / (2 * lNumberOfPasses));
				lDithering = (float) pow(	getDithering(pRenderLayerIndex) * (1.0f * (lNumberOfPasses - lPassIndex) / lNumberOfPasses),
											2);
				lPhase = getAdaptiveLODController().getPhase();
				lClear = (lPassIndex == lNumberOfPasses - 1) || (lPassIndex == 0)	? 0
																					: 1;

				final float[] lLightVector = getLightVector();

				mCLDevice.setArgs(	mCurrentRenderKernel,
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

			mCLDevice.run(	mCurrentRenderKernel,
							getRenderWidth(),
							getRenderHeight());

		}
		else
		{
			mCLDevice.setArgs(	mClearKernel,
								mCLRenderBuffers[pRenderLayerIndex],
								getRenderWidth(),
								getRenderHeight());

			mCLDevice.run(	mClearKernel,
							getRenderWidth(),
							getRenderHeight());

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

	private void runProcessorHook(final int pRenderLayerIndex)
	{

		for (final ProcessorInterface<?> lProcessor : mProcessorInterfacesMap.values())
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
										getVolumeSizeX(pRenderLayerIndex),
										getVolumeSizeY(pRenderLayerIndex),
										getVolumeSizeZ(pRenderLayerIndex));
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

	@Override
	public GL getGL() {
		return super.getGL();
	}

	@Override
	public Scene getScene() {
		return this.scene;
	}

	@Override
	public void setScene(Scene s) {
		this.scene = s;
	}

}
