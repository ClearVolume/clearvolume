package clearvolume.demo.fauxscope;

import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;
import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import static java.lang.Math.*;

/**
 * Created by ulrik on 12/02/15.
 */
public class Fauxscope implements ProcessorResultListener<float[]>
{
	private final double mDriftAmplitude = 0.5;

	protected final SCIFIO mSCIFIO;
	protected ArrayList<Reader> readers = new ArrayList<>();
	protected volatile int mCurrentReaderIndex = 0;

	protected FauxscopeRandomizer mFauxscopeRandomizer;

	protected int mResolutionX;
	protected int mResolutionY;
	protected int mResolutionZ;
	protected int mTotalNumberOfTimePoints;

	private volatile float mStageX;
	private volatile float mStageY;
	private volatile float mStageZ;

	private volatile float mDriftX;
	private volatile float mDriftY;
	private volatile float mDriftZ;

	private volatile float mCenterLockX;
	private volatile float mCenterLockY;
	private volatile float mCenterLockZ;

	protected ByteBuffer mVolumeDataArray;
	protected ByteBuffer mTranslatedVolumeDataArray;

	private volatile boolean mFirstCall = true;

	private volatile boolean mCorrectionActive = true;

	public Fauxscope()
	{
		this(null);
	}

	public Fauxscope(FauxscopeRandomizer pFauxscopeRandomizer)
	{
		mFauxscopeRandomizer = pFauxscopeRandomizer;
		mSCIFIO = new SCIFIO();
	}

	public float[] getResolution()
	{
		return new float[]
		{ mResolutionX,
			mResolutionY,
			mResolutionZ,
			mTotalNumberOfTimePoints };
	}

	public void moveStage(float dX, float dY, float dZ)
	{
		System.out.format("Moving stage by dx=%g, dy=%g, dz=%g \n",
											dX,
											dY,
											dZ);
		mStageX += dX;
		mStageY += dY;
		mStageZ += dZ;
	}

	private void setCenterLock(	float pCenterX,
															float pCenterY,
															float pCenterZ)
	{
		System.out.format("Set center lock to x=%g, y=%g, z=%g \n",
											pCenterX,
											pCenterY,
											pCenterZ);
		mCenterLockX = pCenterX;
		mCenterLockY = pCenterY;
		mCenterLockZ = pCenterZ;
	}

	public boolean isCorrectionActive()
	{
		return mCorrectionActive;
	}

	public void setCorrectionActive(boolean pCorrectionActive)
	{
		mCorrectionActive = pCorrectionActive;
	}

	public void use4DStacksFromDirectory(String pImagesDirectory)
	{
    File pImageDir;

    if(pImagesDirectory != null) {
      pImageDir = new File(pImagesDirectory);
      if(!pImageDir.exists() || !pImageDir.isDirectory()) {
        pImagesDirectory = null;
      }
    }

    if (pImagesDirectory == null)
		{
			final JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			final int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				System.out.println("You chose to open this file: " + chooser.getSelectedFile()
																																		.getName());
			}
			else
			{
				return;
			}
			pImagesDirectory = chooser.getSelectedFile().getAbsolutePath();
		}

		final File folder = new File(pImagesDirectory);
		final File[] filesInFolder = folder.listFiles();
		final ArrayList<String> tiffFiles = new ArrayList<>();

		for (final File f : filesInFolder)
		{
			if (f.exists() && f.isFile()
					&& f.canRead()
					&& (f.getName().endsWith(".tiff") || f.getName()
																								.endsWith(".tif")))
			{
				System.out.println("Found TIFF: " + f.getName());
				tiffFiles.add(f.getAbsolutePath());
			}
		}

		mTotalNumberOfTimePoints = tiffFiles.size();

