package clearvolume.renderer.clearopencl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GLEventListener;

import jcuda.CudaException;
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;

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
															final int pNumberOfRenderLayers)
	{

		super("[OpenCL] " + pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					pMaxTextureWidth,
					pMaxTextureHeight,
					pNumberOfRenderLayers);

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
		mCLDevice.compileKernel(OpenCLVolumeRenderer.class.getResource("kernels/volume_render.cl"),
														"max_project");

		mCLInvModelViewBuffer = mCLDevice.createInputFloatBuffer(16);
		mCLInvProjectionBuffer = mCLDevice.createInputFloatBuffer(16);

		int lRenderBufferSize = getTextureHeight() * getTextureWidth();

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
	protected void registerPBO(	int pRenderLayerIndex,
															int pPixelBufferObjectId)
	{
		// no need to do anything here, as we're not using PBOs
	}

	@Override
	protected void unregisterPBO(	int pRenderLayerIndex,
																int pPixelBufferObjectId)
	{
		// no need to do anything here, as we're not using PBOs
	}

	private void prepareVolumeDataArray(final int pRenderLayerIndex,
																			ByteBuffer pByteBuffer)
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

	private void prepareTransferFunctionArray(int pRenderLayerIndex)
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
	protected boolean[] renderVolume(	float[] pInvModelViewMatrix,
																		float[] pInvProjectionMatrix)
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
		catch (CudaException e)
		{
			System.err.println(e.getLocalizedMessage());
			return null;
		}

	}

	private boolean[] updateBufferAndRunKernel()
	{
		boolean[] lUpdated = new boolean[getNumberOfRenderLayers()];

		lUpdated[0] = true;

		boolean lAnyVolumeDataUpdated = false;

		for (int i = 0; i < getNumberOfRenderLayers(); i++)
		{
			ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer(i);

			if (lVolumeDataBuffer != null)
			{
				synchronized (getSetVolumeDataBufferLock(i))
				{
					clearVolumeDataBufferReference(i);

					if (haveVolumeDimensionsChanged() || mCLVolumeImages[i] == null)
					{
						if (mCLVolumeImages[i] != null)
						{

							mCLVolumeImages[i].release();
						}

						prepareVolumeDataArray(i, lVolumeDataBuffer);
					}
					else
					{
						lVolumeDataBuffer.rewind();

						fillWithByteBufferAsShort(mCLVolumeImages[i],
																			lVolumeDataBuffer);

					}

					notifyCompletionOfDataBufferCopy(i);
					lAnyVolumeDataUpdated |= true;
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

	private void fillWithByteBufferAsShort(	CLImage3D clImage3D,
																					ByteBuffer lVolumeDataBuffer)
	{
		lVolumeDataBuffer.rewind();
		mCLDevice.writeImage(clImage3D, lVolumeDataBuffer);
	}

	private void runKernel(int pRenderLayerIndex)
	{
		// System.out.println("kernel");
		// System.out.println(mCLVolumeImages[i].getHeight());

		prepareTransferFunctionArray(pRenderLayerIndex);

		mCLDevice.setArgs(mCLRenderBuffers[pRenderLayerIndex],
											getTextureWidth(),
											getTextureHeight(),
											(float) getBrightness(),
											(float) getTransferRangeMin(),
											(float) getTransferRangeMax(),
											(float) getGamma(),
											// mCLTranferFunctionImages[pRenderLayerIndex],
											mCLTransferColorBuffers[pRenderLayerIndex],
											mCLInvProjectionBuffer,
											mCLInvModelViewBuffer,
											mCLVolumeImages[pRenderLayerIndex]);

		// long startTime = System.nanoTime();

		mCLDevice.run(getTextureWidth(), getTextureHeight());

		copyBufferToTexture(pRenderLayerIndex,
												mCLDevice.readIntBufferAsByte(mCLRenderBuffers[pRenderLayerIndex]));

		// long endTime = System.nanoTime();

		// System.out.println("time to render: " + (endTime - startTime)
		// / 1000000.
		// + " ms");

	}
}
