package clearvolume.renderer.jogl.overlay.std;

import cleargl.*;
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.jogl.overlay.JOGLOverlay;

import javax.media.opengl.GL;
import javax.media.opengl.GL4;
import java.io.IOException;
import java.nio.FloatBuffer;

public class BoxOverlay extends JOGLOverlay
{
	private static final float cBoxLineWidth = 1.f; // only cBoxLineWidth = 1.f
	// seems to be supported

	private static final FloatBuffer cBoxColor = FloatBuffer.wrap(new float[]
	{ 1.f, 0.f, 0.0f, 0.1f });

	private GLProgram mBoxGLProgram;

	private GLAttribute mBoxPositionAttribute;
  private GLAttribute mTexCoordAttribute;
	private GLVertexArray mBoxVertexArray;
	private GLVertexAttributeArray mBoxPositionAttributeArray;
  private GLVertexAttributeArray mTexCoordAttributeArray;
	private GLUniform mBoxColorUniform;

  private CLGeometryObject geom;

	private GLUniform mOverlayModelViewMatrixUniform;
	private GLUniform mOverlayProjectionMatrixUniform;


	@Override
	public String getName()
	{
		return "box";
	}

  @Override
  public void init(GL4 pGL4)
  {
    // box display: construct the program and related objects
    try
    {
      geom = new CLGeometryObject(pGL4, 3, GL4.GL_TRIANGLES);

      mBoxGLProgram = GLProgram.buildProgram(	pGL4,
              JOGLClearVolumeRenderer.class,
              "shaders/new_box.vs",
              "shaders/new_box.fs");

      geom.setProgram(mBoxGLProgram);

      pGL4.getGL().glFrontFace(GL.GL_CW);
      //pGL4.getGL().glEnable(GL.GL_CULL_FACE);
      //pGL4.getGL().glCullFace(GL.GL_FRONT);

      //mOverlayModelViewMatrixUniform = mBoxGLProgram.getUniform("modelview");
      //mOverlayProjectionMatrixUniform = mBoxGLProgram.getUniform("projection");

      // set the line with of the box
      pGL4.glLineWidth(cBoxLineWidth);

      // get all the shaders uniform locations
      //mBoxPositionAttribute = mBoxGLProgram.getAtribute("position");

      //mBoxColorUniform = mBoxGLProgram.getUniform("color");

      // FIXME this should be done with IndexArrays, but lets be lazy for
      // now...
      final GLFloatArray lVerticesFloatArray = new GLFloatArray(24, 4);

      final float w = 0.5f;

      // Front
      lVerticesFloatArray.add(-w, -w, w);
      lVerticesFloatArray.add(w, -w, w);
      lVerticesFloatArray.add(w, w, w);
      lVerticesFloatArray.add(-w, w, w);
      // Right
      lVerticesFloatArray.add(w, -w, w);
      lVerticesFloatArray.add(w, -w, -w);
      lVerticesFloatArray.add(w, w, -w);
      lVerticesFloatArray.add(w, w, w);
      // Back
      lVerticesFloatArray.add(w, -w, -w);
      lVerticesFloatArray.add(-w, w, -w);
      lVerticesFloatArray.add(w, w, -w);
      lVerticesFloatArray.add(w, -w, -w);
      // Left
      lVerticesFloatArray.add(-w, -w, w);
      lVerticesFloatArray.add(-w, w, w);
      lVerticesFloatArray.add(-w, w, -w);
      lVerticesFloatArray.add(-w, -w, -w);
      // Bottom
      lVerticesFloatArray.add(-w, -w, w);
      lVerticesFloatArray.add(-w, -w, -w);
      lVerticesFloatArray.add(w, -w, -w);
      lVerticesFloatArray.add(w, -w, w);
      // Top
      lVerticesFloatArray.add(w, w, w);
      lVerticesFloatArray.add(w, w, w);
      lVerticesFloatArray.add(w, w, -w);
      lVerticesFloatArray.add(w, w, -w);

      final GLIntArray lIndexIntArray = new GLIntArray(36, 1);

      lIndexIntArray.add(0, 1, 2, 0, 2, 3,
              4, 5, 6, 4, 6, 7,
              8, 9, 10, 8, 10, 11,
              12, 13, 14, 12, 14, 15,
              16, 17, 18, 16, 18, 19,
              20, 21, 22, 20, 22, 23);

      final GLFloatArray lTexCoordFloatArray = new GLFloatArray(24, 2);

      lTexCoordFloatArray.add(0.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 0.0f);
      lTexCoordFloatArray.add(1.0f, 1.0f);
      lTexCoordFloatArray.add(0.0f, 1.0f);

      geom.setVerticesAndCreateBuffer(lVerticesFloatArray.getFloatBuffer());
      geom.setNormalsAndCreateBuffer(lVerticesFloatArray.getFloatBuffer());
      geom.setTextureCoordsAndCreateBuffer(lTexCoordFloatArray.getFloatBuffer());
      geom.setIndicesAndCreateBuffer(lIndexIntArray.getIntBuffer());

    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }

	@Override
	public void render(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			//pGL4.glEnable(GL4.GL_CULL_FACE);

			// invert Matrix is the modelview used by renderer which is actually the
			// inverted modelview Matrix
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

      geom.setModelView(lInvBoxMatrix);
      geom.setProjection(pProjectionMatrix);
      geom.draw();

      //pGL4.glDisable(GL4.GL_CULL_FACE);
		}
	}



}
