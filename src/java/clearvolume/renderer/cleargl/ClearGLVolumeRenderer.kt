@file:JvmName("ClearGLVolumeRenderer")
package clearvolume.renderer.cleargl

import cleargl.*
import cleargl.util.recorder.GLVideoRecorder
import clearvolume.controller.OculusRiftController
import clearvolume.controller.RotationControllerWithRenderNotification
import clearvolume.renderer.ClearVolumeRendererBase
import clearvolume.renderer.cleargl.overlay.Overlay
import clearvolume.renderer.cleargl.overlay.Overlay2D
import clearvolume.renderer.cleargl.overlay.Overlay3D
import clearvolume.renderer.cleargl.overlay.o3d.BoxOverlay
import clearvolume.renderer.cleargl.utils.ScreenToEyeRay
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode
import com.jogamp.newt.awt.NewtCanvasAWT
import com.jogamp.newt.event.MouseEvent
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.math.Quaternion
import coremem.types.NativeTypeEnum
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.SystemUtils
import scenery.DetachedHeadCamera
import scenery.Scene
import java.io.File
import java.io.IOException
import java.lang.Math.max
import java.lang.Math.min
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstract Class JoglPBOVolumeRenderer

 * Classes that derive from this abstract class are provided with basic
 * JOGL-based display capability for implementing a ClearVolumeRenderer.

 * @author Loic Royer 2014
 */
abstract class ClearGLVolumeRenderer
/**
 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
 * name, its dimensions, number of bytes-per-voxel, max texture width,
 * height and number of render layers.

 * @param pWindowName
 * *            window name
 * *
 * @param pWindowWidth
 * *            window width
 * *
 * @param pWindowHeight
 * *            window height
 * *
 * @param pNativeTypeEnum
 * *            native type
 * *
 * @param pMaxRenderWidth
 * *            max render width
 * *
 * @param pMaxRenderHeight
 * *            max render height
 * *
 * @param pNumberOfRenderLayers
 * *            number of render layers
 * *
 * @param pUseInCanvas
 * *            if true, this Renderer will not be displayed in a window of
 * *            it's own, but must be embedded in a GUI as Canvas.
 */
