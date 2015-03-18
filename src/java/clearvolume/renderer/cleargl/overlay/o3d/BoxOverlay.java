package clearvolume.renderer.cleargl.overlay.o3d;

import java.io.IOException;

import javax.media.opengl.GL4;

import cleargl.ClearGeometryObject;
import cleargl.GLFloatArray;
import cleargl.GLIntArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.cleargl.overlay.Overlay3D;
import clearvolume.renderer.cleargl.overlay.OverlayBase;
import clearvolume.renderer.cleargl.overlay.SingleKeyToggable;

import com.jogamp.newt.event.KeyEvent;

/**
 * BoxOverlay - Nice shader based box and grid 3D overlay.
 *
 * @author Ulrik Guenther (2015), Loic Royer (2015)
 *
 */
public class BoxOverlay extends OverlayBase	implements
																						Overlay3D,
																						SingleKeyToggable
{
	protected GLProgram mBoxGLProgram;
	protected ClearGeometryObject mClearGeometryObject;
	private volatile boolean mHasChanged = true;

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
		return mHasChanged;
	}

	@Override
	public boolean toggleDisplay()
	{
		mHasChanged = true;
		return super.toggleDisplay();
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.SingleKeyToggable#toggleKeyCode()
	 */
	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_B;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.SingleKeyToggable#toggleKeyModifierMask()
	 */
	@Override
	public int toggleKeyModifierMask()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#init(javax.media.opengl.GL4, clearvolume.renderer.DisplayRequestInterface)
	 */
	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		try
		{
			mBoxGLProgram = GLProgram.buildProgram(	pGL4,
																							BoxOverlay.class,
																							"shaders/newbox_vert.glsl",
																							"shaders/newbox_frag.glsl");

			mClearGeometryObject = new ClearGeometryObject(	mBoxGLProgram,
																											3,
																											GL4.GL_TRIANGLES);

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

			lIndexIntArray.add(	0,
													1,
													2,
													0,
													2,
													3,
													4,
													5,
													6,
													4,
													6,
													7,
													8,
													9,
													10,
													8,
													10,
													11,
													12,
													13,
													14,
													12,
													14,
													15,
													16,
													17,
													18,
													16,
													18,
													19,
													20,
													21,
													22,
													20,
													22,
													23);

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

			mClearGeometryObject.setVerticesAndCreateBuffer(lVerticesFloatArray.getFloatBuffer());
			mClearGeometryObject.setNormalsAndCreateBuffer(lNormalArray.getFloatBuffer());
			mClearGeometryObject.setTextureCoordsAndCreateBuffer(lTexCoordFloatArray.getFloatBuffer());

			mClearGeometryObject.setIndicesAndCreateBuffer(lIndexIntArray.getIntBuffer());

		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay3D#render3D(javax.media.opengl.GL4, cleargl.GLMatrix, cleargl.GLMatrix)
	 */
	@Override
	public void render3D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			final GLMatrix lInvBoxMatrix = new GLMatrix();
			lInvBoxMatrix.copy(pInvVolumeMatrix);
			lInvBoxMatrix.transpose();
			lInvBoxMatrix.invert();

			mClearGeometryObject.setModelView(lInvBoxMatrix);
			mClearGeometryObject.setProjection(pProjectionMatrix);

			pGL4.glDisable(GL4.GL_DEPTH_TEST);
			pGL4.glEnable(GL4.GL_CULL_FACE);
			pGL4.glEnable(GL4.GL_BLEND);
			pGL4.glBlendFunc(GL4.GL_ONE, GL4.GL_ONE);
			pGL4.glBlendEquation(GL4.GL_MAX);
			pGL4.glFrontFace(GL4.GL_CW);
			mBoxGLProgram.use(pGL4);
			mClearGeometryObject.draw();

			mHasChanged = false;
		}
	}

}
