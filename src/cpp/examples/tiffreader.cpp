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
#include <sstream>
#include <fstream>
#include <sys/mman.h>
#include <sys/types.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <assert.h>

#define T uint16_t

size_t getFileSize(string& filename) {
    struct stat st;
    stat(filename.c_str(), &st);
    return st.st_size;
}

void DummyHandler(const char* module, const char* fmt, va_list ap)
{
    // ignore errors and warnings (or handle them your own way)
}

TIFFReader::TIFFReader(string filename) {
    int z_start = 0;
    int z_end = -1;
   // vector<char> fbuffer(filesize);
    //file.read(&fbuffer[0], filesize);

   // stream.rdbuf()->pubsetbuf(&fbuffer[0], filesize);
   //
    TIFFSetWarningHandler(DummyHandler);

    TIFF* tif = TIFFOpen(filename.c_str(), "r");
    unsigned int dircount = 0;
    unsigned int width;
    unsigned int height;
    unsigned short nbits;
    unsigned samples;
    void* raster;

    if (tif) {
        TIFFGetField(tif, TIFFTAG_IMAGEWIDTH, &width);
        TIFFGetField(tif, TIFFTAG_IMAGELENGTH, &height);
        TIFFGetField(tif, TIFFTAG_BITSPERSAMPLE, &nbits);
        TIFFGetField(tif, TIFFTAG_SAMPLESPERPIXEL, &samples);
    } else {
        cerr <<  "Could not open TIFF file " << filename << "." << std::endl;
        return;
    }

    
    dircount = TIFFNumberOfDirectories(tif);

    this->dimensions.push_back(width);
    this->dimensions.push_back(height);
    this->dimensions.push_back(dircount);
    this->imageBitDepth = (unsigned int)nbits;

    cerr << "TIFF stats: " << width << "x" << height << "x" << dircount << ", " << nbits << " bits, " << samples << " spp" << endl;
    unsigned long bufferSize = dimensions[0] * dimensions[1] * dimensions[2];
    dataBuffer = new uint16[bufferSize];
    
    long StripSize =  TIFFStripSize(tif);
    long numStrips = TIFFNumberOfStrips(tif);
    
    tdata_t buf;
    tstrip_t strip;

    buf = _TIFFmalloc(StripSize*numStrips);

    for(int i = 0; i < dircount; i++) {
        long bytes = 0;
        TIFFSetDirectory(tif, i);

        for (strip = 0; strip < numStrips; strip++)
            bytes += TIFFReadEncodedStrip(tif, strip, (uint8*)buf+strip*StripSize, (tsize_t) -1);

        memcpy((uint8*)dataBuffer+i*bytes, buf, bytes);
    }    
    
    _TIFFfree(buf);

    TIFFClose(tif);
}

void TIFFReader::convertBuffer(unsigned int* buffer16, unsigned int size16, unsigned char* buffer8, unsigned int size8) {
    for(unsigned long src = 0, dest = 0; src < size16; dest++) {
        uint32 value16 = buffer16[src++];
        value16 = value16 + (buffer16[src++] << 8);
        buffer8[dest] = (unsigned int)(value16/257.0 + 0.5);
    }
}

TIFFReader::~TIFFReader() {
    delete[] dataBuffer;
}
