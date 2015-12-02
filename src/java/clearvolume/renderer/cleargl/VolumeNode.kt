package clearvolume.renderer.cleargl;

import cleargl.*
import cleargl.scenegraph.Node

import clearvolume.renderer.ClearVolumeRendererInterface

import clearvolume.renderer.processors.ProcessorInterface
import clearvolume.transferf.TransferFunction
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL4
import com.jogamp.opengl.GL2ES3
import coremem.types.NativeTypeEnum
import org.apache.commons.io.IOUtils

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

class VolumeNode(private val gl: GL4, private val backend: ClearVolumeRendererInterface) : Node("Volume") {

    // Shader attributes, uniforms and arrays:
    protected var mPositionAttribute: GLAttribute
    protected var mQuadVertexArray: GLVertexArray
    protected var mPositionAttributeArray: GLVertexAttributeArray
    protected var mQuadProjectionMatrixUniform: GLUniform
    protected var mTexCoordAttribute: GLAttribute
    protected var mTexUnits: Array<GLUniform>
    protected var mTexCoordAttributeArray: GLVertexAttributeArray

    protected var renderLayers: Array<GLTexture>
    var numberOfRenderLayers = 0
        protected set

    // volume rendering attributes
    var gamma: Double = 0.toDouble()
    var processors: Collection<ProcessorInterface<*>>
    var brightness: Double = 0.toDouble()
    var bytesPerVoxel: Int = 0
    var clipBox: FloatArray
    var transferFunction: TransferFunction
    var transferRangeMax: Double = 0.toDouble()
    var transferRangeMin: Double = 0.toDouble()
    var renderQuality: Double = 0.toDouble()


    init {
        this.name = "Volume"

        this.gl.swapInterval = 1

        this.gl.glDisable(GL.GL_DEPTH_TEST)
        this.gl.glEnable(GL.GL_BLEND)
        this.gl.glDisable(GL.GL_STENCIL_TEST)

        this.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        this.gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

        if (backend.initVolumeRenderer()) {

            // texture display: construct the program and related objects
            try {
                val lVertexShaderResourceAsStream = VolumeNode::class.java.getResourceAsStream("shaders/tex_vert.glsl")
                val lFragmentShaderResourceAsStream = VolumeNode::class.java.getResourceAsStream("shaders/tex_frag.glsl")

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

                this.setProgram(GLProgram.buildProgram(this.gl,
                        lVertexShaderSource,
                        lFragmentShaderSource))
                mQuadProjectionMatrixUniform = this.program!!.getUniform("projection")
                mPositionAttribute = this.program!!.getAtribute("position")
                mTexCoordAttribute = this.program!!.getAtribute("texcoord")
                mTexUnits = arrayOfNulls<GLUniform>(numberOfRenderLayers)
                for (i in 0..numberOfRenderLayers - 1) {
                    mTexUnits[i] = this.program!!.getUniform("texUnit" + i)
                    mTexUnits[i].setInt(i)
                }

                mQuadVertexArray = GLVertexArray(this.program)
                mQuadVertexArray.bind()
                mPositionAttributeArray = GLVertexAttributeArray(mPositionAttribute,
                        4)

                val lVerticesFloatArray = GLFloatArray(6,

                        4)

                lVerticesFloatArray.add(-1, -1, 0, 1)
                lVerticesFloatArray.add(1, -1, 0, 1)
                lVerticesFloatArray.add(1, 1, 0, 1)
                lVerticesFloatArray.add(-1, -1, 0, 1)
                lVerticesFloatArray.add(1, 1, 0, 1)
                lVerticesFloatArray.add(-1, 1, 0, 1)

                mQuadVertexArray.addVertexAttributeArray(mPositionAttributeArray,
                        lVerticesFloatArray.floatBuffer)

                mTexCoordAttributeArray = GLVertexAttributeArray(mTexCoordAttribute,
                        2)

                val lTexCoordFloatArray = GLFloatArray(6,
                        2)
                lTexCoordFloatArray.add(0, 0)
                lTexCoordFloatArray.add(1, 0)
                lTexCoordFloatArray.add(1, 1)
                lTexCoordFloatArray.add(0, 0)
                lTexCoordFloatArray.add(1, 1)
                lTexCoordFloatArray.add(0, 1)

                mQuadVertexArray.addVertexAttributeArray(mTexCoordAttributeArray,
                        lTexCoordFloatArray.floatBuffer)

            } catch (e: IOException) {
                e.printStackTrace()
            }

            ensureTextureAllocated()

        }
    }

    private fun ensureTextureAllocated() {
        if (mUpdateTextureWidthHeight) {
            getDisplayLock().lock()
            try {
                for (i in 0..numberOfRenderLayers - 1) {
                    if (this.renderLayers[i] != null)
                        this.renderLayers[i].close()

                    this.renderLayers[i] = GLTexture(this.program,
                            NativeTypeEnum.UnsignedByte,
                            4,
                            mRenderWidth,
                            mRenderHeight,
                            1,
                            true,
                            2)
                    this.renderLayers[i].clear()

                }

                notifyChangeOfTextureDimensions()
                notifyChangeOfVolumeRenderingParameters()

            } finally {
                mUpdateTextureWidthHeight = false
                if (getDisplayLock().isHeldByCurrentThread())
                    getDisplayLock().unlock()
            }
        }
    }

    override fun draw() {
        this.gl.glDisable(GL.GL_CULL_FACE)
        this.gl.glEnable(GL.GL_BLEND)
        this.gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE)
        this.gl.glBlendEquation(GL2ES3.GL_MAX)

        GLError.printGLErrors(this.gl, "BEFORE RENDER VOLUME: " + this.name)

        VolumeRenderer.renderVolume(this.modelview!!.clone().invert().transpose().floatArray,
                this.getProjection().clone().invert().transpose().floatArray)

        clearChangeOfVolumeParametersFlag()

        GLError.printGLErrors(this.gl, "AFTER RENDER VOLUME:" + this.name)

        this.program!!.use(this.gl)

        for (i in 0..numberOfRenderLayers - 1)
            this.renderLayers[i].bind(i)

        mQuadProjectionMatrixUniform.setFloatMatrix(mQuadProjectionMatrix.getFloatArray(),
                false)

        mQuadVertexArray.draw(GL.GL_TRIANGLES)
    }
}