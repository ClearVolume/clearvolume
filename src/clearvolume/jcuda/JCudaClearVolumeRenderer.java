package clearvolume.jcuda;

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
import static jcuda.driver.JCudaDriver.cuGLUnmapBufferObject;
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
import java.util.Collections;
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
import clearvolume.jogl.JoglPBOVolumeRenderer;

public class JCudaClearVolumeRenderer extends JoglPBOVolumeRenderer	implements
																																		GLEventListener
{

	private final CUmodule mCUmodule = new CUmodule();
	private CUfunction mVolumeRenderingFunction;

	private CUdeviceptr mCUdeviceptr;

	final CUdeviceptr mInvertedViewMatrix = new CUdeviceptr();
	final float invViewMatrix[] = new float[12];

	private CUarray mTransferFunctionCUarray, mVolumeDataCUarray;
	private CUtexref mVolumeDataTexture, mTransferFunctionTexture;

	private final dim3 mBlockSize = new dim3(32, 32, 1);

	private dim3 mGridSize = new dim3(getTextureWidth() / mBlockSize.x,
																		getTextureHeight() / mBlockSize.y,
																		1);

	private Pointer mKernelParametersPointer;

	public JCudaClearVolumeRenderer(final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight)
	{
		super(pWindowName, pWindowWidth, pWindowHeight);
	}

	/**
	 * Initialize CUDA and the 3D texture with the current volume data.
	 * 
	 * @throws IOException
	 */
	@Override
	protected boolean initVolumeRenderer()
	{
		try
		{
			final InputStream lInputStreamCUFile = JCudaClearVolumeRenderer.class.getResourceAsStream("./kernels/VolumeRender.cu");

			mVolumeRenderingFunction = JCudaUtils.initCuda(	mCUmodule,
																											lInputStreamCUFile,
																											"_Z8d_renderPjjjfffffff",
																											Collections.singletonMap(	Pattern.quote("/*ProjectionAlgorythm*/"),
																																								getProjectionAlgorythm().name()));

			// Obtain the global pointer to the inverted view matrix from
			// the module
			cuModuleGetGlobal(mInvertedViewMatrix,
												new long[1],
												mCUmodule,
												"c_invViewMatrix");

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

	private void prepareVolumeDataTexture()
	{
		if (!isVolumeDataAvailable())
			return;
		synchronized (getSetVolumeDataBufferLock())
		{
			final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer();

			mVolumeDataCUarray = new CUarray();
			final int lSizeX = getVolumeSizeX();
			final int lSizeY = getVolumeSizeY();
			final int lSizeZ = getVolumeSizeZ();

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

	private void prepareTransfertFunctionTexture()
	{
		final float[] lTransferFunctionArray = getTransfertFunctionArray();

		allocateTransfertFunctionTexture(	mTransferFunctionCUarray,
																			lTransferFunctionArray.length);

		configureCudaTransfertFunctionTextureReference(	mTransferFunctionCUarray,
																										mTransferFunctionTexture);

		copyTransfertFunctionTexture(	mTransferFunctionCUarray,
																	lTransferFunctionArray);
	}

	private void allocateVolumeDataTexture(	final CUarray pVolumeArrayCUarray,
																					final int pVolumeSizeX,
																					final int pVolumeSizeY,
																					final int pVolumeSizeZ)
	{
		final CUDA_ARRAY3D_DESCRIPTOR lAllocate3DArrayDescriptor = new CUDA_ARRAY3D_DESCRIPTOR();
		lAllocate3DArrayDescriptor.Width = pVolumeSizeX;
		lAllocate3DArrayDescriptor.Height = pVolumeSizeY;
		lAllocate3DArrayDescriptor.Depth = pVolumeSizeZ;
		lAllocate3DArrayDescriptor.Format = CUarray_format.CU_AD_FORMAT_UNSIGNED_INT8;
		lAllocate3DArrayDescriptor.NumChannels = 1;
		/*System.out.format("cuArray3DCreate(%d,%d,%d)\n",
											mVolumeSizeX,
											mVolumeSizeY,
											mVolumeSizeZ);/**/
		cuArray3DCreate(pVolumeArrayCUarray, lAllocate3DArrayDescriptor);
		cuCtxSynchronize();
	}

	private void freeCudaArray(final CUarray pVolumeArrayCUarray)
	{
		cuArrayDestroy(pVolumeArrayCUarray);
	}

	private void allocateTransfertFunctionTexture(final CUarray pTransferFunctionCUarray,
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

	private void freeTransfertFunctionTexture(final CUarray pTransferFunctionCUarray)
	{
		cuArrayDestroy(pTransferFunctionCUarray);
	}

	/*
	public void copyVolumeDataIntoTexture(final ByteBuffer pByteBuffer)
	{
		copyVolumeDataIntoTexture(mVolumeDataCUarray, pByteBuffer);
	}/**/

	private void copyVolumeDataIntoTexture(	final CUarray pVolumeArrayCUarray,
																					final ByteBuffer pByteBuffer,
																					final int pVolumeSizeX,
																					final int pVolumeSizeY,
																					final int pVolumeSizeZ)
	{
		// Copy the volume data data to the 3D array
		final CUDA_MEMCPY3D copy = new CUDA_MEMCPY3D();
		copy.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
		copy.srcHost = Pointer.to(pByteBuffer);
		copy.srcPitch = pVolumeSizeX;
		copy.srcHeight = pVolumeSizeY;
		copy.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_ARRAY;
		copy.dstArray = pVolumeArrayCUarray;
		copy.dstPitch = pVolumeSizeX;
		copy.dstHeight = pVolumeSizeY;
		copy.WidthInBytes = pVolumeSizeX;
		copy.Height = pVolumeSizeY;
		copy.Depth = pVolumeSizeZ;

		/*
		System.out.format("cuMemcpy3D(%d,%d,%d) prod=%d and pByteBuffer.capacity()=%d \n",
											pVolumeSizeX,
											pVolumeSizeY,
											pVolumeSizeZ,
											pVolumeSizeX * pVolumeSizeY * pVolumeSizeZ,
											pByteBuffer.capacity());/**/
		cuMemcpy3D(copy);
		cuCtxSynchronize();
	}

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
		cuTexRefSetFormat(pVolumeDataTexture,
											CUarray_format.CU_AD_FORMAT_UNSIGNED_INT8,
											1);
		cuTexRefSetFlags(	pVolumeDataTexture,
											CU_TRSF_NORMALIZED_COORDINATES);
		cuTexRefSetArray(	pVolumeDataTexture,
											pVolumeArrayCUarray,
											CU_TRSA_OVERRIDE_FORMAT);

		cuParamSetTexRef(	mVolumeRenderingFunction,
											CU_PARAM_TR_DEFAULT,
											pVolumeDataTexture);
	}

	private void configureCudaTransfertFunctionTextureReference(final CUarray pTransferFunctionCUarray,
																															final CUtexref pTransfertFunctionCUtexref)
	{
		// Obtain the transfer texture reference from the module,
		// set its parameters and assign the transfer function
		// array as its reference.
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
		updateBufferAndRunKernel();

		// drawPBOToScreen(gl);
		drawPBOToTextureToScreen(gl);
	}

	/**
	 * Call the kernel function, rendering the 3D volume data image into the PBO
	 */
	void updateBufferAndRunKernel()
	{

		final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer();

		if (lVolumeDataBuffer != null)
		{
			synchronized (getSetVolumeDataBufferLock())
			{
				final int lSizeX = getVolumeSizeX();
				final int lSizeY = getVolumeSizeY();
				final int lSizeZ = getVolumeSizeZ();

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
			}

		}

		if (mVolumeDataCUarray != null)
			runKernel();
	}

	private void runKernel()
	{

		mCUdeviceptr = new CUdeviceptr();

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
																					{ (float) getDensity() }),
																					Pointer.to(new float[]
																					{ (float) getBrightness() }),
																					Pointer.to(new float[]
																					{ (float) getTransferOffset() }),
																					Pointer.to(new float[]
																					{ (float) getTransferScale() }));

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

	@Override
	public void close() throws IOException
	{
		freeCudaArray(mVolumeDataCUarray);
		freeCudaArray(mTransferFunctionCUarray);
		JCudaUtils.closeCuda();
		super.close();
	}
}