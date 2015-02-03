package clearvolume.renderer.jogl.overlay.std;

import java.io.IOException;
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
import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.jogl.overlay.JOGLOverlay;

public class BoxOverlay extends JOGLOverlay
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
			mBoxGLProgram = GLProgram.buildProgram(	pGL4,
																							JOGLClearVolumeRenderer.class,
																							"shaders/box_vert.glsl",
																							"shaders/box_frag.glsl");

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

			// FIXME this should be done with IndexArrays, but lets be lazy for
			// now...
			final GLFloatArray lVerticesFloatArray = new GLFloatArray(24, 4);

			final float w = 0.5f;

			lVerticesFloatArray.add(w, w, w, w);
			lVerticesFloatArray.add(-w, w, w, w);
			lVerticesFloatArray.add(-w, w, w, w);
			lVerticesFloatArray.add(-w, -w, w, w);
			lVerticesFloatArray.add(-w, -w, w, w);
			lVerticesFloatArray.add(w, -w, w, w);
			lVerticesFloatArray.add(w, -w, w, w);
			lVerticesFloatArray.add(w, w, w, w);
			lVerticesFloatArray.add(w, w, -w, w);
			lVerticesFloatArray.add(-w, w, -w, w);
			lVerticesFloatArray.add(-w, w, -w, w);
			lVerticesFloatArray.add(-w, -w, -w, w);
			lVerticesFloatArray.add(-w, -w, -w, w);
			lVerticesFloatArray.add(w, -w, -w, w);
			lVerticesFloatArray.add(w, -w, -w, w);
			lVerticesFloatArray.add(w, w, -w, w);
			lVerticesFloatArray.add(w, w, w, w);
			lVerticesFloatArray.add(w, w, -w, w);
			lVerticesFloatArray.add(-w, w, w, w);
			lVerticesFloatArray.add(-w, w, -w, w);
			lVerticesFloatArray.add(-w, -w, w, w);
			lVerticesFloatArray.add(-w, -w, -w, w);
			lVerticesFloatArray.add(w, -w, w, w);
			lVerticesFloatArray.add(w, -w, -w, w);

			mBoxVertexArray.addVertexAttributeArray(mBoxPositionAttributeArray,
																							lVerticesFloatArray.getFloatBuffer());

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
			mBoxGLProgram.use(pGL4);

			// invert Matrix is the modelview used by renderer which is actually the
			// inverted modelview Matrix
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

			mOverlayModelViewMatrixUniform.setFloatMatrix(lInvBoxMatrix.getFloatArray(),
																										false);

			mOverlayProjectionMatrixUniform.setFloatMatrix(	pProjectionMatrix.getFloatArray(),
																											false);

			mBoxColorUniform.setFloatVector4(cBoxColor);

			mBoxVertexArray.draw(GL.GL_LINES);

		}
	}



}
