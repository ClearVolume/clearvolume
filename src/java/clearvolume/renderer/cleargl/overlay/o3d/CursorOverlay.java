package clearvolume.renderer.cleargl.overlay.o3d;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

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
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.cleargl.overlay.Overlay3D;
import clearvolume.renderer.cleargl.overlay.OverlayBase;
import clearvolume.renderer.cleargl.utils.ScreenToEyeRay.EyeRay;
import clearvolume.renderer.listeners.EyeRayListener;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

/**
 * CursorOverlay - Displays a movable 3D cursor
 *
 * @author Loic Royer (2015)
 *
 */
public class CursorOverlay extends OverlayBase implements
																							Overlay3D,
																							SingleKeyToggable,
																							EyeRayListener
{
	protected GLProgram mBoxGLProgram;
	protected ClearGeometryObject mPlaneX, mPlaneY, mPlaneZ;
	private volatile boolean mHasChanged = true;

	private final String mName;
	private volatile float x = 0.5f, y = 0.5f, z = 0.5f;
	private volatile float px, py;
	private volatile boolean mMovable = true;
	private volatile float mDistanceThreshold = 30f;
	private float[] mColor = new float[]
	{ 0.8f, 0.8f, 1f, 1f };
	private volatile float mAlpha = 1;
	private volatile float mLinePeriod = 0.5f;
	private volatile float mLineThickness = 0.005f;
	private volatile float mLineLength = 0.15f;
	private volatile float mBoxLinesAlpha = 0.5f;

	private GLFloatArray mVerticesFloatArrayPlaneX;
	private GLFloatArray mVerticesFloatArrayPlaneY;
	private GLFloatArray mVerticesFloatArrayPlaneZ;

	public CursorOverlay(String pName)
	{
		super();
		mName = pName;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#getName()
	 */
	@Override
	public String getName()
	{
		return "cursor" + mName;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay3D#hasChanged3D()
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
	 * @see clearvolume.renderer.jogl.overlay.SingleKeyToggable#toggleKeyCode()
	 */
	@Override
	public short toggleKeyCode()
	{
		return KeyEvent.VK_C;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.SingleKeyToggable#toggleKeyModifierMask()
	 */
	@Override
	public int toggleKeyModifierMask()
	{
		return 0;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#init(javax.media.opengl.GL, clearvolume.renderer.DisplayRequestInterface)
	 */
	@Override
	public void init(	GL pGL,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		try
		{
			mBoxGLProgram = GLProgram.buildProgram(	pGL,
																							CursorOverlay.class,
																							"shaders/cursor_vert.glsl",
																							"shaders/cursor_frag.glsl");

			mPlaneX = new ClearGeometryObject(mBoxGLProgram,
																				3,
																				GL.GL_TRIANGLES);
			mPlaneX.setDynamic(true);/**/

			mPlaneY = new ClearGeometryObject(mBoxGLProgram,
																				3,
																				GL.GL_TRIANGLES);
			mPlaneY.setDynamic(true);

			mPlaneZ = new ClearGeometryObject(mBoxGLProgram,
																				3,
																				GL.GL_TRIANGLES);
			mPlaneZ.setDynamic(true);/**/

			mVerticesFloatArrayPlaneX = new GLFloatArray(4, 3);
			mVerticesFloatArrayPlaneY = new GLFloatArray(4, 3);
			mVerticesFloatArrayPlaneZ = new GLFloatArray(4, 3);

			setPlanesVertices();

			final GLFloatArray lNormalArrayPlaneX = new GLFloatArray(4, 3);
			final GLFloatArray lNormalArrayPlaneY = new GLFloatArray(4, 3);
			final GLFloatArray lNormalArrayPlaneZ = new GLFloatArray(4, 3);

			// X Plane Normals
			lNormalArrayPlaneX.add(1.0f, 0.0f, 0.0f);
			lNormalArrayPlaneX.add(1.0f, 0.0f, 0.0f);
			lNormalArrayPlaneX.add(1.0f, 0.0f, 0.0f);
			lNormalArrayPlaneX.add(1.0f, 0.0f, 0.0f);

			// Y Plane Normals
			lNormalArrayPlaneY.add(0.0f, 1.0f, 0.0f);
			lNormalArrayPlaneY.add(0.0f, 1.0f, 0.0f);
			lNormalArrayPlaneY.add(0.0f, 1.0f, 0.0f);
			lNormalArrayPlaneY.add(0.0f, 1.0f, 0.0f);

			// Z Plane Normals
			lNormalArrayPlaneZ.add(0.0f, 0.0f, 1.0f);
			lNormalArrayPlaneZ.add(0.0f, 0.0f, 1.0f);
			lNormalArrayPlaneZ.add(0.0f, 0.0f, 1.0f);
			lNormalArrayPlaneZ.add(0.0f, 0.0f, 1.0f);

			final GLIntArray lIndexIntArrayPlane = new GLIntArray(6, 1);

			lIndexIntArrayPlane.add(0, 1, 2, 0, 2, 3);

			final GLFloatArray lTexCoordFloatArrayPlane = new GLFloatArray(	4,
																																			2);

			lTexCoordFloatArrayPlane.add(0.0f, 0.0f);
			lTexCoordFloatArrayPlane.add(1.0f, 0.0f);
			lTexCoordFloatArrayPlane.add(1.0f, 1.0f);
			lTexCoordFloatArrayPlane.add(0.0f, 1.0f);

			mPlaneX.setVerticesAndCreateBuffer(mVerticesFloatArrayPlaneX.getFloatBuffer());
			mPlaneX.setNormalsAndCreateBuffer(lNormalArrayPlaneX.getFloatBuffer());
			mPlaneX.setTextureCoordsAndCreateBuffer(lTexCoordFloatArrayPlane.getFloatBuffer());
			mPlaneX.setIndicesAndCreateBuffer(lIndexIntArrayPlane.getIntBuffer());/**/

			mPlaneY.setVerticesAndCreateBuffer(mVerticesFloatArrayPlaneY.getFloatBuffer());
			mPlaneY.setNormalsAndCreateBuffer(lNormalArrayPlaneY.getFloatBuffer());
			mPlaneY.setTextureCoordsAndCreateBuffer(lTexCoordFloatArrayPlane.getFloatBuffer());
			mPlaneY.setIndicesAndCreateBuffer(lIndexIntArrayPlane.getIntBuffer());

			mPlaneZ.setVerticesAndCreateBuffer(mVerticesFloatArrayPlaneZ.getFloatBuffer());
			mPlaneZ.setNormalsAndCreateBuffer(lNormalArrayPlaneZ.getFloatBuffer());
			mPlaneZ.setTextureCoordsAndCreateBuffer(lTexCoordFloatArrayPlane.getFloatBuffer());
			mPlaneZ.setIndicesAndCreateBuffer(lIndexIntArrayPlane.getIntBuffer());/**/

		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	private void setPlanesVertices()
	{
		final float lx = 2 * x - 1;
		final float ly = 2 * y - 1;
		final float lz = 2 * z - 1;

		// X Plane
		mVerticesFloatArrayPlaneX.clear();
		mVerticesFloatArrayPlaneX.add(lx, -1, -1);
		mVerticesFloatArrayPlaneX.add(lx, 1, -1);
		mVerticesFloatArrayPlaneX.add(lx, 1, 1);
		mVerticesFloatArrayPlaneX.add(lx, -1, 1);/**/

		// Y Plane
		mVerticesFloatArrayPlaneY.clear();
		mVerticesFloatArrayPlaneY.add(-1, ly, -1);
		mVerticesFloatArrayPlaneY.add(1, ly, -1);
		mVerticesFloatArrayPlaneY.add(1, ly, 1);
		mVerticesFloatArrayPlaneY.add(-1, ly, 1);

		// Z Plane
		mVerticesFloatArrayPlaneZ.clear();
		mVerticesFloatArrayPlaneZ.add(-1, -1, lz);
		mVerticesFloatArrayPlaneZ.add(1, -1, lz);
		mVerticesFloatArrayPlaneZ.add(1, 1, lz);
		mVerticesFloatArrayPlaneZ.add(-1, 1, lz);/**/
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay3D#render3D(javax.media.opengl.GL, cleargl.GLMatrix, cleargl.GLMatrix)
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
			mPlaneX.setModelView(pModelViewMatrix);
			mPlaneY.setModelView(pModelViewMatrix);
			mPlaneZ.setModelView(pModelViewMatrix);

			mPlaneX.setProjection(pProjectionMatrix);
			mPlaneY.setProjection(pProjectionMatrix);
			mPlaneZ.setProjection(pProjectionMatrix);

			pGL.glDisable(GL.GL_DEPTH_TEST);
			pGL.glDisable(GL.GL_CULL_FACE);
			pGL.glEnable(GL.GL_BLEND);
			pGL.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			pGL.glBlendEquation(GL2.GL_MAX);

			mBoxGLProgram.use(pGL);

			setPlanesVertices();
			mPlaneX.updateVertices(mVerticesFloatArrayPlaneX.getFloatBuffer());
			mPlaneY.updateVertices(mVerticesFloatArrayPlaneY.getFloatBuffer());
			mPlaneZ.updateVertices(mVerticesFloatArrayPlaneZ.getFloatBuffer());

			mBoxGLProgram.getUniform("alpha").setFloat(getAlpha());
			mBoxGLProgram.getUniform("linethick")
										.setFloat(1.f / getLineThickness());
			mBoxGLProgram.getUniform("linelength")
										.setFloat(1.f / getLineLength());
			mBoxGLProgram.getUniform("lineperiod")
										.setFloat(getLinePeriod());
			mBoxGLProgram.getUniform("boxlinesalpha")
										.setFloat(getBoxLinesAlpha());
			mBoxGLProgram.getUniform("color").setFloatVector4(mColor);

			mBoxGLProgram.getUniform("linepos").setFloatVector2(y, z);
			mPlaneX.draw();

			mBoxGLProgram.getUniform("linepos").setFloatVector2(x, z);
			mPlaneY.draw();

			mBoxGLProgram.getUniform("linepos").setFloatVector2(x, y);
			mPlaneZ.draw();

			mHasChanged = false;

			final float[] lProject = project(	new float[]
																				{ 2 * x - 1,
																					2 * y - 1,
																					2 * z - 1,
																					1 },
																				pModelViewMatrix,
																				pProjectionMatrix);
			px = pWidth * (0.5f * lProject[0] + 0.5f);
			py = pHeight * (1 - (0.5f * lProject[1] + 0.5f));

			// System.out.format("px=%g py=%g \n", px, py);
		}
	}

	@Override
	public boolean notifyEyeRay(ClearGLVolumeRenderer pRenderer,
															MouseEvent pMouseEvent,
															EyeRay pEyeRay)
	{
		if (!mMovable)
			return false;

		// System.out.println(pMouseEvent);

		final boolean lCorrectModifier = true; // pMouseEvent.isShiftDown();
		final boolean lCorrectMouseButton = pMouseEvent.getButton() == 1;
		final boolean lCorrectMouseEvent = (pMouseEvent.getEventType() == MouseEvent.EVENT_MOUSE_CLICKED || pMouseEvent.getEventType() == MouseEvent.EVENT_MOUSE_DRAGGED);

		if (!(lCorrectMouseButton && lCorrectMouseEvent && lCorrectModifier))
			return false;

		final float[] lX = new float[]
		{ x, y, z };

		final float[] lO2X = GLMatrix.clone(lX);
		GLMatrix.sub(lO2X, pEyeRay.org);

		final float lProjectionLength = GLMatrix.dot(lO2X, pEyeRay.dir);
		final float[] lClosestPoint = GLMatrix.clone(pEyeRay.dir);
		GLMatrix.mult(lClosestPoint, lProjectionLength);
		GLMatrix.add(lClosestPoint, pEyeRay.org);

		final float mx = pMouseEvent.getX();
		final float my = pMouseEvent.getY();

		// System.out.format("MX=%g MY=%g \n", mx, my);

		final double lDistance = sqrt((mx - px) * (mx - px)
																	+ (my - py)
																	* (my - py));

		// final float[] lCP2X = GLMatrix.clone(lX);
		// GLMatrix.distance(lCP2X, lClosestPoint);
		// final float lDistanceToClosestPoint = GLMatrix.norm(lCP2X);

		/*System.out.println(pEyeRay);
		System.out.println(Arrays.toString(lClosestPoint));
		System.out.println("lDistanceToClosestPoint=" + lDistanceToClosestPoint);
		/**/

		if (lDistance < getDistanceThreshold())
		{
			x = clamp(lClosestPoint[0]);
			y = clamp(lClosestPoint[1]);
			z = clamp(lClosestPoint[2]);
			mHasChanged = true;
			return true;
		}

		return false;

	}

	public static final float[] project(float[] pVector,
																			GLMatrix pModelViewMatrix,
																			GLMatrix pProjectionMatrix)
	{
		float[] lResult = pModelViewMatrix.mult(pVector);
		lResult = pProjectionMatrix.mult(lResult);
		GLMatrix.mult(lResult, 1.0f / lResult[3]);
		return lResult;
	}

	private float clamp(float pValue)
	{
		return min(max(pValue, 0), 1);
	}

	public boolean isMovable()
	{
		return mMovable;
	}

	public void setMovable(boolean pMovable)
	{
		mMovable = pMovable;
	}

	public void setPosition(float pX, float pY, float pZ)
	{
		x = pX;
		y = pY;
		z = pZ;
	}

	public double getX()
	{
		return x;
	}

	public void setX(double pX)
	{
		x = (float) pX;
	}

	public double getY()
	{
		return y;
	}

	public void setY(double pY)
	{
		y = (float) pY;
	}

	public double getZ()
	{
		return z;
	}

	public void setZ(double pZ)
	{
		z = (float) pZ;
	}

	public float getDistanceThreshold()
	{
		return mDistanceThreshold;
	}

	public void setDistanceThreshold(float pDistanceThreshold)
	{
		mDistanceThreshold = pDistanceThreshold;
	}

	public float[] getColor()
	{
		return mColor;
	}

	public void setColor(float... pColor)
	{
		mColor = pColor;
	}

	public float getBoxLinesAlpha()
	{
		return mBoxLinesAlpha;
	}

	public void setBoxLinesAlpha(float pBoxLines)
	{
		mBoxLinesAlpha = pBoxLines;
	}

	public float getLineLength()
	{
		return mLineLength;
	}

	public void setLineLength(float pLineLength)
	{
		mLineLength = pLineLength;
	}

	public float getLineThickness()
	{
		return mLineThickness;
	}

	public void setLineThickness(float pLineThickness)
	{
		mLineThickness = pLineThickness;
	}

	public float getLinePeriod()
	{
		return mLinePeriod;
	}

	public void setLinePeriod(float pLinePeriod)
	{
		mLinePeriod = pLinePeriod;
	}

	public float getAlpha()
	{
		return mAlpha;
	}

	public void setAlpha(float pAlpha)
	{
		mAlpha = pAlpha;
	}

}
