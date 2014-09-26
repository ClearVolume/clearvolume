package clearvolume.renderer.jcuda;

/*
 * JCuda - Java bindings for NVIDIA CUDA driver and runtime API
 * http://www.jcuda.org
 *
 * Copyright 2009-2011 Marco Hutter - http://www.jcuda.org
 */

import static jcuda.driver.JCudaDriver.CU_TRSF_NORMALIZED_COORDINATES;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;
import javax.media.opengl.GLEventListener;

import jcuda.Pointer;
import jcuda.driver.CUaddress_mode;
import jcuda.driver.CUfilter_mode;
import jcuda.runtime.dim3;
import clearcudaj.CudaArray;
import clearcudaj.CudaCompiler;
import clearcudaj.CudaContext;
import clearcudaj.CudaDevice;
import clearcudaj.CudaDevicePointer;
import clearcudaj.CudaFunction;
import clearcudaj.CudaModule;
import clearcudaj.CudaOpenGLBufferObject;
import clearcudaj.CudaTextureReference;
import clearvolume.renderer.jogl.JOGLPBOClearVolumeRenderer;

/**
 * Class JCudaClearVolumeRenderer
 * 
 * Implements a JOGLPBOClearVolumeRenderer usin a CUDA kernel for rendering the
 * 2D projection.
 *
 * @author Loic Royer 2014
 *
 */
