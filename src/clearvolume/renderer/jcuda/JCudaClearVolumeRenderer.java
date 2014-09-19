package clearvolume.renderer.jcuda;

/*
 * JCuda - Java bindings for NVIDIA CUDA driver and runtime API
 * http://www.jcuda.org
 *
 * Copyright 2009-2011 Marco Hutter - http://www.jcuda.org
 */

import static jcuda.driver.JCudaDriver.CU_PARAM_TR_DEFAULT;
import static jcuda.driver.JCudaDriver.CU_TRSA_OVERRIDE_FORMAT;
import static jcuda.driver.JCudaDriver.CU_TRSF_NORMALIZED_COORDINATES;
import static jcuda.driver.JCudaDriver.cuArray3DCreate;
import static jcuda.driver.JCudaDriver.cuArrayCreate;
import static jcuda.driver.JCudaDriver.cuArrayDestroy;
import static jcuda.driver.JCudaDriver.cuCtxSynchronize;
import static jcuda.driver.JCudaDriver.cuGLMapBufferObject;
import static jcuda.driver.JCudaDriver.cuGLRegisterBufferObject;
import static jcuda.driver.JCudaDriver.cuGLUnmapBufferObject;
import static jcuda.driver.JCudaDriver.cuGLUnregisterBufferObject;
import static jcuda.driver.JCudaDriver.cuLaunchKernel;
import static jcuda.driver.JCudaDriver.cuMemcpy2D;
import static jcuda.driver.JCudaDriver.cuMemcpy3D;
import static jcuda.driver.JCudaDriver.cuMemcpyHtoD;
import static jcuda.driver.JCudaDriver.cuMemsetD32;
import static jcuda.driver.JCudaDriver.cuModuleGetGlobal;
import static jcuda.driver.JCudaDriver.cuModuleGetTexRef;
import static jcuda.driver.JCudaDriver.cuParamSetTexRef;
import static jcuda.driver.JCudaDriver.cuTexRefSetAddressMode;
import static jcuda.driver.JCudaDriver.cuTexRefSetArray;
import static jcuda.driver.JCudaDriver.cuTexRefSetFilterMode;
import static jcuda.driver.JCudaDriver.cuTexRefSetFlags;
import static jcuda.driver.JCudaDriver.cuTexRefSetFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.media.opengl.GL2;
import javax.media.opengl.GLEventListener;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUDA_ARRAY3D_DESCRIPTOR;
import jcuda.driver.CUDA_ARRAY_DESCRIPTOR;
import jcuda.driver.CUDA_MEMCPY2D;
import jcuda.driver.CUDA_MEMCPY3D;
import jcuda.driver.CUaddress_mode;
import jcuda.driver.CUarray;
import jcuda.driver.CUarray_format;
import jcuda.driver.CUdeviceptr;
import jcuda.driver.CUfilter_mode;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmemorytype;
import jcuda.driver.CUmodule;
import jcuda.driver.CUtexref;
import jcuda.runtime.dim3;
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
public class JCudaClearVolumeRenderer	extends
																			JOGLPBOClearVolumeRenderer implements
																																GLEventListener
{

	/**
	 * CUDA module.
	 */
	private final CUmodule mCUmodule = new CUmodule();

	/**
	 * Volume rendering CUDA function
	 */
	private CUfunction mVolumeRenderingFunction;

	/**
	 * CUDA Device pointers to the device itself, to the inverted view-matrix and
	 * to the size of the transfer function.
	 */
	final CUdeviceptr mCUdeviceptr = new CUdeviceptr(),
			mInvertedViewMatrix = new CUdeviceptr(),
			mSizeOfTransfertFunction = new CUdeviceptr();

	/**
	 * inverted view-matrix as float array.
	 */
	final float invViewMatrix[] = new float[12];

	/**
	 * CUDA arrays to the transfer fucntion and volume data.
	 */
	private CUarray mTransferFunctionCUarray, mVolumeDataCUarray;

	/**
	 * CUDA references to the transfer function and volume data textures.
	 */
	private CUtexref mVolumeDataTexture, mTransferFunctionTexture;

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

	@Override
	protected void registerPBO(int pPixelBufferObjectId)
	{
		// Register the PBO for usage with CUDA
		cuGLRegisterBufferObject(mPixelBufferObjectId);
	}

	@Override
	protected void unregisterPBO(int pPixelBufferObjectId)
	{
		// Unregisters the PBO for usage with CUDA
		cuGLUnregisterBufferObject(pPixelBufferObjectId);
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
			InputStream lInputStreamCUFile = JCudaClearVolumeRenderer.class.getResourceAsStream("kernels/VolumeRender.cu");
			InputStream lInputStreamBackupPTX = JCudaClearVolumeRenderer.class.getResourceAsStream("ptx/VolumeRender.ptx");

			if (lInputStreamCUFile == null)
				lInputStreamCUFile = JCudaClearVolumeRenderer.class.getResourceAsStream("./kernels/VolumeRender.cu");

			if (lInputStreamBackupPTX == null)
				lInputStreamBackupPTX = JCudaClearVolumeRenderer.class.getResourceAsStream("./ptx/VolumeRender.ptx");

			final HashMap<String, String> lSubstitutionMap = new HashMap<String, String>();

			lSubstitutionMap.put(	Pattern.quote("/*ProjectionAlgorythm*/"),
														getProjectionAlgorythm().name());
			lSubstitutionMap.put(	Pattern.quote("/*BytesPerVoxel*/"),
														"" + getBytesPerVoxel());

			mVolumeRenderingFunction = JCudaUtils.initCuda(	mCUmodule,
																											lInputStreamCUFile,
																											lInputStreamBackupPTX,
																											"_Z8d_renderPjjjfffffff",
																											lSubstitutionMap);

			// Obtain the global pointer to the inverted view matrix from
			// the module
			cuModuleGetGlobal(mInvertedViewMatrix,
												new long[1],
												mCUmodule,
												"c_invViewMatrix");

			cuModuleGetGlobal(mSizeOfTransfertFunction,
												new long[1],
												mCUmodule,
												"c_sizeOfTransfertFunction");

			cuMemcpyHtoD(mSizeOfTransfertFunction, Pointer.to(new float[]
			{ getTransfertFunctionArray().length }), 1 * Sizeof.FLOAT);

			mVolumeDataTexture = new CUtexref();
			mTransferFunctionTexture = new CUtexref();

			mTransferFunctionCUarray = new CUarray();

			prepareVolumeDataTexture();
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
	private void prepareVolumeDataTexture()
	{
		if (!isVolumeDataAvailable())
			return;
		synchronized (getSetVolumeDataBufferLock())
		{
			final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer();

			mVolumeDataCUarray = new CUarray();
			final long lSizeX = getVolumeSizeX();
			final long lSizeY = getVolumeSizeY();
			final long lSizeZ = getVolumeSizeZ();

			allocateVolumeDataTexture(mVolumeDataCUarray,
																lSizeX,
																lSizeY,
																lSizeZ);

			copyVolumeDataIntoTexture(mVolumeDataCUarray,
																lVolumeDataBuffer,
																lSizeX,
																lSizeY,
																lSizeZ);

			configureCudaTextureReference(mVolumeDataCUarray,
																		mVolumeDataTexture);
		}
	}

	/**
	 * Allocates CUDSA array for transfer function, configures texture and copies
	 * transfer function data.
	 */
	private void prepareTransfertFunctionTexture()
	{
		final float[] lTransferFunctionArray = getTransfertFunctionArray();

		allocateTransfertFunctionCUDAarray(	mTransferFunctionCUarray,
																				lTransferFunctionArray.length);

		configureCudaTransfertFunctionTextureReference(	mTransferFunctionCUarray,
																										mTransferFunctionTexture);

		copyTransfertFunctionTexture(	mTransferFunctionCUarray,
																	lTransferFunctionArray);
	}

	/**
	 * Allocates 3D CUDA array for storing the 3D volume data.
	 * 
	 * @param pVolumeArrayCUarray
	 * @param pVolumeSizeX
	 * @param pVolumeSizeY
	 * @param pVolumeSizeZ
	 */
	private void allocateVolumeDataTexture(	final CUarray pVolumeArrayCUarray,
																					final long pVolumeSizeX,
																					final long pVolumeSizeY,
																					final long pVolumeSizeZ)
	{
		final CUDA_ARRAY3D_DESCRIPTOR lAllocate3DArrayDescriptor = new CUDA_ARRAY3D_DESCRIPTOR();
		lAllocate3DArrayDescriptor.Width = pVolumeSizeX;
		lAllocate3DArrayDescriptor.Height = pVolumeSizeY;
		lAllocate3DArrayDescriptor.Depth = pVolumeSizeZ;
		if (getBytesPerVoxel() == 1)
		{
			lAllocate3DArrayDescriptor.Format = CUarray_format.CU_AD_FORMAT_UNSIGNED_INT8;
		}
		else if (getBytesPerVoxel() == 2)
		{
			lAllocate3DArrayDescriptor.Format = CUarray_format.CU_AD_FORMAT_UNSIGNED_INT16;
		}
		lAllocate3DArrayDescriptor.NumChannels = 1;
		/*System.out.format("cuArray3DCreate(%d,%d,%d)\n",
											mVolumeSizeX,
											mVolumeSizeY,
											mVolumeSizeZ);/**/
		cuArray3DCreate(pVolumeArrayCUarray, lAllocate3DArrayDescriptor);
		cuCtxSynchronize();
	}

	/**
	 * Destroys/frees the provided CUDA array.
	 * 
	 * @param pVolumeArrayCUarray
	 *          CUDA array to free/destroy.
	 */
	private void freeCudaArray(final CUarray pVolumeArrayCUarray)
	{
		if (pVolumeArrayCUarray != null)
			cuArrayDestroy(pVolumeArrayCUarray);
	}

	/**
	 * Allocates CUDA array for transfer function.
	 * 
	 * @param pTransferFunctionCUarray
	 * @param pTransferFunctionArrayLength
	 */
	private void allocateTransfertFunctionCUDAarray(final CUarray pTransferFunctionCUarray,
																									final int pTransferFunctionArrayLength)
	{
		// Create the 2D (float4) array that will contain the
		// transfer function data.
		final CUDA_ARRAY_DESCRIPTOR ad = new CUDA_ARRAY_DESCRIPTOR();
		ad.Format = CUarray_format.CU_AD_FORMAT_FLOAT;
		ad.Width = pTransferFunctionArrayLength / 4;
		ad.Height = 1;
		ad.NumChannels = 4;
		cuArrayCreate(pTransferFunctionCUarray, ad);
		cuCtxSynchronize();
	}

	/**
	 * Copies 3D volume data from ByteBuffer to CUDA array.
	 * 
	 * @param pVolumeArrayCUarray
	 * @param pByteBuffer
	 * @param pVolumeSizeX
	 * @param pVolumeSizeY
	 * @param pVolumeSizeZ
	 */
	private void copyVolumeDataIntoTexture(	final CUarray pVolumeArrayCUarray,
																					final ByteBuffer pByteBuffer,
																					final long pVolumeSizeX,
																					final long pVolumeSizeY,
																					final long pVolumeSizeZ)
	{
		// Copy the volume data data to the 3D array
		final CUDA_MEMCPY3D copy = new CUDA_MEMCPY3D();
		copy.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
		copy.srcHost = Pointer.to(pByteBuffer);
		copy.srcPitch = pVolumeSizeX * getBytesPerVoxel();
		copy.srcHeight = pVolumeSizeY;
		copy.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_ARRAY;
		copy.dstArray = pVolumeArrayCUarray;
		copy.dstPitch = pVolumeSizeX * getBytesPerVoxel();
		copy.dstHeight = pVolumeSizeY;
		copy.WidthInBytes = pVolumeSizeX * getBytesPerVoxel();
		copy.Height = pVolumeSizeY;
		copy.Depth = pVolumeSizeZ;

		cuMemcpy3D(copy);
		cuCtxSynchronize();
	}

	/**
	 * Copies transfer function from java array to CUDA array
	 * 
	 * @param pTransferFunctionCUarray
	 *          CUDA array
	 * @param pTransferFunctionArray
	 *          Java array
	 */
	private void copyTransfertFunctionTexture(final CUarray pTransferFunctionCUarray,
																						final float[] pTransferFunctionArray)
	{
		// Copy the transfer function data to the array
		final CUDA_MEMCPY2D copy2 = new CUDA_MEMCPY2D();
		copy2.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
		copy2.srcHost = Pointer.to(pTransferFunctionArray);
		copy2.srcPitch = pTransferFunctionArray.length * Sizeof.FLOAT;
		copy2.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_ARRAY;
		copy2.dstArray = pTransferFunctionCUarray;
		copy2.WidthInBytes = pTransferFunctionArray.length * Sizeof.FLOAT;
		copy2.Height = 1;
		cuMemcpy2D(copy2);
		cuCtxSynchronize();
	}

	/**
	 * Configures CUDA texture reference for transfer function.
	 * 
	 * @param pVolumeArrayCUarray
	 *          CUDA array
	 * @param pVolumeDataTexture
	 *          CUDA texture reference
	 */
	private void configureCudaTextureReference(	final CUarray pVolumeArrayCUarray,
																							final CUtexref pVolumeDataTexture)
	{
		// Obtain the 3D texture reference for the volume data from
		// the module, set its parameters and assign the 3D volume
		// data array as its reference.
		cuModuleGetTexRef(pVolumeDataTexture, mCUmodule, "tex");
		cuTexRefSetFilterMode(pVolumeDataTexture,
													CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
		cuTexRefSetAddressMode(	pVolumeDataTexture,
														0,
														CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		cuTexRefSetAddressMode(	pVolumeDataTexture,
														1,
														CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);

		if (getBytesPerVoxel() == 1)
		{
			cuTexRefSetFormat(pVolumeDataTexture,
												CUarray_format.CU_AD_FORMAT_UNSIGNED_INT8,
												1);
		}
		else if (getBytesPerVoxel() == 2)
		{
			cuTexRefSetFormat(pVolumeDataTexture,
												CUarray_format.CU_AD_FORMAT_UNSIGNED_INT16,
												1);
		}

		cuTexRefSetFlags(	pVolumeDataTexture,
											CU_TRSF_NORMALIZED_COORDINATES);
		cuTexRefSetArray(	pVolumeDataTexture,
											pVolumeArrayCUarray,
											CU_TRSA_OVERRIDE_FORMAT);

		cuParamSetTexRef(	mVolumeRenderingFunction,
											CU_PARAM_TR_DEFAULT,
											pVolumeDataTexture);
	}

	/**
	 * Configures CUDA texture reference for transfer function.
	 * 
	 * @param pTransferFunctionCUarray
	 *          CUDA array
	 * @param pTransfertFunctionCUtexref
	 *          CUDA texture reference
	 */
	private void configureCudaTransfertFunctionTextureReference(final CUarray pTransferFunctionCUarray,
																															final CUtexref pTransfertFunctionCUtexref)
	{
		cuModuleGetTexRef(pTransfertFunctionCUtexref,
											mCUmodule,
											"transferTex");
		cuTexRefSetFilterMode(pTransfertFunctionCUtexref,
													CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
		cuTexRefSetAddressMode(	pTransfertFunctionCUtexref,
														0,
														CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
		cuTexRefSetFlags(	pTransfertFunctionCUtexref,
											CU_TRSF_NORMALIZED_COORDINATES);
		cuTexRefSetFormat(pTransfertFunctionCUtexref,
											CUarray_format.CU_AD_FORMAT_FLOAT,
											4);
		cuTexRefSetArray(	pTransfertFunctionCUtexref,
											pTransferFunctionCUarray,
											CU_TRSA_OVERRIDE_FORMAT);
		// Set the texture references as parameters for the function call
		cuParamSetTexRef(	mVolumeRenderingFunction,
											CU_PARAM_TR_DEFAULT,
											pTransfertFunctionCUtexref);
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
	protected void renderVolume(final GL2 gl, final float[] modelView)
	{
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

		// Copy the inverted view matrix to the global variable that
		// was obtained from the module. The inverted view matrix
		// will be used by the kernel during rendering.
		cuMemcpyHtoD(	mInvertedViewMatrix,
									Pointer.to(invViewMatrix),
									invViewMatrix.length * Sizeof.FLOAT);

		// Render and fill the PBO with pixel data
		if (updateBufferAndRunKernel())
			drawPBOToTextureToScreen(gl);
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

				final long lSizeX = getVolumeSizeX();
				final long lSizeY = getVolumeSizeY();
				final long lSizeZ = getVolumeSizeZ();

				if (hasVolumeDimensionsChanged())
				{

					if (mVolumeDataCUarray == null)
						mVolumeDataCUarray = new CUarray();
					else
						freeCudaArray(mVolumeDataCUarray);

					allocateVolumeDataTexture(mVolumeDataCUarray,
																		lSizeX,
																		lSizeY,
																		lSizeZ);

					copyVolumeDataIntoTexture(mVolumeDataCUarray,
																		lVolumeDataBuffer,
																		lSizeX,
																		lSizeY,
																		lSizeZ);

					configureCudaTextureReference(mVolumeDataCUarray,
																				mVolumeDataTexture);
					clearVolumeDimensionsChanged();

				}
				else
				{

					copyVolumeDataIntoTexture(mVolumeDataCUarray,
																		lVolumeDataBuffer,
																		lSizeX,
																		lSizeY,
																		lSizeZ);

				}

				notifyCompletionOfDataBufferCopy();
			}

		}

		if (mVolumeDataCUarray != null)
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
		mKernelParametersPointer = Pointer.to(Pointer.to(mCUdeviceptr),
																					Pointer.to(new int[]
																					{ getTextureWidth() }),
																					Pointer.to(new int[]
																					{ getTextureHeight() }),
																					Pointer.to(new float[]
																					{ (float) getScaleX() }),
																					Pointer.to(new float[]
																					{ (float) getScaleY() }),
																					Pointer.to(new float[]
																					{ (float) getScaleZ() }),
																					Pointer.to(new float[]
																					{ (float) getBrightness() }),
																					Pointer.to(new float[]
																					{ (float) getTransferRangeMin() }),
																					Pointer.to(new float[]
																					{ (float) getTransferRangeMax() }),
																					Pointer.to(new float[]
																					{ (float) getGamma() }));

		/*System.out.format("min=%g, max=%g, gamma=%g \n",
											getTransferRangeMin(),
											getTransferRangeMax(),
											getGamma());/**/

		cuGLMapBufferObject(mCUdeviceptr,
												new long[1],
												mPixelBufferObjectId);

		if (getIsUpdateVolumeParameters())
		{
			cuMemsetD32(mCUdeviceptr,
									0,
									getTextureWidth() * getTextureHeight());
			cuLaunchKernel(	mVolumeRenderingFunction,
											mGridSize.x,
											mGridSize.y,
											1,
											mBlockSize.x,
											mBlockSize.y,
											1,
											0,
											null,
											mKernelParametersPointer,
											null);
			cuCtxSynchronize();
			clearIsUpdateVolumeParameters();
		}

		cuGLUnmapBufferObject(mPixelBufferObjectId);
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
			freeCudaArray(mVolumeDataCUarray);
			freeCudaArray(mTransferFunctionCUarray);
			JCudaUtils.closeCuda();
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