# ClearVolume #

![ClearVolumeLogo512_crop.png](https://bitbucket.org/repo/GXoqjE/images/845422319-ClearVolumeLogo512_crop.png)

ClearVolume is a real-time live 3D visualisation library designed for high-end volumetric microscopes such as SPIM and DLSM microscopes. With ClearVolume you can see live on your screen the stacks acquired by your microscope instead of waiting for offline post-processing to give view an intuitive and comprehensive view on your data. The biologists can immediately decide whether a sample is worth imaging. 

ClearVolume can easily be integrated into existing Java, C/C++, or LabVIEW based microscope software. Moreover, it has a dedicated interface to MicroManager/OpenSpim/OpenSpin control software. In addition, it offers the possibility of remote viewing of volume data

--------    

[TOC]

--------

## Installing
### Prerequisites

 1. Java JDK or JRE version 8 (1.8)  
 2. Gradle 2.1 (get it [here](http://www.gradle.org/downloads))

#### For CUDA backend

 1. NVidia graphics card (for now)
 2. CUDA SDK 6.5 (get it [here](http://developer.nvidia.com/cuda-downloads))
 3. On windows you need VisualStudio installed (get it [here](http://www.visualstudio.com/downloads/download-visual-studio-vs#d-express-windows-desktop))
 If you use an 'express' version of VisualStudio, you need to copy and rename the file 'vcvarsx86_amd64.bat' located in folder 'Microsoft Visual Studio 12.0\VC\bin\x86_amd64'  to 'vcvars64.bat' that should be placed at the root of the VisualStudio folder e.g. 'C:\Program Files (x86)\Microsoft Visual Studio 12.0'. run gradle test an check for messages that suggest that that file could not be found.

#### For OpenCL backend:

 1. OpenCL 1.2 capable graphics card with preferably at least 1G of GPU RAM.

#### For LabVIEW bindings:

 1. LabVIEW 2012 64 bit. 

### Building the project with Gradle

* Get Gradle [here](http://www.gradle.org/)

* Go to the project folder root and run:

     ./build.sh

This will generate a jar executable here:

    ./build/executable/ClearVolume.exe.jar

(It uses the magic Capsule plugin for Gradle for that)

## Quick demo

Start the demo server:

    ./build/executable/ClearVolume.exe.jar --demo-server > log.txt &

Start the network client:

    ./build/executable/ClearVolume.exe.jar

Connect to the server on localhost. Et Voila! You should see a high-speed 3D Volume stream
displayed.

Consult the [KeyShortcuts Page](KeyShortcuts) for how to adjust the gamma value and other parameters of the rendering.   



## How do I integrate into my control software?

ClearVolume comes with bindings with al major languages used for developing
microscope control software: Python, Java, C/C++, LabVIEW.
There are two types possibilities for integration:
1.  Network    : ClearVolume receives data over the network via streaming
2.  In-Process: ClearVolume lives in the same process and thus data can be transferred at maximal speed.
It is possible to have both: in-process with ClearVolume server listening for incoming connections, this mode
offers the possibility to monitor long-term time-lapses remotely.

## ClearVolume Wiki-based manual

The [ClearVolume wiki](http://bitbucket.org/clearvolume/clearvolume/wiki/Home) has detailed information on how to
integrate, use and develop with ClearVolume.

### Contribution guidelines

* torture of animals with the exception of bugs is prohibited
* exhibit openness of mind towards idiosyncratic ideas
* be nice

### Contributors

* Loic Royer (royer -at- mpi-cbg -point- de)
* Martin Weigert (mweigert -at- mpi-cbg -point- de)
* Ulrik Guenther (guenther -at- mpi-cbg -point- de)