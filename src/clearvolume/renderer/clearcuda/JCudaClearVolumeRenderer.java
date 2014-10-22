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

/**
 * Class JCudaClearVolumeRenderer
 * 
 * Implements a JOGLPBOClearVolumeRenderer usin a CUDA kernel for rendering the
 * 2D projection.
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

	private volatile CudaOpenGLBufferObject mOpenGLBufferDevicePointer;

	/**
	 * CUDA Device pointers to the device itself, whcih are in constant memory:
	 * inverted view-matrix, projection matrix, transfer function.
	 */

	private CudaDevicePointer mInvertedViewMatrix,
			mInvertedProjectionMatrix, mSizeOfTransfertFunction;

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
	public JCudaClearVolumeRenderer(final String pWindowName,
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
	public JCudaClearVolumeRenderer(final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final int pBytesPerVoxel)
	{
		super(pWindowName, pWindowWidth, pWindowHeight, pBytesPerVoxel);
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
																	final int pBytesPerVoxel,
																	final int pMaxTextureWidth,
																	final int pMaxTextureHeight)
	{
		super(pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					pMaxTextureWidth,
					pMaxTextureHeight);
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

			mCudaContext = new CudaContext(mCudaDevice, true);

			Class<?> lRootClass = JCudaClearVolumeRenderer.class;

			File lPTXFile = compileCUDA(lRootClass);

			mCudaModule = CudaModule.moduleFromPTX(lPTXFile);

			mInvertedViewMatrix = mCudaModule.getGlobal("c_invViewMatrix");
			mInvertedProjectionMatrix = mCudaModule.getGlobal("c_invProjectionMatrix");
			mSizeOfTransfertFunction = mCudaModule.getGlobal("c_sizeOfTransfertFunction");
			mSizeOfTransfertFunction.setFloat(getTransfertFunctionArray().length);

			mVolumeRenderingFunction = mCudaModule.getFunction("volumerender");

			prepareVolumeDataTexture(null);
			prepareTransfertFunctionTexture();

			return true;
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	private File compileCUDA(Class<?> lRootClass) throws IOException
	{
		File lPTXFile;

		try
		{
			CudaCompiler lCudaCompiler = new CudaCompiler(mCudaDevice,
																										lRootClass.getSimpleName());

			lCudaCompiler.setParameter(	Pattern.quote("/*ProjectionAlgorythm*/"),
																	getProjectionAlgorythm().name());
			lCudaCompiler.setParameter(	Pattern.quote("/*BytesPerVoxel*/"),
																	"" + getBytesPerVoxel());

			File lCUFile = lCudaCompiler.addFile(	lRootClass,
																						"kernels/VolumeRenderPerspective.cu");

			lCudaCompiler.addFiles(	lRootClass,
															"kernels/helper_cuda.h",
															"kernels/helper_math.h",
															"kernels/helper_string.h");

			lPTXFile = lCudaCompiler.compile(lCUFile);
		}
		catch (Exception e)
		{

			InputStream lInputStreamPTXFile = lRootClass.getResourceAsStream("kernels/VolumeRender.backup.ptx");
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
																								false,
																								false);
		mTransferFunctionCudaArray.copyFrom(lTransferFunctionArray, true);

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
	 * @see clearvolume.renderer.jogl.JOGLClearVolumeRenderer#renderVolume(javax.media.opengl.GL2,
	 *      float[])
	 */
	@Override
	protected boolean renderVolume(	float[] modelView,
																	float[] invProjection)
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

		mInvertedViewMatrix.copyFrom(invViewMatrix, true);

		mInvertedProjectionMatrix.copyFrom(invProjection, true);

		return updateBufferAndRunKernel();
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

			mVolumeRenderingFunction.setGridDim(iDivUp(	getTextureWidth(),
																									cBlockSize),
																					iDivUp(	getTextureHeight(),
																									cBlockSize),
																					1);

			mVolumeRenderingFunction.setBlockDim(cBlockSize, cBlockSize, 1);

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
	 * @see clearvolume.renderer.jogl.JOGLClearVolumeRenderer#close()
	 */
	@Override
	public void close()
	{
		try
		{
			if (mVolumeDataCudaArray != null)
				mVolumeDataCudaArray.close();
			mTransferFunctionCudaArray.close();
			mCudaModule.close();
			mCudaContext.close();
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
	}

}