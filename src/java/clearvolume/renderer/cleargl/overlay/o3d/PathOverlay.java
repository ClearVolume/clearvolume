package clearvolume.renderer.cleargl.overlay.o3d;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import cleargl.ClearGeometryObject;
import cleargl.GLAttribute;
import cleargl.GLFloatArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.cleargl.overlay.Overlay3D;
import clearvolume.renderer.cleargl.overlay.OverlayBase;

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

	protected FloatBuffer mStartColor = FloatBuffer.wrap(new float[]
	{ 1.0f, 1.0f, 1.0f, 1.0f });
	protected FloatBuffer mEndColor = FloatBuffer.wrap(new float[]
	{ 1.0f, 1.0f, 1.0f, 1.0f });

	public PathOverlay()
	{
		super();
		mPathPoints = new GLFloatArray(0, 3);
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#getName()
	 */
	@Override
	public String getName()
	{
		return "path";
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay3D#hasChanged3D()
	 */
	@Override
	public boolean hasChanged3D()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#init(javax.media.opengl.GL, clearvolume.renderer.DisplayRequestInterface)
	 */
	@Override
	public void init(	GL pGL,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		// box display: construct the program and related objects
		try
		{
			mBoxGLProgram = GLProgram.buildProgram(	pGL,
																							PathOverlay.class,
																							new String[]
																							{ "shaders/path_vert.glsl",
																								"shaders/path_geom.glsl",
																								"shaders/path_frag.glsl" });

			mPath = new ClearGeometryObject(mBoxGLProgram,
																			3,
																			GL.GL_LINE_STRIP);
			mPath.setDynamic(true);

			mPath.setVerticesAndCreateBuffer(mPathPoints.getFloatBuffer());
			mStartColor = FloatBuffer.wrap(new float[]
			{ 1.0f, 0.0f, 0.0f, 1.0f });

		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

	public void setStartEndColor(float[] startColor, float[] endColor)
	{
		mStartColor = FloatBuffer.wrap(startColor);
		mEndColor = FloatBuffer.wrap(endColor);
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay3D#render3D(javax.media.opengl.GL, cleargl.GLMatrix, cleargl.GLMatrix)
	 */
	@Override
	public void render3D(	GL pGL,
												int pWidth,
												int pHeight,
												GLMatrix pProjectionMatrix,
												GLMatrix pModelViewMatrix)
	{
		if (isDisplayed())
		{
			// mPath.getProgram().use(pGL);
			mBoxGLProgram.use(pGL);

			mPath.getProgram()
						.getUniform("vertexCount")
						.setInt((mPathPoints.getFloatBuffer().capacity() / 3));
			mPath.getProgram()
						.getUniform("startColor")
						.setFloatVector4(mStartColor);
			mPath.getProgram()
						.getUniform("endColor")
						.setFloatVector4(mEndColor);

			mPath.setModelView(pModelViewMatrix);
			mPath.setProjection(pProjectionMatrix);

			pGL.glDisable(GL.GL_BLEND);
			pGL.glDisable(GL.GL_CULL_FACE);
			pGL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			pGL.glBlendEquation(GL.GL_FUNC_ADD);

			mPath.draw();

			pGL.glEnable(GL.GL_BLEND);

			// mPathPoints.add(-0.2f + (float) Math.random() * ((0.2f - (-0.2f)) +
			// 0.4f), -0.2f + (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f), -0.2f +
			// (float)Math.random()* ((0.2f - (-0.2f)) + 0.4f));
			mPath.updateVertices(mPathPoints.getFloatBuffer());
		}
	}

	public FloatBuffer getPathPoints()
	{
		return mPathPoints.getFloatBuffer();
	}

	public void addPathPoint(float x, float y, float z)
	{
		mPathPoints.add(x, y, z);
	}
}
