package clearvolume.renderer.jcuda;

import static jcuda.driver.JCudaDriver.cuCtxDestroy;
import static jcuda.driver.JCudaDriver.cuDeviceGet;
import static jcuda.driver.JCudaDriver.cuGLCtxCreate;
import static jcuda.driver.JCudaDriver.cuInit;
import static jcuda.driver.JCudaDriver.cuModuleGetFunction;
import static jcuda.driver.JCudaDriver.cuModuleLoad;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUfunction;
import jcuda.driver.CUmodule;
import jcuda.driver.JCudaDriver;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

public class JCudaUtils
{
	private static CUcontext sCUcontext;

	/**
	 * Initialises CUDA and returns a given CUDA function
	 * 
	 * @param pCUmodule
	 * @param pSourceCodeInputStream
	 * @param pPlanBPTXInputStream
	 * @param pFunctionSignature
	 * @param pCompiledParameters
	 * @return
	 * @throws IOException
	 */
	public static final CUfunction initCuda(final CUmodule pCUmodule,
																					final InputStream pSourceCodeInputStream,
																					final InputStream pPlanBPTXInputStream,
																					final String pFunctionSignature,
																					final Map<String, String> pCompiledParameters) throws IOException
	{
		// Initialize the JCudaDriver. Note that this has to be done from
		// the same thread that will later use the JCudaDriver API.
		JCudaDriver.setExceptionsEnabled(true);
		cuInit(0);
		final CUdevice dev = new CUdevice();
		cuDeviceGet(dev, 0);
		sCUcontext = new CUcontext();
		cuGLCtxCreate(sCUcontext, 0, dev);

		final File lPTXFile = nvccCompile(pSourceCodeInputStream,
																			pPlanBPTXInputStream,
																			pCompiledParameters);

		// Load the PTX file containing the kernel
		// final CUmodule module = new CUmodule();
		cuModuleLoad(pCUmodule, lPTXFile.getAbsolutePath());

		// Obtain a function pointer to the kernel function. This function
		// will later be called in the display method of this
		// GLEventListener.
		final CUfunction function = new CUfunction();
		cuModuleGetFunction(function, pCUmodule, pFunctionSignature);

		return function;
	}

	/**
	 * Closes the CUDA context.
	 */
	public static final void closeCuda()
	{
		cuCtxDestroy(sCUcontext);
	}

	/**
	 * Compiles a .cu or otherwise uses an available .ptx file both accessed as
	 * resources (InputStream) additional compilation parameters can be provided.
	 * 
	 * @param pInputStreamCUFileInputStream
	 * @param pBackupPTXFileInputStream
	 * @param pCompiledParameters
	 * @return
	 * @throws IOException
	 */
	private static File nvccCompile(final InputStream pInputStreamCUFileInputStream,
																	final InputStream pBackupPTXFileInputStream,
																	final Map<String, String> pCompiledParameters) throws IOException
	{
		try
		{
			final File lCUFile = File.createTempFile("jcuda", ".cu");
			final File lPTXFile = File.createTempFile("jcuda", ".ptx");
			
			System.out.println(lPTXFile);

			final StringWriter writer = new StringWriter();
			IOUtils.copy(	pInputStreamCUFileInputStream,
										writer,
										Charset.defaultCharset());
			String lCUFileString = writer.toString();

			if (pCompiledParameters != null)
				for (final Entry<String, String> lEntry : pCompiledParameters.entrySet())
				{
					final String lPattern = lEntry.getKey();
					final String lReplacement = lEntry.getValue();

					lCUFileString = lCUFileString.replaceAll(	lPattern,
																										lReplacement);
				}


			FileUtils.write(lCUFile, lCUFileString);

			nvccCompile(lCUFile, lPTXFile);

			return lPTXFile;
		}
		catch (Exception e)
		{
			if (pBackupPTXFileInputStream == null)
			{
				e.printStackTrace();
				return null;
			}

			final File lPTXFile = File.createTempFile("jcuda", ".ptx");
			FileUtils.copyInputStreamToFile(pBackupPTXFileInputStream,
																			lPTXFile);
			return lPTXFile;
		}
	}



	/**
	 * Compiles a .cu file into a .ptx file.
	 * 
	 * @param pCUFile
	 * @param pPTXFile
	 * @throws IOException
	 */
	private static void nvccCompile(final File pCUFile,
																	final File pPTXFile) throws IOException
	{

		final InputStream lCudaHelper = JCudaUtils.class.getResourceAsStream("./kernels/helper_cuda.h");
		FileUtils.copyInputStreamToFile(lCudaHelper,
														new File(	pCUFile.getParent(),
																				"helper_cuda.h"));

		final InputStream lCudaMathHelper = JCudaUtils.class.getResourceAsStream("./kernels/helper_math.h");
		FileUtils.copyInputStreamToFile(lCudaMathHelper,
														new File(	pCUFile.getParent(),
																						"helper_math.h"));

		final InputStream lCudaStringHelper = JCudaUtils.class.getResourceAsStream("./kernels/helper_string.h");
		FileUtils.copyInputStreamToFile(lCudaStringHelper,
														new File(	pCUFile.getParent(),
																							"helper_string.h"));

		if (!pCUFile.exists())
		{
			throw new IOException("Input file not found: " + pCUFile.getName());
		}

		final String modelString = "-m" + System.getProperty("sun.arch.data.model");

		String lCompilerBinDir = "";

		if (System.getProperty("os.name").toLowerCase().contains("osx") || System.getProperty("os.name")
																																							.toLowerCase()
																																							.contains("os x"))
		{
			lCompilerBinDir = " --compiler-bindir=/opt/local/bin/gcc-mp-4.6";
		}

		final String command = getNVCCPath() + " -I. -I"
														+ pCUFile.getParentFile()
																			.getAbsolutePath()
														+ " "
														+ modelString
														+ lCompilerBinDir
														+ " -ptx "
														+ pCUFile.getAbsolutePath()
														+ " -o "
														+ pPTXFile.getAbsolutePath();

		final Process process = Runtime.getRuntime().exec(command);

		final String errorMessage = new String(IOUtils.toByteArray(process.getErrorStream()));
		final String outputMessage = new String(IOUtils.toByteArray(process.getInputStream()));

		int exitValue = 0;
		try
		{
			exitValue = process.waitFor();
		}
		catch (final InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for nvcc output",
														e);
		}

		if (exitValue != 0)
		{
			System.out.println("nvcc process exitValue " + exitValue);
			System.out.println("errorMessage:\n" + errorMessage);
			System.out.println("outputMessage:\n" + outputMessage);
			throw new IOException("Could not create .ptx file: " + errorMessage);
		}

	}

	/**
	 * Returns the path of he nvcc executable. TODO: be smarter and use the env
	 * variables on the system.
	 * 
	 * @return
	 */
	private static String getNVCCPath()
	{
		if (SystemUtils.IS_OS_MAC_OSX)
		{
			return "/Developer/NVIDIA/CUDA-6.0/bin/nvcc";
		}
		else if (SystemUtils.IS_OS_WINDOWS)
		{
			return "\"C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v5.0/bin/nvcc.exe\"";
		}
		return null;
	}




}
