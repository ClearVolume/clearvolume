package clearvolume.renderer.jogl.overlay.o2d;

import static java.lang.Math.max;
import static java.lang.Math.min;
import gnu.trove.list.linked.TFloatLinkedList;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL4;

import cleargl.ClearGeometryObject;
import cleargl.GLError;
import cleargl.GLFloatArray;
import cleargl.GLIntArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import clearvolume.audio.audioplot.AudioPlot;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.Overlay2D;
import clearvolume.renderer.jogl.overlay.OverlayBase;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;

public class GraphOverlay extends OverlayBase	implements
																							Overlay2D,
																							ProcessorResultListener<Double>,
																							AutoCloseable
{

	private static final int cMaximalWaitTimeForDrawingInMilliseconds = 10;
	private static final float cLineWidth = 2.f; // only cLineWidth = 1.f

	// seems to be supported

	private GLProgram mGLProgram;
	private ClearGeometryObject mClearGeometryObject;

	private volatile FloatBuffer mGraphColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 1f });

	private final TFloatLinkedList mDataY = new TFloatLinkedList();

	private final ReentrantLock mReentrantLock = new ReentrantLock();

	private int mMaxNumberOfDataPoints;

	private DisplayRequestInterface mDisplayRequestInterface;
	private volatile boolean mHasChanged = false;

	private GLFloatArray mVerticesFloatArray;
	private GLIntArray mIndexIntArray;
	private GLFloatArray mNormalArray;
	private GLFloatArray mTexCoordFloatArray;

	private volatile float mOffsetX = -1, mOffsetY = 2f / 3;
	private volatile float mScaleX = 1, mScaleY = 1f / 3;
	private volatile float mMin = 0;
	private volatile float mMax = 1;
	private final float mAlpha = 0.01f;

	private final AudioPlot mAudioPlot = new AudioPlot();

	public GraphOverlay(int pMaxNumberOfDataPoints)
	{
		super();
		setMaxNumberOfDataPoints(pMaxNumberOfDataPoints);

		/*for (int i = 0; i < mMaxNumberOfDataPoints; i++)
			addPoint(0);/**/

		mAudioPlot.setInvertRange(true);
	}

	@Override
	public String getName()
	{
		return "graph";
	}

	@Override
	public boolean hasChanged2D()
	{
		return mHasChanged;
	}

	public void setColor(double pR, double pG, double pB, double pA)
	{
		mGraphColor = FloatBuffer.wrap(new float[]
		{ (float) pR, (float) pG, (float) pB, (float) pA });
	}

	public int getMaxNumberOfDataPoints()
	{
		return mMaxNumberOfDataPoints;
	}

	public void setMaxNumberOfDataPoints(int pMaxNumberOfDataPoints)
	{
		mMaxNumberOfDataPoints = pMaxNumberOfDataPoints;
	}

	@Override
	public void notifyResult(Processor<Double> pSource, Double pResult)
	{
		addPoint(pResult);
	}

	public void addPoint(double pY)
	{
		mAudioPlot.setValue(normalizeAndClamp((float) pY));

		mReentrantLock.lock();
		try
		{
			mDataY.add((float) pY);
			if (mDataY.size() > getMaxNumberOfDataPoints())
				mDataY.removeAt(0);

			mHasChanged = true;
		}
		finally
		{
			mReentrantLock.unlock();
		}

		if (mDisplayRequestInterface != null)
			mDisplayRequestInterface.requestDisplay();
	}

	private void computeMinMax()
	{
		try
		{
			final boolean lIsLocked = mReentrantLock.tryLock(	cMaximalWaitTimeForDrawingInMilliseconds,
																												TimeUnit.MILLISECONDS);

			if (lIsLocked)
			{
				if (mDataY.size() == 0)
					return;

				final float lMin = mDataY.min();
				final float lMax = mDataY.max();

				mMin = mAlpha * lMin + (1 - mAlpha) * mMin;
				mMax = mAlpha * lMax + (1 - mAlpha) * mMax;
			}
		}
		catch (final InterruptedException e)
		{
		}
		finally
		{
			mReentrantLock.unlock();
		}

	}


	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		mAudioPlot.start();

		mDisplayRequestInterface = pDisplayRequestInterface;
		// box display: construct the program and related objects
		mReentrantLock.lock();
		try
		{
			mGLProgram = GLProgram.buildProgram(pGL4,
																					GraphOverlay.class,
																					"shaders/graph_vert.glsl",
																					"shaders/graph_frag.glsl");

			mClearGeometryObject = new ClearGeometryObject(	mGLProgram,
																											3,
																											GL4.GL_TRIANGLE_STRIP);
			mClearGeometryObject.setDynamic(true);

			final int lNumberOfPointsToDraw = 2 * getMaxNumberOfDataPoints();

			mVerticesFloatArray = new GLFloatArray(lNumberOfPointsToDraw, 3);
			mNormalArray = new GLFloatArray(lNumberOfPointsToDraw, 3);
			mIndexIntArray = new GLIntArray(lNumberOfPointsToDraw, 1);
			mTexCoordFloatArray = new GLFloatArray(lNumberOfPointsToDraw, 2);

			mVerticesFloatArray.fillZeros();
			mNormalArray.fillZeros();
			mIndexIntArray.fillZeros();
			mTexCoordFloatArray.fillZeros();

			mClearGeometryObject.setVerticesAndCreateBuffer(mVerticesFloatArray.getFloatBuffer());
			mClearGeometryObject.setNormalsAndCreateBuffer(mNormalArray.getFloatBuffer());
			mClearGeometryObject.setTextureCoordsAndCreateBuffer(mTexCoordFloatArray.getFloatBuffer());
			mClearGeometryObject.setIndicesAndCreateBuffer(mIndexIntArray.getIntBuffer());

			GLError.printGLErrors(pGL4, "AFTER GRAPH OVERLAY INIT");

		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			mReentrantLock.unlock();
		}
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
	public void render2D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{

		if (isDisplayed())
		{
			try
			{
				final boolean lIsLocked = mReentrantLock.tryLock(	cMaximalWaitTimeForDrawingInMilliseconds,
																													TimeUnit.MILLISECONDS);

				if (lIsLocked)
				{

					// System.out.format("________________________________________\n");
					// System.out.println(mDataY.size());

					mIndexIntArray.clear();
					mVerticesFloatArray.clear();
					mTexCoordFloatArray.clear();

					final float lStepX = 1f / mDataY.size();
					int i = 0;
					for (i = 0; i < mDataY.size(); i++)
					{
						if (mMax == mMin)
							mMax = mMin + 0.01f;
						final float lNormalizedValue = normalizeAndClamp(mDataY.get(i));

						final float x = transformX(i * lStepX);
						final float y = transformY(lNormalizedValue);

						mVerticesFloatArray.add(x, transformY(0), -10);
						mTexCoordFloatArray.add(x, 0);
						mIndexIntArray.add(2 * i);
						mVerticesFloatArray.add(x, y, -10);
						mTexCoordFloatArray.add(x, 1);
						mIndexIntArray.add(2 * i + 1);
						// System.out.format("%g\t%g\n", x, y);
						// System.out.println(y);
					}

					/*mVerticesFloatArray.add(transformX(1), transformY(0), -10);
					mIndexIntArray.add(i++);
					mVerticesFloatArray.add(transformX(0), transformY(0), -10);
					mIndexIntArray.add(i++);/**/

					mVerticesFloatArray.padZeros();
					mTexCoordFloatArray.padZeros();
					mIndexIntArray.padZeros();

					/*System.out.println("mVerticesFloatArray.getFloatBuffer().limit()=" + mVerticesFloatArray.getFloatBuffer()
																																																	.limit());
					System.out.println("mTexCoordFloatArray.getFloatBuffer().limit()=" + mTexCoordFloatArray.getFloatBuffer()
																																																	.limit());
					System.out.println("mIndexIntArray.getFloatBuffer().limit()=" + mIndexIntArray.getIntBuffer()
																																												.limit());/**/

					mClearGeometryObject.updateVertices(mVerticesFloatArray.getFloatBuffer());
					GLError.printGLErrors(pGL4,
																"AFTER mClearGeometryObject.updateVertices");
					mClearGeometryObject.updateTextureCoords(mTexCoordFloatArray.getFloatBuffer());
					GLError.printGLErrors(pGL4,
																"AFTER mClearGeometryObject.updateTextureCoords");
					mClearGeometryObject.updateIndices(mIndexIntArray.getIntBuffer());
					GLError.printGLErrors(pGL4,
																"AFTER mClearGeometryObject.updateIndices");

					// mGLProgram.use(pGL4);
					mClearGeometryObject.setProjection(pProjectionMatrix);

					pGL4.glEnable(GL4.GL_BLEND);
					pGL4.glBlendFunc(	GL4.GL_SRC_ALPHA,
														GL4.GL_ONE_MINUS_SRC_ALPHA);
					pGL4.glBlendEquation(GL4.GL_FUNC_ADD);/**/

					mClearGeometryObject.draw(0, mDataY.size() * 2);

					mHasChanged = false;
				}

			}
			catch (final InterruptedException e)
			{
			}
			finally
			{
				mReentrantLock.unlock();
			}
		}
	}

	private float normalizeAndClamp(float pValue)
	{
		float lValue = (pValue - mMin) / (mMax - mMin);
		lValue = min(max(lValue, 0), 1);
		return lValue;
	}

	@Override
	public void close()
	{
		mAudioPlot.stop();
	}

}
