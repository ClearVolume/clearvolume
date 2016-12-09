package clearvolume.renderer.opencl.test;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLImage3D;
import com.nativelibs4java.opencl.CLImageFormat;
import com.nativelibs4java.opencl.CLKernel;

import clearcl.ClearCLBuffer;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearvolume.renderer.opencl.OpenCLAvailability;
import clearvolume.renderer.opencl.OpenCLDevice;

public class OpenCLTests
{

  @Test
  public void test_creation()
  {
    if (!OpenCLAvailability.isOpenCLAvailable())
      return;

    try
    {
      final OpenCLDevice dev = new OpenCLDevice();
      dev.initCL();
      dev.printInfo();

      final int N = 512;

      // create the buffer/image type we would need for the renderer

      final ClearCLBuffer clBufIn = dev.createInputFloatBuffer(N);
      final ClearCLBuffer clBufOut = dev.createOutputIntBuffer(N);

      final ClearCLImage img = dev.createGenericImage3D(N,
                                                        N,
                                                        N,
                                                        ImageChannelDataType.SignedInt16);
    }
    catch (final Throwable e)
    {

      fail();
      e.printStackTrace();
    }

  }

  @Test
  public void test_compile()
  {
    if (!OpenCLAvailability.isOpenCLAvailable())
      return;

    try
    {
      final OpenCLDevice lOpenCLDevice = new OpenCLDevice();
      lOpenCLDevice.initCL();
      lOpenCLDevice.printInfo();

      final ClearCLKernel lCLKernel =
                                    lOpenCLDevice.compileKernel(OpenCLTests.class,
                                                                "kernels/test.cl",
                                                                "test_char");
    }
    catch (final Throwable e)
    {
      e.printStackTrace();
      fail();
    }

  }

  @Test
  public void test_run()
  {
    if (!OpenCLAvailability.isOpenCLAvailable())
      return;

    try
    {
      final OpenCLDevice lOpenCLDevice = new OpenCLDevice();
      lOpenCLDevice.initCL();
      lOpenCLDevice.printInfo();
      final int N = 100;

      final ClearCLKernel lCLKernel =
                                    lOpenCLDevice.compileKernel(OpenCLTests.class,
                                                                "kernels/test.cl",
                                                                "test_float");

      final ClearCLBuffer clBufIn =
                                  lOpenCLDevice.createOutputFloatBuffer(N);

      lCLKernel.setArguments(clBufIn, N);

      lOpenCLDevice.run(lCLKernel, N);

    }
    catch (final Throwable e)
    {
      e.printStackTrace();
      fail();
    }

  }
}
