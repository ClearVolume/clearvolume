cmake_minimum_required(VERSION 2.8)

project (ClearVolumeC)
set(CMAKE_BUILD_TYPE Release)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "build/output")

find_package(JNI)

if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()

include_directories(${JNI_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_library(clearvolume SHARED src/cvlib.c)
target_link_libraries(clearvolume ${JNI_LIBRARIES})

add_executable(Example_Simple examples/simple.cpp)
target_link_libraries(Example_Simple clearvolume)
IF (APPLE)
   FIND_LIBRARY(COREFOUNDATION_LIBRARY CoreFoundation)
   target_link_libraries(Example_Simple ${COREFOUNDATION_LIBRARY})
ENDIF (APPLE)


