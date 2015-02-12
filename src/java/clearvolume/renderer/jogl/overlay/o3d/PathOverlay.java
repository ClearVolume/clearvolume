package clearvolume.renderer.jogl.overlay.o3d;

import cleargl.*;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.Overlay3D;
import clearvolume.renderer.jogl.overlay.OverlayBase;

import javax.media.opengl.GL4;
import java.nio.FloatBuffer;

/**
 * Single Path 3D Overlay.
 *
 * @author Loic Royer, Ulrik Guenther (2015)
 *
 */
public class PathOverlay extends OverlayBase implements Overlay3D
{
  protected static final FloatBuffer cBoxColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 1.f });

	protected GLProgram mBoxGLProgram;

  protected GLAttribute mBoxPositionAttribute;
  protected GLVertexArray mBoxVertexArray;
  protected GLVertexAttributeArray mBoxPositionAttributeArray;
  protected GLUniform mBoxColorUniform;

  protected GLUniform mOverlayModelViewMatrixUniform;
  protected GLUniform mOverlayProjectionMatrixUniform;

  protected ClearGeometryObject mPath;
  protected GLFloatArray mPathPoints;

  protected FloatBuffer mStartColor = FloatBuffer.wrap(new float[]{1.0f, 1.0f, 1.0f, 1.0f});
  protected FloatBuffer mEndColor = FloatBuffer.wrap(new float[]{1.0f, 1.0f, 1.0f, 1.0f});

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#getName()
	 */
	@Override
	public String getName()
	{
		return "path";
	}


	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay3D#hasChanged3D()
	 */
	@Override
	public boolean hasChanged3D()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#init(javax.media.opengl.GL4, clearvolume.renderer.DisplayRequestInterface)
	 */
	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		// box display: construct the program and related objects
		try
    {
      mBoxGLProgram =
              GLProgram.buildProgram(	pGL4,
                      PathOverlay.class,
                      new String[]{"shaders/path_vert.glsl",
                      "shaders/path_frag.glsl"});
      mPath = new ClearGeometryObject(mBoxGLProgram,
              3, GL4.GL_LINE_STRIP );
      mPath.setDynamic(true);

      mPathPoints = new GLFloatArray(0, 3);

      mPath.setVerticesAndCreateBuffer(mPathPoints.getFloatBuffer());
      mStartColor = FloatBuffer.wrap(new float[]{1.0f, 0.0f, 0.0f, 1.0f});

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

  public void setStartEndColor(float[] startColor, float[] endColor) {
    mStartColor = FloatBuffer.wrap(startColor);
    mEndColor = FloatBuffer.wrap(endColor);
  }

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay3D#render3D(javax.media.opengl.GL4, cleargl.GLMatrix, cleargl.GLMatrix)
	 */
	@Override
	public void render3D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
      //mPath.getProgram().use(pGL4);
      mBoxGLProgram.use(pGL4);

      mPath.getProgram().getUniform("vertexCount").set((mPathPoints.getFloatBuffer().capacity()/3));
      mPath.getProgram().getUniform("startColor").setFloatVector4(mStartColor);
      mPath.getProgram().getUniform("endColor").setFloatVector4(mEndColor);

			// invert Matrix is the mModelViewMatrix used by renderer which is actually the
			// inverted mModelViewMatrix Matrix
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

      mPath.setModelView(lInvBoxMatrix);
      mPath.setProjection(pProjectionMatrix);

      pGL4.glEnable(GL4.GL_BLEND);
      pGL4.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
      pGL4.glBlendEquation(GL4.GL_FUNC_ADD);

      mPath.draw();

      //mPathPoints.add(-0.2f + (float) Math.random() * ((0.2f - (-0.2f)) + 0.4f), -0.2f + (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f), -0.2f + (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f));
      mPath.updateVertices(mPathPoints.getFloatBuffer());
		}
	}

  public FloatBuffer getPathPoints() {
    return mPathPoints.getFloatBuffer();
  }
}
