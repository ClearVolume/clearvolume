package clearvolume.renderer.jogl.overlay.std;


import cleargl.*;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.jogl.overlay.JOGLOverlay;

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
  public void init(	GL4 pGL4,
                     DisplayRequestInterface pDisplayRequestInterface)
  {
    try
    {
      geom = new CLGeometryObject(pGL4, 3, GL4.GL_TRIANGLES);

      mBoxGLProgram = GLProgram.buildProgram(	pGL4,
              JOGLClearVolumeRenderer.class,
              "shaders/new_box.vs",
              "shaders/new_box.fs");

      geom.setProgram(mBoxGLProgram);

      final GLFloatArray lVerticesFloatArray = new GLFloatArray(24, 3);

      final float w = 1.0f;

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
      lVerticesFloatArray.add(-w, -w, -w);
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
      lVerticesFloatArray.add(-w, w, w);
      lVerticesFloatArray.add(w, w, w);
      lVerticesFloatArray.add(w, w, -w);
      lVerticesFloatArray.add(-w, w, -w);

      final GLFloatArray lNormalArray = new GLFloatArray(24, 3);

      // Front
      lNormalArray.add(0.0f, 0.0f, 1.0f);
      lNormalArray.add(0.0f, 0.0f, 1.0f);
      lNormalArray.add(0.0f, 0.0f, 1.0f);
      lNormalArray.add(0.0f, 0.0f, 1.0f);
      // Right
      lNormalArray.add(1.0f, 0.0f, 0.0f);
      lNormalArray.add(1.0f, 0.0f, 0.0f);
      lNormalArray.add(1.0f, 0.0f, 0.0f);
      lNormalArray.add(1.0f, 0.0f, 0.0f);
      // Back
      lNormalArray.add(0.0f, 0.0f, -1.0f);
      lNormalArray.add(0.0f, 0.0f, -1.0f);
      lNormalArray.add(0.0f, 0.0f, -1.0f);
      lNormalArray.add(0.0f, 0.0f, -1.0f);
      // Left
      lNormalArray.add(-1.0f, 0.0f, 0.0f);
      lNormalArray.add(-1.0f, 0.0f, 0.0f);
      lNormalArray.add(-1.0f, 0.0f, 0.0f);
      lNormalArray.add(-1.0f, 0.0f, 0.0f);
      // Bottom
      lNormalArray.add(0.0f, -1.0f, 0.0f);
      lNormalArray.add(0.0f, -1.0f, 0.0f);
      lNormalArray.add(0.0f, -1.0f, 0.0f);
      lNormalArray.add(0.0f, -1.0f, 0.0f);
      // Top
      lNormalArray.add(0.0f, 1.0f, 0.0f);
      lNormalArray.add(0.0f, 1.0f, 0.0f);
      lNormalArray.add(0.0f, 1.0f, 0.0f);
      lNormalArray.add(0.0f, 1.0f, 0.0f);


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
      geom.setNormalsAndCreateBuffer(lNormalArray.getFloatBuffer());
      geom.setTextureCoordsAndCreateBuffer(lTexCoordFloatArray.getFloatBuffer());

      geom.setIndicesAndCreateBuffer(lIndexIntArray.getIntBuffer());

    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }

	@Override
	public boolean hasChanged()
	{
		return false;
	}

	@Override
	public void render(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
    {
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

      geom.setModelView(lInvBoxMatrix);
      geom.setProjection(pProjectionMatrix);

      pGL4.glEnable(GL4.GL_DEPTH_TEST);
      pGL4.glEnable(GL4.GL_CULL_FACE);
      pGL4.glFrontFace(GL4.GL_CW);
      geom.draw();
      pGL4.glDisable(GL4.GL_DEPTH_TEST);
      pGL4.glDisable(GL4.GL_CULL_FACE);
		}
	}



}
