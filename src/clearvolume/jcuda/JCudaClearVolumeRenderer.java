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
import static jcuda.driver.JCudaDriver.align;
import static jcuda.driver.JCudaDriver.cuArray3DCreate;
import static jcuda.driver.JCudaDriver.cuArrayCreate;
import static jcuda.driver.JCudaDriver.cuCtxSynchronize;
import static jcuda.driver.JCudaDriver.cuFuncSetBlockShape;
import static jcuda.driver.JCudaDriver.cuGLMapBufferObject;
import static jcuda.driver.JCudaDriver.cuGLRegisterBufferObject;
import static jcuda.driver.JCudaDriver.cuGLUnmapBufferObject;
import static jcuda.driver.JCudaDriver.cuGLUnregisterBufferObject;
import static jcuda.driver.JCudaDriver.cuLaunchGrid;
import static jcuda.driver.JCudaDriver.cuMemcpy2D;
import static jcuda.driver.JCudaDriver.cuMemcpy3D;
import static jcuda.driver.JCudaDriver.cuMemcpyHtoD;
import static jcuda.driver.JCudaDriver.cuMemsetD32;
import static jcuda.driver.JCudaDriver.cuModuleGetGlobal;
import static jcuda.driver.JCudaDriver.cuModuleGetTexRef;
import static jcuda.driver.JCudaDriver.cuParamSetSize;
import static jcuda.driver.JCudaDriver.cuParamSetTexRef;
import static jcuda.driver.JCudaDriver.cuParamSetv;
import static jcuda.driver.JCudaDriver.cuTexRefSetAddressMode;
import static jcuda.driver.JCudaDriver.cuTexRefSetArray;
import static jcuda.driver.JCudaDriver.cuTexRefSetFilterMode;
import static jcuda.driver.JCudaDriver.cuTexRefSetFlags;
import static jcuda.driver.JCudaDriver.cuTexRefSetFormat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

import com.jogamp.opengl.util.Animator;

public class JCudaClearVolumeRenderer implements GLEventListener
{

	private final GLCanvas mGLComponent;
	private final Animator Animator;
	private int pbo = 0;

	private int width = 0;
	private int height = 0;

	private final AtomicReference<ByteBuffer> mVolumeDataBuffer = new AtomicReference<ByteBuffer>();

	private final CUmodule mCUmodule = new CUmodule();
	private CUfunction mVolumeRenderingFunction;

	private final dim3 volumeSize = new dim3();
	private final dim3 blockSize = new dim3(16, 16, 1);
	private dim3 gridSize = new dim3(	width / blockSize.x,
																		height / blockSize.y,
																		1);

	private final CUdeviceptr mInvertedViewMatrix = new CUdeviceptr();
	private final float invViewMatrix[] = new float[12];

	private final float scaleX = 1.0f;
	private final float scaleY = 1.0f;
	private final float scaleZ = 1.0f;

	private float density = 0.05f;
	private float brightness = 1.0f;
	private float transferOffset = 0.0f;
	private float transferScale = 1.0f;

	private CUarray mTransferFunctionCUarray, mVolumeDataCUarray;
	private CUtexref mVolumeDataTexture, mTransferFunctionTexture;

	float translationX = 0, translationY = 0, translationZ = -4;
	float rotationX = 0, rotationY = 0;

	private int step = 0;
	private long prevTimeNS = -1;

	private final Frame frame;
	private final String mWindowName;
	private final String mProjectionAlgorythm = "MaxProjection";

	public JCudaClearVolumeRenderer(final String pWindowName,
																	final int pWindowWidth,
																	final int pWindowHeight,
																	final GLCapabilities capabilities,
																	final ByteBuffer pVolumeDataBuffer,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ)
	{
		mWindowName = pWindowName;
		mVolumeDataBuffer.set(pVolumeDataBuffer);
		volumeSize.x = sizeX;
		volumeSize.y = sizeY;
		volumeSize.z = sizeZ;

		width = pWindowWidth;
		height = pWindowHeight;

		// Initialize the GL component
		mGLComponent = new GLCanvas(capabilities);
		mGLComponent.addGLEventListener(this);

		// Initialize the mouse controls
		final MouseControl mouseControl = new MouseControl(this);
		mGLComponent.addMouseMotionListener(mouseControl);
		mGLComponent.addMouseWheelListener(mouseControl);

		// Create the main frame
		frame = new JFrame(mWindowName);
		frame.setLayout(new BorderLayout());
		mGLComponent.setPreferredSize(new Dimension(width, height));
		frame.add(mGLComponent, BorderLayout.CENTER);
		frame.add(createControlPanel(), BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);

		// Create and start the animator
		Animator = new Animator(mGLComponent);
		Animator.setRunAsFastAsPossible(true);
		Animator.start();
	}

