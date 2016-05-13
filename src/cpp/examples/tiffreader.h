//
/**
 * ClearVolume LibTIFF wrapper 
 * 
 * Provides an object-oriented and very, veeeeery slim interface to LibTIFF for
 * loading data.
 * 
 * @author Ulrik Günther 2015
 *
 */

#ifndef __tiffreader_h__
#define __tiffreader_h__

#include <iostream>
#include <vector>
#include <deque>
#include <string>
#include <sstream>

#include <tiffio.h>
#include <tiffio.hxx>

using namespace std;

class TIFFReader {
public:
    TIFFReader(string filename);
    ~TIFFReader();
    vector<unsigned long>* getDimensions() { return &dimensions; };
    unsigned int getBitDepth() { return imageBitDepth; };
    void* getBuffer() { return dataBuffer; };
    unsigned long getBufferSize() { return dimensions[0] * dimensions[1] * dimensions[2] * imageBitDepth/8; }
    bool isValid() { return this->valid; };
    
protected:
    void* dataBuffer;
    vector<unsigned long> dimensions;
    unsigned int imageBitDepth;
    
    bool valid = true;
    
    void convertBuffer(unsigned int* buffer16, unsigned int size16, unsigned char* buffer8, unsigned int size8);
};

#endif /* defined(__tiffreader_h__) */