public class JCudaClearVolumeRendererNew extends
																				JOGLPBOClearVolumeRenderer implements
																																	GLEventListener
{

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

	private volatile CudaOpenGLBufferObject mOpenGLBufferDevicePointer;

	/**
	 * CUDA Device pointers to the device itself, to the inverted view-matrix and
	 * to the size of the transfer function.
	 */
	private CudaDevicePointer mInvertedViewMatrix,
			mSizeOfTransfertFunction;

	/**
	 * CUDA arrays to the transfer function and volume data.
	 */
	private CudaArray mTransferFunctionCudaArray, mVolumeDataCudaArray;

	/**
	 * CUDA references to the transfer function and volume data textures.
	 */
	private CudaTextureReference mVolumeDataCudaTexture,
			mTransferFunctionTexture;

	/**
	 * Block dimensions.
	 */
	private final dim3 mBlockSize = new dim3(32, 32, 1);

	/**
	 * GRid dimensions.
	 */
	private dim3 mGridSize = new dim3(getTextureWidth() / mBlockSize.x,
																		getTextureHeight() / mBlockSize.y,
																		1);

	/**
	 * Pointer to kernel parameters.
	 */
	private Pointer mKernelParametersPointer;

	/**
	 * inverted view-matrix as float array.
	 */
	final float invViewMatrix[] = new float[12];

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width and height.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 */
	public JCudaClearVolumeRendererNew(	final String pWindowName,
																			final int pWindowWidth,
																			final int pWindowHeight)
	{
		super(pWindowName, pWindowWidth, pWindowHeight);
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
	public JCudaClearVolumeRendererNew(	final String pWindowName,
																			final int pWindowWidth,
																			final int pWindowHeight,
																			final int pBytesPerVoxel)
	{
		super(pWindowName, pWindowWidth, pWindowHeight, pBytesPerVoxel);
	}

	@Override
	protected void registerPBO(int pPixelBufferObjectId)
	{
		mOpenGLBufferDevicePointer = new CudaOpenGLBufferObject(pPixelBufferObjectId);
	}

	@Override
	protected void unregisterPBO(int pPixelBufferObjectId)
	{
		mOpenGLBufferDevicePointer.close();
		mOpenGLBufferDevicePointer = null;
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

			mCudaContext = new CudaContext(mCudaDevice);

			Class<?> lRootClass = JCudaClearVolumeRendererNew.class;
			CudaCompiler lCudaCompiler = new CudaCompiler(mCudaDevice,
																										lRootClass.getSimpleName());

			lCudaCompiler.setParameter(	Pattern.quote("/*ProjectionAlgorythm*/"),
																	getProjectionAlgorythm().name());
			lCudaCompiler.setParameter(	Pattern.quote("/*BytesPerVoxel*/"),
																	"" + getBytesPerVoxel());

			File lCUFile = lCudaCompiler.addFile(	lRootClass,
																						"kernels/VolumeRender.cu");

			lCudaCompiler.addFiles(	lRootClass,
															"kernels/helper_cuda.h",
															"kernels/helper_math.h",
															"kernels/helper_string.h");

			File lPTXFile = lCudaCompiler.compile(lCUFile);

			mCudaModule = CudaModule.moduleFromPTX(lPTXFile);

			mInvertedViewMatrix = mCudaModule.getGlobal("c_invViewMatrix");
			mSizeOfTransfertFunction = mCudaModule.getGlobal("c_sizeOfTransfertFunction");
			mSizeOfTransfertFunction.setFloat(getTransfertFunctionArray().length);

			mVolumeRenderingFunction = mCudaModule.getFunction("volumerender");

			prepareVolumeDataTexture(null);
			prepareTransfertFunctionTexture();





			calculateGridSize();

			return true;
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Allocates, configures and copies 3D volume data.
	 */
	private void prepareVolumeDataTexture(ByteBuffer pByteBuffer)
	{
		if (!isVolumeDataAvailable())
			return;
		synchronized (getSetVolumeDataBufferLock())
		{
			ByteBuffer lVolumeDataBuffer = pByteBuffer;
			if (lVolumeDataBuffer == null)
				lVolumeDataBuffer = getVolumeDataBuffer();

			final long lWidth = getVolumeSizeX();
			final long lHeight = getVolumeSizeY();
			final long lDepth = getVolumeSizeZ();

			mVolumeDataCudaArray = new CudaArray(	1,
																						lWidth,
																						lHeight,
																						lDepth,
																						getBytesPerVoxel(),
																						false,
																						false);
			mVolumeDataCudaArray.copyFrom(lVolumeDataBuffer, true);

			mVolumeDataCudaTexture = mCudaModule.getTexture("tex");
			mVolumeDataCudaTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
			mVolumeDataCudaTexture.setAddressMode(0,
																						CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
			mVolumeDataCudaTexture.setAddressMode(1,
																						CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
			mVolumeDataCudaTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);
			mVolumeDataCudaTexture.setTo(mVolumeDataCudaArray);
			mVolumeRenderingFunction.setTexture(mVolumeDataCudaTexture);


		}
	}

	/**
	 * Allocates CUDA array for transfer function, configures texture and copies
	 * transfer function data.
	 */
	private void prepareTransfertFunctionTexture()
	{
		final float[] lTransferFunctionArray = getTransfertFunctionArray();
		final int lTransferFunctionArrayLength = lTransferFunctionArray.length;

		mTransferFunctionCudaArray = new CudaArray(	4,
																								lTransferFunctionArrayLength / 4,
																								1,
																								1,
																								4,
																								true,
																								false);
		mTransferFunctionCudaArray.copyFloatsFrom(lTransferFunctionArray,
																							true);

		mTransferFunctionTexture = mCudaModule.getTexture("transferTex");

		mTransferFunctionTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
		mTransferFunctionTexture.setAddressMode(0,
																						CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		mTransferFunctionTexture.setAddressMode(1,
																						CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		mTransferFunctionTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);
		mTransferFunctionTexture.setTo(mTransferFunctionCudaArray);
		mVolumeRenderingFunction.setTexture(mTransferFunctionTexture);

	}

	private void calculateGridSize()
	{
		// Calculate new grid size
		mGridSize = new dim3(	iDivUp(getTextureWidth(), mBlockSize.x),
													iDivUp(getTextureHeight(), mBlockSize.y),
													1);
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
	 * @see clearvolume.renderer.jogl.JOGLPBOClearVolumeRenderer#renderVolume(javax.media.opengl.GL2,
	 *      float[])
	 */
	@Override
	protected void renderVolume(final GL2 gl, float[] modelView)
	{
		mCudaContext.setCurrent();

		// Build the inverted view matrix
		invViewMatrix[0] = modelView[0];
		invViewMatrix[1] = modelView[4];
		invViewMatrix[2] = modelView[8];
		invViewMatrix[3] = modelView[12];
		invViewMatrix[4] = modelView[1];
		invViewMatrix[5] = modelView[5];
		invViewMatrix[6] = modelView[9];
		invViewMatrix[7] = modelView[13];
		invViewMatrix[8] = modelView[2];
		invViewMatrix[9] = modelView[6];
		invViewMatrix[10] = modelView[10];
		invViewMatrix[11] = modelView[14];

		mInvertedViewMatrix.copyFloatsFrom(invViewMatrix, true);

		if (updateBufferAndRunKernel())
		{
			drawPBOToTextureToScreen(gl);
		}

	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.jogl.JOGLPBOClearVolumeRenderer#renderedImageHook(javax.media.opengl.GL2,
	 *      int)
	 */
	@Override
	public void renderedImageHook(final GL2 pGl,
																final int pPixelBufferObjectId)
	{
	}

	/**
	 * Call the kernel function, rendering the 3D volume data image into the PBO
	 * 
	 * @return
	 */
	boolean updateBufferAndRunKernel()
	{

		final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer();

		if (lVolumeDataBuffer != null)
		{
			synchronized (getSetVolumeDataBufferLock())
			{
				clearVolumeDataBufferReference();

				if (haveVolumeDimensionsChanged())
				{
					if (mVolumeDataCudaArray != null)
						mVolumeDataCudaArray.close();

					prepareVolumeDataTexture(lVolumeDataBuffer);
					clearVolumeDimensionsChanged();
				}
				else
				{
					mVolumeDataCudaArray.copyFrom(lVolumeDataBuffer, true);
				}

				notifyCompletionOfDataBufferCopy();
			}

		}

		if (mVolumeDataCudaArray != null)
		{
			runKernel();
			return true;
		}

		return false;
	}

	/**
	 * Runs 3D to 2D rendering kernel.
	 */
	private void runKernel()
	{
		if (mOpenGLBufferDevicePointer == null)
			return;

		if (getIsUpdateVolumeParameters())
		{
			mOpenGLBufferDevicePointer.map();
			mOpenGLBufferDevicePointer.set(0, true);

			mVolumeRenderingFunction.setGridDim(mGridSize.x, mGridSize.y, 1);

			mVolumeRenderingFunction.setBlockDim(	mBlockSize.x,
																						mBlockSize.y,
																						1);

			mVolumeRenderingFunction.launch(mOpenGLBufferDevicePointer,
																			getTextureWidth(),
																			getTextureHeight(),
																			(float) getScaleX(),
																			(float) getScaleY(),
																			(float) getScaleZ(),
																			(float) getBrightness(),
																			(float) getTransferRangeMin(),
																			(float) getTransferRangeMax(),
																			(float) getGamma());
			mCudaContext.synchronize();
			mOpenGLBufferDevicePointer.unmap();
			clearIsUpdateVolumeParameters();
		}

	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.renderer.jogl.JOGLPBOClearVolumeRenderer#close()
	 */
	@Override
	public void close()
	{
		try
		{
			mVolumeDataCudaArray.close();
			mTransferFunctionCudaArray.close();
			mCudaModule.close();
			mCudaContext.close();
			mCudaDevice.close();
			super.close();
		}
		catch (final Throwable e)
		{
			throw new RuntimeException(	"Exception while closing " + this.getClass()
																																		.getSimpleName(),
																	e);
		}
	}

}