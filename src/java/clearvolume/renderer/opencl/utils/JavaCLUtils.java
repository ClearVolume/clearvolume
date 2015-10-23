package clearvolume.renderer.opencl.utils;

import static org.bridj.Pointer.pointerToSizeTs;

import java.lang.reflect.Method;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage;
import com.nativelibs4java.opencl.CLQueue;

public class JavaCLUtils
{
	public static Method sWriteMethod;
	static
	{
		try
		{
			sWriteMethod = CLImage.class.getDeclaredMethod(	"write",
															CLQueue.class,
															Pointer.class,
															Pointer.class,
															long.class,
															long.class,
															Pointer.class,
															boolean.class,
															CLEvent[].class);
			sWriteMethod.setAccessible(true);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

	public static Method sReadMethod;
	static
	{
		try
		{
			sReadMethod = CLImage.class.getDeclaredMethod(	"read",
															CLQueue.class,
															Pointer.class,
															Pointer.class,
															long.class,
															long.class,
															Pointer.class,
															boolean.class,
															CLEvent[].class);
			sReadMethod.setAccessible(true);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

	public static CLEvent readImage3D(	CLImage pCLImage,
										CLQueue pCLQueue,
										Pointer<?> pPointer,
										long pX,
										long pY,
										long pZ,
										long pWidth,
										long pHeight,
										long pDepth,
										boolean pBlocking)
	{
		try
		{
			return (CLEvent) JavaCLUtils.sReadMethod.invoke(pCLImage,
															pCLQueue,
															pointerToSizeTs(pX,
																			pY,
																			pZ),
															pointerToSizeTs(pWidth,
																			pHeight,
																			pDepth),
															0L,
															0L,
															pPointer,
															pBlocking,
															new CLEvent[0]);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static CLEvent writeImage3D(	CLImage pImageCL,
										CLQueue pCLQueue,
										Pointer<?> pPointer,
										long pX,
										long pY,
										long pZ,
										long pWidth,
										long pHeight,
										long pDepth,
										boolean pBlocking)
	{
		try
		{
			return (CLEvent) JavaCLUtils.sWriteMethod.invoke(	pImageCL,
																pCLQueue,
																pointerToSizeTs(pX,
																				pY,
																				pZ),
																pointerToSizeTs(pWidth,
																				pHeight,
																				pDepth),
																0L,
																0L,
																pPointer,
																pBlocking,
																new CLEvent[0]);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
