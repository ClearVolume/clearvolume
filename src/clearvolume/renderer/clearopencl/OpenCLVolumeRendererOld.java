package clearvolume.renderer.clearopencl;
//package clearvolume.renderer.clearopencl;
//
//import java.nio.ByteBuffer;
//import java.nio.FloatBuffer;
//import java.nio.ShortBuffer;
//
//import javax.media.opengl.GLEventListener;
//
//import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
//
//import com.nativelibs4java.opencl.CLBuffer;
//import com.nativelibs4java.opencl.CLImage3D;
//
//public class OpenCLVolumeRenderer extends JOGLClearVolumeRenderer	implements
//																																	GLEventListener
//{
//
//	private OpenCLDevice mCLDevice;
//
//	private FloatBuffer mInvModelViewBuffer, mInvProjectionBuffer;
//
//	private CLBuffer mCLInvModelViewBuffer, mCLInvProjectionBuffer;
//
//	private CLImage3D mCLVolumeImage;
//
//	private ShortBuffer mRenderBuffer;
//	private FloatBuffer mRenderBufferNormalized;
//
//	private CLBuffer mCLRenderBuffer;
//
//	public void foo()
//	{
//
//		System.out.println("FOO");
//		final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer();
//
//		System.out.println(lVolumeDataBuffer.capacity());
//
//	}
//
//	public OpenCLVolumeRenderer(String pWindowName,
//															int pWindowWidth,
//															int pWindowHeight)
//	{
//		super(pWindowName, pWindowWidth, pWindowHeight);
//
//	}
//
//	public OpenCLVolumeRenderer(final String pWindowName,
//															final int pWindowWidth,
//															final int pWindowHeight,
//															final int pBytesPerVoxel)
//	{
//		super(pWindowName, pWindowWidth, pWindowHeight, pBytesPerVoxel);
//	}
//
//	/**
//	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
//	 * name, width, height, and bytes=per-voxel.
//	 * 
//	 * @param pWindowName
//	 * @param pWindowWidth
//	 * @param pWindowHeight
//	 * @param pBytesPerVoxel
//	 */
//	public OpenCLVolumeRenderer(final String pWindowName,
//															final int pWindowWidth,
//															final int pWindowHeight,
//															final int pBytesPerVoxel,
//															final int pMaxTextureWidth,
//															final int pMaxTextureHeight)
//	{
//		super(pWindowName,
//					pWindowWidth,
//					pWindowHeight,
//					pBytesPerVoxel,
//					pMaxTextureWidth,
//					pMaxTextureHeight);
//	}
//
//	@Override
//	protected boolean initVolumeRenderer()
//	{
//		try
//		{
//
//			setRenderBackend(RENDER_BACKEND_OPENCL);
//
//			mCLDevice = new OpenCLDevice();
//
//			// FIXME using existing OpenGL context does not work yet
//			// mCLDevice.initCL(true);
//
//			mCLDevice.initCL(false);
//
//			mCLDevice.printInfo();
//			mCLDevice.compileKernel(OpenCLVolumeRenderer.class.getResource("kernels/volume_render.cl"),
//															"max_project_Short");
//
//			mCLInvModelViewBuffer = mCLDevice.createInputFloatBuffer(16);
//			mCLInvProjectionBuffer = mCLDevice.createInputFloatBuffer(16);
//
//			mCLRenderBuffer = mCLDevice.createOutputShortBuffer(getTextureWidth() * getTextureHeight());
//
//			mRenderBuffer = ShortBuffer.allocate(getTextureWidth() * getTextureHeight());
//
//			prepareVolumeDataTexture(null);
//
//			return true;
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return false;
//		}
//
//	}
//
//	@Override
//	protected void registerPBO(int pPixelBufferObjectId)
//	{
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	protected void unregisterPBO(int pPixelBufferObjectId)
//	{
//		// TODO Auto-generated method stub
//
//	}
//
//	private void prepareVolumeDataTexture(ByteBuffer pByteBuffer)
//	{
//
//		if (!isVolumeDataAvailable())
//			return;
//		synchronized (getSetVolumeDataBufferLock())
//		{
//			ByteBuffer lVolumeDataBuffer = pByteBuffer;
//			if (lVolumeDataBuffer == null)
//				lVolumeDataBuffer = getVolumeDataBuffer();
//
//			final long lWidth = getVolumeSizeX();
//			final long lHeight = getVolumeSizeY();
//			final long lDepth = getVolumeSizeZ();
//
//			System.out.println("prepare: " + lVolumeDataBuffer.capacity());
//
//			mCLVolumeImage = mCLDevice.createShortImage3D(lWidth,
//																										lHeight,
//																										lDepth);
//
//			// FIXME this is just a workaround as something crashes if no copying is
//			// done
//			ShortBuffer tmp = lVolumeDataBuffer.asShortBuffer();
//
//			short[] foo = new short[(int) (lWidth * lHeight * lDepth)];
//
//			for (int i = 0; i < foo.length; i++)
//			{
//				foo[i] = lVolumeDataBuffer.get(i);
//			}
//
//			mCLDevice.writeShortImage(mCLVolumeImage, ShortBuffer.wrap(foo));
//
//			// mVolumeDataCudaArray = new CudaArray( 1,
//			// lWidth,
//			// lHeight,
//			// lDepth,
//			// getBytesPerVoxel(),
//			// false,
//			// false,
//			// false);
//			// mVolumeDataCudaArray.copyFrom(lVolumeDataBuffer, true);
//			//
//			// mVolumeDataCudaTexture = mCudaModule.getTexture("tex");
//			// mVolumeDataCudaTexture.setFilterMode(CUfilter_mode.CU_TR_FILTER_MODE_LINEAR);
//			// mVolumeDataCudaTexture.setAddressMode(0,
//			// CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
//			// mVolumeDataCudaTexture.setAddressMode(1,
//			// CUaddress_mode.CU_TR_ADDRESS_MODE_CLAMP);
//			// mVolumeDataCudaTexture.setFlags(CU_TRSF_NORMALIZED_COORDINATES);
//			// mVolumeDataCudaTexture.setTo(mVolumeDataCudaArray);
//			// mVolumeRenderingFunction.setTexture(mVolumeDataCudaTexture);
//
//		}
//	}
//
//	@Override
//	protected boolean renderVolume(	float[] invModelView,
//																	float[] invProjection)
//	{
//		// mCudaContext.setCurrent();
//		//
//		// mInvertedViewMatrix.copyFrom(invModelView, true);
//		//
//		// mInvertedProjectionMatrix.copyFrom(invProjection, true);
//
//		mCLDevice.writeFloatBuffer(	mCLInvModelViewBuffer,
//																FloatBuffer.wrap(invModelView));
//
//		mCLDevice.writeFloatBuffer(	mCLInvProjectionBuffer,
//																FloatBuffer.wrap(invProjection));
//
//		return updateBufferAndRunKernel();
//	}
//
//	/**
//	 * Call the kernel function, rendering the 3D volume data image into the PBO
//	 * 
//	 * @return
//	 */
//	boolean updateBufferAndRunKernel()
//	{
//
//		final ByteBuffer lVolumeDataBuffer = getVolumeDataBuffer();
//
//		if (lVolumeDataBuffer != null)
//		{
//
//			System.out.println("haha");
//			System.out.println(lVolumeDataBuffer.capacity());
//			System.out.println(lVolumeDataBuffer.capacity());
//
//			synchronized (getSetVolumeDataBufferLock())
//			{
//
//				System.out.println("dims changed");
//				clearVolumeDataBufferReference();
//
//				if (haveVolumeDimensionsChanged())
//				{
//					// if (mVolumeDataCudaArray != null)
//					// mVolumeDataCudaArray.close();
//
//					prepareVolumeDataTexture(lVolumeDataBuffer);
//					clearVolumeDimensionsChanged();
//				}
//				else
//				{
//					System.out.println("writing!");
//					System.out.println(lVolumeDataBuffer.capacity());
//
//					final long lWidth = getVolumeSizeX();
//					final long lHeight = getVolumeSizeY();
//					final long lDepth = getVolumeSizeZ();
//
//					ShortBuffer tmp = lVolumeDataBuffer.asShortBuffer();
//
//					short[] foo = new short[(int) (lWidth * lHeight * lDepth)];
//
//					for (int i = 0; i < foo.length; i++)
//					{
//						foo[i] = lVolumeDataBuffer.get(i);
//					}
//
//					mCLDevice.writeShortImage(mCLVolumeImage,
//																		ShortBuffer.wrap(foo));
//				}
//
//				notifyCompletionOfDataBufferCopy();
//			}
//
//		}
//		else
//		{
//			System.out.println("VolumeDataBuffer is null");
//		}
//
//		if (mCLVolumeImage != null)
//		{
//			runKernel();
//			return true;
//		}
//		// if (mVolumeDataCudaArray != null)
//		// {
//		// runKernel();
//		// return true;
//		// }
//
//		return false;
//	}
//
//	private void runKernel()
//	{
//		if (getIsUpdateVolumeParameters())
//		{
//
//			mCLDevice.setArgs(mCLRenderBuffer,
//												getTextureWidth(),
//												getTextureHeight(),
//												mCLInvProjectionBuffer,
//												mCLInvModelViewBuffer,
//												mCLVolumeImage);
//
//			// System.out.println(mCLVolumeImage.getWidth());
//			//
//			// mCLDevice.setArgs(mCLVolumeImage);
//
//			mCLDevice.run(getTextureWidth(), getTextureHeight());
//
//			mRenderBuffer = mCLDevice.readShortBuffer(mCLRenderBuffer);
//
//			System.out.println("rendered:  " + mRenderBuffer.get(getTextureWidth() / 2
//																														+ getTextureHeight()
//																														/ 2
//																														* getTextureWidth()));
//
//			clearIsUpdateVolumeParameters();
//		}
//
//		// if (mOpenGLBufferDevicePointer == null)
//		// return;
//		//
//		// if (getIsUpdateVolumeParameters())
//		// {
//		// mOpenGLBufferDevicePointer.map();
//		// mOpenGLBufferDevicePointer.set(0, true);
//		//
//		// mVolumeRenderingFunction.setGridDim(iDivUp( getTextureWidth(),
//		// cBlockSize),
//		// iDivUp( getTextureHeight(),
//		// cBlockSize),
//		// 1);
//		//
//		// mVolumeRenderingFunction.setBlockDim(cBlockSize, cBlockSize, 1);
//		//
//		// mVolumeRenderingFunction.launch(mOpenGLBufferDevicePointer,
//		// getTextureWidth(),
//		// getTextureHeight(),
//		// (float) getScaleX(),
//		// (float) getScaleY(),
//		// (float) getScaleZ(),
//		// (float) getBrightness(),
//		// (float) getTransferRangeMin(),
//		// (float) getTransferRangeMax(),
//		// (float) getGamma());
//		// mCudaContext.synchronize();
//		// mOpenGLBufferDevicePointer.unmap();
//		// clearIsUpdateVolumeParameters();
//		// }
//
//	}
//
//	@Override
//	public FloatBuffer getRenderBuffer()
//	{
//
//		FloatBuffer tmp = FloatBuffer.allocate(100 * 100);
//
//		for (int i = 0; i < tmp.capacity(); i++)
//		{
//			tmp.put(1.f * i);
//		}
//		return tmp;
//
//	}
// }