	/**
	 * Create the control panel containing the sliders for setting the
	 * visualization parameters.
	 * 
	 * @return The control panel
	 */
	private JPanel createControlPanel()
	{
		final JPanel controlPanel = new JPanel(new GridLayout(2, 2));
		JPanel panel = null;
		JSlider slider = null;

		// Density
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Density:"));
		slider = new JSlider(0, 100, 5);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				density = a;
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Brightness
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Brightness:"));
		slider = new JSlider(0, 100, 10);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				brightness = a * 10;
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Transfer offset
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Transfer Offset:"));
		slider = new JSlider(0, 100, 55);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				transferOffset = (-0.5f + a) * 2;
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		// Transfer scale
		panel = new JPanel(new GridLayout(1, 2));
		panel.add(new JLabel("Transfer Scale:"));
		slider = new JSlider(0, 100, 10);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				final JSlider source = (JSlider) e.getSource();
				final float a = source.getValue() / 100.0f;
				transferScale = a * 10;
			}
		});
		slider.setPreferredSize(new Dimension(0, 0));
		panel.add(slider);
		controlPanel.add(panel);

		return controlPanel;
	}

	/**
	 * Implementation of GLEventListener: Called to initialize the GLAutoDrawable.
	 * This method will initialize the JCudaDriver and cause the initialization of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
		// Perform the default GL initialization
		final GL gl = drawable.getGL();
		gl.setSwapInterval(0);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		setupView(drawable);

		try
		{
			// Initialize CUDA with the current volume data

			initCuda();
			// Initialize the OpenGL pixel buffer object
			initPBO(gl);
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Initialize CUDA and the 3D texture with the current volume data.
	 * 
	 * @throws IOException
	 */
	void initCuda() throws IOException
	{
		final InputStream lInputStreamCUFile = JCudaClearVolumeRenderer.class.getResourceAsStream("./kernels/VolumeRender.cu");

		mVolumeRenderingFunction = JCudaUtils.initCuda(	mCUmodule,
																										lInputStreamCUFile,
																										"_Z8d_renderPjjjfffffff",
																										Collections.singletonMap(	Pattern.quote("/*ProjectionAlgorythm*/"),
																																							mProjectionAlgorythm));

		// Obtain the global pointer to the inverted view matrix from
		// the module
		cuModuleGetGlobal(mInvertedViewMatrix,
											new long[1],
											mCUmodule,
											"c_invViewMatrix");

		mTransferFunctionCUarray = new CUarray();
		mVolumeDataCUarray = new CUarray();

		mVolumeDataTexture = prepareVolumeDataTexture();
		mTransferFunctionTexture = prepareTransfertFunctionTexture();
	}

	private CUtexref prepareVolumeDataTexture()
	{
		final CUtexref lVolumeDataTexture = allocateVolumeDataTexture(mVolumeDataCUarray);

		final ByteBuffer lVolumeDataBuffer = mVolumeDataBuffer.getAndSet(null);
		copyVolumeDataIntoTexture(mVolumeDataCUarray, lVolumeDataBuffer);

		configureCudaTextureReference(mVolumeDataCUarray,
																	lVolumeDataTexture);

		return lVolumeDataTexture;
	}

	private CUtexref prepareTransfertFunctionTexture()
	{
		final float[] lTransferFunctionArray = getTransfertFunction();

		final CUtexref lTransfertFunctionTexture = new CUtexref();

		allocateTransfertFunctionTexture(	mTransferFunctionCUarray,
																			lTransferFunctionArray.length);

		configureCudaTransfertFunctionTextureReference(	mTransferFunctionCUarray,
																										lTransfertFunctionTexture);

		copyTransfertFunctionTexture(	mTransferFunctionCUarray,
																	lTransferFunctionArray);

		return lTransfertFunctionTexture;
	}

	private CUtexref allocateVolumeDataTexture(final CUarray pVolumeArrayCUarray)
	{
		final CUtexref lVolumeDataTexture = new CUtexref();

		// Create the 3D array that will contain the volume data
		// and will be accessed via the 3D texture
		final CUDA_ARRAY3D_DESCRIPTOR allocateArray = new CUDA_ARRAY3D_DESCRIPTOR();
		allocateArray.Width = volumeSize.x;
		allocateArray.Height = volumeSize.y;
		allocateArray.Depth = volumeSize.z;
		allocateArray.Format = CUarray_format.CU_AD_FORMAT_UNSIGNED_INT8;
		allocateArray.NumChannels = 1;
		cuArray3DCreate(pVolumeArrayCUarray, allocateArray);
		return lVolumeDataTexture;
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
	}

	public void copyVolumeDataIntoTexture(final ByteBuffer pByteBuffer)
	{
		copyVolumeDataIntoTexture(mVolumeDataCUarray, pByteBuffer);
	}

	private void copyVolumeDataIntoTexture(	final CUarray pVolumeArrayCUarray,
																					final ByteBuffer pByteBuffer)
	{
		// Copy the volume data data to the 3D array
		final CUDA_MEMCPY3D copy = new CUDA_MEMCPY3D();
		copy.srcMemoryType = CUmemorytype.CU_MEMORYTYPE_HOST;
		copy.srcHost = Pointer.to(pByteBuffer);
		copy.srcPitch = volumeSize.x;
		copy.srcHeight = volumeSize.y;
		copy.dstMemoryType = CUmemorytype.CU_MEMORYTYPE_ARRAY;
		copy.dstArray = pVolumeArrayCUarray;
		copy.dstPitch = volumeSize.x;
		copy.dstHeight = volumeSize.y;
		copy.WidthInBytes = volumeSize.x;
		copy.Height = volumeSize.y;
		copy.Depth = volumeSize.z;
		cuMemcpy3D(copy);
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

	private float[] getTransfertFunction()
	{
		// The RGBA components of the transfer function texture
		final float transferFunc[] = new float[]
		{ 0.0f,
			0.0f,
			0.0f,
			0.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			1.0f,
			0.5f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			1.0f,
			0.0f,
			1.0f,
			0.0f,
			1.0f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			1.0f,
			1.0f,
			0.0f,
			1.0f,
			1.0f,
			0.0f,
			0.0f,
			0.0f,
			0.0f };
		return transferFunc;
	}

	/**
	 * Creates a pixel buffer object (PBO) which stores the image that is created
	 * by the kernel, and which will later be rendered by JOGL.
	 * 
	 * @param gl
	 *          The GL context
	 */
	private void initPBO(final GL gl)
	{
		if (pbo != 0)
		{
			cuGLUnregisterBufferObject(pbo);
			gl.glDeleteBuffers(1, new int[]
			{ pbo }, 0);
			pbo = 0;
		}

		// Create and bind a pixel buffer object with the current
		// width and height of the rendering component.
		final int pboArray[] = new int[1];
		gl.glGenBuffers(1, pboArray, 0);
		pbo = pboArray[0];
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, pbo);
		gl.glBufferData(GL2.GL_PIXEL_UNPACK_BUFFER,
										width * height * Sizeof.BYTE * 4,
										null,
										GL.GL_DYNAMIC_DRAW);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);

		// Register the PBO for usage with CUDA
		cuGLRegisterBufferObject(pbo);

		// Calculate new grid size
		gridSize = new dim3(iDivUp(width, blockSize.x),
												iDivUp(height, blockSize.y),
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
	 * Set up a default view for the given GLAutoDrawable
	 * 
	 * @param drawable
	 *          The GLAutoDrawable to set the view for
	 */
	private void setupView(final GLAutoDrawable drawable)
	{
		final GL2 gl = drawable.getGL().getGL2();

		gl.glViewport(0, 0, drawable.getWidth(), drawable.getHeight());

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0.0, 1.0, 0.0, 1.0, 0.0, 1.0);
	}

	/**
	 * Call the kernel function, rendering the 3D volume data image into the PBO
	 */
	private void render()
	{
		final ByteBuffer lVolumeDataBuffer = mVolumeDataBuffer.getAndSet(null);
		if (lVolumeDataBuffer != null)
		{
			copyVolumeDataIntoTexture(mVolumeDataCUarray, lVolumeDataBuffer);
		}

		// Map the PBO to get a CUDA device pointer
		final CUdeviceptr d_output = new CUdeviceptr();
		cuGLMapBufferObject(d_output, new long[1], pbo);
		cuMemsetD32(d_output, 0, width * height);

		// Set up the execution parameters for the kernel:
		// - One pointer for the output that is mapped to the PBO
		// - Two ints for the width and height of the image to render
		// - Four floats for the visualization parameters of the renderer
		final Pointer dOut = Pointer.to(d_output);
		final Pointer pWidth = Pointer.to(new int[]
		{ width });
		final Pointer pHeight = Pointer.to(new int[]
		{ height });

		final Pointer pDensity = Pointer.to(new float[]
		{ density });

		final Pointer pScaleX = Pointer.to(new float[]
		{ scaleX });

		final Pointer pScaleY = Pointer.to(new float[]
		{ scaleY });

		final Pointer pScaleZ = Pointer.to(new float[]
		{ scaleZ });
		final Pointer pBrightness = Pointer.to(new float[]
		{ brightness });
		final Pointer pTransferOffset = Pointer.to(new float[]
		{ transferOffset });
		final Pointer pTransferScale = Pointer.to(new float[]
		{ transferScale });

		int offset = 0;

		offset = align(offset, Sizeof.POINTER);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								dOut,
								Sizeof.POINTER);
		offset += Sizeof.POINTER;

		offset = align(offset, Sizeof.INT);
		cuParamSetv(mVolumeRenderingFunction, offset, pWidth, Sizeof.INT);
		offset += Sizeof.INT;

		offset = align(offset, Sizeof.INT);
		cuParamSetv(mVolumeRenderingFunction, offset, pHeight, Sizeof.INT);
		offset += Sizeof.INT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pScaleX,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pScaleY,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pScaleZ,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pDensity,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pBrightness,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pTransferOffset,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		offset = align(offset, Sizeof.FLOAT);
		cuParamSetv(mVolumeRenderingFunction,
								offset,
								pTransferScale,
								Sizeof.FLOAT);
		offset += Sizeof.FLOAT;

		cuParamSetSize(mVolumeRenderingFunction, offset);

		// Call the CUDA kernel, writing the results into the PBO
		cuFuncSetBlockShape(mVolumeRenderingFunction,
												blockSize.x,
												blockSize.y,
												1);
		cuLaunchGrid(mVolumeRenderingFunction, gridSize.x, gridSize.y);
		cuCtxSynchronize();
		cuGLUnmapBufferObject(pbo);
	}

	/**
	 * Implementation of GLEventListener: Called when the given GLAutoDrawable is
	 * to be displayed.
	 */
	@Override
	public void display(final GLAutoDrawable drawable)
	{
		final GL2 gl = drawable.getGL().getGL2();

		// Use OpenGL to build view matrix
		final float modelView[] = new float[16];
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glRotatef(-rotationX, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(-rotationY, 0.0f, 1.0f, 0.0f);
		gl.glTranslatef(-translationX, -translationY, -translationZ);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelView, 0);
		gl.glPopMatrix();

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
		render();

		// Draw the image from the PBO
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glRasterPos2i(0, 0);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, pbo);
		gl.glDrawPixels(width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, 0);
		gl.glBindBuffer(GL2.GL_PIXEL_UNPACK_BUFFER, 0);

		// Update FPS information in main frame title
		step++;
		final long currentTime = System.nanoTime();
		if (prevTimeNS == -1)
		{
			prevTimeNS = currentTime;
		}
		final long diff = currentTime - prevTimeNS;
		if (diff > 1e9)
		{
			final double fps = (diff / 1e9) * step;
			String t = mWindowName + "- ";
			t += String.format("%.2f", fps) + " FPS";
			frame.setTitle(t);
			prevTimeNS = currentTime;
			step = 0;
		}

	}

	/**
	 * Implementation of GLEventListener: Called then the GLAutoDrawable was
	 * reshaped
	 */
	@Override
	public void reshape(final GLAutoDrawable drawable,
											final int x,
											final int y,
											final int width,
											final int height)
	{
		this.width = width;
		this.height = height;

		initPBO(drawable.getGL());

		setupView(drawable);

	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose(final GLAutoDrawable arg0)
	{
	}

	public void setVolumeDataBuffer(final ByteBuffer pByteBuffer)
	{
		mVolumeDataBuffer.compareAndSet(null, pByteBuffer);
	}

}