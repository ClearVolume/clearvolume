cmake_minimum_required(VERSION 3.2)

project (ClearVolumeC)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "build/output")

find_package(JNI)
find_package(TIFF)

set(WITH_LIBTIFF "$ENV{ENABLE_LIBTIFF}")
if (JNI_FOUND)
    message (STATUS "JNI found, using:")
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

if (TIFF_FOUND AND WITH_LIBTIFF)
    message (STATUS "Building with LibTIFF support")
    include_directories(${TIFF_INCLUDE_DIRS})
else()
    message (STATUS "LibTIFF not found or disabled, building only non-LibTIFF examples.")
endif()

include_directories(${JNI_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_library(clearvolume SHARED src/cvlib.c)
target_link_libraries(clearvolume ${JNI_LIBRARIES})

# examples
## 1. Simple
add_executable(Example_Simple examples/simple.cpp)
target_link_libraries(Example_Simple clearvolume)
IF (APPLE)
   FIND_LIBRARY(COREFOUNDATION_LIBRARY CoreFoundation)
   target_link_libraries(Example_Simple ${COREFOUNDATION_LIBRARY})
ENDIF (APPLE)

# libtiff-dependent examples
# 2. readtiff
if (TIFF_FOUND AND WITH_LIBTIFF)
    add_executable(Example_ReadTIFF examples/readtiff.cpp examples/tiffreader.cpp)
    target_compile_features(Example_ReadTIFF PRIVATE cxx_enum_forward_declarations)
    target_link_libraries(Example_ReadTIFF clearvolume)
    target_link_libraries(Example_ReadTIFF ${TIFF_LIBRARIES})
    IF (APPLE)
        FIND_LIBRARY(COREFOUNDATION_LIBRARY CoreFoundation)
        target_link_libraries(Example_ReadTIFF ${COREFOUNDATION_LIBRARY})
    ENDIF (APPLE)
endif()
