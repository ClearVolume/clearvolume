Interfacing ClearVolume with C/C++ code
=======================================

## Prerequisites

### General

* current JDK version (1.8.0+)
* current CMake version (3.2+)

### Mac OS X

* Xcode Command Line Tools

### Windows

* Microsoft Visual Studio 2012+ (Express Edition is sufficient)


## Building

1. open a command line window (e.g. Terminal on OSX, cmd or PowerShell on Windows) and change to ClearVolume/src/cpp directory
2. create a directory named `build` by running either `mkdir build` (OSX and Linux) or `md build` (Windows) and go to that directory
3. run `cmake ..` - this will set up the build environment and check if all prerequisites are met - for Windows you might need to run e.g. `cmake .. -G"Visual Studio 12 2012 x64" to select the correct generator for CMake
4. on OSX and Linux, run `make`, on Windows open the created Visual Studio Solution files and run a build
5. the ClearVolume C library will be created, as will the examples. On Linux and OSX, they are found in `build`, on Windows in `build\Debug`.
6. copy the ClearVolume JAR and all dependencies to a directory named `jars` in the directory with the build products
