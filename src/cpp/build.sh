#!/bin/sh

CC=clang
CXX=clang++
JAVA_INCLUDES="-I/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/include -I/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/include/darwin"
JAVA_LIBS="-L/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/jli"
LIB_LINKER_FLAGS="-dynamiclib -undefined suppress $JAVA_INCLUDES -flat_namespace"
LINKER_FLAGS="-L./ $JAVA_LIBS -lclearvolume $JAVA_INCLUDES -ljli"

echo "Building shared library..."

$CC $LIB_LINKER_FLAGS cvlib.cpp -o libclearvolume.dylib

echo "Building example application... "
$CXX $LINKER_FLAGS cvlib_test.cpp -o cvlib_test

echo "Done :-)"
