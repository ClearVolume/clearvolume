package clearvolume.renderer.jogl;

import static java.lang.Math.max;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLProfile;

import org.apache.commons.io.IOUtils;

import cleargl.ClearGLEventListener;
import cleargl.ClearGLWindow;
import cleargl.GLAttribute;
import cleargl.GLFloatArray;
import cleargl.GLMatrix;
import cleargl.GLPixelBufferObject;
import cleargl.GLProgram;
import cleargl.GLTexture;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;
import clearvolume.renderer.ClearVolumeRendererBase;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

/**
 * Abstract Class JoglPBOVolumeRenderer
 *
 * Classes that derive from this abstract class are provided with basic
 * JOGL-based display capability for implementing a ClearVolumeRenderer.
 *
 * @author Loic Royer 2014
 *
 */
public abstract class JOGLClearVolumeFrameRenderer extends ClearVolumeRendererBase implements ClearGLEventListener {

	static {
		// attempt at solving Jug's Dreadlock bug:
		final GLProfile lProfile = GLProfile.get( GLProfile.GL4 );
//		System.out.println( lProfile );

		// load icons:
		ClearGLWindow.setWindowIcons( "clearvolume/icon/ClearVolumeIcon16.png", "clearvolume/icon/ClearVolumeIcon32.png", "clearvolume/icon/ClearVolumeIcon64.png", "clearvolume/icon/ClearVolumeIcon128.png", "clearvolume/icon/ClearVolumeIcon256.png", "clearvolume/icon/ClearVolumeIcon512.png" );
	}

	// ClearGL Window.
	private ClearGLWindow mClearGLWindow;
	private volatile int mLastWindowWidth, mLastWindowHeight;
	private final ReentrantLock mDisplayReentrantLock = new ReentrantLock();

	// pixelbuffer objects.
	protected GLPixelBufferObject[] mPixelBufferObjects;

	// texture and its dimensions.
	private final GLTexture< Byte >[] mLayerTextures;

	// Internal fields for calculating FPS.
	private volatile int step = 0;
	private volatile long prevTimeNS = -1;

	// Box
	private volatile boolean mRenderBox = true;
	private static final float cBoxLineWidth = 1.f; // only cBoxLineWidth = 1.f
													// seems to be supported

	private static final FloatBuffer cBoxColor = FloatBuffer.wrap( new float[] { 1.f, 1.f, 1.f, 1.f } );

	// Window:
	private final String mWindowName;
	private GLProgram mGLProgram;
	private GLProgram mBoxGLProgram;

	// Shader attributes, uniforms and arrays:
	private GLAttribute mPositionAttribute;
	private GLVertexArray mQuadVertexArray;
	private GLVertexAttributeArray mPositionAttributeArray;
	private GLUniform mQuadProjectionMatrixUniform;
	private GLAttribute mTexCoordAttribute;
	private GLUniform[] mTexUnits;
	private GLVertexAttributeArray mTexCoordAttributeArray;

	private GLAttribute mBoxPositionAttribute;
	private GLVertexArray mBoxVertexArray;
	private GLVertexAttributeArray mBoxPositionAttributeArray;
	private GLUniform mBoxColorUniform;
	private GLUniform mBoxModelViewMatrixUniform;
	private GLUniform mBoxProjectionMatrixUniform;

	private final GLMatrix mBoxModelViewMatrix = new GLMatrix();
	private final GLMatrix mVolumeViewMatrix = new GLMatrix();
	private final GLMatrix mQuadProjectionMatrix = new GLMatrix();

	private final int mMaxTextureWidth = 768, mMaxTextureHeight = 768;
	private final int mTextureWidth, mTextureHeight;

