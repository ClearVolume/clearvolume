package clearvolume.renderer.jogl.overlay.o2d;

import gnu.trove.list.linked.TDoubleLinkedList;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL4;

import cleargl.GLAttribute;
import cleargl.GLFloatArray;
import cleargl.GLMatrix;
import cleargl.GLProgram;
import cleargl.GLUniform;
import cleargl.GLVertexArray;
import cleargl.GLVertexAttributeArray;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.OverlayBase;

public class GraphOverlay extends OverlayBase
{

	private static final int cMaximalWaitTimeForDrawingInMilliseconds = 10;
	private static final float cLineWidth = 2.f; // only cLineWidth = 1.f
	// seems to be supported

	private GLProgram mGLProgram;

	private GLAttribute mPositionAttribute;
	private GLVertexArray mVertexArray;
	private GLVertexAttributeArray mPositionAttributeArray;
	private GLUniform mColorUniform;

	private GLUniform mOverlayProjectionMatrixUniform;

	private volatile FloatBuffer mGraphColor = FloatBuffer.wrap(new float[]
	{ 1.f, 1.f, 1.f, 1f });

	private TDoubleLinkedList mDataY = new TDoubleLinkedList();

	private final ReentrantLock mReentrantLock = new ReentrantLock();

	private volatile int mMaxCapacity = 512;

	private int mMaxNumberOfPoints;
	private GLFloatArray mVerticesFloatArray;

	private DisplayRequestInterface mDisplayRequestInterface;
	private volatile boolean mHasChanged = false;

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

	@Override
	public boolean hasChanged3D()
	{
		return mHasChanged;
	}

	public void setColor(double pR, double pG, double pB, double pA)
	{
		mGraphColor = FloatBuffer.wrap(new float[]
		{ (float) pR, (float) pG, (float) pB, (float) pA });
	}

	public int getMaxCapacity()
	{
		return mMaxCapacity;
	}

	public void setMaxCapacity(int pMaxCapacity)
	{
		mMaxCapacity = pMaxCapacity;
	}

	public void addPoint(double pY)
	{
		mReentrantLock.lock();
		try
		{
			mDataY.add(pY);
			if (mDataY.size() > getMaxCapacity())
				mDataY.removeAt(0);
			mHasChanged = true;
		}
		finally
		{
			mReentrantLock.unlock();
		}
		mDisplayRequestInterface.requestDisplay();
	}

	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		mDisplayRequestInterface = pDisplayRequestInterface;
		// box display: construct the program and related objects
		try
		{
			mGLProgram = GLProgram.buildProgram(pGL4,
																					GraphOverlay.class,
																					"shaders/graph_vert.glsl",
																					"shaders/graph_frag.glsl");

			mOverlayProjectionMatrixUniform = mGLProgram.getUniform("projection");

			// set the line with of the box
			pGL4.glLineWidth(cLineWidth);

			// get all the shaders uniform locations
			mPositionAttribute = mGLProgram.getAtribute("position");

			mColorUniform = mGLProgram.getUniform("color");

			// set up the vertices of the box
			mVertexArray = new GLVertexArray(mGLProgram);
			mVertexArray.bind();
			mPositionAttributeArray = new GLVertexAttributeArray(	mPositionAttribute,
																														4);

			mMaxNumberOfPoints = 1024;

			mVerticesFloatArray = new GLFloatArray(mMaxNumberOfPoints, 4);

		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void render2D(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			mGLProgram.use(pGL4);

			mOverlayProjectionMatrixUniform.setFloatMatrix(	pProjectionMatrix.getFloatArray(),
																											false);

			mColorUniform.setFloatVector4(mGraphColor);

			float lXOffset = -4, lYOffset = 3;

			mVerticesFloatArray.rewind();

			try
			{
				boolean lIsLocked = mReentrantLock.tryLock(	cMaximalWaitTimeForDrawingInMilliseconds,
																										TimeUnit.MILLISECONDS);

				float x = lXOffset, y = 0;
				float lStepX = 4.0f / mDataY.size();
				// System.out.format("________________________________________\n");
				if (lIsLocked)
					for (int i = 0; i < mDataY.size(); i++)
					{
						mVerticesFloatArray.add(x, y, -10, 1.0f);
						// System.out.format("%g\t%g\n", x, y);
						x += lStepX;
						y = (float) (lYOffset + mDataY.get(i));
					}

			}
			catch (InterruptedException e)
			{
			}
			finally
			{
				mReentrantLock.unlock();
			}

			mVertexArray.addVertexAttributeArray(	mPositionAttributeArray,
																						mVerticesFloatArray.getFloatBuffer());

			mVertexArray.draw(GL4.GL_LINE_STRIP);
			mHasChanged = false;
		}
	}

	@Override
	public void render3D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			// TODO: do something!

		}
	}

}