@SuppressWarnings("unchecked")
constructor(// Window:
        private val mWindowName: String = "ClearVolume",
        @Volatile var viewportWidth: Int = 1024,
        @Volatile var viewportHeight: Int = 1024,
        pNativeTypeEnum: NativeTypeEnum = NativeTypeEnum.UnsignedByte,
        // textures width and height:
        private val mMaxRenderWidth: Int = 1024,
        private val mMaxRenderHeight: Int = 1024,
        pNumberOfRenderLayers: Int = 1,
        pUseInCanvas: Boolean = false) :

        ClearVolumeRendererBase(pNumberOfRenderLayers), ClearGLEventListener, RendererInterface {

    // ClearGL Window.
    @Volatile private var mClearGLWindow: ClearGLWindow? = null

    private var mNewtCanvasAWT: NewtCanvasAWT? = null
    @Volatile private var mLastWindowWidth: Int = 0
    @Volatile private var mLastWindowHeight: Int = 0
    @Volatile private var mViewportX: Int = 0
    @Volatile private var mViewportY: Int = 0

    // texture and its dimensions.
    private val mLayerTextures: ArrayList<GLTexture> = ArrayList<GLTexture>()

    // Internal fields for calculating FPS.
    private val step = 0
    private val prevTimeNS = -1

    // Overlay3D stuff:
    private val mOverlayMap = ConcurrentHashMap<String, Overlay>()
    private var mGLProgram: GLProgram? = null

    // Shader attributes, uniforms and arrays:
    private var mPositionAttribute: GLAttribute? = null
    private var mQuadVertexArray: GLVertexArray? = null
    private var mPositionAttributeArray: GLVertexAttributeArray? = null
    private var mQuadProjectionMatrixUniform: GLUniform? = null
    private var mTexCoordAttribute: GLAttribute? = null
    private var mTexUnits: ArrayList<GLUniform>? = ArrayList<GLUniform>()
    private var mTexCoordAttributeArray: GLVertexAttributeArray? = null

    private val mQuadProjectionMatrix = GLMatrix()
    /**
     * Returns the render texture width.

     * @return texture width
     */
    @Volatile var renderWidth: Int = 0
        private set
    /**
     * Returns the render texture height.

     * @return texture height
     */
    @Volatile var renderHeight: Int = 0
        private set
    @Volatile private var mUpdateTextureWidthHeight = true

    @Volatile private var mRequestDisplay = true

    // Recorder:
    private val mGLVideoRecorder = GLVideoRecorder(File(SystemUtils.USER_HOME,
            "Videos/ClearVolume"))

    var lightVector = floatArrayOf(-1.0f, 1.0f, 1.0f)
        set(pLight) {
            lightVector[0] = pLight[0]
            lightVector[1] = pLight[1]
            lightVector[2] = pLight[2]
            notifyChangeOfVolumeRenderingParameters()
        }

    private val ovr: OculusRiftController?
    /**
     * Constructs an instance of the JoglPBOVolumeRenderer class given a window
     * name, its dimensions, and bytes-per-voxel.

     * @param pWindowName
     * *            window name
     * *
     * @param pWindowWidth
     * *            window width
     * *
     * @param pWindowHeight
     * *            window height
     * *
     * @param pNativeTypeEnum
     * *            native type
     * *
     * @param pMaxRenderWidth
     * *            max render width
     * *
     * @param pMaxRenderHeight
     * *            max render height
     */
    @JvmOverloads constructor(pWindowName: String,
                              pWindowWidth: Int,
                              pWindowHeight: Int,
                              pNativeTypeEnum: NativeTypeEnum = NativeTypeEnum.UnsignedByte,
                              pMaxRenderWidth: Int = 768,
                              pMaxRenderHeight: Int = 768) : this(pWindowName,
            pWindowWidth,
            pWindowHeight,
            pNativeTypeEnum,
            pMaxRenderWidth,
            pMaxRenderHeight,
            1) {
    }

    /**
     * Constructs an instance of the JoglPBOVolumeRenderer class given a window
     * name, its dimensions, and bytes-per-voxel.

     * @param pWindowName
     * *            window name
     * *
     * @param pWindowWidth
     * *            window width
     * *
     * @param pWindowHeight
     * *            window height
     * *
     * @param pNativeTypeEnum
     * *            native type
     * *
     * @param pMaxRenderWidth
     * *            max render width
     * *
     * @param pMaxRenderHeight
     * *            max render height
     * *
     * @param pUseInCanvas
     * *            if true, this Renderer will not be displayed in a window of
     * *            it's own, but must be embedded in a GUI as Canvas.
     */
    constructor(pWindowName: String,
                pWindowWidth: Int,
                pWindowHeight: Int,
                pNativeTypeEnum: NativeTypeEnum,
                pMaxRenderWidth: Int,
                pMaxRenderHeight: Int,
                pUseInCanvas: Boolean) : this(pWindowName,
            pWindowWidth,
            pWindowHeight,
            pNativeTypeEnum,
            pMaxRenderWidth,
            pMaxRenderHeight,
            1,
            pUseInCanvas) {
    }

    /**
     * Constructs an instance of the JoglPBOVolumeRenderer class given a window
     * name, its dimensions, number of bytes-per-voxel, max texture width,
     * height and number of render layers.

     * @param pWindowName
     * *            window name
     * *
     * @param pWindowWidth
     * *            window width
     * *
     * @param pWindowHeight
     * *            window height
     * *
     * @param pNativeTypeEnum
     * *            native type
     * *
     * @param pMaxTextureWidth
     * *            max render width
     * *
     * @param pMaxTextureHeight
     * *            max render height
     * *
     * @param pNumberOfRenderLayers
     * *            number of render layers
     */
    constructor(pWindowName: String,
                pWindowWidth: Int,
                pWindowHeight: Int,
                pNativeTypeEnum: NativeTypeEnum,
                pMaxTextureWidth: Int,
                pMaxTextureHeight: Int,
                pNumberOfRenderLayers: Int) : this(pWindowName,
            pWindowWidth,
            pWindowHeight,
            pNativeTypeEnum,
            pMaxTextureWidth,
            pMaxTextureHeight,
            1,
            false) {
    }

    init {

        renderWidth = min(mMaxRenderWidth, viewportWidth)
        renderHeight = min(mMaxRenderHeight, viewportHeight)
        mLastWindowWidth = viewportWidth
        mLastWindowHeight = viewportHeight
        numberOfRenderLayers = pNumberOfRenderLayers

        resetBrightnessAndGammaAndTransferFunctionRanges()
        resetRotationTranslation()
        nativeType = pNativeTypeEnum

        // addOverlay(new BoxOverlay(0, .2f, false, "box_plain"));
        addOverlay(BoxOverlay(this, 10, 1.0f, true, "box"))

        mClearGLWindow = ClearGLWindow(mWindowName,
                viewportWidth,
                viewportHeight,
                this)
        mClearGLWindow!!.setFPS(60)

        mClearGLWindow!!.start()

        if (pUseInCanvas) {
            mNewtCanvasAWT = mClearGLWindow!!.newtCanvasAWT
            mNewtCanvasAWT!!.shallUseOffscreenLayer = true
        } else {
            mNewtCanvasAWT = null
        }

        // Initialize the mouse controls
        val lMouseControl = MouseControl(this)
        mClearGLWindow!!.addMouseListener(lMouseControl)

        // Initialize the keyboard controls
        val lKeyboardControl = KeyboardControl(this,
                lMouseControl)
        mClearGLWindow!!.addKeyListener(lKeyboardControl)

        mClearGLWindow!!.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyNotify(pE: WindowEvent?) {
                super.windowDestroyNotify(pE)
            }
        })

        if (translationRotationControllers.size == 0 && System.getProperty("ClearVolume.EnableVR") != null) {
            ovr = OculusRiftController(0, this)
            this.addTranslationRotationController(ovr)
            ovr.connectAsynchronouslyOrWait()
        } else {
            ovr = null
        }

    }

    override fun addOverlay(pOverlay: Overlay) {
        mOverlayMap.put(pOverlay.name, pOverlay)
    }

    private fun anyIsTrue(pBooleanArray: BooleanArray): Boolean {
        for (lBoolean in pBooleanArray)
            if (lBoolean)
                return true
        return false
    }

    fun clearTexture(pRenderLayerIndex: Int) {
        mLayerTextures[pRenderLayerIndex].clear()
    }

    override fun close() {

        if (mNewtCanvasAWT != null) {
            mNewtCanvasAWT = null

            return
        }

        try {
            mClearGLWindow!!.close()
        } catch (e: NullPointerException) {
        } catch (e: Throwable) {
            System.err.println(e.getMessage())
        }

        super.close()

    }

    fun copyBufferToTexture(pRenderLayerIndex: Int,
                            pByteBuffer: ByteBuffer) {
        pByteBuffer.rewind()
        mLayerTextures[pRenderLayerIndex].copyFrom(pByteBuffer)
        mLayerTextures[pRenderLayerIndex].updateMipMaps()
    }

    override fun disableClose() {
        mClearGLWindow!!.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE)
    }

    /**
     * Implementation of GLEventListener: Called when the given GLAutoDrawable
     * is to be displayed.
     */
    override fun display(pDrawable: GLAutoDrawable) {
        displayInternal(pDrawable)
    }

    private fun displayInternal(pDrawable: GLAutoDrawable) {
        // use scenegraph functionality if we do have a scene
        if (this.scene != null) {
            displayInternalScenegraph(pDrawable)
            return
        }

        val lTryLock = true

        pDrawable.gl

        mDisplayReentrantLock.lock()

        if (lTryLock)
            try {
                ensureTextureAllocated()

                val lOverlay2DChanged = isOverlay2DChanged
                val lOverlay3DChanged = isOverlay3DChanged


                val lGL = pDrawable.gl
                val w: Int
                val h: Int

                lGL.glEnable(GL.GL_SCISSOR_TEST)

                w = windowWidth
                h = windowHeight

                val lLastRenderPass = adaptiveLODController.beforeRendering()
                var eyeShift: FloatArray
                val eyeCount: Int

                if (translationRotationControllers.size == 0) {
                    eyeShift = floatArrayOf(-0.1f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f)
                } else {
                    eyeShift = translationRotationControllers[0].eyeShift
                }

                if (System.getProperty("ClearVolume.EnableVR") != null) {
                    eyeCount = 2
                } else if (System.getProperty("ClearVolume.Anaglyph") != null) {
                    eyeShift = floatArrayOf(-0.03f, 0.0f, 0.0f, 0.03f, 0.0f, 0.0f)
                    eyeCount = 2
                } else {
                    eyeShift = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                    eyeCount = 1
                }

                lGL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

                for (eye in 0..eyeCount - 1) {
                    if (System.getProperty("ClearVolume.EnableVR") == null) {
                        lGL.glViewport(0, 0, w, h)
                        lGL.glScissor(0, 0, w, h)
                    } else if (System.getProperty("ClearVolume.Anaglyph") == null) {
                        lGL.glViewport(0, 0, w, h)
                        lGL.glScissor(0, 0, w, h)
                    } else {
                        lGL.glViewport(w / 2 * eye, 0, w / 2, h)
                        lGL.glScissor(w / 2 * eye, 0, w / 2, h)
                    }

                    if (System.getProperty("ClearVolume.Anaglyph") != null && eye == 0) {
                        lGL.glDisable(GL.GL_BLEND)
                        //setTransferFunction(0, TransferFunctions.getGradientForColor(0));
                        lGL.glColorMask(true, false, false, false)
                    }
                    if (System.getProperty("ClearVolume.Anaglyph") != null && eye == 1) {
                        lGL.glClear(GL.GL_DEPTH_BUFFER_BIT)
                        //setTransferFunction(0, TransferFunctions.getGradientForColor(1));
                        lGL.glColorMask(false, true, true, false)
                    }

                    lGL.glClearColor(0f, 0f, 0f, 1f)
                    if (System.getProperty("ClearVolume.Anaglyph") == null) {
                        lGL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
                    }

                    if (haveVolumeRenderingParametersChanged() || isNewVolumeDataAvailable)
                        adaptiveLODController.renderingParametersOrVolumeDataChanged()

                    // <volume-specific>
                    lGL.glDisable(GL.GL_CULL_FACE)
                    lGL.glEnable(GL.GL_BLEND)
                    lGL.glBlendFunc(GL.GL_ONE, GL.GL_ONE)
                    lGL.glBlendEquation(GL2ES3.GL_MAX)

                    setDefaultProjectionMatrix()

                    val lModelViewMatrix = getModelViewMatrix(floatArrayOf(eyeShift[0 + 3 * eye], eyeShift[1 + 3 * eye], eyeShift[2 + 3 * eye]))
                    val lProjectionMatrix = defaultProjectionMatrix

                    GLError.printGLErrors(lGL, "BEFORE RENDER VOLUME")


                    renderVolume(lModelViewMatrix.clone().invert().transpose().floatArray,
                            lProjectionMatrix.clone().invert().transpose().floatArray)




                    clearChangeOfVolumeParametersFlag()

                    GLError.printGLErrors(lGL, "AFTER RENDER VOLUME")

                    mGLProgram!!.use(lGL)

                    for (i in 0..numberOfRenderLayers - 1)
                        mLayerTextures[i].bind(i)

                    mQuadProjectionMatrixUniform!!.setFloatMatrix(mQuadProjectionMatrix.floatArray,
                            false)

                    mQuadVertexArray!!.draw(GL.GL_TRIANGLES)
                    // </volume-specific>

                    adaptiveLODController.afterRendering()
                    val lAspectRatioCorrectedProjectionMatrix = aspectRatioCorrectedProjectionMatrix

                    renderOverlays3D(lGL,
                            lAspectRatioCorrectedProjectionMatrix,
                            lModelViewMatrix)

                    renderOverlays2D(lGL, cOverlay2dProjectionMatrix)

                    updateFrameRateDisplay()

                    if (lLastRenderPass)
                        mGLVideoRecorder.screenshot(pDrawable,
                                !autoRotateController.isRotating)

                    if (System.getProperty("ClearVolume.Anaglyph") != null && eye == 1) {
                        lGL.glColorMask(true, true, true, true)
                    }
                }

            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                if (mDisplayReentrantLock.isHeldByCurrentThread)
                    mDisplayReentrantLock.unlock()
            }

    }

    private fun displayInternalScenegraph(pDrawable: GLAutoDrawable) {
        if(this.scene == null) {
            displayInternal(pDrawable)
            return
        }

        val lTryLock = true

        this.scene.initList.forEach { n -> n.init() }
        this.scene.initList.clear()

        mDisplayReentrantLock.lock()

        if (lTryLock)
            try {
                ensureTextureAllocated()

                val lOverlay2DChanged = isOverlay2DChanged
                val lOverlay3DChanged = isOverlay3DChanged


                val lGL = pDrawable.gl
                val w: Int
                val h: Int

                lGL.glEnable(GL.GL_SCISSOR_TEST)

                w = windowWidth
                h = windowHeight

                val lLastRenderPass = adaptiveLODController.beforeRendering()
                var eyeShift: FloatArray
                val eyeCount: Int

                if (translationRotationControllers.size == 0) {
                    eyeShift = floatArrayOf(-0.1f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f)
                } else {
                    eyeShift = translationRotationControllers[0].eyeShift
                }

                if (System.getProperty("ClearVolume.EnableVR") != null) {
                    eyeCount = 2
                } else if (System.getProperty("ClearVolume.Anaglyph") != null) {
                    eyeShift = floatArrayOf(-0.03f, 0.0f, 0.0f, 0.03f, 0.0f, 0.0f)
                    eyeCount = 2
                } else {
                    eyeShift = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                    eyeCount = 1
                }

                lGL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
                synchronized(this, {
                for (eye in 0..eyeCount - 1) {
                    if (System.getProperty("ClearVolume.EnableVR") == null) {
                        lGL.glViewport(0, 0, w, h)
                        lGL.glScissor(0, 0, w, h)
                    } else if (System.getProperty("ClearVolume.Anaglyph") == null) {
                        lGL.glViewport(0, 0, w, h)
                        lGL.glScissor(0, 0, w, h)
                    }

                    if(System.getProperty("ClearVolume.EnableVR") != null) {
                        //System.out.println("Setting viewports for VR...")
                        lGL.glViewport(w / 2 * eye, 0, w / 2, h)
                        lGL.glScissor(w / 2 * eye, 0, w / 2, h)
                    }

                    if (System.getProperty("ClearVolume.Anaglyph") != null && eye == 0) {
                        lGL.glDisable(GL.GL_BLEND)
                        //setTransferFunction(0, TransferFunctions.getGradientForColor(0));
                        lGL.glColorMask(true, false, false, false)
                    }
                    if (System.getProperty("ClearVolume.Anaglyph") != null && eye == 1) {
                        lGL.glClear(GL.GL_DEPTH_BUFFER_BIT)
                        //setTransferFunction(0, TransferFunctions.getGradientForColor(1));
                        lGL.glColorMask(false, true, true, false)
                    }

                    lGL.glClearColor(0f, 0f, 0f, 1f)
                    if (System.getProperty("ClearVolume.Anaglyph") == null) {
                        lGL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
                    }

                    if (haveVolumeRenderingParametersChanged() || isNewVolumeDataAvailable)
                        adaptiveLODController.renderingParametersOrVolumeDataChanged()


                    lGL.glFrontFace(GL.GL_CCW)
                    lGL.glEnable(GL.GL_CULL_FACE)
                    lGL.glCullFace(GL.GL_BACK)
                    lGL.glEnable(GL.GL_DEPTH_TEST)
                    lGL.glDisable(GL.GL_BLEND)

                    val rootNode = this.scene
                    // find observer
                    val cam = rootNode!!.findObserver()
                    // convert scenegraph to render tree

                    adaptiveLODController.beforeRendering()
                    // recursively render nodes
                    renderLoop@ for (n in rootNode!!.children) {

                        if(n.nodeType == "VolumeNode") {
                            // set the volume's matrices according to current camera settings
                            n.model = GLMatrix.getIdentity()
                            n.model.translate(n.position!!.x(), n.position!!.y(), n.position!!.z())

                            /*val lScaleX = getVolumeSizeX(0) * getVoxelSizeX(0)
                            val lScaleY = getVolumeSizeY(0) * getVoxelSizeY(0)
                            val lScaleZ = getVolumeSizeZ(0) * getVoxelSizeZ(0)

                            val lMaxScale = max(max(lScaleX, lScaleY), lScaleZ)

                            n.model.scale((lScaleX / lMaxScale).toFloat(),
                                    (lScaleY / lMaxScale).toFloat(),
                                    (lScaleZ / lMaxScale).toFloat())*/
                        }

                        val camrot = GLMatrix.fromQuaternion(cam.rotation)
                        var mv: GLMatrix = cam.view!!.clone()
                        if(System.getProperty("ClearVolume.EnableVR") != null) {
                            mv.translate(eyeShift[3*eye + 0]*5,
                                    eyeShift[3*eye + 1]*5,
                                    eyeShift[3*eye + 2]*5)

                            if(cam is DetachedHeadCamera) {
                                mv.translate(
                                        translationRotationControllers[0].translationVector[0],
                                        translationRotationControllers[0].translationVector[1],
                                        translationRotationControllers[0].translationVector[2]
                                )

                            }

                            mv.mult(translationRotationControllers[0].quaternion)

                        }
                        mv.mult(camrot)

                        mv.translate(translationX,
                                translationY,
                                translationZ)

                        mv.mult(quaternion)

                        mv.mult(n.model)

                        val proj = cam.projection!!.clone();

                        val mvp = proj.clone()
                        mvp.mult(mv)

                        if(n.nodeType == "VolumeNode") {
                            n.modelView = mv.clone()
                            n.projection = proj.clone()//cam.projection!!.clone()
                        }

                        if(n.nodeType == "Camera") { continue@renderLoop }
                        if(n.visible == false) { continue@renderLoop }


                        n.program?.let {
                            n.program!!.use(lGL)
                            n.program!!.getUniform("ModelViewMatrix")!!.setFloatMatrix(mv, false)
                            n.program!!.getUniform("ProjectionMatrix")!!.setFloatMatrix(cam.projection, false)
                            n.program!!.getUniform("MVP")!!.setFloatMatrix(mvp, false)
                            n.program!!.getUniform("offset")!!.setFloatVector3(n.position?.toFloatBuffer())

                            n.program!!.getUniform("Light.Ld").setFloatVector3(1.0f, 1.0f, 0.8f);
                            n.program!!.getUniform("Light.Position").setFloatVector3(-5.0f, 5.0f, 5.0f);
                            n.program!!.getUniform("Light.La").setFloatVector3(0.4f, 0.4f, 0.4f);
                            n.program!!.getUniform("Light.Ls").setFloatVector3(0.0f, 0.0f, 0.0f);
                            n.program!!.getUniform("Material.Shinyness").setFloat(0.5f);
                            n.program!!.getUniform("Material.Ka").setFloatVector3(n.position?.toFloatBuffer());
                            n.program!!.getUniform("Material.Kd").setFloatVector3(1.0f, 0.5f, 0.0f);
                            n.program!!.getUniform("Material.Ks").setFloatVector3(1.0f, 0.0f, 0.8f);
                        }

                        if(n.program == null) {
                            System.err.println("Could not find program for ${n.toString()}")
                        }

                        if(n.nodeType == "VolumeNode") {
                            mLayerTextures[(n as VolumeNode).layer].bind((n as VolumeNode).layer)
                        }
                        n.draw()

                        //GLError.printGLErrors(lGL, "Rendering of ${n.toString()} failed")
                    }
                    adaptiveLODController.afterRendering()

                    updateFrameRateDisplay()

                    if (lLastRenderPass)
                        mGLVideoRecorder.screenshot(pDrawable,
                                !autoRotateController.isRotating)

                    if (System.getProperty("ClearVolume.Anaglyph") != null && eye == 1) {
                        lGL.glColorMask(true, true, true, true)
                    }

                    ovr?.increaseFrameIndex();

                }})

            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                if (mDisplayReentrantLock.isHeldByCurrentThread)
                    mDisplayReentrantLock.unlock()
            }

    }

    /**
     * Implementation of GLEventListener - not used
     */
    override fun dispose(arg0: GLAutoDrawable) {
        mClearGLWindow!!.stop()
    }

    private fun ensureTextureAllocated() {
        if (mUpdateTextureWidthHeight) {
            displayLock.lock()
            mLayerTextures.clear()

            try {
                for (i in 0..numberOfRenderLayers - 1) {
                    mLayerTextures.add(GLTexture(mGLProgram,
                            NativeTypeEnum.UnsignedByte,
                            4,
                            renderWidth,
                            renderHeight,
                            1,
                            true,
                            2))
                    mLayerTextures[i].clear()

                }

                notifyChangeOfTextureDimensions()
                notifyChangeOfVolumeRenderingParameters()

            } finally {
                mUpdateTextureWidthHeight = false
                if (displayLock.isHeldByCurrentThread)
                    displayLock.unlock()
            }
        }
    }

    private /**/ val aspectRatioCorrectedProjectionMatrix: GLMatrix
        get() {
            val lProjectionMatrix = GLMatrix()
            lProjectionMatrix.setPerspectiveProjectionMatrix(fov,
                    1f,
                    .1f,
                    1000f)
            lProjectionMatrix.mult(0, 0, mQuadProjectionMatrix.get(0, 0))
            lProjectionMatrix.mult(1, 1, mQuadProjectionMatrix.get(1, 1))
            return lProjectionMatrix
        }

    override fun getClearGLWindow(): ClearGLWindow? {
        return mClearGLWindow
    }

    private val defaultProjectionMatrix: GLMatrix
        get() {
            val lProjectionMatrix = GLMatrix()
            lProjectionMatrix.setPerspectiveProjectionMatrix(fov,
                    1f,
                    .1f,
                    1000f)
            return lProjectionMatrix
        }

    private // scaling...
            // TODO: Hack - the first volume decides for the next ones, scene graph
            // will
            // solve this problem...
            // building up the inverse Modelview matrix
            /**//**/// lInvVolumeMatrix.mult(lEulerMatrix);
            // lInvVolumeMatrix.transpose();
    val modelViewMatrix: GLMatrix
        get() {
            val lScaleX = getVolumeSizeX(0) * getVoxelSizeX(0)
            val lScaleY = getVolumeSizeY(0) * getVoxelSizeY(0)
            val lScaleZ = getVolumeSizeZ(0) * getVoxelSizeZ(0)

            val lMaxScale = max(max(lScaleX, lScaleY), lScaleZ)

            applyControllersTransform()

            val lModelViewMatrix = GLMatrix()
            lModelViewMatrix.setIdentity()

            lModelViewMatrix.translate(translationX,
                    translationY,
                    translationZ)

            lModelViewMatrix.mult(quaternion)

            lModelViewMatrix.scale((lScaleX / lMaxScale).toFloat(),
                    (lScaleY / lMaxScale).toFloat(),
                    (lScaleZ / lMaxScale).toFloat())

            return lModelViewMatrix
        }

    private fun getModelViewMatrix(eyeShift: FloatArray): GLMatrix {
        // scaling...

        // TODO: Hack - the first volume decides for the next ones, scene graph
        // will
        // solve this problem...
        val lScaleX = getVolumeSizeX(0) * getVoxelSizeX(0)
        val lScaleY = getVolumeSizeY(0) * getVoxelSizeY(0)
        val lScaleZ = getVolumeSizeZ(0) * getVoxelSizeZ(0)

        val lMaxScale = max(max(lScaleX, lScaleY), lScaleZ)

        // building up the inverse Modelview matrix

        applyControllersTransform()

        val lModelViewMatrix = GLMatrix()
        lModelViewMatrix.setIdentity()

        lModelViewMatrix.translate(translationX,
                translationY,
                translationZ)/**/



        lModelViewMatrix.mult(quaternion)

        lModelViewMatrix.scale((lScaleX / lMaxScale).toFloat(),
                (lScaleY / lMaxScale).toFloat(),
                (lScaleZ / lMaxScale).toFloat())/**/

        // lInvVolumeMatrix.mult(lEulerMatrix);

        // lInvVolumeMatrix.transpose();

        lModelViewMatrix.translate(eyeShift[0], eyeShift[1], eyeShift[2])

        return lModelViewMatrix
    }

    /**
     * @return the mNewtCanvasAWT
     */
    override fun getNewtCanvasAWT(): NewtCanvasAWT {
        return mNewtCanvasAWT as NewtCanvasAWT
    }

    override fun getOverlays(): Collection<Overlay> {
        return mOverlayMap.values
    }

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.getWindowHeight
     */
    override fun getWindowHeight(): Int {
        return mClearGLWindow!!.height
    }

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.getWindowName
     */
    override fun getWindowName(): String {
        return mWindowName
    }

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.getWindowWidth
     */
    override fun getWindowWidth(): Int {
        return mClearGLWindow!!.width
    }

    /**
     * Implementation of GLEventListener: Called to initialize the
     * GLAutoDrawable. This method will initialize the JCudaDriver and cause the
     * initialization of CUDA and the OpenGL PBO.
     */
    override fun init(drawable: GLAutoDrawable) {
        val lGL = drawable.gl
        lGL.swapInterval = 1

        lGL.glDisable(GL.GL_DEPTH_TEST)
        lGL.glEnable(GL.GL_BLEND)
        lGL.glDisable(GL.GL_STENCIL_TEST)

        lGL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        lGL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

        if (initVolumeRenderer()) {

            // texture display: construct the program and related objects
            try {
                val lVertexShaderResourceAsStream = ClearGLVolumeRenderer::class.java.getResourceAsStream("shaders/tex_vert.glsl")
                val lFragmentShaderResourceAsStream = ClearGLVolumeRenderer::class.java.getResourceAsStream("shaders/tex_frag.glsl")

                val lVertexShaderSource = IOUtils.toString(lVertexShaderResourceAsStream,
                        "UTF-8")
                var lFragmentShaderSource = IOUtils.toString(lFragmentShaderResourceAsStream,
                        "UTF-8")

                for (i in 1..numberOfRenderLayers - 1) {
                    val lStringToInsert1 = "uniform sampler2D texUnit%d; \n//insertpoint1".format(i)
                    val lStringToInsert2 = "tempOutColor = max(tempOutColor,texture(texUnit%d, ftexcoord));\n//insertpoint2".format(i)

                    lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint1",
                            lStringToInsert1)
                    lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint2",
                            lStringToInsert2)
                }
                // System.out.println(lFragmentShaderSource);

                mGLProgram = GLProgram.buildProgram(lGL,
                        lVertexShaderSource,
                        lFragmentShaderSource)
                mQuadProjectionMatrixUniform = mGLProgram!!.getUniform("projection")
                mPositionAttribute = mGLProgram!!.getAtribute("position")
                mTexCoordAttribute = mGLProgram!!.getAtribute("texcoord")

                for (i in 0..numberOfRenderLayers - 1) {
                    mTexUnits?.add(mGLProgram!!.getUniform("texUnit" + i))
                    mTexUnits?.last()?.setInt(i)
                }

                mQuadVertexArray = GLVertexArray(mGLProgram)
                mQuadVertexArray!!.bind()
                mPositionAttributeArray = GLVertexAttributeArray(mPositionAttribute,
                        4)

                val lVerticesFloatArray = GLFloatArray(6,

                        4)

                lVerticesFloatArray.add(-1.0f, -1.0f, 0.0f, 1.0f)
                lVerticesFloatArray.add(1.0f, -1.0f, 0.0f, 1.0f)
                lVerticesFloatArray.add(1.0f, 1.0f, 0.0f, 1.0f)
                lVerticesFloatArray.add(-1.0f, -1.0f, 0.0f, 1.0f)
                lVerticesFloatArray.add(1.0f, 1.0f, 0.0f, 1.0f)
                lVerticesFloatArray.add(-1.0f, 1.0f, 0.0f, 1.0f)

                mQuadVertexArray!!.addVertexAttributeArray(mPositionAttributeArray,
                        lVerticesFloatArray.floatBuffer)

                mTexCoordAttributeArray = GLVertexAttributeArray(mTexCoordAttribute,
                        2)

                val lTexCoordFloatArray = GLFloatArray(6,
                        2)
                lTexCoordFloatArray.add(0.0f, 0.0f)
                lTexCoordFloatArray.add(1.0f, 0.0f)
                lTexCoordFloatArray.add(1.0f, 1.0f)
                lTexCoordFloatArray.add(0.0f, 0.0f)
                lTexCoordFloatArray.add(1.0f, 1.0f)
                lTexCoordFloatArray.add(0.0f, 1.0f)

                mQuadVertexArray!!.addVertexAttributeArray(mTexCoordAttributeArray,
                        lTexCoordFloatArray.floatBuffer)

            } catch (e: IOException) {
                e.printStackTrace()
            }

            for (lOverlay in mOverlayMap.values) {
                try {
                    lOverlay.init(lGL, this)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }

            ensureTextureAllocated()

        }

    }

    private fun applyControllersTransform() {
        synchronized(this, {
            if (rotationControllers.size > 0) {
                val lQuaternion = Quaternion()

                for (lRotationController in rotationControllers)
                    if (lRotationController.isActive) {
                        if (lRotationController is RotationControllerWithRenderNotification) {
                            lRotationController.notifyRender(this)
                        }
                        lQuaternion.mult(lRotationController.quaternion)

                        notifyChangeOfVolumeRenderingParameters()
                    }

                lQuaternion.mult(quaternion)
                quaternion = lQuaternion
            }

            //		if(getTranslationRotationControllers().size() > 0) {
            //			final Quaternion lQuaternion = new Quaternion();
            //
            //			for(final TranslationRotationControllerInterface lTRController : getTranslationRotationControllers())
            //				if(lTRController.isActive()) {
            //					lQuaternion.set(lTRController.getQuaternion());
            //
            //					notifyChangeOfVolumeRenderingParameters();
            //				}
            //
            //			setQuaternion(lQuaternion);
            //		}
        })
    }

    /**
     * @return true if the implemented renderer initialized successfully.
     */
    protected abstract fun initVolumeRenderer(): Boolean

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.isFullScreen
     */
    override fun isFullScreen(): Boolean {
        return mClearGLWindow!!.isFullscreen
    }

    private val isOverlay2DChanged: Boolean
        get() {
            var lHasAnyChanged = false
            for (lOverlay in mOverlayMap.values)
                if (lOverlay is Overlay2D) {
                    lHasAnyChanged = lHasAnyChanged or lOverlay.hasChanged2D()
                }
            return lHasAnyChanged
        }

    private val isOverlay3DChanged: Boolean
        get() {
            var lHasAnyChanged = false
            for (lOverlay in mOverlayMap.values)
                if (lOverlay is Overlay3D) {
                    lHasAnyChanged = lHasAnyChanged or lOverlay.hasChanged3D()
                }
            return lHasAnyChanged
        }

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.isShowing
     */
    override fun isShowing(): Boolean {
        try {
            if (mNewtCanvasAWT != null)
                return mNewtCanvasAWT!!.isVisible

            if (mClearGLWindow != null)
                return mClearGLWindow!!.isVisible
        } catch (e: NullPointerException) {
            return false
        }

        return false
    }

    protected abstract fun notifyChangeOfTextureDimensions()

    /**
     * Notifies eye ray listeners.

     * @param pRenderer
     * *            renderer that calls listeners
     * *
     * @param pMouseEvent
     * *            associated mouse event.
     * *
     * @return true if event captured
     */
    fun notifyEyeRayListeners(pRenderer: ClearGLVolumeRenderer,
                              pMouseEvent: MouseEvent): Boolean {
        if (mEyeRayListenerList.isEmpty())
            return false

        val lX = pMouseEvent.x
        val lY = pMouseEvent.y

        val lInverseModelViewMatrix = modelViewMatrix.clone().invert()
        val lInverseProjectionMatrix = clearGLWindow!!.projectionMatrix.clone().invert()

        val lAspectRatioCorrectedProjectionMatrix = aspectRatioCorrectedProjectionMatrix.invert()

        // lInverseModelViewMatrix.transpose();
        // lInverseProjectionMatrix.invert();
        // lInverseProjectionMatrix.transpose();

        val lEyeRay = ScreenToEyeRay.convert(viewportWidth,
                viewportHeight,
                lX,
                lY,
                lInverseModelViewMatrix,
                lAspectRatioCorrectedProjectionMatrix)

        var lPreventOtherDisplayChanges = false

        for (lEyeRayListener in mEyeRayListenerList) {
            lPreventOtherDisplayChanges = lPreventOtherDisplayChanges or lEyeRayListener.notifyEyeRay(pRenderer,
                    pMouseEvent,
                    lEyeRay)
            if (lPreventOtherDisplayChanges)
                break
        }
        return lPreventOtherDisplayChanges
    }

    private fun renderOverlays2D(lGL: GL,
                                 pProjectionMatrix: GLMatrix) {
        synchronized(this, {
            try {
                mOverlayMap.values.filter { overlay -> overlay is Overlay2D }.map {
                    overlay ->
                    (overlay as Overlay2D).render2D(this,
                            lGL,
                            windowWidth,
                            windowHeight,
                            pProjectionMatrix)
                }
                GLError.printGLErrors(lGL, "AFTER OVERLAYS")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        })
    }

    private fun renderOverlays3D(lGL: GL,
                                 pProjectionMatrix: GLMatrix,
                                 pModelViewMatrix: GLMatrix) {
        synchronized(this, {
            try {
                mOverlayMap.values.filter { overlay -> overlay is Overlay3D }.map {
                   overlay -> (overlay as Overlay3D).render3D(this,
                            lGL,
                            windowWidth,
                            windowHeight,
                            pProjectionMatrix,
                            pModelViewMatrix)
                }

                GLError.printGLErrors(lGL, "AFTER OVERLAYS")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        })

    }

    /**
     * @param pModelViewMatrix
     * *            Model-mViewMatrix matrix as float array
     * *
     * @param pProjectionMatrix
     * *            Projection matrix as float array
     * *
     * @return boolean array indicating for each layer if it was updated.
     */
    public abstract fun renderVolume(pModelViewMatrix: FloatArray,
                                        pProjectionMatrix: FloatArray): BooleanArray

    /**
     * Interface method implementation

     * @see clearvolume.renderer.DisplayRequestInterface.requestDisplay
     */
    override fun requestDisplay() {
        mRequestDisplay = true
        // NOT NEEDED ANYMORE
        // getAdaptiveLODController().requestDisplay();
        // notifyChangeOfVolumeRenderingParameters();
    }

    /* (non-Javadoc)
	 * @see com.jogamp.opengl.GLEventListener#reshape(com.jogamp.opengl.GLAutoDrawable, int, int, int, int)
	 */
    override fun reshape(pDrawable: GLAutoDrawable,
                         x: Int,
                         y: Int,
                         pWidth: Int,
                         pHeight: Int) {
        var pWidth = pWidth
        var pHeight = pHeight
        try {
            adaptiveLODController.notifyUserInteractionInProgress()

            // final GL lGl = pDrawable.getGL();
            // lGl.glClearColor(0, 0, 0, 1);
            // lGl.glClear(GL.GL_COLOR_BUFFER_BIT);

            mViewportX = x
            mViewportY = y
            viewportWidth = pWidth
            viewportHeight = pHeight

            if (pHeight < 16)
                pHeight = 16

            if (pWidth < 16)
                pWidth = 16

            val lAspectRatio = (1.0f * pWidth) / pHeight

            if (lAspectRatio >= 1)
                mQuadProjectionMatrix.setOrthoProjectionMatrix(-1f,
                        1f,
                        -1 / lAspectRatio,
                        1 / lAspectRatio,
                        0f,
                        1000f)
            else
                mQuadProjectionMatrix.setOrthoProjectionMatrix(-lAspectRatio,
                        lAspectRatio,
                        -1f,
                        1f,
                        0f,
                        1000f)/**/

            // FIXME: first layer decides... this is temporary hack, should be
            // resolvd
            // by using the scene graph
            val lMaxVolumeDimension = max(getVolumeSizeX(0),
                    max(getVolumeSizeY(0),
                            getVolumeSizeZ(0))).toInt()

            val lMaxTextureWidth = min(mMaxRenderWidth,
                    2 * lMaxVolumeDimension)
            val lMaxTextureHeight = min(mMaxRenderHeight,
                    2 * lMaxVolumeDimension)

            val lCandidateTextureWidth = ((min(lMaxTextureWidth,
                    viewportWidth) / 128) * 128)
            val lCandidateTextureHeight = ((min(lMaxTextureHeight,
                    viewportHeight) / 128) * 128)

            if (lCandidateTextureWidth == 0 || lCandidateTextureHeight == 0)
                return

            var lRatioWidth = (renderWidth.toFloat()) / lCandidateTextureWidth
            var lRatioHeight = (renderHeight.toFloat()) / lCandidateTextureHeight
            var lRatioAspect = ((renderWidth.toFloat()) / renderHeight) / (lCandidateTextureWidth.toFloat() / lCandidateTextureHeight)

            if (lRatioWidth > 0 && lRatioWidth < 1)
                lRatioWidth = 1f / lRatioWidth

            if (lRatioHeight > 0 && lRatioHeight < 1)
                lRatioHeight = 1f / lRatioHeight

            if (lRatioAspect > 0 && lRatioAspect < 1)
                lRatioAspect = 1f / lRatioAspect

            if (lRatioWidth > cTextureDimensionChangeRatioThreshold || lRatioHeight > cTextureDimensionChangeRatioThreshold || lRatioAspect > cTextureAspectChangeRatioThreshold) {
                renderWidth = lCandidateTextureWidth
                renderHeight = lCandidateTextureHeight
                mUpdateTextureWidthHeight = true

            }

        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    override fun setClearGLWindow(pClearGLWindow: ClearGLWindow) {

        mClearGLWindow = pClearGLWindow
    }

    private fun setDefaultProjectionMatrix() {
        if (clearGLWindow != null) {
            if (System.getProperty("ClearVolume.Anaglyph") == null) {
                clearGLWindow!!.setPerspectiveProjectionMatrix(fov,
                        1f,
                        .1f,
                        1000f)
            } else {
                val ed = 0.01f
                val conv = 1.0f
                System.err.println("Eye distance: $ed, convergence: $conv")
                clearGLWindow!!.setPerspectiveAnaglyphProjectionMatrix(fov,
                        conv,
                        (viewportWidth / viewportHeight).toFloat(),
                        ed,
                        0.01f,
                        1000f)
            }
        }

    }

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.setVisible
     */
    override fun setVisible(pIsVisible: Boolean) {
        if (mNewtCanvasAWT == null)
            mClearGLWindow!!.isVisible = pIsVisible
    }

    /**
     * @param pTitleString
     */
    private fun setWindowTitle(pTitleString: String) {
        mClearGLWindow!!.windowTitle = pTitleString
    }

    /**
     * Toggles box display.
     */
    override fun toggleBoxDisplay() {
        mOverlayMap["box"]?.toggle()
    }

    /**
     * Interface method implementation

     * @see clearvolume.renderer.ClearVolumeRendererInterface.toggleFullScreen
     */
    override fun toggleFullScreen() {
        try {
            if (mClearGLWindow!!.isFullscreen) {
                if (mLastWindowWidth > 0 && mLastWindowHeight > 0)
                    mClearGLWindow!!.setSize(mLastWindowWidth,
                            mLastWindowHeight)
                mClearGLWindow!!.isFullscreen = false
            } else {
                mLastWindowWidth = windowWidth
                mLastWindowHeight = windowHeight
                mClearGLWindow!!.isFullscreen = true
            }
            // notifyUpdateOfVolumeRenderingParameters();
            requestDisplay()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Toggles recording of rendered window frames.
     */
    override fun toggleRecording() {
        mGLVideoRecorder.toggleActive()
    }

    /**
     * Updates the display of the framerate.
     */
    private fun updateFrameRateDisplay() {
        if (mNewtCanvasAWT != null)
            return
    }

    override fun getGL() : GL {
        return mClearGLWindow!!.gl
    }

    override fun getScene() : Scene {
       return this.scene
    }

    companion object {

        private val cTextureDimensionChangeRatioThreshold = 1.05
        private val cTextureAspectChangeRatioThreshold = 1.05
        private val cMaxWaitingTimeForAcquiringDisplayLockInMs = 200

        private val cOverlay2dProjectionMatrix = GLMatrix.getOrthoProjectionMatrix(-1f,
                1f,
                -1f,
                1f,
                0f,
                1000f)/**/

        init {
            // attempt at solving Jug's Dreadlock bug:
            val lProfile = GLProfile.get(GLProfile.GL3)
            // System.out.println( lProfile );
        }
    }

}
/**
 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
 * name and its dimensions.

 * @param pWindowName
 * *            window name
 * *
 * @param pWindowWidth
 * *            window width
 * *
 * @param pWindowHeight
 * *            window height
 */
/**
 * Constructs an instance of the JoglPBOVolumeRenderer class given a window
 * name, its dimensions, and bytes-per-voxel.

 * @param pWindowName
 * *            window name
 * *
 * @param pWindowWidth
 * *            window width
 * *
 * @param pWindowHeight
 * *            window height
 * *
 * @param pNativeTypeEnum
 * *            native type
 */
