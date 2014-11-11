# ClearVolume #

![ClearVolumeLogo512_crop.png](https://bitbucket.org/repo/GXoqjE/images/845422319-ClearVolumeLogo512_crop.png)

ClearVolume is a real-time live 3D visualisation library designed for high-end volumetric microscopes such as SPIM and DLSM microscopes. With ClearVolume you can see live on your screen the stacks acquired by your microscope instead of waiting for offline post-processing to give view an intuitive and comprehensive view on your data. The biologists can immediately decide whether a sample is worth imaging. 

ClearVolume can easily be integrated into existing Java, C, or LabView based microscope software. Moreover, it has a dedicated interface to MicroManager/OpenSpim/OpenSpin control software. In addition, it offers the possibility of remote viewing of volume data

### Prerequisites:

 1. Java JDK or JRE version 8 (1.8)  
 2. Gradle 2.1 (get it [here](http://www.gradle.org/downloads))
 3. NVidia graphics card (for now)
 4. CUDA SDK 6.5 (get it [here](http://developer.nvidia.com/cuda-downloads))
 5. On windows you need VisualStudio installed (get it [here](http://www.visualstudio.com/downloads/download-visual-studio-vs#d-express-windows-desktop))

Notes:
 
  a. On windows, if you use an 'express' version of VisualStudio, you need to copy and rename the file 'vcvarsx86_amd64.bat' located in folder 'Microsoft Visual Studio 12.0\VC\bin\x86_amd64'  to 'vcvars64.bat' that should be placed at the root of the VisualStudio folder e.g. 'C:\Program Files (x86)\Microsoft Visual Studio 12.0'. run gradle test an check for messages that suggest that that file could not be found.
  b. We hope to soon have a shader-based fall-back pipeline for maximal compatibility.

### How to build project with Gradle

* Get Gradle [here](http://www.gradle.org/)

* Go to the project folder root and run:

     ./build.sh

This will generate a jar executable here:

    ./build/executable/ClearVolume.exe.jar

(It uses the magic Capsule plugin for Gradle for that)

### Quick demo:

Start the demo server:

    ./build/executable/ClearVolume.exe.jar -demoserver > log.txt &

Start the network client:

    ./build/executable/ClearVolume.exe.jar

Connect to the server on localhost. Et Voila!
You should see a high-speed 3D Volume stream
displayed.



### How do I integrate into my control software? ###

There are two possibilities for integration:
1.  Network    : ClearVolume receives data over the network via streaming
2.  In-Process: ClearVolume lives in the same process and thus data can be transferred at maximal speed.
It is possible to have both: in-process with ClearVolume server listening for incoming connections, this mode
offers the possibility to monitor long-term time-lapses remotely.

* Integration onto Java-based microscope control software

The API relies on the metaphor of volume (or stacks) sinks and sources.
The following code creates a ClearVolume renderer, wraps it with an asynchronous sink,
and repeatedly updates the volume data:


```
#!java

int lMaxInUseVolumes = 20; 
VolumeManager lVolumeManager = new VolumeManager(lMaxInUseVolumes);


try (final ClearVolumeRendererInterface lClearVolumeRenderer =
       ClearVolumeRendererFactory.newBestRenderer("ClearVolumeTest",
                                                  pWindowSize,
                                                  pWindowSize,
                                                  pBytesPerVoxel))
 {
      lClearVolumeRenderer.setTransfertFunction(TransfertFunctions.getGrayLevel());
      ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(lClearVolumeRenderer,
                                                                                     cMaxMillisecondsToWaitForCopy,
                                                                                     TimeUnit.MILLISECONDS);

      AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(lClearVolumeRendererSink,
                                                                                                       cMaxQueueLength,
                                                                                                       cMaxMillisecondsToWait,
                                                                                                       TimeUnit.MILLISECONDS);

      lAsynchronousVolumeSinkAdapter.start();

      for(int i=0; i<1000; i++)
      {
        // send volume data:

        Volume<?> lVolume = mVolumeManager.requestAndWaitForNextAvailableVolume(1,
                                                                                TimeUnit.MILLISECONDS, 
                                                                                Byte.class, 
                                                                                1,
                                                                                128,
                                                                                128,
                                                                                128);

        // Here update the contents of the ByteBuffer provided by lVolume.getVolumeData()
        // ... bla bla ...
      
        // send new volume to ClearVolume
        mVolumeSink.sendVolume(lVolume);
      }

      lAsynchronousVolumeSinkAdapter.stop();

}
```


* Integration onto LabView-based microscope control software

* Integration onto C-based microscope control software

the cvlib native library for windows is generated in the build folder: .build/cvlib/
together with a test executable. Try it:

    ./build/cvlib/cvlib_test.exe


### Contribution guidelines ###

* torture of animals with the exception of bugs is prohibited
* exhibit openness of mind towards idiosyncratic ideas
* be nice

### Contributors ###

* Loic Royer (royer -at- mpi-cbg -point- de)
* Martin Weigert (mweigert -at- mpi-cbg -point- de)
* Ulrik Guenther (guenther -at- mpi-cbg -point- de)