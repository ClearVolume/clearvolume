package clearvolume.demo;

import static java.lang.Math.abs;
import static java.lang.Math.random;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Test;

import clearvolume.controller.ExternalRotationController;
import clearvolume.projections.ProjectionAlgorithm;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.VolumeCaptureListener;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.renderer.jogl.overlay.o2d.GraphOverlay;
import clearvolume.renderer.jogl.overlay.o2d.ImageQualityOverlay;
import clearvolume.renderer.jogl.overlay.o3d.DriftOverlay;
import clearvolume.renderer.jogl.overlay.o3d.PathOverlay;
import clearvolume.renderer.opencl.OpenCLVolumeRenderer;
import clearvolume.renderer.processors.Processor;
import clearvolume.renderer.processors.ProcessorResultListener;
import clearvolume.renderer.processors.impl.CUDAProcessorTest;
import clearvolume.renderer.processors.impl.OpenCLCenterMass;
import clearvolume.renderer.processors.impl.OpenCLHistogram;
import clearvolume.renderer.processors.impl.OpenCLTenengrad;
import clearvolume.renderer.processors.impl.OpenCLTest;
import clearvolume.transferf.TransferFunctions;

import com.jogamp.newt.awt.NewtCanvasAWT;

public class ClearVolumeDemo
{

	private static ClearVolumeRendererInterface mClearVolumeRenderer;

	public static void main(final String[] argv) throws ClassNotFoundException
	{
		if (argv.length == 0)
		{
			final Class<?> c = Class.forName("clearvolume.demo.ClearVolumeDemo");

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
			final ClearVolumeDemo cvdemo = new ClearVolumeDemo();
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
																																																						1,
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

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
																																																						1,
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

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
																																																						1,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		final ImageQualityOverlay lImageQualityOverlay = new ImageQualityOverlay();
		lClearVolumeRenderer.addOverlay(lImageQualityOverlay);
		lClearVolumeRenderer.addProcessors(lImageQualityOverlay.getProcessors());

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
																																																						1,
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

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
																																																						1,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		lClearVolumeRenderer.addOverlay(new PathOverlay());
		lClearVolumeRenderer.addProcessor(new CUDAProcessorTest());

		final OpenCLHistogram histoProcessor = new OpenCLHistogram();
		histoProcessor.addResultListener(new ProcessorResultListener<IntBuffer>()
		{

			@Override
			public void notifyResult(	final Processor<IntBuffer> pSource,
																final IntBuffer pResult)
			{
				System.out.println("histogram: " + Arrays.toString(pResult.array()));
			}
		});

		lClearVolumeRenderer.addProcessor(histoProcessor);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
	public void demoDriftOverlay() throws InterruptedException,
																IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						1,
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

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
																																																					1,
																																																					512,
																																																					512,
																																																					1,
																																																					false);
		lClearVolumeRenderer.addOverlay(new PathOverlay());
		lClearVolumeRenderer.addProcessor(new CUDAProcessorTest());
		lClearVolumeRenderer.addProcessor(new OpenCLTest());

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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
	public void demoGraphOverlay() throws InterruptedException,
																IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					512,
																																																					512,
																																																					1,
																																																					512,
																																																					512,
																																																					1,
																																																					false);

		final GraphOverlay lGraphOverlay = new GraphOverlay(64);
		lClearVolumeRenderer.addOverlay(lGraphOverlay);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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

