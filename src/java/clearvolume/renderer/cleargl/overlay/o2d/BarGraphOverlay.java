package clearvolume.renderer.cleargl.overlay.o2d;

import static java.lang.Math.log1p;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

import com.jogamp.opengl.GL;

import cleargl.ClearGeometryObject;
import cleargl.ClearTextRenderer;
import cleargl.GLError;
import cleargl.GLFloatArray;
import cleargl.GLIntArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.SingleKeyToggable;
import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.cleargl.overlay.Overlay2D;
import clearvolume.renderer.cleargl.overlay.OverlayBase;
import clearvolume.renderer.panels.HasGUIPanel;
import clearvolume.renderer.processors.ProcessorInterface;
import clearvolume.renderer.processors.ProcessorResultListener;
import clearvolume.utils.ClearVolumeDefaultFont;

public abstract class BarGraphOverlay extends OverlayBase	implements
															Overlay2D,
															SingleKeyToggable,
															ProcessorResultListener<FloatBuffer>,
															AutoCloseable,
															HasGUIPanel
{

	private static final Color cTextColor = new Color(	0.2f,
														0.6f,
														1.0f);

	private GLProgram mGLProgram;

	private ClearTextRenderer mClearTextRenderer;

	private ClearGeometryObject mClearGeometryObjectBars;

	private GLFloatArray mVerticesFloatArray;
	private GLIntArray mIndexIntArray;
	private GLFloatArray mNormalArray;
	private GLFloatArray mTexCoordFloatArray;

	private FloatBuffer mBarHeightData;
	private final ReentrantLock mReentrantLock = new ReentrantLock();

	private volatile boolean mHasChanged = false;

	private volatile float mOffsetX = -1, mOffsetY = 2f / 3;
	private volatile float mScaleX = 1, mScaleY = 1f / 3;
	private volatile float mMin, mMax;

	private volatile float mRangeMin = 0, mRangeMax = 1;

	private volatile boolean mLogarithm = false;

	public BarGraphOverlay()
	{
		super();
		setMinMax(0.f, 1.3f);
	}

	public void setLogarithm(boolean pLogarithm)
	{
		mLogarithm = pLogarithm;
	}

	public void setDisplayedRange(float pRangeMin, float pRangeMax)
	{
		mRangeMin = pRangeMin;
		mRangeMax = pRangeMax;
	}

	public void setMinMax(float pMin, float pMax)
	{
		mMin = pMin;
		mMax = pMax;
	}

	@Override
	public String getName()
	{
		return "graph";
	}

	@Override
	public abstract short toggleKeyCode();

	@Override
	public abstract int toggleKeyModifierMask();

	@Override
	public boolean hasChanged2D()
	{
		return mHasChanged;
	}

	public int getNumberOfBins()
	{
		return mBarHeightData == null ? 0 : mBarHeightData.capacity();
	}

	@Override
	public void notifyResult(	ProcessorInterface<FloatBuffer> pSource,
								FloatBuffer pResult)
	{
		setCounts(pResult);
	}

	public void setCounts(FloatBuffer pCounts)
	{

		mReentrantLock.lock();
		try
		{

			if (mBarHeightData == null || mBarHeightData.capacity() != pCounts.limit())
			{
				mBarHeightData = FloatBuffer.allocate(pCounts.limit());
			}

			float maxVal = 0.f;
			for (int i = 0; i < mBarHeightData.capacity(); i++)
			{

				final float newVal = (float) (mLogarithm ? log1p(pCounts.get(i))
														: pCounts.get(i));
				mBarHeightData.put(i, newVal);
				maxVal = Math.max(maxVal, newVal);
			}

			// setMinMax(0.f, 1.2f * maxVal);

			for (int i = 0; i < mBarHeightData.capacity(); i++)
			{
				mBarHeightData.put(i, mBarHeightData.get(i) / maxVal);
			}

			mHasChanged = true;
		}
		finally
		{
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}

	}

	public void clear()
	{
		mReentrantLock.lock();
		try
		{
			mHasChanged = true;
		}
		finally
		{
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}

	}

	@Override
	public void init(	GL pGL,
						DisplayRequestInterface pDisplayRequestInterface)
	{
		mReentrantLock.lock();
		try
		{
			mGLProgram = GLProgram.buildProgram(pGL,
												BarGraphOverlay.class,
												"shaders/bargraph_vert.glsl",
												"shaders/bargraph_frag.glsl");

			GLError.printGLErrors(pGL, "AFTER BAR GRAPH OVERLAY INIT");

			mClearTextRenderer = new ClearTextRenderer(pGL, true);

		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}
	}

	private void ensureGeometryCreated()
	{
		final int lNumberOfPointsToDraw = 6 * getNumberOfBins();

		final boolean lUpdateNeeded = mVerticesFloatArray == null || mVerticesFloatArray.getNumberOfElements() != lNumberOfPointsToDraw;

		if (!lUpdateNeeded)
			return;

		if (mClearGeometryObjectBars != null)
			mClearGeometryObjectBars.close();

		mClearGeometryObjectBars = new ClearGeometryObject(	mGLProgram,
															3,
															GL.GL_TRIANGLES);
		mClearGeometryObjectBars.setDynamic(true);

		mVerticesFloatArray = new GLFloatArray(	lNumberOfPointsToDraw,
												3);
		mNormalArray = new GLFloatArray(lNumberOfPointsToDraw, 3);
		mIndexIntArray = new GLIntArray(lNumberOfPointsToDraw, 1);
		mTexCoordFloatArray = new GLFloatArray(	lNumberOfPointsToDraw,
												2);

		mVerticesFloatArray.fillZeros();
		mNormalArray.fillZeros();
		mIndexIntArray.fillZeros();
		mTexCoordFloatArray.fillZeros();

		mClearGeometryObjectBars.setVerticesAndCreateBuffer(mVerticesFloatArray.getFloatBuffer());
		mClearGeometryObjectBars.setNormalsAndCreateBuffer(mNormalArray.getFloatBuffer());
		mClearGeometryObjectBars.setTextureCoordsAndCreateBuffer(mTexCoordFloatArray.getFloatBuffer());
		mClearGeometryObjectBars.setIndicesAndCreateBuffer(mIndexIntArray.getIntBuffer());

	}

	private final float transformX(float pX)
	{
		return mOffsetX + mScaleX * pX;
	}

	private final float transformY(float pY)
	{
		return mOffsetY + mScaleY * pY;
	}

	@Override
	public void render2D(	ClearGLVolumeRenderer pClearGLVolumeRenderer,
							GL pGL,
							int pWidth,
							int pHeight,
							GLMatrix pProjectionMatrix)
	{
		if (isDisplayed())
		{
			try
			{

				mReentrantLock.lock();
				{
					if (mBarHeightData == null)
						return;

					ensureGeometryCreated();

					mGLProgram.use(pGL);

					mIndexIntArray.clear();
					mVerticesFloatArray.clear();
					mTexCoordFloatArray.clear();

					final float lStepX = 1.f / mBarHeightData.capacity();
					final float relBarWidth = .4f;

					int i = 0;
					for (i = 0; i < mBarHeightData.capacity(); i++)
					{
						final float lNormalizedValue = normalizeAndClamp(mBarHeightData.get(i));

						final float x = transformX(i * lStepX);
						final float y = transformY(lNormalizedValue);

						mVerticesFloatArray.add(x		- relBarWidth
														* lStepX,
												transformY(0),
												-10);
						mTexCoordFloatArray.add(x, 1.f);
						mIndexIntArray.add(6 * i);

						mVerticesFloatArray.add(x - relBarWidth
												* lStepX, y, -10);
						mTexCoordFloatArray.add(x, 1.f);
						mIndexIntArray.add(6 * i + 1);

						mVerticesFloatArray.add(x		+ relBarWidth
														* lStepX,
												transformY(0),
												-10);
						mTexCoordFloatArray.add(x, 1.f);
						mIndexIntArray.add(6 * i + 2);

						mVerticesFloatArray.add(x		+ relBarWidth
														* lStepX,
												transformY(0),
												-10);
						mTexCoordFloatArray.add(x, 1.f);
						mIndexIntArray.add(6 * i + 3);

						mVerticesFloatArray.add(x - relBarWidth
												* lStepX, y, -10);
						mTexCoordFloatArray.add(x, 1.f);
						mIndexIntArray.add(6 * i + 4);

						mVerticesFloatArray.add(x + relBarWidth
												* lStepX, y, -10);
						mTexCoordFloatArray.add(x, 1.f);
						mIndexIntArray.add(6 * i + 5);
					}

					mVerticesFloatArray.padZeros();
					mTexCoordFloatArray.padZeros();
					mIndexIntArray.padZeros();

					mClearGeometryObjectBars.updateVertices(mVerticesFloatArray.getFloatBuffer());
					GLError.printGLErrors(	pGL,
											"AFTER mClearGeometryObject.updateVertices");
					mClearGeometryObjectBars.updateTextureCoords(mTexCoordFloatArray.getFloatBuffer());
					GLError.printGLErrors(	pGL,
											"AFTER mClearGeometryObject.updateTextureCoords");
					mClearGeometryObjectBars.updateIndices(mIndexIntArray.getIntBuffer());
					GLError.printGLErrors(	pGL,
											"AFTER mClearGeometryObject.updateIndices");

					mClearGeometryObjectBars.setProjection(pProjectionMatrix);

					pGL.glDisable(GL.GL_DEPTH_TEST);
					pGL.glEnable(GL.GL_BLEND);
					pGL.glBlendFunc(GL.GL_SRC_ALPHA,
									GL.GL_ONE_MINUS_SRC_ALPHA);
					pGL.glBlendEquation(GL.GL_FUNC_ADD);/**/

					mClearGeometryObjectBars.draw(	0,
													mBarHeightData.capacity() * 6);

					final Font lFont = ClearVolumeDefaultFont.getFontPlain(12);

					mClearTextRenderer.drawTextAtPosition(	String.format(	"%.4f",
																			mRangeMin),
															10,
															(int) ((5.0 / 6) * pHeight) - 12,
															lFont,
															cTextColor,
															false);

					mClearTextRenderer.drawTextAtPosition(	String.format(	"%.4f",
																			mRangeMax),
															(pWidth / 2) - 5 * 12,
															(int) ((5.0 / 6) * pHeight) - 12,
															lFont,
															cTextColor,
															false);

					mHasChanged = false;
				}

			}
			finally
			{
				if (mReentrantLock.isHeldByCurrentThread())
					mReentrantLock.unlock();
			}
		}
	}

	private float normalizeAndClamp(float pValue)
	{
		if (mMax == mMin)
			return 0;
		float lValue = (pValue - mMin) / (mMax - mMin);
		lValue = min(max(lValue, 0), 1);
		return lValue;
	}

	@Override
	public void close()
	{
		if (mGLProgram != null)
			mGLProgram.close();

		if (mClearGeometryObjectBars != null)
			mClearGeometryObjectBars.close();

	}

}
