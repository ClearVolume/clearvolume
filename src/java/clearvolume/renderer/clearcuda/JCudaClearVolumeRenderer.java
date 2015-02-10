package clearvolume.renderer.clearcuda;

/*
 * JCuda - Java bindings for NVIDIA CUDA driver and runtime API
 * http://www.jcuda.org
 *
 * Copyright 2009-2011 Marco Hutter - http://www.jcuda.org
 */

import static jcuda.driver.JCudaDriver.CU_TRSF_NORMALIZED_COORDINATES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import javax.media.opengl.GLEventListener;

import jcuda.CudaException;
import jcuda.Pointer;
import jcuda.driver.CUaddress_mode;
import jcuda.driver.CUfilter_mode;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import clearcuda.CudaArray;
import clearcuda.CudaCompiler;
import clearcuda.CudaContext;
import clearcuda.CudaDevice;
import clearcuda.CudaDevicePointer;
import clearcuda.CudaFunction;
import clearcuda.CudaModule;
import clearcuda.CudaOpenGLBufferObject;
import clearcuda.CudaTextureReference;
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.processors.CUDAProcessor;
import clearvolume.renderer.processors.Processor;

/**
 * Class JCudaClearVolumeRenderer
 *
 * Implements a JOGLPBOClearVolumeRenderer usin a CUDA kernel for rendering the
 * 2D mProjectionMatrix.
 *
 * @author Loic Royer 2014
 *
 */
