package clearvolume.renderer.cleargl.overlay.o2d;

import static java.lang.Math.max;
import static java.lang.Math.min;
import gnu.trove.list.linked.TFloatLinkedList;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import cleargl.ClearGeometryObject;
import cleargl.GLError;
import cleargl.GLFloatArray;
import cleargl.GLIntArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.cleargl.overlay.Overlay2D;
import clearvolume.renderer.cleargl.overlay.OverlayBase;
import clearvolume.renderer.cleargl.overlay.SingleKeyToggable;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;

import com.jogamp.newt.event.KeyEvent;

public class HistogramOverlay extends OverlayBase implements Overlay2D,
		SingleKeyToggable, ProcessorResultListener<FloatBuffer>, AutoCloseable {

	private static final int cMaximalWaitTimeForLockInMilliseconds = 10;

	// seems to be supported

	private GLProgram mGLProgram;
	private ClearGeometryObject mClearGeometryObject;

	private GLProgram mGLProgramLines;
	private ClearGeometryObject mClearGeometryObjectLines;

	private GLFloatArray mVerticesFloatArray;
	private GLIntArray mIndexIntArray;
	private GLFloatArray mNormalArray;
	private GLFloatArray mTexCoordFloatArray;

	private final TFloatLinkedList mDataY = new TFloatLinkedList();

	private FloatBuffer mData;
	private final ReentrantLock mReentrantLock = new ReentrantLock();

	private int mNumberOfBins;

	private DisplayRequestInterface mDisplayRequestInterface;
	private volatile boolean mHasChanged = false;

	private volatile float mOffsetX = -1, mOffsetY = 2f / 3;
	private volatile float mScaleX = 1, mScaleY = 1f / 3;
	private volatile float mMin;
	private volatile float mMax;
	private final float mAlpha = 0.04f;

	protected volatile boolean mStopSignal = false;

	public HistogramOverlay(int pNumberOfBins) {
		super();
		mData = FloatBuffer.allocate(pNumberOfBins);

		setNumberOfBins(pNumberOfBins);

		setMinMax(0.f, 1.f);
		final Runnable lRunnable = new Runnable() {

			@Override
			public void run() {
				while (!mStopSignal) {
					if (mDisplayRequestInterface != null)
						mDisplayRequestInterface.requestDisplay();
					try {
						Thread.sleep(50);
					} catch (final InterruptedException e) {
					}

				}

			}
		};

		final Thread lMinMaxCalculationThread = new Thread(lRunnable,
				HistogramOverlay.class.getSimpleName()
						+ ".MinMaxCalculationThread");
		lMinMaxCalculationThread.setDaemon(true);
		lMinMaxCalculationThread.setPriority(Thread.MIN_PRIORITY);
		lMinMaxCalculationThread.start();

	}

	private void setMinMax(float pMin, float pMax) {
		mMin = pMin;
		mMax = pMax;

	}

	@Override
	public boolean toggleDisplay() {
		final boolean lNewState = super.toggleDisplay();
		return lNewState;
	}

	@Override
	public String getName() {
		return "graph";
	}

	@Override
	public short toggleKeyCode() {
		return KeyEvent.VK_G;
	}

	@Override
	public int toggleKeyModifierMask() {
		return 0;
	}

	@Override
	public boolean hasChanged2D() {
		return mHasChanged;
	}

	public int getNumberOfBins() {
		return mNumberOfBins;
	}

	public void setNumberOfBins(int pNumberOfBins) {
		mNumberOfBins = pNumberOfBins;
		mData = FloatBuffer.allocate(mNumberOfBins);

	}

	@Override
	public void notifyResult(Processor<FloatBuffer> pSource, FloatBuffer pResult) {

		setCounts(pResult);
	}

	public void setCounts(FloatBuffer pCounts) {

		mReentrantLock.lock();
		try {
			for (int i = 0; i < mData.capacity(); i++) {
				mData.put(i, pCounts.get(i));
			}

			mHasChanged = true;
		} finally {
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}

	}

	public void clear() {
		mReentrantLock.lock();
		try {
			mHasChanged = true;
		} finally {
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}

	}

	@Override
	public void init(GL pGL, DisplayRequestInterface pDisplayRequestInterface) {

		mDisplayRequestInterface = pDisplayRequestInterface;
		// box display: construct the program and related objects
		mReentrantLock.lock();
		try {
			mGLProgram = GLProgram.buildProgram(pGL, HistogramOverlay.class,
					"shaders/fancygraph_vert.glsl",
					"shaders/fancygraph_frag.glsl");

			mClearGeometryObject = new ClearGeometryObject(mGLProgram, 3,
					GL.GL_TRIANGLE_STRIP);
			mClearGeometryObject.setDynamic(true);

			final int lNumberOfPointsToDraw = 2 * getNumberOfBins();

			mVerticesFloatArray = new GLFloatArray(lNumberOfPointsToDraw, 3);
			mNormalArray = new GLFloatArray(lNumberOfPointsToDraw, 3);
			mIndexIntArray = new GLIntArray(lNumberOfPointsToDraw, 1);
			mTexCoordFloatArray = new GLFloatArray(lNumberOfPointsToDraw, 2);

			mVerticesFloatArray.fillZeros();
			mNormalArray.fillZeros();
			mIndexIntArray.fillZeros();
			mTexCoordFloatArray.fillZeros();

			mClearGeometryObject.setVerticesAndCreateBuffer(mVerticesFloatArray
					.getFloatBuffer());
			mClearGeometryObject.setNormalsAndCreateBuffer(mNormalArray
					.getFloatBuffer());
			mClearGeometryObject
					.setTextureCoordsAndCreateBuffer(mTexCoordFloatArray
							.getFloatBuffer());
			mClearGeometryObject.setIndicesAndCreateBuffer(mIndexIntArray
					.getIntBuffer());

			GLError.printGLErrors(pGL, "AFTER GRAPH OVERLAY INIT");

			mGLProgramLines = GLProgram.buildProgram(pGL,
					HistogramOverlay.class, new String[] {
							"shaders/fancylines_vert.glsl",
							"shaders/fancylines_geom.glsl",
							"shaders/fancylines_frag.glsl" });

			mClearGeometryObjectLines = new ClearGeometryObject(
					mGLProgramLines, 3, GL.GL_LINE_STRIP);
			mClearGeometryObjectLines.setDynamic(true);

			mClearGeometryObjectLines
					.setVerticesAndCreateBuffer(mVerticesFloatArray
							.getFloatBuffer());
			mClearGeometryObjectLines.setNormalsAndCreateBuffer(mNormalArray
					.getFloatBuffer());
			mClearGeometryObjectLines
					.setTextureCoordsAndCreateBuffer(mTexCoordFloatArray
							.getFloatBuffer());
			mClearGeometryObjectLines.setIndicesAndCreateBuffer(mIndexIntArray
					.getIntBuffer());

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (mReentrantLock.isHeldByCurrentThread())
				mReentrantLock.unlock();
		}
	}

	private final float transformX(float pX) {
		return mOffsetX + mScaleX * pX;
	}

	private final float transformY(float pY) {
		return mOffsetY + mScaleY * pY;
	}

	// @Override
	// public void render2D(GL pGL, int pWidth, int pHeight,
	// GLMatrix pProjectionMatrix) {
	// if (isDisplayed()) {
	// try {
	// mReentrantLock.lock();
	// {
	//
	// // the graph
	//
	// mGLProgram.use(pGL);
	// mIndexIntArray.clear();
	// mVerticesFloatArray.clear();
	// mTexCoordFloatArray.clear();
	//
	// final float lStepX = 1f / mDataY.size();
	// int i = 0;
	// for (i = 0; i < mDataY.size(); i++) {
	// final float lNormalizedValue = normalizeAndClamp(mDataY
	// .get(i));
	//
	// final float x = transformX(i * lStepX);
	// final float y = transformY(lNormalizedValue);
	//
	// mVerticesFloatArray.add(x, transformY(0), -10);
	// mTexCoordFloatArray.add(x, 0);
	// mIndexIntArray.add(2 * i);
	// mVerticesFloatArray.add(x, y, -10);
	// mTexCoordFloatArray.add(x, lNormalizedValue);
	//
	// // System.out.println("normed" + lNormalizedValue);
	// mIndexIntArray.add(2 * i + 1);
	// }
	//
	// mVerticesFloatArray.padZeros();
	// mTexCoordFloatArray.padZeros();
	// mIndexIntArray.padZeros();
	//
	// mClearGeometryObject.updateVertices(mVerticesFloatArray
	// .getFloatBuffer());
	// GLError.printGLErrors(pGL,
	// "AFTER mClearGeometryObject.updateVertices");
	// mClearGeometryObject
	// .updateTextureCoords(mTexCoordFloatArray
	// .getFloatBuffer());
	// GLError.printGLErrors(pGL,
	// "AFTER mClearGeometryObject.updateTextureCoords");
	// mClearGeometryObject.updateIndices(mIndexIntArray
	// .getIntBuffer());
	// GLError.printGLErrors(pGL,
	// "AFTER mClearGeometryObject.updateIndices");
	//
	// mClearGeometryObject.setProjection(pProjectionMatrix);
	//
	// pGL.glDisable(GL.GL_DEPTH_TEST);
	// pGL.glEnable(GL.GL_BLEND);
	// pGL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
	// pGL.glBlendEquation(GL.GL_FUNC_ADD);/**/
	//
	// mClearGeometryObject.draw(0, mDataY.size() * 2);
	//
	// // the lines
	// mGLProgramLines.use(pGL);
	// mIndexIntArray.clear();
	// mVerticesFloatArray.clear();
	// mTexCoordFloatArray.clear();
	//
	// for (i = 0; i < mDataY.size(); i++) {
	// final float lNormalizedValue = normalizeAndClamp(mDataY
	// .get(i));
	//
	// final float x = transformX(i * lStepX);
	// final float y = transformY(lNormalizedValue);
	//
	// mVerticesFloatArray.add(x, y, -10);
	// mTexCoordFloatArray.add(x, 0);
	// mIndexIntArray.add(i);
	//
	// }
	//
	// mVerticesFloatArray.padZeros();
	// mTexCoordFloatArray.padZeros();
	// mIndexIntArray.padZeros();
	//
	// mClearGeometryObjectLines
	// .updateVertices(mVerticesFloatArray
	// .getFloatBuffer());
	// // GLError.printGLErrors(pGL,
	// // "AFTER mClearGeometryObject.updateVertices");
	// mClearGeometryObjectLines
	// .updateTextureCoords(mTexCoordFloatArray
	// .getFloatBuffer());
	// GLError.printGLErrors(pGL,
	// "AFTER mClearGeometryObject.updateTextureCoords");
	// mClearGeometryObjectLines.updateIndices(mIndexIntArray
	// .getIntBuffer());
	// GLError.printGLErrors(pGL,
	// "AFTER mClearGeometryObject.updateIndices");
	//
	// mClearGeometryObjectLines.setProjection(pProjectionMatrix);
	//
	// pGL.glDisable(GL.GL_DEPTH_TEST);
	// pGL.glEnable(GL.GL_BLEND);
	// pGL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
	// pGL.glBlendEquation(GL2.GL_MAX);/**/
	// //
	// mClearGeometryObjectLines.draw(0, mDataY.size());
	//
	// mHasChanged = false;
	// }
	//
	// } finally {
	// if (mReentrantLock.isHeldByCurrentThread())
	// mReentrantLock.unlock();
	// }
	// }
	// }

	@Override
	public void render2D(GL pGL, int pWidth, int pHeight,
			GLMatrix pProjectionMatrix) {
		if (isDisplayed()) {
			try {
				mReentrantLock.lock();
				{

					// the graph

					mGLProgram.use(pGL);
					mIndexIntArray.clear();
					mVerticesFloatArray.clear();
					mTexCoordFloatArray.clear();

					final float lStepX = 1f / mData.capacity();
					int i = 0;
					for (i = 0; i < mData.capacity(); i++) {
						final float lNormalizedValue = normalizeAndClamp(mData
								.get(i));

						final float x = transformX(i * lStepX);
						final float y = transformY(lNormalizedValue);

						mVerticesFloatArray.add(x, transformY(0), -10);
						mTexCoordFloatArray.add(x, 0);
						mIndexIntArray.add(2 * i);
						mVerticesFloatArray.add(x, y, -10);
						mTexCoordFloatArray.add(x, lNormalizedValue);

						// System.out.println("normed" + lNormalizedValue);
						mIndexIntArray.add(2 * i + 1);
					}

					mVerticesFloatArray.padZeros();
					mTexCoordFloatArray.padZeros();
					mIndexIntArray.padZeros();

					mClearGeometryObject.updateVertices(mVerticesFloatArray
							.getFloatBuffer());
					GLError.printGLErrors(pGL,
							"AFTER mClearGeometryObject.updateVertices");
					mClearGeometryObject
							.updateTextureCoords(mTexCoordFloatArray
									.getFloatBuffer());
					GLError.printGLErrors(pGL,
							"AFTER mClearGeometryObject.updateTextureCoords");
					mClearGeometryObject.updateIndices(mIndexIntArray
							.getIntBuffer());
					GLError.printGLErrors(pGL,
							"AFTER mClearGeometryObject.updateIndices");

					mClearGeometryObject.setProjection(pProjectionMatrix);

					pGL.glDisable(GL.GL_DEPTH_TEST);
					pGL.glEnable(GL.GL_BLEND);
					pGL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
					pGL.glBlendEquation(GL.GL_FUNC_ADD);/**/

					mClearGeometryObject.draw(0, mDataY.size() * 2);

					// the lines
					mGLProgramLines.use(pGL);
					mIndexIntArray.clear();
					mVerticesFloatArray.clear();
					mTexCoordFloatArray.clear();

					for (i = 0; i < mData.capacity(); i++) {
						final float lNormalizedValue = normalizeAndClamp(mData
								.get(i));

						final float x = transformX(i * lStepX);
						final float y = transformY(lNormalizedValue);

						mVerticesFloatArray.add(x, y, -10);
						mTexCoordFloatArray.add(x, 0);
						mIndexIntArray.add(i);

					}

					mVerticesFloatArray.padZeros();
					mTexCoordFloatArray.padZeros();
					mIndexIntArray.padZeros();

					mClearGeometryObjectLines
							.updateVertices(mVerticesFloatArray
									.getFloatBuffer());
					// GLError.printGLErrors(pGL,
					// "AFTER mClearGeometryObject.updateVertices");
					mClearGeometryObjectLines
							.updateTextureCoords(mTexCoordFloatArray
									.getFloatBuffer());
					GLError.printGLErrors(pGL,
							"AFTER mClearGeometryObject.updateTextureCoords");
					mClearGeometryObjectLines.updateIndices(mIndexIntArray
							.getIntBuffer());
					GLError.printGLErrors(pGL,
							"AFTER mClearGeometryObject.updateIndices");

					mClearGeometryObjectLines.setProjection(pProjectionMatrix);

					pGL.glDisable(GL.GL_DEPTH_TEST);
					pGL.glEnable(GL.GL_BLEND);
					pGL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
					pGL.glBlendEquation(GL2.GL_MAX);/**/
					//
					mClearGeometryObjectLines.draw(0, mData.capacity());

					mHasChanged = false;
				}

			} finally {
				if (mReentrantLock.isHeldByCurrentThread())
					mReentrantLock.unlock();
			}
		}
	}

	private float normalizeAndClamp(float pValue) {
		if (mMax == mMin)
			return 0;
		float lValue = (pValue - mMin) / (mMax - mMin);
		lValue = min(max(lValue, 0), 1);
		return lValue;
	}

	@Override
	public void close() {

		mStopSignal = true;
	}

}
