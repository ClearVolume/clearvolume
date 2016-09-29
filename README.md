# Welcome to *ClearVolume* #

[![Join the chat at https://gitter.im/ClearVolume/ClearVolume](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ClearVolume/ClearVolume?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status on master](https://travis-ci.org/ClearVolume/ClearVolume.svg?branch=master)](https://travis-ci.org/ClearVolume/ClearVolume) [![Build status](https://ci.appveyor.com/api/projects/status/9bvpkg91vqcr5v9h/branch/master?svg=true)](https://ci.appveyor.com/project/skalarproduktraum/clearvolume/branch/master)

![ClearVolume Logo](artwork/ClearVolumeLogo.png "Logo")

ClearVolume is a real-time live 3D visualization library designed for high-end volumetric microscopes such as SPIM and DLSM microscopes. With ClearVolume you can see live on your screen the stacks acquired by your microscope instead of waiting for offline post-processing to give you an intuitive and comprehensive view on your data. The biologists can immediately decide whether a sample is worth imaging. 

ClearVolume can easily be integrated into existing Java, C/C++, Python, or LabVIEW based microscope software. Moreover, it has a dedicated interface to MicroManager/OpenSpim/OpenSpin control software. In addition, it offers the possibility of remote viewing of volume data

## Wiki and Website

For up to date information about the project,
please go to the [website](http://clearvolume.github.io) or have a look at the [wiki](http://github.com/clearvolume/clearvolume/wiki/Home)

## Building ClearVolume

To build ClearVolume from source, run the following from a command line (if you are on Windows, replace all invocations of `./gradlew` with `gradlew.bat`):

```
./gradlew build
```

This will also run all of the unit and integration tests. If you do not wish to do that, run

```
./gradlew build -x test
```

If you are used to build software with Maven, please be aware that in contrast to Maven, Gradle does not automatically download the latest dependencies. If you want to force Gradle to refresh the dependency tree, run 

```
./gradlew --refresh-dependencies
```

## Building against local versions of ClearGL, ClearAudio, etc. 

If you want to develop against local versions of ClearGL, ClearAudio, etc., you need to setup a Gradle Multiproject build. We provide the configuration for that in the file `settings.gradle.multiproject`.

To complete this setup, make a new directory, e.g. called `ClearVolume-base`, then clone all projects into that directory, and link or copy the multiproject build file into this directory:

```
$ mkdir ClearVolume-base
$ cd ClearVolume-base
$ git clone https://github.com/ClearVolume/ClearVolume
$ git clone https://github.com/ClearVolume/ClearGL
$ git clone https://github.com/ClearVolume/ClearAudio
$ git clone https://github.com/ClearVolume/ClearCL
$ cp ClearVolume/settings.gradle.multiproject ./settings.gradle
```

It's important that the file resides in the root project folder (`ClearVolume-base`) and is named `settings.gradle` for Gradle to pick it up correctly.

Now to run a build against the local versions, run

```
./gradlew build -Plocal=true
```

e.g. from the `ClearVolume-base/ClearVolume` directory. Gradle will let you know it's using the local sources with the output `Using local clearX sources`.

This multiproject build can also be used from IDEs. Both Eclipse and IntelliJ support it natively -- you just need to point them to the settings.gradle file in the project import.