		double lTrend = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
			lTrend += 0.05 * (Math.random() - 0.5);
			final double lValue = lTrend + 0.02 * Math.random();
			lGraphOverlay.addPoint(lValue);
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoPathOverlay3D()	throws InterruptedException,
																	IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					1024,
																																																					1024,
																																																					1,
																																																					512,
																																																					512,
																																																					1,
																																																					false);

		final PathOverlay lPathOverlay = new PathOverlay();
		lClearVolumeRenderer.addOverlay(lPathOverlay);

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
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

		float x = 0, y = 0, z = 0;

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
			lPathOverlay.addPathPoint(x, y, z);
			x += 0.01 * (random() - 0.5);
			y += 0.01 * (random() - 0.5);
			z += 0.01 * (random() - 0.5);
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoRendererInJFrame() throws InterruptedException,
																		IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					512,
																																																					512,
																																																					1,
																																																					512,
																																																					512,
																																																					1,
																																																					true);
		final NewtCanvasAWT lNewtCanvasAWT = lClearVolumeRenderer.getNewtCanvasAWT();

		final JFrame lJFrame = new JFrame("ClearVolume");
		lJFrame.setLayout(new BorderLayout());
		final Container lContainer = new Container();
		lContainer.setLayout(new BorderLayout());
		lContainer.add(lNewtCanvasAWT, BorderLayout.CENTER);
		lJFrame.setSize(new Dimension(1024, 1024));
		lJFrame.add(lContainer);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				lJFrame.setVisible(true);
			}
		});

		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
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

		while (lClearVolumeRenderer.isShowing() && lJFrame.isVisible())
		{
			Thread.sleep(100);
			lJFrame.setTitle("BRAVO! THIS IS A JFRAME! It WORKS!");
		}

		lClearVolumeRenderer.close();
		lJFrame.dispose();

	}

	@Test
	public void demoWith8BitGeneratedDataset() throws InterruptedException,
																						IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					1024,
																																																					1024,
																																																					1,
																																																					1024,
																																																					1024,
																																																					1,
																																																					false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 768;
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
	public void demoDitheringAndResolutionCuda() throws InterruptedException,
																							IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newCudaRenderer(	"ClearVolumeTest",
																																																					1024,
																																																					1024,
																																																					1,
																																																					512,
																																																					512,
																																																					1,
																																																					false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
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

					lVolumeDataArray[lIndex] = (byte) (255 * (1.0 / (1.0 + 0.5 * abs(z - lResolutionZ
																																						/ 2))));
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
	public void demoDitheringAndResolutionOpenCL() throws InterruptedException,
																								IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						1024,
																																																						1024,
																																																						1,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
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

					final int lCenter = lResolutionZ / 2;

					final int lDistance = (x - lCenter) * (x - lCenter)
																+ (y - lCenter)
																* (y - lCenter)
																+ (z - lCenter)
																* (z - lCenter);

					lVolumeDataArray[lIndex] = (byte) (255 * Math.exp(-.001 * abs(lDistance - lCenter
																																				* lCenter
																																				/ 5)));

					/*
					 * mVolumeDataArray[lIndex] = (byte) (255 * (1.0 / (1.0 +
					 * 0.5 * abs(z - lResolutionZ / 2))));/*
					 */
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
	public void demoAspectRatio()	throws InterruptedException,
																IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														512,
																																														512,
																																														1,
																																														512,
																																														512,
																																														1,
																																														false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 128;
		final int lResolutionY = 128;
		final int lResolutionZ = 128;

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
	public void demoAspectRatioPreset()	throws InterruptedException,
																			IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														512,
																																														512,
																																														1,
																																														512,
																																														512,
																																														1,
																																														false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 400;
		final int lResolutionY = 100;
		final int lResolutionZ = 200;

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

		lClearVolumeRenderer.setVoxelSize(lResolutionX * 5.0,
																			lResolutionY * 4.0,
																			lResolutionZ * 3.0);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith16BitGeneratedDataset()	throws InterruptedException,
																							IOException
	{
		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																					512,
																																																					512,
																																																					2,
																																																					false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ
																							* 2];

		for (int z = 0; z < lResolutionZ; z++)
			for (int y = 0; y < lResolutionY; y++)
				for (int x = 0; x < lResolutionX; x++)
				{
					final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
																													* z);
					lVolumeDataArray[lIndex + 1] = (byte) (((byte) x ^ (byte) y ^ (byte) z));
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);
		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

		lClearVolumeRenderer.close();

	}

	@Test
	public void demoWithGeneratedDatasetWithEgg3D()	throws InterruptedException,
																									IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer8Bit(	"ClearVolumeTest",
																																																							512,
																																																							512,
																																																							false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);
		lClearVolumeRenderer.setProjectionAlgorithm(ProjectionAlgorithm.MaxProjection);

		ExternalRotationController lEgg3DController = null;
		try
		{
			lEgg3DController = new ExternalRotationController(ExternalRotationController.cDefaultEgg3DTCPport,
																												lClearVolumeRenderer);
			lClearVolumeRenderer.setQuaternionController(lEgg3DController);
			lEgg3DController.connectAsynchronouslyOrWait();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}

		final int lResolutionX = 128;
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
					lVolumeDataArray[lIndex] = (byte) (x ^ y ^ z);
				}

		final ByteBuffer lWrappedArray = ByteBuffer.wrap(lVolumeDataArray);
		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							lWrappedArray,
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

		lClearVolumeRenderer.close();

		if (lEgg3DController != null)
			lEgg3DController.close();

	}

	@Test
	public void demoWithFileDatasets()
	{

		try
		{
			startSample("./data/Bucky.raw", 1, 32, 32, 32);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

	}

	@Test
	public void demoWith8BitGeneratedDataset2LayersJCuda() throws InterruptedException,
																												IOException

	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																														512,
																																														512,
																																														1,
																																														512,
																																														512,
																																														2,
																																														false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
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
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray0),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();
		Thread.sleep(2000);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
																							ByteBuffer.wrap(lVolumeDataArray1),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			lClearVolumeRenderer.setLayerVisible(	i % 2,
																						!lClearVolumeRenderer.isLayerVisible(i % 2));

			lClearVolumeRenderer.requestDisplay();
			i++;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith8BitGeneratedDataset2LayersOpenCL()	throws InterruptedException,
																													IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																																												512,
																																												512,
																																												1,
																																												512,
																																												512,
																																												2,
																																												false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
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
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray0),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();
		Thread.sleep(2000);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
																							ByteBuffer.wrap(lVolumeDataArray1),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			lClearVolumeRenderer.setLayerVisible(	i % 2,
																						!lClearVolumeRenderer.isLayerVisible(i % 2));/**/

			lClearVolumeRenderer.requestDisplay();
			i++;
		}

		lClearVolumeRenderer.close();
	}

	@Test
	public void demoWith8BitGeneratedDataset3LayersOpenCL()	throws InterruptedException,
																													IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = new OpenCLVolumeRenderer(	"ClearVolumeTest",
																																												// new
																																												// JCudaClearVolumeRenderer(
																																												// "ClearVolumeTest",
																																												1024,
																																												1024,
																																												1,
																																												1024,
																																												1024,
																																												3,
																																												false);

		lClearVolumeRenderer.setVisible(true);

		final int lResolutionX = 512;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray0 = new byte[lResolutionX * lResolutionY
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
					lVolumeDataArray0[lIndex] = (byte) lCharValue;
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(lVolumeDataArray0),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);

		lClearVolumeRenderer.requestDisplay();
		Thread.sleep(2000);

		final byte[] lVolumeDataArray1 = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = x + lResolutionX
															* y
															+ lResolutionX
															* lResolutionY
															* z;
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray1[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	1,
																							ByteBuffer.wrap(lVolumeDataArray1),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		final byte[] lVolumeDataArray2 = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		for (int z = 0; z < lResolutionZ / 2; z++)
			for (int y = 0; y < lResolutionY / 2; y++)
				for (int x = 0; x < lResolutionX / 2; x++)
				{
					final int lIndex = lResolutionX / 2
															+ x
															+ lResolutionX
															* (lResolutionY / 2 + y)
															+ lResolutionX
															* lResolutionY
															* (lResolutionZ / 2 + z);
					int lCharValue = 255 - (((byte) (x) ^ (byte) (y + z) ^ (byte) z));
					if (lCharValue < 12)
						lCharValue = 0;
					lVolumeDataArray2[lIndex] = (byte) (lCharValue);
				}

		lClearVolumeRenderer.setVolumeDataBuffer(	2,
																							ByteBuffer.wrap(lVolumeDataArray2),
																							lResolutionX,
																							lResolutionY,
																							lResolutionZ);/**/

		lClearVolumeRenderer.requestDisplay();

		int i = 0;
		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

			lClearVolumeRenderer.setLayerVisible(	i % 3,
																						!lClearVolumeRenderer.isLayerVisible(i % 3));/**/

			lClearVolumeRenderer.requestDisplay();
			i++;
		}

		lClearVolumeRenderer.close();
	}

	private static void startSample(final String pRessourceName,
																	final int pBytesPerVoxel,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ)	throws IOException,
																										InterruptedException
	{
		final InputStream lResourceAsStream = ClearVolumeDemo.class.getResourceAsStream(pRessourceName);
		startSample(lResourceAsStream,
								pBytesPerVoxel,
								pSizeX,
								pSizeY,
								pSizeZ);
	}

	private static void startSample(final InputStream pInputStream,
																	final int pBytesPerVoxel,
																	final int pSizeX,
																	final int pSizeY,
																	final int pSizeZ)	throws IOException,
																										InterruptedException
	{

		final byte[] data = loadData(	pInputStream,
																	pBytesPerVoxel,
																	pSizeX,
																	pSizeY,
																	pSizeZ);

		mClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer("ClearVolumeTest",
																																			512,
																																			512,
																																			pBytesPerVoxel,
																																			false);

		mClearVolumeRenderer.setTransferFunction(TransferFunctions.getRainbow());
		mClearVolumeRenderer.setVisible(true);

		mClearVolumeRenderer.setVolumeDataBuffer(	0,
																							ByteBuffer.wrap(data),
																							pSizeX,
																							pSizeY,
																							pSizeZ);

		mClearVolumeRenderer.requestDisplay();

		while (mClearVolumeRenderer.isShowing())
		{
			Thread.sleep(100);
		}

	}

	@Test
	public void demoStressTest() throws InterruptedException,
															IOException
	{
		for (int i = 0; i < 50; i++)
		{

			final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
																																																						i % 2 == 0 ? 768
																																																											: 512,
																																																						i % 2 == 0 ? 768
																																																											: 512,
																																																						2,
																																																						false);
			lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
			lClearVolumeRenderer.setVisible(true);

			final int lResolutionX = 512;
			final int lResolutionY = lResolutionX;
			final int lResolutionZ = lResolutionX;

			final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																								* lResolutionZ
																								* 2];

			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
																														* z);
						lVolumeDataArray[lIndex + 1] = (byte) (((byte) x ^ (byte) y ^ (byte) z));
					}

			lClearVolumeRenderer.setVolumeDataBuffer(	0,
																								ByteBuffer.wrap(lVolumeDataArray),
																								lResolutionX,
																								lResolutionY,
																								lResolutionZ);
			lClearVolumeRenderer.requestDisplay();

			// Thread.sleep(100);

			lClearVolumeRenderer.close();
		}

	}

	@Test
	public void demoStressTestNewtCanvasAWT()	throws InterruptedException,
																						IOException,
																						InvocationTargetException
	{
		for (int nfi = 0; nfi < 50; nfi++)
		{
			final int i = nfi;

			final JFrame lJFrame = new JFrame("ClearVolume");
			;

			final ClearVolumeRendererInterface lClearVolumeRenderer = new JCudaClearVolumeRenderer(	"ClearVolumeTest",
																																															(i % 2 == 0) ? 768
																																																					: 64,
																																															(i % 2 == 0) ? 768
																																																					: 64,
																																															1,
																																															(i % 2 == 0) ? 768
																																																					: 64,
																																															(i % 2 == 0) ? 768
																																																					: 64,
																																															1,
																																															true);

			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{

					/*
					 * final ClearVolumeRendererInterface lClearVolumeRenderer =
					 * ClearVolumeRendererFactory.newBestRenderer(
					 * "ClearVolumeTest", i % 2 == 0 ? 768 : 512, i % 2 == 0 ?
					 * 768 : 512, 1, i % 2 == 0 ? 768 : 512, i % 2 == 0 ? 768 :
					 * 512, 1, true);/*
					 */
					final NewtCanvasAWT lNewtCanvasAWT = lClearVolumeRenderer.getNewtCanvasAWT();

					System.out.println("lJFrame.setTitle(...");
					lJFrame.setTitle("BRAVO! THIS IS A JFRAME! It WORKS! I=" + i);
					lJFrame.setLayout(new BorderLayout());
					final Container lContainer = new Container();
					lContainer.setLayout(new BorderLayout());

					System.out.println("lContainer.add(lNewtCanvasAWT, BorderLayout.CENTER);");
					lContainer.add(lNewtCanvasAWT, BorderLayout.CENTER);
					lJFrame.setSize(new Dimension(1024, 1024));
					lJFrame.add(lContainer);

					System.out.println("lJFrame.setVisible(true);");
					lJFrame.setVisible(true);
				}
			});

			lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
			lClearVolumeRenderer.setVisible(true);

			final int lResolutionX = 512;
			final int lResolutionY = lResolutionX;
			final int lResolutionZ = lResolutionX;

			final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																								* lResolutionZ];

			for (int d = 0; d < 1; d++)
			{
				for (int z = 0; z < lResolutionZ; z++)
					for (int y = 0; y < lResolutionY; y++)
						for (int x = 0; x < lResolutionX; x++)
						{
							final int lIndex = x + lResolutionX
																	* y
																	+ lResolutionX
																	* lResolutionY
																	* z;
							int lCharValue = (((byte) x ^ (byte) y ^ (byte) z ^ (byte) d));
							if (lCharValue < 12)
								lCharValue = 0;
							lVolumeDataArray[lIndex] = (byte) lCharValue;
						}

				lClearVolumeRenderer.setCurrentRenderLayer(0);
				lClearVolumeRenderer.setVolumeDataBuffer(	0,
																									ByteBuffer.wrap(lVolumeDataArray),
																									lResolutionX,
																									lResolutionY,
																									lResolutionZ);

				System.out.println("lClearVolumeRenderer.requestDisplay();");
				lClearVolumeRenderer.requestDisplay();
			}

			/*
			 * try { //Thread.sleep(500); } catch (final InterruptedException e)
			 * { e.printStackTrace(); }/*
			 */

			SwingUtilities.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					System.out.println("lJFrame.setVisible(true); lJFrame.dispose();");
					// lJFrame.setVisible(false);
					lJFrame.dispose();
				}
			});/**/

			lClearVolumeRenderer.close();
		}

	}

	@Test
	public void demoCaptureVolumeData()	throws InterruptedException,
																			IOException
	{

		final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
																																																						512,
																																																						512,
																																																						1,
																																																						512,
																																																						512,
																																																						1,
																																																						false);
		lClearVolumeRenderer.setTransferFunction(TransferFunctions.getGrayLevel());
		lClearVolumeRenderer.setVisible(true);

		lClearVolumeRenderer.addVolumeCaptureListener(new VolumeCaptureListener()
		{

			@Override
			public void capturedVolume(	final ByteBuffer[] pCaptureBuffers,
																	final boolean pFloatType,
																	final int pBytesPerVoxel,
																	final long pVolumeWidth,
																	final long pVolumeHeight,
																	final long pVolumeDepth,
																	final double pVoxelWidth,
																	final double pVoxelHeight,
																	final double pVoxelDepth)
			{
				System.out.format("Captured %d volume %s bpv=%d (%d, %d, %d) (%g, %g, %g) %s\n",
													pCaptureBuffers.length,
													pFloatType ? "float" : "int",
													pBytesPerVoxel,
													pVolumeWidth,
													pVolumeHeight,
													pVolumeDepth,
													pVoxelWidth,
													pVoxelHeight,
													pVoxelDepth,
													pCaptureBuffers[0].toString());

			}
		});

		final int lResolutionX = 256;
		final int lResolutionY = lResolutionX;
		final int lResolutionZ = lResolutionX;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
																							* lResolutionZ];

		lClearVolumeRenderer.requestDisplay();

		while (lClearVolumeRenderer.isShowing())
		{
			Thread.sleep(500);

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
		}

		lClearVolumeRenderer.close();
	}

	private static byte[] loadData(	final InputStream pInputStream,
																	final int pBytesPerVoxel,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException
	{
		// Try to read the specified file
		byte data[] = null;
		final InputStream fis = pInputStream;
		try
		{
			final int size = pBytesPerVoxel * sizeX * sizeY * sizeZ;
			data = new byte[size];
			fis.read(data);
		}
		catch (final IOException e)
		{
			System.err.println("Could not load input file");
			e.printStackTrace();
			return null;
		}
		fis.close();
		return data;
	}

	private static byte[] loadData(	final String pRessourceName,
																	final int pBytesPerVoxel,
																	final int sizeX,
																	final int sizeY,
																	final int sizeZ) throws IOException
	{
		final InputStream lResourceAsStream = ClearVolumeDemo.class.getResourceAsStream(pRessourceName);

		return loadData(lResourceAsStream,
										pBytesPerVoxel,
										sizeX,
										sizeY,
										sizeZ);
	}
}
