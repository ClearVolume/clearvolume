package clearvolume.renderer.jogl.overlay.o3d;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL4;

import cleargl.GLAttribute;
import cleargl.GLFloatArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.Overlay3D;
import clearvolume.renderer.jogl.overlay.OverlayBase;

/**
 * Single Path 3D Overlay.
 *
 * @author Loic Royer (2015)
 *
 */
public class PathOverlay extends OverlayBase implements Overlay3D
{
	private static final float cBoxLineWidth = 1.f; // only cBoxLineWidth = 1.f
	// seems to be supported

	private static final FloatBuffer cBoxColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 1.f });

	private GLProgram mBoxGLProgram;

	private GLAttribute mBoxPositionAttribute;
	private GLVertexArray mBoxVertexArray;
	private GLVertexAttributeArray mBoxPositionAttributeArray;
	private GLUniform mBoxColorUniform;

	private GLUniform mOverlayModelViewMatrixUniform;
	private GLUniform mOverlayProjectionMatrixUniform;

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
			mBoxGLProgram = GLProgram.buildProgram(	pGL4,
																							PathOverlay.class,
																							"shaders/path_vert.glsl",
																							"shaders/path_frag.glsl");

			mOverlayModelViewMatrixUniform = mBoxGLProgram.getUniform("modelview");
			mOverlayProjectionMatrixUniform = mBoxGLProgram.getUniform("projection");

			// set the line with of the box
			pGL4.glLineWidth(cBoxLineWidth);

			// get all the shaders uniform locations
			mBoxPositionAttribute = mBoxGLProgram.getAtribute("position");

			mBoxColorUniform = mBoxGLProgram.getUniform("color");

			// set up the vertices of the box
			mBoxVertexArray = new GLVertexArray(mBoxGLProgram);
			mBoxVertexArray.bind();
			mBoxPositionAttributeArray = new GLVertexAttributeArray(mBoxPositionAttribute,
																															4);

			int lNumberOfPoints = 1000;

			final GLFloatArray lVerticesFloatArray = new GLFloatArray(lNumberOfPoints,
																																4);

			float x = 0, y = 0, z = 0;

			for (int i = 0; i < lNumberOfPoints; i++)
			{
				lVerticesFloatArray.add(x, y, z, 1.0f);
				x += (Math.random() - 0.5) * 0.1;
				y += (Math.random() - 0.5) * 0.1;
				z += (Math.random() - 0.5) * 0.1;
			}

			mBoxVertexArray.addVertexAttributeArray(mBoxPositionAttributeArray,
																							lVerticesFloatArray.getFloatBuffer());

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
			mBoxGLProgram.use(pGL4);

			// invert Matrix is the mModelViewMatrix used by renderer which is actually the
			// inverted mModelViewMatrix Matrix
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

			mOverlayModelViewMatrixUniform.setFloatMatrix(lInvBoxMatrix.getFloatArray(),
																										false);

			mOverlayProjectionMatrixUniform.setFloatMatrix(	pProjectionMatrix.getFloatArray(),
																											false);

			mBoxColorUniform.setFloatVector4(cBoxColor);

			mBoxVertexArray.draw(GL.GL_LINE_STRIP);

		}
	}



}
