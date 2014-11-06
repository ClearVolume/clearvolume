package clearvolume.renderer.clearopencl;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GLEventListener;

import jcuda.CudaException;
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;

public class OpenCLVolumeRenderer extends JOGLClearVolumeRenderer	implements
																																	GLEventListener
{
	private OpenCLDevice mCLDevice;

	private CLBuffer<Integer>[] mCLRenderBuffers;

	private CLImage3D[] mCLVolumeImage;

	private ByteBuffer RenderBuffers;

	private CLBuffer mCLInvModelViewBuffer, mCLInvProjectionBuffer;

	public OpenCLVolumeRenderer(final String pWindowName,
															final int pWindowWidth,
															final int pWindowHeight)
	{
		super(pWindowName, pWindowWidth, pWindowHeight);

	}

	public OpenCLVolumeRenderer(final String pWindowName,
															final int pWindowWidth,
															final int pWindowHeight,
															final int pBytesPerVoxel)
	{
		super(pWindowName, pWindowWidth, pWindowHeight, pBytesPerVoxel);

	}

	public OpenCLVolumeRenderer(final String pWindowName,
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

	public OpenCLVolumeRenderer(final String pWindowName,
															final int pWindowWidth,
															final int pWindowHeight,
															final int pBytesPerVoxel,
															final int pMaxTextureWidth,
															final int pMaxTextureHeight,
															final int pNumberOfRenderLayers)
	{

		super(pWindowName,
					pWindowWidth,
					pWindowHeight,
					pBytesPerVoxel,
					pMaxTextureWidth,
					pMaxTextureHeight,
					pNumberOfRenderLayers);

		mCLRenderBuffers = new CLBuffer[pNumberOfRenderLayers];

		mCLVolumeImage = new CLImage3D[pNumberOfRenderLayers];

		// ByteBuffer foo = ByteBuffer.allocateDirect(100)
		// .order(ByteOrder.nativeOrder());

		// mTransferFunctionCudaArrays = new CudaArray[pNumberOfRenderLayers];
		// mVolumeDataCudaArrays = new CudaArray[pNumberOfRenderLayers];
		// mOpenGLBufferDevicePointers = new
		// CudaOpenGLBufferObject[pNumberOfRenderLayers];

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
														"test");

		int lRenderBufferSize = getTextureHeight() * getTextureWidth();

		System.out.println(lRenderBufferSize);

		// setting up the OpenCL Renderbuffer we will write the render result into
		for (int i = 0; i < getNumberOfRenderLayers(); i++)
		{
			mCLRenderBuffers[i] = (CLBuffer<Integer>) mCLDevice.createOutputIntBuffer(lRenderBufferSize);
		}
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

	@Override
	protected boolean[] renderVolume(	float[] pModelViewMatrix,
																		float[] pProjectionMatrix)
	{

		System.out.println("render");
		try
		{
			// mInvertedViewMatrix.copyFrom(invModelView, true);
			//
			// mInvertedProjectionMatrix.copyFrom(invProjection, true);

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

					if (haveVolumeDimensionsChanged() || mCLVolumeImage[i] == null)
					{
						if (mCLVolumeImage[i] != null)
						{

							mCLVolumeImage[i].release();
						}

						prepareVolumeDataArray(i, lVolumeDataBuffer);
					}
					else
					{
						lVolumeDataBuffer.rewind();
						// CLVolumeImage[i].copyFrom(lVolumeDataBuffer, true);
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
				if (mCLVolumeImage[i] != null)
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

			mCLVolumeImage[pRenderLayerIndex] = mCLDevice.createImage3D(lWidth,
																																	lHeight,
																																	lDepth,
																																	CLImageFormat.ChannelDataType.SignedInt16);

			lVolumeDataBuffer.rewind();

			// FIXME: somehow, opencl doesnt like writing from direct allocated
			// ByteBuffer!!!!
			// so we have to copy it unfortunately, waitung for the magic workaround
			// Loic will surely come up with

			ByteBuffer tmp = ByteBuffer.allocate(lVolumeDataBuffer.capacity());

			ShortBuffer tmp2 = ShortBuffer.allocate(lVolumeDataBuffer.capacity() / 2);

			tmp.put(lVolumeDataBuffer);
			tmp.rewind();

			tmp2.put(tmp.getShort());

			mCLDevice.writeShortImage(mCLVolumeImage[pRenderLayerIndex],
																tmp2);

		}
	}

	private void runKernel(int i)
	{
		System.out.println("kernel");
		System.out.println(mCLVolumeImage[i].getHeight());

		mCLDevice.setArgs(mCLRenderBuffers[i],
											getTextureWidth(),
											getTextureHeight(),
											mCLVolumeImage[i]);

		mCLDevice.run(getTextureWidth(), getTextureHeight());

		copyBufferToTexture(i,
												mCLDevice.readIntBufferAsByte(mCLRenderBuffers[i]));

	}

}

// private boolean[] updateBufferAndRunKernel()
// {
// System.out.println("render");
// final int size = getTextureHeight() * getTextureWidth() * 4;
//
// mCLDevice.setArgs(CLRenderBuffers[0],
// getTextureWidth(),
// getTextureHeight());
//
// System.out.println("running!");
// mCLDevice.run(getTextureWidth(), getTextureHeight());
//
// for (int i = 0; i < getNumberOfRenderLayers(); i++)
// {
// copyBufferToTexture(i,
// mCLDevice.readIntBufferAsByte(CLRenderBuffers[i]));
// }
//
// boolean[] retArray = new boolean[getNumberOfRenderLayers()];
// Arrays.fill(retArray, true);
//
// return retArray;
// }
//
// }

