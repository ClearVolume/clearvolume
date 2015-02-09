package clearvolume.renderer.jogl.overlay.o3d;

import cleargl.*;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.OverlayBase;

import javax.media.opengl.GL4;
import java.nio.FloatBuffer;

/**
 * Single Path 3D Overlay.
 *
 * @author Loic Royer (2015)
 *
 */
public class PathOverlay extends OverlayBase
{
	private static final FloatBuffer cBoxColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 1.f });

	private GLProgram mBoxGLProgram;

	private GLAttribute mBoxPositionAttribute;
	private GLVertexArray mBoxVertexArray;
	private GLVertexAttributeArray mBoxPositionAttributeArray;
	private GLUniform mBoxColorUniform;

	private GLUniform mOverlayModelViewMatrixUniform;
	private GLUniform mOverlayProjectionMatrixUniform;

  private ClearGeometryObject mPath;
  private GLFloatArray mPathPoints;

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#getName()
	 */
	@Override
	public String getName()
	{
		return "path";
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay2D#hasChanged2D()
	 */
	@Override
	public boolean hasChanged2D()
	{
		return false;
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

      mPathPoints = new GLFloatArray(2, 3);

      // Front
      mPathPoints.add(0, 0, 0);
      mPathPoints.add((float)Math.random()*0.4f, (float)Math.random()*0.4f, (float)Math.random()*0.4f);

      mPath.setVerticesAndCreateBuffer(mPathPoints.getFloatBuffer());

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
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

			// invert Matrix is the mModelViewMatrix used by renderer which is actually the
			// inverted mModelViewMatrix Matrix
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

      mPath.setModelView(lInvBoxMatrix);
      mPath.setProjection(pProjectionMatrix);

			mPath.draw();

      mPathPoints.add(-0.2f + (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f), -0.2f + (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f), -0.2f + (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f));
      mPath.updateVertices(mPathPoints.getFloatBuffer());
		}
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay2D#render2D(javax.media.opengl.GL4, cleargl.GLMatrix, cleargl.GLMatrix)
	 */
	@Override
	public void render2D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			// draw someything
		}
	}

}
