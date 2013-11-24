package clearvolume.jcuda;

import static jcuda.driver.JCudaDriver.cuCtxDestroy;
import static jcuda.driver.JCudaDriver.cuDeviceGet;
import static jcuda.driver.JCudaDriver.cuGLCtxCreate;
import static jcuda.driver.JCudaDriver.cuInit;
import static jcuda.driver.JCudaDriver.cuModuleGetFunction;
import static jcuda.driver.JCudaDriver.cuModuleLoad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class JCudaUtils
{
	private static CUcontext sCUcontext;

	public static final CUfunction initCuda(final CUmodule pCUmodule,
																					final InputStream pInputStream,
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

		final File lPTXFile = nvccCompile(pInputStream,
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

	public static final void closeCuda()
	{
		cuCtxDestroy(sCUcontext);
	}

	private static File nvccCompile(final InputStream pInputStreamCUFile,
																	final Map<String, String> pCompiledParameters) throws IOException
	{
		final File lCUFile = File.createTempFile("jcuda", ".cu");
		final File lPTXFile = File.createTempFile("jcuda", ".ptx");

		final StringWriter writer = new StringWriter();
		IOUtils.copy(pInputStreamCUFile, writer, Charset.defaultCharset());
		String lCUFileString = writer.toString();

		if (pCompiledParameters != null)
			for (final Entry<String, String> lEntry : pCompiledParameters.entrySet())
			{
				final String lPattern = lEntry.getKey();
				final String lReplacement = lEntry.getValue();

				lCUFileString = lCUFileString.replaceAll(	lPattern,
																									lReplacement);
			}

		// System.out.println(lCUFileString);

		FileUtils.write(lCUFile, lCUFileString);

		nvccCompile(lCUFile, lPTXFile);

		return lPTXFile;
	}

	private static void nvccCompile(final File pCUFile,
																	final File pPTXFile) throws IOException
	{

		final InputStream lCudaHelper = JCudaUtils.class.getResourceAsStream("./kernels/helper_cuda.h");
		streamToFile(lCudaHelper, new File(	pCUFile.getParent(),
																				"helper_cuda.h"));

		final InputStream lCudaMathHelper = JCudaUtils.class.getResourceAsStream("./kernels/helper_math.h");
		streamToFile(lCudaMathHelper, new File(	pCUFile.getParent(),
																						"helper_math.h"));

		final InputStream lCudaStringHelper = JCudaUtils.class.getResourceAsStream("./kernels/helper_string.h");
		streamToFile(lCudaStringHelper, new File(	pCUFile.getParent(),
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

		final String errorMessage = new String(toByteArray(process.getErrorStream()));
		final String outputMessage = new String(toByteArray(process.getInputStream()));

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

		// System.out.println("Finished creating PTX file");

	}

	private static String getNVCCPath()
	{
		final String lOsNameString = System.getProperty("os.name");
		System.out.println("OS: " + lOsNameString);
		if (lOsNameString.toLowerCase().contains("osx") || lOsNameString.toLowerCase()
																																		.contains("mac"))
		{
			return "/Developer/NVIDIA/CUDA-5.0/bin/nvcc";
		}
		else if (lOsNameString.toLowerCase().contains("win"))
		{
			return "\"C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v5.0/bin/nvcc.exe\"";
		}
		return null;
	}

	/**
	 * Fully reads the given InputStream and returns it as a byte array
	 * 
	 * @param inputStream
	 *          The input stream to read
	 * @return The byte array containing the data from the input stream
	 * @throws IOException
	 *           If an I/O error occurs
	 */
	private static byte[] toByteArray(final InputStream inputStream) throws IOException
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte buffer[] = new byte[8192];
		while (true)
		{
			final int read = inputStream.read(buffer);
			if (read == -1)
			{
				break;
			}
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}

	public static final void streamToFile(final InputStream pInputStream,
																				final File pFile) throws IOException
	{
		int lBufferSize = Math.min(	10000000,
																pInputStream.available() / 10);
		lBufferSize = lBufferSize == 0 ? 1000 : lBufferSize;
		final InputStreamReader lInputStreamReader = new InputStreamReader(pInputStream);
		final BufferedReader lBufferedReader = new BufferedReader(lInputStreamReader,
																															lBufferSize);

		final BufferedWriter lBufferedFileWriter = new BufferedWriter(new FileWriter(pFile),
																																	lBufferSize);

		int c;
		while ((c = lBufferedReader.read()) != -1)
		{
			lBufferedFileWriter.write(c);
		}
		lBufferedReader.close();
		lBufferedFileWriter.close();
	}

}