public class JCudaClearVolumeRenderer extends JOGLClearVolumeRenderer	implements
GLEventListener
{

	private static final int cBlockSize = 32;

	/**
	 * CUDA context.
	 */
	private CudaDevice mCudaDevice;

	/**
	 * CUDA context.
	 */
	private CudaContext mCudaContext;

	/**
	 * CUDA module.
	 */
	private CudaModule mCudaModule;

	/**
	 * Volume rendering CUDA function
	 */
	private CudaFunction mVolumeRenderingFunction;

	private volatile CudaOpenGLBufferObject[] mOpenGLBufferDevicePointers;

	/**
	 * CUDA Device pointers to the device itself, which are in constant memory:
	 * inverted mViewMatrix-matrix, mProjectionMatrix matrix, transfer function.
	 */

	private CudaDevicePointer mInvertedViewMatrix,
	mInvertedProjectionMatrix, mSizeOfTransferFunction;

	/**
	 * CUDA arrays to the transfer function and volume data.
	 */
	private CudaArray[] mTransferFunctionCudaArrays = new CudaArray[1];
	private CudaArray[] mVolumeDataCudaArrays = new CudaArray[1];

	/**
	 * CUDA references to the transfer function and volume data textures.
	 */
	private CudaTextureReference mVolumeDataCudaTexture;
	private CudaTextureReference mTransferFunctionTexture;

	/**
	 * Pointer to kernel parameters.
	 */
	private Pointer mKernelParametersPointer;

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width and height.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 */
	public JCudaClearVolumeRenderer(final String pWindowName,
	                                final int pWindowWidth,
	                                final int pWindowHeight)
	{
		super("[CUDA] " + pWindowName, pWindowWidth, pWindowHeight);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 */
	public JCudaClearVolumeRenderer(final String pWindowName,
	                                final int pWindowWidth,
	                                final int pWindowHeight,
	                                final int pBytesPerVoxel)
	{
		super("[CUDA] " + pWindowName,
		      pWindowWidth,
		      pWindowHeight,
		      pBytesPerVoxel);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 */
	public JCudaClearVolumeRenderer(final String pWindowName,
	                                final int pWindowWidth,
	                                final int pWindowHeight,
	                                final int pBytesPerVoxel,
	                                final int pMaxTextureWidth,
	                                final int pMaxTextureHeight)
	{
		super("[CUDA] " + pWindowName,
		      pWindowWidth,
		      pWindowHeight,
		      pBytesPerVoxel,
		      pMaxTextureWidth,
		      pMaxTextureHeight);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param pNumberOfRenderLayers
	 */
	public JCudaClearVolumeRenderer(final String pWindowName,
	                                final int pWindowWidth,
	                                final int pWindowHeight,
	                                final int pBytesPerVoxel,
	                                final int pMaxTextureWidth,
	                                final int pMaxTextureHeight,
	                                final int pNumberOfRenderLayers,
	                                final boolean pUseInCanvas)
	{
		super("[CUDA] " + pWindowName,
		      pWindowWidth,
		      pWindowHeight,
		      pBytesPerVoxel,
		      pMaxTextureWidth,
		      pMaxTextureHeight,
		      pNumberOfRenderLayers,
		      pUseInCanvas);
		mTransferFunctionCudaArrays = new CudaArray[pNumberOfRenderLayers];
		mVolumeDataCudaArrays = new CudaArray[pNumberOfRenderLayers];
		mOpenGLBufferDevicePointers = new CudaOpenGLBufferObject[pNumberOfRenderLayers];
	}

	@Override
	protected void registerPBO(	final int pRenderLayerIndex,
	                           	final int pPixelBufferObjectId)
	{
		mOpenGLBufferDevicePointers[pRenderLayerIndex] = new CudaOpenGLBufferObject(pPixelBufferObjectId);
	}

	@Override
	protected void unregisterPBO(	final int pRenderLayerIndex,
	                             	final int pPixelBufferObjectId)
	{
		mOpenGLBufferDevicePointers[pRenderLayerIndex].close();
		mOpenGLBufferDevicePointers[pRenderLayerIndex] = null;
	}

	/**
	 * Initialises CUDA and the 3D texture with the current volume data.
	 *
	 * @throws IOException
	 */
	@Override
	protected boolean initVolumeRenderer()
	{
		try
		{
			mCudaDevice = new CudaDevice(0);

			mCudaContext = new CudaContext(mCudaDevice, true);

			final Class<?> lRootClass = JCudaClearVolumeRenderer.class;

			final File lPTXFile = compileCUDA(lRootClass);

			mCudaModule = CudaModule.moduleFromPTX(lPTXFile);

			mInvertedViewMatrix = mCudaModule.getGlobal("c_invViewMatrix");

			mInvertedProjectionMatrix = mCudaModule.getGlobal("c_invProjectionMatrix");
			mSizeOfTransferFunction = mCudaModule.getGlobal("c_sizeOfTransfertFunction");

			mVolumeRenderingFunction = mCudaModule.getFunction("volumerender");

			for (int i = 0; i < getNumberOfRenderLayers(); i++)
				prepareVolumeDataArray(i, null);

			prepareVolumeDataTexture();

			for (int i = 0; i < getNumberOfRenderLayers(); i++)
			{
				prepareTransferFunctionArray(i);
				copyTransferFunctionArray(i);
			}
			prepareTransferFunctionTexture();

			for (final Processor<?> lProcessor : mProcessorsMap.values())
				if (lProcessor.isCompatibleRenderer(getClass()))
				{
					final CUDAProcessor<?> lCUDAProcessor = (CUDAProcessor<?>) lProcessor;
					lCUDAProcessor.setDeviceAndContext(	mCudaDevice,
					                                   	mCudaContext);
				}

			return true;
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	private File compileCUDA(final Class<?> lRootClass) throws IOException
	{
		File lPTXFile;

		try
		{
			final CudaCompiler lCudaCompiler = new CudaCompiler(mCudaDevice,
			                                                    lRootClass.getSimpleName());

			lCudaCompiler.setParameter(	Pattern.quote("/*ProjectionAlgorithm*/"),
			                           	getProjectionAlgorithm().name());
			lCudaCompiler.setParameter(	Pattern.quote("/*BytesPerVoxel*/"),
			                           	"" + getBytesPerVoxel());

			final File lCUFile = lCudaCompiler.addFile(	lRootClass,
			                                           	"kernels/VolumeRenderPerspective.cu",
			                                           	true);

			lCudaCompiler.addFiles(	CudaCompiler.class,
			                       	true,
			                       	"includes/helper_cuda.h",
			                       	"includes/helper_math.h",
					"includes/helper_string.h");

			lPTXFile = lCudaCompiler.compile(lCUFile);
		}
		catch (final Exception e)
		{

			final InputStream lInputStreamPTXFile = lRootClass.getResourceAsStream("kernels/VolumeRender.backup.ptx");
			final StringWriter lStringWriter = new StringWriter();
			IOUtils.copy(	lInputStreamPTXFile,
			             	lStringWriter,
			             	Charset.defaultCharset());

			lPTXFile = File.createTempFile(	this.getClass().getSimpleName(),
					".ptx");
			FileUtils.write(lPTXFile, lStringWriter.toString());

		}

		return lPTXFile;
	}

	/**
	 * Allocates, configures and copies 3D volume data.
	 */
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

			mVolumeDataCudaArrays[pRenderLayerIndex] = new CudaArray(	1,
			                                                         	lWidth,
			                                                         	lHeight,
			                                                         	lDepth,
			                                                         	getBytesPerVoxel(),
			                                                         	false,
			                                                         	false,
			                                                         	false);
			lVolumeDataBuffer.rewind();

			mVolumeDataCudaArrays[pRenderLayerIndex].copyFrom(lVolumeDataBuffer,
			                                                  true);

		}
	}

	private void prepareVolumeDataTexture()
	{
		mVolumeDataCudaTexture = mCudaModule.getTexture("tex");
		mVolumeDataCudaTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
		mVolumeDataCudaTexture.setAddressMode(0,
		                                      CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		mVolumeDataCudaTexture.setAddressMode(1,
		                                      CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		mVolumeDataCudaTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);
	}

	private void pointTextureToArray(final int pRenderLayerIndex)
	{
		mVolumeDataCudaTexture.setTo(mVolumeDataCudaArrays[pRenderLayerIndex]);
		// mVolumeRenderingFunction.setTexture(mVolumeDataCudaTexture);
	}

	/**
	 * Allocates CUDA array for transfer function, configures texture
	 */
	private void prepareTransferFunctionArray(final int pRenderLayerIndex)
	{
		final float[] lTransferFunctionArray = getTransfertFunction(pRenderLayerIndex).getArray();
		final int lTransferFunctionArrayLength = lTransferFunctionArray.length;

		mTransferFunctionCudaArrays[pRenderLayerIndex] = new CudaArray(	4,
		                                                               	lTransferFunctionArrayLength / 4,
		                                                               	1,
		                                                               	1,
		                                                               	4,
		                                                               	true,
		                                                               	false,
		                                                               	false);

		mTransferFunctionCudaArrays[pRenderLayerIndex].copyFrom(lTransferFunctionArray,
		                                                        true);

	}

	/**
	 * Copies transfer function data.
	 */
	private void copyTransferFunctionArray(final int pRenderLayerIndex)
	{
		final float[] lTransferFunctionArray = getTransfertFunction(pRenderLayerIndex).getArray();

		mTransferFunctionCudaArrays[pRenderLayerIndex].copyFrom(lTransferFunctionArray,
		                                                        true);

	}

	private void prepareTransferFunctionTexture()
	{
		mTransferFunctionTexture = mCudaModule.getTexture("transferTex");

		mTransferFunctionTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
		mTransferFunctionTexture.setAddressMode(0,
		                                        CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		mTransferFunctionTexture.setAddressMode(1,
		                                        CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		mTransferFunctionTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);

	}

	private void pointTransferFunctionTextureToArray(final int pRenderLayerIndex)
	{
		mSizeOfTransferFunction.setFloat(getTransfertFunction(pRenderLayerIndex).getArray().length);
		mTransferFunctionTexture.setTo(mTransferFunctionCudaArrays[pRenderLayerIndex]);
		mVolumeRenderingFunction.setTexture(mTransferFunctionTexture);
	}

	/**
	 * Integral division, rounding the result to the next highest integer.
	 *
	 * @param a
	 *          Dividend
	 * @param b
	 *          Divisor
	 * @return a/b rounded to the next highest integer.
	 */
	private static int iDivUp(final int a, final int b)
	{
		return (a % b != 0) ? (a / b + 1) : (a / b);
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.jogl.JOGLClearVolumeFrameRenderer#renderVolume(float[],
	 *      float[])
	 */
	@Override
	protected boolean[] renderVolume(	final float[] invModelView,
	                                 	final float[] invProjection)
	{
		if (mCudaContext == null)
			return null;
		mCudaContext.setCurrent();

		try
		{
			mInvertedViewMatrix.copyFrom(invModelView, true);

			mInvertedProjectionMatrix.copyFrom(invProjection, true);

			return updateBufferAndRunKernel();
		}
		catch (final CudaException e)
		{
			System.err.println(e.getLocalizedMessage());
			return null;
		}
	}

	/**
	 * Call the kernel function, rendering the 3D volume data image into PBOs
	 *
	 * @return boolean array indicating which layer was updated.
	 */
	boolean[] updateBufferAndRunKernel()
	{
		final boolean[] lUpdatedLayer = new boolean[getNumberOfRenderLayers()];

		boolean lAnyVolumeDataUpdated = false;

		for (int lLayerIndex = 0; lLayerIndex < getNumberOfRenderLayers(); lLayerIndex++)
		{
			final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer(lLayerIndex);

			if (lVolumeDataBuffer != null)
			{
				synchronized (getSetVolumeDataBufferLock(lLayerIndex))
				{
					clearVolumeDataBufferReference(lLayerIndex);

					if (haveVolumeDimensionsChanged() || mVolumeDataCudaArrays[lLayerIndex] == null)
					{
						if (mVolumeDataCudaArrays[lLayerIndex] != null)
							mVolumeDataCudaArrays[lLayerIndex].close();

						prepareVolumeDataArray(lLayerIndex, lVolumeDataBuffer);
					}
					else
					{
						lVolumeDataBuffer.rewind();
						mVolumeDataCudaArrays[lLayerIndex].copyFrom(lVolumeDataBuffer,
						                                            true);
					}

					notifyCompletionOfDataBufferCopy(lLayerIndex);
					lAnyVolumeDataUpdated |= true;

					runProcessorsHook(lLayerIndex);
				}

			}
		}

		final long startTime = System.nanoTime();

		if (lAnyVolumeDataUpdated || getIsUpdateVolumeRenderingParameters())
			for (int i = 0; i < getNumberOfRenderLayers(); i++)
			{
				if (mVolumeDataCudaArrays[i] != null)
				{
					runKernel(i);
					lUpdatedLayer[i] = true;
				}
			}
		clearIsUpdateVolumeParameters();
		clearVolumeDimensionsChanged();

		final long endTime = System.nanoTime();

		/*System.out.println("time to render: " + (endTime - startTime)
												/ 1000000.
												+ " ms");/**/

		return lUpdatedLayer;
	}

	/**
	 * Runs 3D to 2D rendering kernel.
	 */
	private void runKernel(final int pRenderLayerIndex)
	{
		if (mOpenGLBufferDevicePointers[pRenderLayerIndex] == null)
			return;

		copyTransferFunctionArray(pRenderLayerIndex);

		pointTransferFunctionTextureToArray(pRenderLayerIndex);
		pointTextureToArray(pRenderLayerIndex);

		mOpenGLBufferDevicePointers[pRenderLayerIndex].map();
		mOpenGLBufferDevicePointers[pRenderLayerIndex].set(0, true);

		if (isLayerVisible(pRenderLayerIndex))
		{
			mVolumeRenderingFunction.setGridDim(iDivUp(	getTextureWidth(),
			                                           	cBlockSize),
			                                           	iDivUp(	getTextureHeight(),
			                                           	       	cBlockSize),
			                                           	       	1);

			mVolumeRenderingFunction.setBlockDim(cBlockSize, cBlockSize, 1);

			mVolumeRenderingFunction.launch(mOpenGLBufferDevicePointers[pRenderLayerIndex],
			                                getTextureWidth(),
			                                getTextureHeight(),
			                                (float) getBrightness(pRenderLayerIndex),
			                                (float) getTransferRangeMin(pRenderLayerIndex),
			                                (float) getTransferRangeMax(pRenderLayerIndex),
			                                (float) getGamma(pRenderLayerIndex));
			mCudaContext.synchronize();
		}

		mOpenGLBufferDevicePointers[pRenderLayerIndex].unmap();

	}

	private void runProcessorsHook(int pRenderLayerIndex)
	{
		for (final Processor<?> lProcessor : mProcessorsMap.values())
			if (lProcessor.isCompatibleRenderer(getClass()))
			{
				final CUDAProcessor<?> lCUDAProcessor = (CUDAProcessor<?>) lProcessor;
				lCUDAProcessor.applyToArray(mVolumeDataCudaArrays[pRenderLayerIndex]);
				lCUDAProcessor.process(	pRenderLayerIndex,
				                       	getVolumeSizeX(),
				                       	getVolumeSizeY(),
				                       	getVolumeSizeZ());
			}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.jogl.JOGLClearVolumeFrameRenderer#close()
	 */
	@Override
	public void close()
	{
		mDisplayReentrantLock.lock();
		try
		{

			final CudaContext lCudaContext = mCudaContext;
			mCudaContext = null;

			try
			{
				for (int i = 0; i < getNumberOfRenderLayers(); i++)
				{
					if (mVolumeDataCudaArrays[i] != null)
						mVolumeDataCudaArrays[i].close();
				}
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
				throw new RuntimeException(	"Exception while closing " + this.getClass()
																																			.getSimpleName(),
																		e);
			}

			try
			{
				for (int i = 0; i < getNumberOfRenderLayers(); i++)
				{
					if (mTransferFunctionCudaArrays[i] != null)
						mTransferFunctionCudaArrays[i].close();
				}
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
				throw new RuntimeException(	"Exception while closing " + this.getClass()
																																			.getSimpleName(),
																		e);
			}

			try
			{
				if (mCudaModule != null)
					mCudaModule.close();
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
				throw new RuntimeException(	"Exception while closing " + this.getClass()
																																			.getSimpleName(),
																		e);
			}

			try
			{
				if (mCudaContext != null)
					lCudaContext.close();
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
				throw new RuntimeException(	"Exception while closing " + this.getClass()
																																			.getSimpleName(),
																		e);
			}

			try
			{
				if (mCudaDevice != null)
					mCudaDevice.close();
				super.close();
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
				throw new RuntimeException(	"Exception while closing " + this.getClass()
																																			.getSimpleName(),
																		e);
			}

			super.close();

		}
		finally
		{
			if (mDisplayReentrantLock.isHeldByCurrentThread())
				mDisplayReentrantLock.unlock();
		}
	}

}