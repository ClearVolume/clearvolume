#!/bin/sh

CC=clang
CXX=clang++
JAVA_INCLUDES="-I/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/include -I/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/include/darwin"
JAVA_LIBS="-L/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/jli"
LIB_LINKER_FLAGS="-dynamiclib -g -undefined suppress $JAVA_INCLUDES -flat_namespace"
LINKER_FLAGS="-L./ $JAVA_LIBS  -Wl,-weak_library,libclearvolume.dylib -undefined dynamic_lookup $JAVA_INCLUDES -ljli -framework CoreFoundation"

echo "Building shared library..."

$CC $LIB_LINKER_FLAGS -std=c99 cvlib.c -o libclearvolume.dylib

echo "Building example application... "
$CXX $LINKER_FLAGS cvlib_test.cpp -g -o cvlib_test

echo "Done :-)"
