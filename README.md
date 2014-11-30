# Welcome to the *ClearVolume* Wiki #

![ClearVolume Logo](https://bitbucket.org/clearvolume/clearvolume/raw/master/artwork/ClearVolumeLogo.png "Logo")

ClearVolume is a real-time live 3D visualization library designed for high-end volumetric microscopes such as SPIM and DLSM microscopes. With ClearVolume you can see live on your screen the stacks acquired by your microscope instead of waiting for offline post-processing to give you an intuitive and comprehensive view on your data. The biologists can immediately decide whether a sample is worth imaging. 

ClearVolume can easily be integrated into existing Java, C/C++, Python, or LabVIEW based microscope software. Moreover, it has a dedicated interface to MicroManager/OpenSpim/OpenSpin control software. In addition, it offers the possibility of remote viewing of volume data

----
[TOC]

----

## Prerequisites

* Java JDK or JRE version 8 (1.8) (get it [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)) 
* Gradle 2.1 (get it [here](http://www.gradle.org/downloads))
* Requirements for either the CUDA or OpenCL backend

#### CUDA backend

 * NVidia graphics card
 * CUDA SDK 6.5 (get it [here](http://developer.nvidia.com/cuda-downloads))
 * On windows you need VisualStudio installed (get it [here](http://www.visualstudio.com/downloads/download-visual-studio-vs#d-express-windows-desktop))
 If you use an 'express' version of VisualStudio, you need to copy and rename the file 'vcvarsx86_amd64.bat' located in folder 'Microsoft Visual Studio 12.0\VC\bin\x86_amd64'  to 'vcvars64.bat' that should be placed at the root of the VisualStudio folder e.g. 'C:\Program Files (x86)\Microsoft Visual Studio 12.0'. run gradle test an check for messages that suggest that that file could not be found.

#### OpenCL backend

 * OpenCL 1.2 capable graphics card with preferably at least 1G of GPU RAM.

####LabVIEW bindings

 * LabVIEW 2012 64 bit. 
 
 Note: LabVIEW users can regenerate the LabVIEW bindings from almost any LabVIEW 64bit version using the LabVIEW wizard for interfacing with native C/C++ libraries. Unfortunately 32 bit is out of the questions in the 21st century -  and makes little sense when handling volume data anyway...
 
## Installing

1. Get the code by either checking out the project using Git

        git clone https://bitbucket.org/clearvolume/clearvolume.git
        
    or go to the [download page](https://bitbucket.org/clearvolume/clearvolume/downloads) and download the whole repository. 

    This download will only provide you with the source code. To obtain usable binaries, you need to follow the instructions below  or have a look at the other downloads provided (for C/C++ and LabVIEW users).

2. Go to the project folder root and run (that steps needs Gradle):

     Linux/OSX:

        ./build.sh

     Windows:

        build.bat

    This will generate a jar executable here:

        ./build/executable/ClearVolume.exe.jar

     (It uses the magic Capsule plugin for Gradle for that)

## Simple demo

### Accessing our demo dataset over the network 

To demonstrate the remote rendering capabilities, there is a data server running on

`clearvolume.mpi-cbg.de`
  
which you can use for a quick demonstration.
 
First, start the network client (Linux or OSX):

    ./build/executable/ClearVolume.exe.jar
    
In windows you might have to type instead:
    
    java -jar ClearVolume.exe.jar
    
You can also simply double click on the ClearVolume.exe.jar file (OSX, Linux)    

Now point the Client to the url mentioned above (that should be the default):
 
![Smaller icon](https://bitbucket.org/clearvolume/clearvolume/raw/master/ClearVolumeClient_small.png "ClearVolume Client ")

and you should see some nice mitotic division of *C. Elegans* cells:

![Smaller icon](https://bitbucket.org/clearvolume/clearvolume/raw/master/Screenshot_worms.png "Screenshot ")


### start a demo server locally

Start the demo server (Linux or OSX):

    ./build/executable/ClearVolume.exe.jar --demo-server > log.txt &

Start the network client (Linux or OSX):

    ./build/executable/ClearVolume.exe.jar
    
In windows you might have to type instead:
    
    java -jar ClearVolume.exe.jar
    
You can also simply double click on the ClearVolume.exe.jar file (OSX, Linux)    

Connect to the server on localhost

![Client](https://bitbucket.org/clearvolume/clearvolume/raw/master/ClearVolumeClient_localhost_small.png "ClearVolume Client ")

 ![ClearVolume Demo](https://bitbucket.org/clearvolume/clearvolume/raw/master/Demo_Screenshot.png "Demo")

## ClearVolume network client

The ClearVolume network client allows you to connect to your ClearVolume enabled microscope software from anywhere in the planet where your internet connection is fast enough. In the worst case scenario it just takes a bit of time to download the data... Since the rendering is happening on the client, once you have a stack/volume on your client computer you can rotate and zoom virtually all you want.
The ClearVolume network client (ClearVolume.exe.jar) is also available for download [here](https://bitbucket.org/clearvolume/clearvolume/downloads)

![Smaller icon](https://bitbucket.org/clearvolume/clearvolume/raw/master/ClearVolumeClient_small.png "ClearVolume Client ")

You need to provide a network address, decide on a window size. Importantly you need to specify the  type of data: typically 16 bit and 1 color. If you provide the wrong information the client will return an error. We will try to improve this in a future version.
If you want to use the time-shift and channel filter features just tick the mark. Note: remote time-shift only works with stacks received by the client.


## How do I integrate *ClearVolume* into my control software? 

One of ClearVolume key feature is to make it easy and painless to integrate into
different systems. For this purpose we have made sure that both the APIs are simple and
elegant as well as to provide bindings for all languages that we know are used for 
microscope control, and in particular light-sheet microscopes.

ClearVolume comes with bindings with all major languages used for developing
microscope control software: Python, Java, C/C++, LabVIEW.
There are two types possibilities for integration:

1.  Network    : ClearVolume receives data over the network via streaming
2.  In-Process: ClearVolume lives in the same process and thus data can be transferred at maximal speed.

It is possible to have both: in-process with ClearVolume server listening for incoming connections, this mode offers the possibility to monitor long-term time-lapses remotely.

Different integration guides can be found here:

- Java    integration guide [[IntegrationJava]]
- C/C++   integration guide [[IntegrationCCPP]]
- LabVIEW integration guide [[IntegrationLabVIEW]]
- Python  integration guide [[IntegrationPython]]

## OpenSPIM and ClearVolume

[OpenSPIM](http://openspim.org/) has democratized light-sheet microscopy and has made it possible for many labs around the world to build high-quality and cheap light-sheet microscopes. To support this effort we integrated ClearVolume into the OpenSPIM plugin for [μManager](http://www.micro-manager.org/). Go to this page [[UsageOpenSPIM]] for details on how you can use ClearVolume with OpenSPIM. If you are interested in how this integration was done, have a look at [[IntegrationOpenSPIM]].

## System Internals and Guide for Devs

ClearVolume is an open project and we welcome contributions, collaborations and integration with other projects. The entry-point for more advanced informations and guides is here: [[SystemInternals]]

## ClearVolume Wiki-based manual
The [ClearVolume wiki](https://bitbucket.org/clearvolume/clearvolume/wiki/Home) has detailed information on how to integrate, use and develop with ClearVolume.


## Contribution guidelines 

* torture of animals with the exception of bugs is prohibited
* exhibit openness of mind towards idiosyncratic ideas
* be nice


## Contributors 

* Loic Royer (royer -at- mpi-cbg -point- de)
* Martin Weigert (mweigert -at- mpi-cbg -point- de)
* Ulrik Guenther (guenther -at- mpi-cbg -point- de)