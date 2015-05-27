//
//  SPIMStackReader.h
//  dive
//
//  Created by Ulrik Guenther on 13/05/14.
//  Copyright (c) 2014 ulrik.is. All rights reserved.
//

#ifndef __tiffreader_h__
#define __tiffreader_h__

#include <iostream>
#include <vector>
#include <deque>
#include <string>

#include <tiffio.h>

using namespace std;

enum class BitDepth {Grayscale8, Grayscale16, RGB8, RGB16};

class TIFFReader {
public:
    TIFFReader(string filename);
    vector<unsigned int>* getDimensions() { return &dimensions; };
    BitDepth getBitDepth();
    unsigned char* getBuffer() { return dataBuffer.data(); };
    unsigned int getBufferSize() { return dataBuffer.size(); };
    vector<unsigned char>* getVector() { return &dataBuffer; };
    
    bool isValid() { return this->valid; };
    
protected:
    vector<unsigned char> dataBuffer;
    vector<unsigned int> dimensions;
    BitDepth imageBitDepth;
    
    bool valid = true;
    
    void convertBuffer(unsigned int* buffer16, unsigned int size16, unsigned char* buffer8, unsigned int size8);
};

#endif /* defined(__tiffreader_h__) */
