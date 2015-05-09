package clearvolume.demo;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.RenderAlgorithm;
import clearvolume.renderer.cleargl.overlay.o2d.GraphOverlay;
import clearvolume.renderer.cleargl.overlay.o2d.ImageQualityOverlay;
import clearvolume.renderer.cleargl.overlay.o3d.DriftOverlay;
import clearvolume.renderer.cleargl.overlay.o3d.PathOverlay;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;
import clearvolume.renderer.processors.impl.CUDAProcessorTest;
import clearvolume.renderer.processors.impl.OpenCLCenterMass;
import clearvolume.renderer.processors.impl.OpenCLDeconv;
import clearvolume.renderer.processors.impl.OpenCLDenoise;
import clearvolume.renderer.processors.impl.OpenCLTenengrad;
import clearvolume.renderer.processors.impl.OpenCLTest;
import clearvolume.transferf.TransferFunctions;
import coremem.types.NativeTypeEnum;

public class ClearVolumeProcessorsDemo
{

	public static void main(final String[] argv) throws ClassNotFoundException
	{
		if (argv.length == 0)
		{
			final Class<?> c = Class.forName("clearvolume.demo.ClearVolumeProcessorsDemo");

			System.out.println("Give one of the following method names as parameter:");

			for (final Member m : c.getMethods())
			{
				final String name = ((Method) m).getName();

				if (name.substring(0, 4).equals("demo"))
				{
					System.out.println("Demo: " + ((Method) m).getName());
				}
			}
		}
		else
		{
			final ClearVolumeProcessorsDemo cvdemo = new ClearVolumeProcessorsDemo();
			Method m;

			try
			{
				m = cvdemo.getClass().getMethod(argv[0]);
			}
			catch (final Exception e)
			{
				System.out.println("Could not launch " + argv[0]
														+ " because ...");
				e.printStackTrace();

				return;
			}

			try
			{
				System.out.println("Running " + argv[0] + "()...");
				m.invoke(cvdemo);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	@Test
	public void demoOpenCLProcessors() throws InterruptedException,
																		IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						1024,
																																																						1024,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		lClearVolumeRenderer.addOverlay(new PathOverlay());
		lClearVolumeRenderer.addProcessor(new CUDAProcessorTest());

		final OpenCLTest myProc = new OpenCLTest();
		myProc.addResultListener(new ProcessorResultListener<Double>()
		{

			@Override
			public void notifyResult(	final Processor<Double> pSource,
																final Double pResult)
			{
				System.out.println(pResult);
			}
		});

		lClearVolumeRenderer.addProcessor(myProc);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoOpenCLTenengrad()	throws InterruptedException,
																		IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		final GraphOverlay lGraphOverlay = new GraphOverlay(1024);
		lClearVolumeRenderer.addOverlay(lGraphOverlay);

		final OpenCLTenengrad lOpenCLTenengrad = new OpenCLTenengrad();
		lOpenCLTenengrad.addResultListener(new ProcessorResultListener<Double>()
		{
			@Override
			public void notifyResult(	final Processor<Double> pSource,
																final Double pResult)
			{
				System.out.println("tenengrad = " + pResult);
			}
		});

		lClearVolumeRenderer.addProcessor(lOpenCLTenengrad);
		lOpenCLTenengrad.addResultListener(lGraphOverlay);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;

					lVolumeDataArray[lIndex] = (byte) lCharValue;
					// mVolumeDataArray[lIndex] = (byte) (255 * x
					// * x
					// / lResolutionX / lResolutionX);

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		final double s = 2.;
		while (lClearVolumeRenderer.isShowing())
		{

			Thread.sleep(100);

			// lOpenCLTenengrad.setSigma(s);
			// s += .5;

			/*
			 * for (int i = 1; i < mVolumeDataArray.length - 1; i++)
			 * mVolumeDataArray[i] = (byte) (((mVolumeDataArray[i - 1] + s
			 * mVolumeDataArray[i] + mVolumeDataArray[i + 1]) / (s + 2)));
			 */
			for (int i = 1; i < lVolumeDataArray.length - 1; i++)
				lVolumeDataArray[i] = (byte) (.99 * lVolumeDataArray[i]);

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();

		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoImageQualityOverlayAndProcessor()	throws InterruptedException,
																										IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		final ImageQualityOverlay lImageQualityOverlay = new ImageQualityOverlay();
		lClearVolumeRenderer.addOverlay(lImageQualityOverlay);
		lClearVolumeRenderer.addProcessors(lImageQualityOverlay.getProcessors());

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;

					lVolumeDataArray[lIndex] = (byte) lCharValue;
					// mVolumeDataArray[lIndex] = (byte) (255 * x
					// * x
					// / lResolutionX / lResolutionX);

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		final double s = 0;
		while (lClearVolumeRenderer.isShowing())
		{

			Thread.sleep(100);

			// lOpenCLTenengrad.setSigma(s);
			// s += .5;

			for (int i = 1; i < lVolumeDataArray.length - 1; i++)
				lVolumeDataArray[i] = (byte) ((lVolumeDataArray[i - 1] + 2
																				* lVolumeDataArray[i] + lVolumeDataArray[i + 1]) / 4);

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoOpenCLCenterOfMass() throws InterruptedException,
																			IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		final DriftOverlay lDriftOverlay = new DriftOverlay();
		lClearVolumeRenderer.addOverlay(lDriftOverlay);

		final OpenCLCenterMass lOpenCLCenterMass = new OpenCLCenterMass();
		lOpenCLCenterMass.addResultListener(new ProcessorResultListener<float[]>()
		{
			@Override
			public void notifyResult(	final Processor<float[]> pSource,
																final float[] pResult)
			{

				System.out.println("center of mass [x,y,z,mass]: " + Arrays.toString(pResult));
			}
		});

		lClearVolumeRenderer.addProcessor(lOpenCLCenterMass);
		// TODO: put that back:
		// lOpenCLCenterMass.addResultListener(lDriftOverlay);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX + 1;
		final int lResolutionZ = 256;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;

					lVolumeDataArray[lIndex] = (byte) (255 * x / lResolutionX);

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		int x0 = 0, y0 = 0, z0 = 0;
		while (lClearVolumeRenderer.isShowing())
		{

			// Thread.sleep(100);

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = x + lResolutionX
																* y
																+ lResolutionX
																* lResolutionY
																* z;

						lVolumeDataArray[lIndex] = (byte) (255 * Math.exp(-.01 * ((x - x0) * (x - x0)
																																			+ (y - y0)
																																			* (y - y0) + (z - z0) * (z - z0))));

					}/**/

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);/**/
			lClearVolumeRenderer.requestDisplay();
			x0 = (x0 + 5) % lResolutionX;
			y0 = (y0 + 10) % lResolutionY;
			z0 = (z0 + 20) % lResolutionZ;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoOpenCLHistogram()	throws InterruptedException,
																		IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		final Random rand = new Random();

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					// int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					// if (lCharValue < 12)
					// lCharValue = 0;

					// lVolumeDataArray[lIndex] = (byte) lCharValue;

					// lVolumeDataArray[lIndex] = (byte) 100;

					// nextInt is normally exclusive of the top value,
					// so add 1 to make it inclusive

					// lVolumeDataArray[lIndex] = (byte) (200 * x * x
					// / lResolutionX / lResolutionX + rand.nextInt(20));
					lVolumeDataArray[lIndex] = (byte) (118 + rand.nextInt(20));

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(300);
			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = x + lResolutionX
																* y
																+ lResolutionX
																* lResolutionY
																* z;

						lVolumeDataArray[lIndex] = (byte) ((lVolumeDataArray[lIndex] - 5 + rand.nextInt(10)) % 256);

					}

		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoDriftOverlay() throws InterruptedException,
																IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		lClearVolumeRenderer.addOverlay(new PathOverlay());

		final RandomWalk RandomWalk = new RandomWalk();
		final DriftOverlay driftOverlay = new DriftOverlay();
		lClearVolumeRenderer.addOverlay(driftOverlay);
		RandomWalk.addResultListener(driftOverlay);

		lClearVolumeRenderer.addProcessor(RandomWalk);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;

					// mVolumeDataArray[lIndex] = (byte) lCharValue;
					lVolumeDataArray[lIndex] = (byte) (255 * x * x

					/ lResolutionX / lResolutionX);

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(1000);
			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoCudaProcessors() throws InterruptedException,
																	IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newCudaRenderer(	"ClearVolumeTest",
																																																					1024,
																																																					1024,
																																																					NativeTypeEnum.UnsignedByte,
																																																					512,
																																																					512,
																																																					1,
																																																					false);
		lClearVolumeRenderer.addOverlay(new PathOverlay());
		lClearVolumeRenderer.addProcessor(new CUDAProcessorTest());
		lClearVolumeRenderer.addProcessor(new OpenCLTest());

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoOpenCLDeconv() throws InterruptedException,
																IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);

		lClearVolumeRenderer.addProcessor(new CUDAProcessorTest());

		final OpenCLDeconv lDeconvProcessor = new OpenCLDeconv();

		lDeconvProcessor.addResultListener(new ProcessorResultListener<Void>()
		{

			@Override
			public void notifyResult(	final Processor<Void> pSource,
																final Void pResult)
			{
				System.out.println("deconv!: ");
			}
		});

		lClearVolumeRenderer.addProcessor(lDeconvProcessor);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 64;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		final Random rand = new Random();

		final int x0 = lResolutionX / 2 - 10, y0 = lResolutionY / 2, z0 = lResolutionZ / 2;
		final int x1 = lResolutionX / 2 + 10, y1 = lResolutionY / 2, z1 = lResolutionZ / 2;

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;

					lVolumeDataArray[lIndex] = (byte) (55 * (Math.exp(-.01 * ((x - x0) * (x - x0)
																																		+ (y - y0)
																																		* (y - y0) + (z - z0) * (z - z0))) + Math.exp(-.01 * ((x - x1) * (x - x1)
																																																													+ (y - y1)
																																																													* (y - y1) + (z - z1) * (z - z1)))) + rand.nextInt(5));

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		float s = 1.f;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(2000);
			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();

			lDeconvProcessor.setSigmas(s, s, s);
			s += 1.f;

		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoOpenCLDenoise()	throws InterruptedException,
																	IOException
	{
		final int noiseLevel = 30;

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						NativeTypeEnum.UnsignedByte,
																																																						512,
																																																						512,
																																																						1,
																																																						false);

		lClearVolumeRenderer.setRenderAlgorithm(RenderAlgorithm.IsoSurface);

		OpenCLDenoise lDenoiseProcessor = null;

		for (final Processor<?> lProcessor : lClearVolumeRenderer.getProcessors())
		{
			if (lProcessor instanceof OpenCLDenoise)
			{

				lDenoiseProcessor = (OpenCLDenoise) lProcessor;
				lDenoiseProcessor.setActive(true);
				lDenoiseProcessor.addResultListener(new ProcessorResultListener<Boolean>()
				{
					@Override
					public void notifyResult(	final Processor<Boolean> pSource,
																		final Boolean pResult)
					{
						if (pResult)
							System.out.println("denoise!");
					}
				});
			}
		}

		assertNotNull(lDenoiseProcessor);


		lDenoiseProcessor.setBlockSize(2);
		lDenoiseProcessor.setSigmaValue(1f * noiseLevel / 256f);
		lDenoiseProcessor.setSigmaSpace(1.5f);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 320;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = 50;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		final Random rand = new Random();

		final int x0 = lResolutionX / 2 - 10, y0 = lResolutionY / 2, z0 = lResolutionZ / 2;
		final int x1 = lResolutionX / 2 + 10, y1 = lResolutionY / 2, z1 = lResolutionZ / 2;

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;

					lVolumeDataArray[lIndex] = (byte) (55 * (Math.exp(-.01 * ((x - x0) * (x - x0)
																																		+ (y - y0)
																																		* (y - y0) + (z - z0) * (z - z0))) + Math.exp(-.01 * ((x - x1) * (x - x1)
																																																													+ (y - y1)
																																																													* (y - y1) + (z - z1) * (z - z1)))) + rand.nextInt(5));

				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();


		while (lClearVolumeRenderer.isShowing())
		{
			// Thread.sleep(2000);
			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = x + lResolutionX
																* y
																+ lResolutionX
																* lResolutionY
																* z;

						lVolumeDataArray[lIndex] = (byte) (((x > lResolutionX / 2) ? 100
																																			: 0) + 100
																								* (Math.exp(-.01 * ((x - x0) * (x - x0)
																																		+ (y - y0)
																																		* (y - y0) + (z - z0) * (z - z0))) + Math.exp(-.01 * ((x - x1) * (x - x1)
																																																													+ (y - y1)
																																																													* (y - y1) + (z - z1) * (z - z1)))) + rand.nextInt(noiseLevel + 1));

					}



		}

		lClearVolumeRenderer.close();
	}
}
