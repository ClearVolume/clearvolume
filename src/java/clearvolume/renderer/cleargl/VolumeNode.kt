package clearvolume.renderer.cleargl;

import cleargl.GLProgram
import cleargl.scenegraph.GeometricalObject
import clearvolume.renderer.opencl.OpenCLVolumeRenderer
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import org.apache.commons.io.IOUtils
import java.nio.FloatBuffer

class VolumeNode(VolumeRenderer: OpenCLVolumeRenderer) : GeometricalObject(3, GL.GL_TRIANGLES) {

    var vrenderer: OpenCLVolumeRenderer
    var vertices: FloatArray
    var texcoords: FloatArray

    var layer: Int = 0

    override var nodeType: String = "VolumeNode"

    init {
        vrenderer = VolumeRenderer
        renderer = VolumeRenderer

        vertices = floatArrayOf(
                -1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f
        )

        texcoords = floatArrayOf(0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f
        )
    }

    override fun init(): Boolean {
        val lVertexShaderResourceAsStream = VolumeNode::class.java.getResourceAsStream("shaders/VolumeRender.vs")
        val lFragmentShaderResourceAsStream = VolumeNode::class.java.getResourceAsStream("shaders/VolumeRender.fs")

        val lVertexShaderSource = IOUtils.toString(lVertexShaderResourceAsStream, "UTF-8")
        var lFragmentShaderSource = IOUtils.toString(lFragmentShaderResourceAsStream, "UTF-8")

        for (i in 1..1) {
            val lStringToInsert1 = "uniform sampler2D texUnit%d; \n//insertpoint1".format(i)
            val lStringToInsert2 = "tempOutColor = max(tempOutColor,texture(texUnit%d, VertexIn.TexCoord));\n//insertpoint2".format(i)

            lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint1",
                    lStringToInsert1)
            //lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint2",
//                    lStringToInsert2)
        }

        System.out.println(lFragmentShaderSource)

        program = GLProgram.buildProgram(gl!!,
                lVertexShaderSource,
                lFragmentShaderSource)

        super.init()

        setVerticesAndCreateBuffer(FloatBuffer.wrap(vertices))
        setTextureCoordsAndCreateBuffer(FloatBuffer.wrap(texcoords))

        return true;
    }

    override fun draw() {
        gl!!.glDisable(GL.GL_CULL_FACE)
        gl!!.glEnable(GL.GL_BLEND)
        gl!!.glBlendFunc(GL.GL_ONE, GL.GL_ONE)
        gl!!.glBlendEquation(GL2ES3.GL_MAX)
        gl!!.glDisable(GL.GL_DEPTH_TEST)

        vrenderer.renderVolume(
                    modelView!!.clone().transpose().floatArray,
                    projection!!.clone().transpose().floatArray, layer)

        vrenderer.clearChangeOfVolumeParametersFlag()
        program!!.getUniform("texUnit0").setInt(layer)
        program!!.getUniform("layer").setInt(layer)

        super.draw()
    }
}