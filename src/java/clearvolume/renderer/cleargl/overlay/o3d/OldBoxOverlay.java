package clearvolume.renderer.cleargl.overlay.o3d;

import java.io.IOException;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

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
 * OldBoxOverlay - old style 3D box overlay.
 *
 * @author Loic Royer (2015)
 *
 */
public class OldBoxOverlay extends OverlayBase implements Overlay3D
{
	private static final float cBoxLineWidth = 1.f; // only cBoxLineWidth = 1.f
	// seems to be supported

	private static final FloatBuffer cBoxColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 0.5f });

	private GLProgram mBoxGLProgram;

	private GLAttribute mBoxPositionAttribute;
	private GLVertexArray mBoxVertexArray;
	private GLVertexAttributeArray mBoxPositionAttributeArray;
	private GLUniform mBoxColorUniform;

	private GLUniform mOverlayModelViewMatrixUniform;
	private GLUniform mOverlayProjectionMatrixUniform;

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#getName()
	 */
	@Override
	public String getName()
	{
		return "box";
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
																							OldBoxOverlay.class,
																							"shaders/oldbox_vert.glsl",
																							"shaders/oldbox_frag.glsl");

			mOverlayModelViewMatrixUniform = mBoxGLProgram.getUniform("modelview");
			mOverlayProjectionMatrixUniform = mBoxGLProgram.getUniform("projection");

			// Makes line anti-aliased:
			// pGL.glEnable(GL.GL_BLEND);
			// pGL.glEnable(GL.GL_LINE_SMOOTH);

			// set the line with of the box
			pGL.glLineWidth(cBoxLineWidth);

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
			mBoxGLProgram.use(pGL);

			mOverlayModelViewMatrixUniform.setFloatMatrix(pModelViewMatrix.getFloatArray(),
																										false);

			mOverlayProjectionMatrixUniform.setFloatMatrix(	pProjectionMatrix.getFloatArray(),
																											false);

			mBoxColorUniform.setFloatVector4(cBoxColor);

			mBoxVertexArray.draw(GL.GL_LINES);

		}
	}

}