	protected boolean mUsePBOs = true;

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name and its dimensions.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 */
	public JOGLClearVolumeFrameRenderer( final String pWindowName, final int pWindowWidth, final int pWindowHeight ) {
		this( pWindowName, pWindowWidth, pWindowHeight, 1 );
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 */
	public JOGLClearVolumeFrameRenderer( final String pWindowName, final int pWindowWidth, final int pWindowHeight, final int pBytesPerVoxel ) {
		this( pWindowName, pWindowWidth, pWindowHeight, pBytesPerVoxel, 768, 768 );
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, and bytes-per-voxel.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 */
	public JOGLClearVolumeFrameRenderer( final String pWindowName, final int pWindowWidth, final int pWindowHeight, final int pBytesPerVoxel, final int pMaxTextureWidth, final int pMaxTextureHeight ) {
		this( pWindowName, pWindowWidth, pWindowHeight, pBytesPerVoxel, pMaxTextureWidth, pMaxTextureHeight, 1 );
	}

	/**
	 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
	 * name, its dimensions, number of bytes-per-voxel, max texture width,
	 * height
	 * and number of render layers.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param pNumberOfRenderLayers
	 */

	@SuppressWarnings( "unchecked" )
	public JOGLClearVolumeFrameRenderer( final String pWindowName, final int pWindowWidth, final int pWindowHeight, final int pBytesPerVoxel, final int pMaxTextureWidth, final int pMaxTextureHeight, final int pNumberOfRenderLayers ) {
		super( pNumberOfRenderLayers );

		mTextureWidth = Math.min( mMaxTextureWidth, pWindowWidth );
		mTextureHeight = Math.min( mMaxTextureHeight, pWindowHeight );

		mWindowName = pWindowName;
		mLastWindowWidth = pMaxTextureWidth;
		mLastWindowHeight = pMaxTextureHeight;
		setNumberOfRenderLayers( pNumberOfRenderLayers );

		mLayerTextures = new GLTexture[ getNumberOfRenderLayers() ];
		mPixelBufferObjects = new GLPixelBufferObject[ getNumberOfRenderLayers() ];

		resetBrightnessAndGammaAndTransferFunctionRanges();
		resetRotationTranslation();
		setBytesPerVoxel( pBytesPerVoxel );

		// Initialize the GL component
		// final GLProfile lProfile = GLProfile.getMaxFixedFunc(true);
		// final GLCapabilities lCapabilities = new GLCapabilities(lProfile);

		mClearGLWindow = new ClearGLWindow( pWindowName, pWindowWidth, pWindowHeight, this );

		// Initialize the mouse controls
		final MouseControl lMouseControl = new MouseControl( this );
		mClearGLWindow.getGLWindow().addMouseListener( lMouseControl );

		// Initialize the keyboard controls
		final KeyboardControl lKeyboardControl = new KeyboardControl( this );
		mClearGLWindow.getGLWindow().addKeyListener( lKeyboardControl );

		mClearGLWindow.getGLWindow().addWindowListener( new WindowAdapter() {

			@Override
			public void windowDestroyNotify( final WindowEvent pE ) {
				super.windowDestroyNotify( pE );
			};
		} );
	}

	@Override
	public void setClearGLWindow( final ClearGLWindow pClearGLWindow ) {

		mClearGLWindow = pClearGLWindow;
	}

	@Override
	public ClearGLWindow getClearGLWindow() {
		return mClearGLWindow;
	};

	@Override
	public void close() {
		super.close();
		try {
			mClearGLWindow.close();
		} catch ( final Throwable e ) {
			System.err.println( e.getLocalizedMessage() );
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isShowing()
	 */
	@Override
	public boolean isShowing() {
		return mClearGLWindow.getGLWindow().isVisible();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#setVisible(boolean)
	 */
	@Override
	public void setVisible( final boolean pIsVisible ) {
		mClearGLWindow.getGLWindow().setVisible( pIsVisible );
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowName()
	 */
	@Override
	public String getWindowName() {
		return mWindowName;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowWidth()
	 */
	@Override
	public int getWindowWidth() {
		return mClearGLWindow.getGLWindow().getWidth();
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#getWindowHeight()
	 */
	@Override
	public int getWindowHeight() {
		return mClearGLWindow.getGLWindow().getHeight();
	}

	/**
	 * Returns the render texture width.
	 *
	 * @return texture width
	 */
	public int getTextureWidth() {
		return mTextureWidth;
	}

	/**
	 * Returns the render texture height.
	 *
	 * @return texture height
	 */
	public int getTextureHeight() {
		return mTextureHeight;
	}

	/**
	 * Implementation of GLEventListener: Called to initialize the
	 * GLAutoDrawable.
	 * This method will initialize the JCudaDriver and cause the initialization
	 * of
	 * CUDA and the OpenGL PBO.
	 */
	@Override
	public void init( final GLAutoDrawable drawable ) {
		final GL4 lGL4 = drawable.getGL().getGL4();
		lGL4.setSwapInterval( 0 );
		lGL4.glDisable( GL4.GL_DEPTH_TEST );
		lGL4.glDisable( GL4.GL_STENCIL_TEST );
		lGL4.glEnable( GL4.GL_TEXTURE_2D );

		lGL4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
		lGL4.glClear( GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT );

		// getClearGLWindow().setOrthoProjectionMatrix(0,
		// drawable.getSurfaceWidth(),
		// 0,
		// drawable.getSurfaceHeight(),
		// 0,
		// 1);

		setDefaultProjectionMatrix();

		mQuadProjectionMatrix.setOrthoProjectionMatrix( -1, 1, -1, 1, 0, 1000 );

		if ( initVolumeRenderer() ) {
			/*
			 * if (mPixelBufferObject != null)
			 * {
			 * unregisterPBO(mPixelBufferObject.getId());
			 * mPixelBufferObject.close();
			 * mPixelBufferObject = null;
			 * }
			 *
			 * /*
			 * if (mTexture != null)
			 * {
			 * mTexture.close();
			 * mTexture = null;
			 * }/*
			 */

			// texture display: construct the program and related objects
			try {
				final InputStream lVertexShaderResourceAsStream = JOGLClearVolumeFrameRenderer.class.getResourceAsStream( "shaders/tex_vert.glsl" );
				final InputStream lFragmentShaderResourceAsStream = JOGLClearVolumeFrameRenderer.class.getResourceAsStream( "shaders/tex_frag.glsl" );

				final String lVertexShaderSource = IOUtils.toString( lVertexShaderResourceAsStream, "UTF-8" );
				String lFragmentShaderSource = IOUtils.toString( lFragmentShaderResourceAsStream, "UTF-8" );

				for ( int i = 1; i < getNumberOfRenderLayers(); i++ ) {
					final String lStringToInsert1 = String.format( "uniform sampler2D texUnit%d; \n//insertpoin1t", i );
					final String lStringToInsert2 = String.format( "tempOutColor = max(tempOutColor,texture(texUnit%d, ftexcoord));\n//insertpoint2", i );

					lFragmentShaderSource = lFragmentShaderSource.replace( "//insertpoint1", lStringToInsert1 );
					lFragmentShaderSource = lFragmentShaderSource.replace( "//insertpoint2", lStringToInsert2 );
				}
				// System.out.println(lFragmentShaderSource);

				mGLProgram = GLProgram.buildProgram( lGL4, lVertexShaderSource, lFragmentShaderSource );
				mQuadProjectionMatrixUniform = mGLProgram.getUniform( "projection" );
				mPositionAttribute = mGLProgram.getAtribute( "position" );
				mTexCoordAttribute = mGLProgram.getAtribute( "texcoord" );
				mTexUnits = new GLUniform[ getNumberOfRenderLayers() ];
				for ( int i = 0; i < getNumberOfRenderLayers(); i++ ) {
					mTexUnits[ i ] = mGLProgram.getUniform( "texUnit" + i );
					mTexUnits[ i ].set( i );
				}

				mQuadVertexArray = new GLVertexArray( mGLProgram );
				mQuadVertexArray.bind();
				mPositionAttributeArray = new GLVertexAttributeArray( mPositionAttribute, 4 );

				final GLFloatArray lVerticesFloatArray = new GLFloatArray( 6, 4 );
				lVerticesFloatArray.add( -1, -1, 0, 1 );
				lVerticesFloatArray.add( 1, -1, 0, 1 );
				lVerticesFloatArray.add( 1, 1, 0, 1 );
				lVerticesFloatArray.add( -1, -1, 0, 1 );
				lVerticesFloatArray.add( 1, 1, 0, 1 );
				lVerticesFloatArray.add( -1, 1, 0, 1 );

				mQuadVertexArray.addVertexAttributeArray( mPositionAttributeArray, lVerticesFloatArray.getFloatBuffer() );

				mTexCoordAttributeArray = new GLVertexAttributeArray( mTexCoordAttribute, 2 );

				final GLFloatArray lTexCoordFloatArray = new GLFloatArray( 6, 2 );
				lTexCoordFloatArray.add( 0, 0 );
				lTexCoordFloatArray.add( 1, 0 );
				lTexCoordFloatArray.add( 1, 1 );
				lTexCoordFloatArray.add( 0, 0 );
				lTexCoordFloatArray.add( 1, 1 );
				lTexCoordFloatArray.add( 0, 1 );

				mQuadVertexArray.addVertexAttributeArray( mTexCoordAttributeArray, lTexCoordFloatArray.getFloatBuffer() );

				for ( int i = 0; i < getNumberOfRenderLayers(); i++ ) {
					mLayerTextures[ i ] = new GLTexture< Byte >( mGLProgram, Byte.class, 4, mTextureWidth, mTextureHeight, 1, true, 3 );

					mPixelBufferObjects[ i ] = new GLPixelBufferObject( mGLProgram, mTextureWidth, mTextureHeight );

					mPixelBufferObjects[ i ].copyFrom( null );

					registerPBO( i, mPixelBufferObjects[ i ].getId() );
				}

			} catch ( final IOException e ) {
				e.printStackTrace();
			}

			// box display: construct the program and related objects
			try {
				mBoxGLProgram = GLProgram.buildProgram( lGL4, JOGLClearVolumeFrameRenderer.class, "shaders/box_vert.glsl", "shaders/box_frag.glsl" );

				// set the line with of the box
				lGL4.glLineWidth( cBoxLineWidth );

				// get all the shaders uniform locations
				mBoxPositionAttribute = mBoxGLProgram.getAtribute( "position" );
				mBoxModelViewMatrixUniform = mBoxGLProgram.getUniform( "modelview" );
				mBoxProjectionMatrixUniform = mBoxGLProgram.getUniform( "projection" );
				mBoxColorUniform = mBoxGLProgram.getUniform( "color" );

				// set up the vertices of the box
				mBoxVertexArray = new GLVertexArray( mBoxGLProgram );
				mBoxVertexArray.bind();
				mBoxPositionAttributeArray = new GLVertexAttributeArray( mBoxPositionAttribute, 4 );

				// FIXME this should be done with IndexArrays, but lets be lazy for
				// now...
				final GLFloatArray lVerticesFloatArray = new GLFloatArray( 24, 4 );

				final float w = .5f;

				lVerticesFloatArray.add( w, w, w, w );
				lVerticesFloatArray.add( -w, w, w, w );
				lVerticesFloatArray.add( -w, w, w, w );
				lVerticesFloatArray.add( -w, -w, w, w );
				lVerticesFloatArray.add( -w, -w, w, w );
				lVerticesFloatArray.add( w, -w, w, w );
				lVerticesFloatArray.add( w, -w, w, w );
				lVerticesFloatArray.add( w, w, w, w );
				lVerticesFloatArray.add( w, w, -w, w );
				lVerticesFloatArray.add( -w, w, -w, w );
				lVerticesFloatArray.add( -w, w, -w, w );
				lVerticesFloatArray.add( -w, -w, -w, w );
				lVerticesFloatArray.add( -w, -w, -w, w );
				lVerticesFloatArray.add( w, -w, -w, w );
				lVerticesFloatArray.add( w, -w, -w, w );
				lVerticesFloatArray.add( w, w, -w, w );
				lVerticesFloatArray.add( w, w, w, w );
				lVerticesFloatArray.add( w, w, -w, w );
				lVerticesFloatArray.add( -w, w, w, w );
				lVerticesFloatArray.add( -w, w, -w, w );
				lVerticesFloatArray.add( -w, -w, w, w );
				lVerticesFloatArray.add( -w, -w, -w, w );
				lVerticesFloatArray.add( w, -w, w, w );
				lVerticesFloatArray.add( w, -w, -w, w );

				mBoxVertexArray.addVertexAttributeArray( mBoxPositionAttributeArray, lVerticesFloatArray.getFloatBuffer() );

			} catch ( final IOException e ) {
				e.printStackTrace();
			}

		}

	}

	private void setDefaultProjectionMatrix() {
		getClearGLWindow().setPerspectiveProjectionMatrix( .785f, 1, .1f, 1000 );
	}

	/**
	 * @return true if the implemented renderer initialized successfully.
	 */
	protected abstract boolean initVolumeRenderer();

	/**
	 * Register PBO object with any descendant of this abstract class.
	 *
	 * @param pRenderLayerIndex
	 * @param pPixelBufferObjectId
	 */
	protected abstract void registerPBO( int pRenderLayerIndex, int pPixelBufferObjectId );

	/**
	 * Unregisters PBO object with any descendant of this abstract class.
	 *
	 * @param pRenderLayerIndex
	 * @param pPixelBufferObjectId
	 */
	protected abstract void unregisterPBO( int pRenderLayerIndex, int pPixelBufferObjectId );

	public void copyBufferToTexture( final int pRenderLayerIndex, final ByteBuffer pByteBuffer ) {
		pByteBuffer.rewind();
		mLayerTextures[ pRenderLayerIndex ].copyFrom( pByteBuffer );
	}

	/**
	 * Implementation of GLEventListener: Called when the given GLAutoDrawable
	 * is
	 * to be displayed.
	 */
	@Override
	public void display( final GLAutoDrawable drawable ) {
		final GL4 lGL4 = drawable.getGL().getGL4();
		lGL4.glClear( GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT );

		setDefaultProjectionMatrix();

		// scaling...

		final double scaleX = getVolumeSizeX() * getVoxelSizeX();
		final double scaleY = getVolumeSizeY() * getVoxelSizeY();
		final double scaleZ = getVolumeSizeZ() * getVoxelSizeZ();

		final double maxScale = max( max( scaleX, scaleY ), scaleZ );

		// building up the inverse Modelview

		final GLMatrix eulerMat = new GLMatrix();

		eulerMat.euler( getRotationX() * 0.01, getRotationY() * 0.01, 0.0f );
		if ( hasRotationController() ) {
			getRotationController().rotate( eulerMat );
			notifyUpdateOfVolumeRenderingParameters();
		}

		final GLMatrix lInvVolumeMatrix = new GLMatrix();
		lInvVolumeMatrix.setIdentity();
		lInvVolumeMatrix.translate( -getTranslationX(), -getTranslationY(), -getTranslationZ() );
		lInvVolumeMatrix.transpose();

		lInvVolumeMatrix.mult( eulerMat );

		lInvVolumeMatrix.scale( ( float ) ( maxScale / scaleX ), ( float ) ( maxScale / scaleY ), ( float ) ( maxScale / scaleZ ) );

		final GLMatrix lInvProjection = new GLMatrix();
		lInvProjection.copy( getClearGLWindow().getProjectionMatrix() );
		lInvProjection.transpose();
		lInvProjection.invert();

		final boolean[] lUpdated = renderVolume( lInvVolumeMatrix.getFloatArray(), lInvProjection.getFloatArray() );

		if ( lUpdated != null ) {
			if ( mUsePBOs )
				for ( int i = 0; i < getNumberOfRenderLayers(); i++ )
					if ( lUpdated[ i ] )
						mLayerTextures[ i ].copyFrom( mPixelBufferObjects[ i ] );

			mGLProgram.use( lGL4 );
			mGLProgram.bind();

			for ( int i = 0; i < getNumberOfRenderLayers(); i++ )
				mLayerTextures[ i ].bind( i );

			mQuadProjectionMatrixUniform.setFloatMatrix( mQuadProjectionMatrix.getFloatArray(), false );

			mQuadVertexArray.draw( GL.GL_TRIANGLES );

			// draw the box
			if ( mRenderBox ) {
				mBoxGLProgram.use( lGL4 );

				// invert Matrix is the modelview used by renderer which is actually the
				// inverted modelview Matrix
				final GLMatrix lInvBoxMatrix = new GLMatrix();
				lInvBoxMatrix.copy( lInvVolumeMatrix );
				lInvBoxMatrix.transpose();
				lInvBoxMatrix.invert();

				mBoxModelViewMatrixUniform.setFloatMatrix( lInvBoxMatrix.getFloatArray(), false );

				final GLMatrix lProjectionMatrix = getClearGLWindow().getProjectionMatrix();

				getClearGLWindow().getProjectionMatrix().mult( 0, 0, mQuadProjectionMatrix.get( 0, 0 ) );
				getClearGLWindow().getProjectionMatrix().mult( 1, 1, mQuadProjectionMatrix.get( 1, 1 ) );

				mBoxProjectionMatrixUniform.setFloatMatrix( getClearGLWindow().getProjectionMatrix().getFloatArray(), false );

				mBoxColorUniform.setFloatVector4( cBoxColor );

				mBoxVertexArray.draw( GL.GL_LINES );

			}

			updateFrameRateDisplay();

		}

	}

	/**
	 * @param pModelViewMatrix
	 *            Model-view matrix as float array
	 * @param pProjectionMatrix
	 *            Projection matrix as float array
	 * @return boolean array indicating for each layer if it was updated.
	 */
	protected abstract boolean[] renderVolume( final float[] pModelViewMatrix, final float[] pProjectionMatrix );

	/**
	 * Updates the display of the framerate.
	 */
	private void updateFrameRateDisplay() {
		step++;
		final long currentTime = System.nanoTime();
		if ( prevTimeNS == -1 ) {
			prevTimeNS = currentTime;
		}
		final long diff = currentTime - prevTimeNS;
		if ( diff > 1e9 ) {
			final double fps = ( diff / 1e9 ) * step;
			String t = getWindowName() + " (";
			t += String.format( "%.2f", fps ) + " fps)";
			setWindowTitle( t );
			prevTimeNS = currentTime;
			step = 0;
		}
	}

	/**
	 * @param pTitleString
	 */
	private void setWindowTitle( final String pTitleString ) {
		mClearGLWindow.getGLWindow().setTitle( pTitleString );
	}

	/**
	 * Interface method implementation
	 *
	 * @see javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable,
	 *      int, int, int, int)
	 */
	@Override
	public void reshape( final GLAutoDrawable drawable, final int x, final int y, final int pWidth, int pHeight ) {
		if ( pHeight == 0 ) pHeight = 1;
		final float lAspectRatio = ( 1.0f * pWidth ) / pHeight;

		if ( lAspectRatio >= 1 )
			mQuadProjectionMatrix.setOrthoProjectionMatrix( -1, 1, -1 / lAspectRatio, 1 / lAspectRatio, 0, 1000 );
		else
			mQuadProjectionMatrix.setOrthoProjectionMatrix( -lAspectRatio, lAspectRatio, -1, 1, 0, 1000 );
	}

	/**
	 * Implementation of GLEventListener - not used
	 */
	@Override
	public void dispose( final GLAutoDrawable arg0 ) {
		for ( int i = 0; i < getNumberOfRenderLayers(); i++ ) {
			mLayerTextures[ i ].close();
			mPixelBufferObjects[ i ].close();
		}

	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#toggleFullScreen()
	 */
	@Override
	public void toggleFullScreen() {
		try {
			if ( mClearGLWindow.getGLWindow().isFullscreen() ) {
				if ( mLastWindowWidth > 0 && mLastWindowHeight > 0 )
					mClearGLWindow.getGLWindow().setSize( mLastWindowWidth, mLastWindowHeight );
				mClearGLWindow.getGLWindow().setFullscreen( false );
			} else {
				mLastWindowWidth = getWindowWidth();
				mLastWindowHeight = getWindowHeight();
				mClearGLWindow.getGLWindow().setFullscreen( true );
			}
			// notifyUpdateOfVolumeRenderingParameters();
			requestDisplay();
		} catch ( final Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.ClearVolumeRendererInterface#isFullScreen()
	 */
	@Override
	public boolean isFullScreen() {
		return mClearGLWindow.getGLWindow().isFullscreen();
	}

	/**
	 * Toggles box display.
	 */
	@Override
	public void toggleBoxDisplay() {
		mRenderBox = !mRenderBox;
	}

	/**
	 * Interface method implementation
	 *
	 * @see clearvolume.renderer.DisplayRequestInterface#requestDisplay()
	 */
	@Override
	public void requestDisplay() {
		final boolean lLocked = mDisplayReentrantLock.tryLock();
		if ( lLocked ) {
			try {
				mClearGLWindow.getGLWindow().display();
				setVisible( true );
			}
			finally {
				mDisplayReentrantLock.unlock();
			}
		}
	}

	@Override
	public void disableClose() {
		mClearGLWindow.getGLWindow().setDefaultCloseOperation( WindowClosingMode.DO_NOTHING_ON_CLOSE );
	}

	private boolean anyIsTrue( final boolean[] pBooleanArray ) {
		for ( final boolean lBoolean : pBooleanArray )
			if ( lBoolean ) return true;
		return false;
	}

}
