package clearvolume.renderer.scenegraph;

import cleargl.GLProgram;
import clearvolume.renderer.scenegraph.opencl.OpenCLVolumeRenderer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import org.apache.commons.io.IOUtils;
import scenery.GeometricalObject;
import java.nio.FloatBuffer;
import java.io.InputStream;
import java.io.IOException;

public class VolumeNode extends GeometricalObject {

    OpenCLVolumeRenderer vrenderer;
    float[] vertices;
    float[] texcoords;

    protected int layer = 0;
    protected String nodeType = "VolumeNode";

    public VolumeNode(OpenCLVolumeRenderer VolumeRenderer) {
        super(3, GL.GL_TRIANGLES);

        vrenderer = VolumeRenderer;
        setRenderer(VolumeRenderer);

        vertices = new float[]{
        -1.0f,-1.0f,0.0f,
        1.0f,-1.0f,0.0f,
        1.0f,1.0f,0.0f,
        -1.0f,-1.0f,0.0f,
        1.0f,1.0f,0.0f,
        -1.0f,1.0f,0.0f
        };

        texcoords = new float[]{0.0f,0.0f,
        1.0f,0.0f,
        1.0f,1.0f,
        0.0f,0.0f,
        1.0f,1.0f,
        0.0f,1.0f
        };
    }

    public boolean init() {
        InputStream lVertexShaderResourceAsStream = VolumeNode.class.getResourceAsStream("shaders/VolumeRender.vs");
        InputStream lFragmentShaderResourceAsStream = VolumeNode.class.getResourceAsStream("shaders/VolumeRender.fs");

        String lVertexShaderSource = "";
        String lFragmentShaderSource = "";

        try {
            lVertexShaderSource = IOUtils.toString(lVertexShaderResourceAsStream, "UTF-8");
            lFragmentShaderSource = IOUtils.toString(lFragmentShaderResourceAsStream, "UTF-8");


            for (int i = 0; i < 1; i++) {
                String lStringToInsert1 = "uniform sampler2D texUnit" + i + "; \n//insertpoint1";
                String lStringToInsert2 = "tempOutColor = max(tempOutColor,texture(texUnit" + i + ", VertexIn.TexCoord));\n//insertpoint2";

                lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint1",
                        lStringToInsert1);
                //lFragmentShaderSource = lFragmentShaderSource.replace("//insertpoint2",
//                    lStringToInsert2)
            }

            System.out.println(lFragmentShaderSource);

            setProgram(GLProgram.buildProgram(getGL(),
                    lVertexShaderSource,
                    lFragmentShaderSource));
        } catch (IOException e) {
            System.err.println("Could not read shaders");
        }

        super.init();

        setVerticesAndCreateBuffer(FloatBuffer.wrap(vertices));
        setTextureCoordsAndCreateBuffer(FloatBuffer.wrap(texcoords));

        return true;
    }

    public int getLayer() {
        return layer;
    }

    public void draw() {
        getGL().glDisable(GL.GL_CULL_FACE);
        getGL().glEnable(GL.GL_BLEND);
        getGL().glBlendFunc(GL.GL_ONE, GL.GL_ONE);
        getGL().glBlendEquation(GL2ES3.GL_MAX);
        getGL().glDisable(GL.GL_DEPTH_TEST);

        vrenderer.renderVolume(
                    getModelView().clone().transpose().getFloatArray(),
                    getModelView().clone().transpose().getFloatArray(), layer);

        vrenderer.clearChangeOfVolumeParametersFlag();
        getProgram().getUniform("texUnit0").setInt(layer);
        getProgram().getUniform("layer").setInt(layer);

        super.draw();
    }
}