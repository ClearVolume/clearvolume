package clearvolume.renderer.cleargl.overlay.o3d;

import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import cleargl.ClearGeometryObject;
import cleargl.GLFloatArray;
import cleargl.GLIntArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.SingleKeyToggable;
import clearvolume.renderer.cleargl.overlay.Overlay3D;
import clearvolume.renderer.cleargl.overlay.OverlayBase;

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
	public boolean toggle()
	{
		mHasChanged = true;
		return super.toggle();
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
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#init(javax.media.opengl.GL, clearvolume.renderer.DisplayRequestInterface)
	 */
	@Override
	public void init(	GL pGL,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		try
		{
			mBoxGLProgram = GLProgram.buildProgram(	pGL,
																							BoxOverlay.class,
																							"shaders/box_vert.glsl",
																							"shaders/box_frag.glsl");

			mClearGeometryObject = new ClearGeometryObject(	mBoxGLProgram,
																											3,
																											GL.GL_TRIANGLES);

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
			mClearGeometryObject.setModelView(pModelViewMatrix);
			mClearGeometryObject.setProjection(pProjectionMatrix);

			pGL.glDisable(GL.GL_DEPTH_TEST);
			pGL.glEnable(GL.GL_CULL_FACE);
			pGL.glEnable(GL.GL_BLEND);
			pGL.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			pGL.glBlendEquation(GL2.GL_MAX);
			pGL.glFrontFace(GL.GL_CW);
			mBoxGLProgram.use(pGL);
			mClearGeometryObject.draw();

			mHasChanged = false;
		}
	}

}
