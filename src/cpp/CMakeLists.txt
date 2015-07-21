cmake_minimum_required(VERSION 3.2)

project (ClearVolumeC)

if( CMAKE_SIZEOF_VOID_P EQUAL 8 )
    MESSAGE( "64 bits compiler detected" )
    SET( EX_PLATFORM 64 )
    SET( EX_PLATFORM_NAME "x64" )
else( CMAKE_SIZEOF_VOID_P EQUAL 8 ) 
    MESSAGE( "32 bits compiler detected" )
    SET( EX_PLATFORM 32 )
    SET( EX_PLATFORM_NAME "x86" )
endif( CMAKE_SIZEOF_VOID_P EQUAL 8 )

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "build/output")

find_package(JNI)
find_package(TIFF)

set(WITH_LIBTIFF "$ENV{ENABLE_LIBTIFF}")
if (JNI_FOUND)
    message (STATUS "JNI found, using:")
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

# correctly detect C++11 support in compiler, see 
# http://www.guyrutenberg.com/2014/01/05/enabling-c11-c0x-in-cmake/
include(CheckCXXCompilerFlag)
CHECK_CXX_COMPILER_FLAG("-std=c++11" COMPILER_SUPPORTS_CXX11)
CHECK_CXX_COMPILER_FLAG("-std=c++0x" COMPILER_SUPPORTS_CXX0X)
if(COMPILER_SUPPORTS_CXX11)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
elseif(COMPILER_SUPPORTS_CXX0X)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++0x")
else()
        message(STATUS "The compiler ${CMAKE_CXX_COMPILER} has no C++11 support. Please use a different C++ compiler.")
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