		try
		{
			for (final String tiff : tiffFiles)
			{

				final Reader reader = mSCIFIO.initializer()
																			.initializeReader(tiff);

				mResolutionX = (int) reader.openPlane(0, 0).getLengths()[0];
				mResolutionY = (int) reader.openPlane(0, 0).getLengths()[1];
				mResolutionZ = (int) reader.getPlaneCount(0);

				System.out.format("Added reader for " + tiff.substring(tiff.lastIndexOf("/"))
															+ ", %d/%d/%d\n",
													mResolutionX,
													mResolutionY,
													mResolutionZ);

				readers.add(reader);
			}
		}
		catch (io.scif.FormatException | IOException e)
		{
			e.printStackTrace();
		}

	}

	private int queryBytesPerPixel() throws FormatException,
																	IOException
	{
		if (readers.size() > 0)
		{
			final int bpp = readers.get(0)
															.openPlane(0, 1)
															.getImageMetadata()
															.getBitsPerPixel() / 8;
			return bpp;
		}
		else
		{
			throw new IOException("No readers registered. Did you click cancel in the file selection dialog?");
		}
	}

	public ByteBuffer queryNextVolume()	throws IOException,
																			FormatException
	{
		applyDrift();

		if (mCurrentReaderIndex + 1 > readers.size())
		{
			mCurrentReaderIndex = 0;
		}

		final Reader thisReader = readers.get(mCurrentReaderIndex);
		final Plane firstPlane = thisReader.openPlane(0, 1);
		final int bytesPerPixel = firstPlane.getImageMetadata()
																				.getBitsPerPixel() / 8;
		final boolean isLittleEndian = firstPlane.getImageMetadata()
																							.isLittleEndian();

		System.out.println("Loading volume from " + thisReader.getCurrentFile()
												+ " ("
												+ bytesPerPixel
												* mResolutionX
												* mResolutionY
												* mResolutionZ
												/ 1024
												/ 1024
												+ "M)");

		System.out.format("W=%d, H=%d, D=%d \n",
											mResolutionX,
											mResolutionY,
											mResolutionZ);

		final int lBufferLengthNeeded = bytesPerPixel * mResolutionX
																		* mResolutionY
																		* mResolutionZ;

		if (mVolumeDataArray == null || mVolumeDataArray.capacity() != lBufferLengthNeeded)
		{
			System.out.println("Allocating new mVolumeDataArray buffer");
			mVolumeDataArray = ByteBuffer.allocate(lBufferLengthNeeded);
			mVolumeDataArray.order(isLittleEndian	? ByteOrder.LITTLE_ENDIAN
																						: ByteOrder.BIG_ENDIAN);

		}

		if (mTranslatedVolumeDataArray == null || mTranslatedVolumeDataArray.capacity() != lBufferLengthNeeded)
		{
			System.out.println("Allocating new mTranslatedVolumeDataArray buffer");
			mTranslatedVolumeDataArray = ByteBuffer.allocate(lBufferLengthNeeded);
			mTranslatedVolumeDataArray.order(isLittleEndian	? ByteOrder.LITTLE_ENDIAN
																											: ByteOrder.BIG_ENDIAN);
		}

		mVolumeDataArray.rewind();
		for (int i = 0; i < mResolutionZ; i++)
		{
			final Plane lPlane = thisReader.openPlane(0, i);
			mVolumeDataArray.put(lPlane.getBytes());
		}

		mVolumeDataArray.rewind();
		mTranslatedVolumeDataArray.rewind();
		for (int z = 0; z < mResolutionZ; z++)
			for (int y = 0; y < mResolutionY; y++)
				for (int x = 0; x < mResolutionX; x++)
				{
					final int lOrgIndex = x + mResolutionX
																* y
																+ mResolutionX
																* mResolutionY
																* z;

					final byte lValue = mVolumeDataArray.get(lOrgIndex);

					// System.out.format("index=%d value=%d \n", lOrgIndex, lValue);

					final int lTranslatedX = clampAndRound(	(x + mDriftX + mStageX),
																									0,
																									mResolutionX - 1);
					final int lTranslatedY = clampAndRound(	(y + mDriftY + mStageY),
																									0,
																									mResolutionY - 1);
					final int lTranslatedZ = clampAndRound(	(z + mDriftZ + mStageZ),
																									0,
																									mResolutionZ - 1);/**/

					final int lDestIndex = lTranslatedX + mResolutionX
																	* lTranslatedY
																	+ mResolutionX
																	* mResolutionY
																	* lTranslatedZ;

					// System.out.format("X=%d, Y=%d, Z=%d \n", x, y, z);

					mTranslatedVolumeDataArray.put(lDestIndex, lValue);
				}

		mCurrentReaderIndex += 1;

		mTranslatedVolumeDataArray.rewind();
		mVolumeDataArray.rewind();

		return mTranslatedVolumeDataArray;
	}

	private void applyDrift()
	{
		if (mFauxscopeRandomizer == null)
		{
			mDriftX += mDriftAmplitude * 2 * (random() - 0.5);
			mDriftY += mDriftAmplitude * 2 * (random() - 0.5);
			mDriftZ += mDriftAmplitude * 2 * (random() - 0.5);

			mDriftX += 1;
			mDriftY += 1;
			mDriftZ += 1;
		}
		else
		{
			final float[] lNextPoint = mFauxscopeRandomizer.getNextPoint();
			mDriftX += lNextPoint[0];
			mDriftY += lNextPoint[1];
			mDriftZ += lNextPoint[2];
		}
	}

	private int clampAndRound(float pValue, int pMin, int pMax)
	{
		final int lClampedAndRoundedValue = round(min(max(pValue, pMin),
																									pMax));
		return lClampedAndRoundedValue;
	}

	@Override
	public void notifyResult(Processor<float[]> pSource, float[] pResult)
	{
		final float lCenterX = (float) (0.5 * (1 + pResult[0]) * mResolutionX);
		final float lCenterY = (float) (0.5 * (1 + pResult[1]) * mResolutionY);
		final float lCenterZ = (float) (0.5 * (1 + pResult[2]) * mResolutionZ);

		System.out.format("Center of mass located at: x=%g, y=%g, z=%g \n",
											lCenterX,
											lCenterY,
											lCenterZ);

		if (mFirstCall)
		{
			setCenterLock(lCenterX, lCenterY, lCenterZ);
			mFirstCall = false;
		}

		if (isCorrectionActive())
		{
			final float lCorrectionX = mCenterLockX - lCenterX;
			final float lCorrectionY = mCenterLockY - lCenterY;
			final float lCorrectionZ = mCenterLockZ - lCenterZ;

			moveStage(lCorrectionX, lCorrectionY, lCorrectionZ);
		}

	}

	public void changeDrift(float pDx, float pDy, float pDz)
	{
		mDriftX += pDx;
		mDriftY += pDy;
		mDriftZ += pDz;
	}

}
