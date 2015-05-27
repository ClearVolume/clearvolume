/**
 * ClearVolume LibTIFF wrapper 
 * 
 * Provides an object-oriented and very, veeeeery slim interface to LibTIFF for
 * loading data.
 * 
 * @author Ulrik Günther 2015
 *
 */

#include "tiffreader.h"
#include <algorithm>

TIFFReader::TIFFReader(string filename) {
    TIFF* tif = TIFFOpen(filename.c_str(), "r");
    int dircount = 0;
    uint32 width;
    uint32 height;
    unsigned short nbits;
    unsigned short samples;
    uint32* raster;
    
    if (tif) {
        TIFFGetField(tif, TIFFTAG_IMAGEWIDTH, &width);
        TIFFGetField(tif, TIFFTAG_IMAGELENGTH, &height);
        TIFFGetField(tif, TIFFTAG_BITSPERSAMPLE, &nbits);
        TIFFGetField(tif, TIFFTAG_SAMPLESPERPIXEL, &samples);
        
        
        if (samples == 3 && nbits == 8) {
            this->imageBitDepth = BitDepth::RGB8;
        } else if(samples == 3 && nbits == 16) {
            this->imageBitDepth = BitDepth::RGB16;
        } else if(samples == 1 && nbits == 8) {
            this->imageBitDepth = BitDepth::Grayscale8;
        } else if(samples == 1 && nbits == 16) {
            this->imageBitDepth = BitDepth::Grayscale16;
        } else {
            cerr <<  "Unknown image type!" << endl;
        }

        do {
            dircount++;
        } while (TIFFReadDirectory(tif));
        cerr << "Found " << dircount << " directories/slices in " << filename << endl;
    } else {
        cerr << "Could not open TIFF file." << endl;
        this->valid = false;
        return;
    }
    this->dimensions.push_back(width);
    this->dimensions.push_back(height);
    this->dimensions.push_back(dircount);
    
    cerr << "Image dimensions: " << width << "x" << height << "x" << samples << "Sx" << nbits << "Bx" << dircount << "SL" << endl;
    
    uint32 imageSize = width*height*sizeof(uint32);
    dataBuffer.reserve(imageSize*dircount);
    
    cout << "TIFFReader: Reading " << dircount << " slices";
    for(int i = 0; i < dircount; i++) {
        TIFFSetDirectory(tif, i);
        raster = new uint32[imageSize];
        
        TIFFReadRGBAImageOriented(tif, width, height, raster, ORIENTATION_TOPLEFT, 0);
        std::copy(raster, raster+imageSize, back_inserter(dataBuffer));
        
        cout << ".";
        
        delete[] raster;
    }
    
    cerr << "Buffer size: " << dataBuffer.size()/1024.0f/1024.0f << "M" << endl;
    
    TIFFClose(tif);
}

void TIFFReader::convertBuffer(unsigned int* buffer16, unsigned int size16, unsigned char* buffer8, unsigned int size8) {
    for(unsigned long src = 0, dest = 0; src < size16; dest++) {
        uint32 value16 = buffer16[src++];
        value16 = value16 + (buffer16[src++] << 8);
        buffer8[dest] = (unsigned int)(value16/257.0 + 0.5);
    }
}
