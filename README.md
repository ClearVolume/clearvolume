# ClearVolume #

ClearVolume is a real-time live 3D visualisation library designed for high-end volumetric microscopes such as SPIM and DLSM microscopes. With ClearVolume you can see live on your screen the stacks acquired by your microscope instead of waiting for offline post-processing to give view an intuitive and comprehensive view on your data. The biologists can immediately decide whether a sample is worth imaging. 

ClearVolume can easily be integrated into existing Java, C, or LabView based microscope software. Moreover, it has a dedicated interface to MicroManager/OpenSpim/OpenSpin control software. In addition, it offers the possibility of remote viewing of volume data

### Prerequisites:

 1- Java JDK or JRE version 8 (1.8)  
 2- Gradle 2.1
 3- NVidia graphics card 
 4- CUDA SDK 6.5
 
We hope to soon have a shader-based fall-back pipeline for maximal compatibility.

### How to build project with Gradle

* Get Gradle [here](http://www.gradle.org/)

* Go to the project folder root and run:

     ./build.sh

This will generate a jar executable at the root of the project:

     ClearVolume.exe.jar

(It uses the magic Capsule plugin for Gradle for that)

If the

### Quick demo:

Start the demo server:

    ./ClearVolume.exe.jar -demoserver > log.txt &

Start the network client (preferably in another shell)

   ./ClearVolume.exe.jar

Connect to the server on localhost. Et Voila!
You should see a high-speed 3D Volume stream
displayed.

### How do I integrate into my control software? ###

* Integration onto Java-based microscope control software

* Integration onto LabView-based microscope control software

* Integration onto C-based microscope control software

### Contribution guidelines ###

* Writing tests
* Code review
* Other guidelines

### Contributors ###

* Loic Royer (royer -at- mpi-cbg -point- de)
* Martin Weigert (mweigert -at- mpi-cbg -point- de)
* Ulrik Guenther (guenter -at- mpi-cbg -point- de)