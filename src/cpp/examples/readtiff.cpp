/**
 * ClearVolume TIFF Reading Example
 * 
 * This is an example of how to instanciate and use a ClearVolume renderer from C/C++.
 * This example will load an 8bit TIFF file and send it to ClearVolume for
 * display.
 * 
 * @author Ulrik Günther, Loic Royer 2014
 *
 */

#ifdef _WIN32
    #include <windows.h>
    #include <tchar.h>
#elif __APPLE__
    #include <CoreServices/CoreServices.h>
#endif
#include <iostream>
#include <thread>
#include <chrono>
extern "C" { 
#include "cvlib.h" 
}

#include "tiffreader.h"

using namespace std;

static string classpath;
static string tiff_file_path;

void run_clearvolume()
{
	// First we initialize the library and provide the location of the ClearVolume jar file.
	// the JVM location is determined automatically using the JAVA_HOME env var.
	int lReturnCode = begincvlib(const_cast<char*>(classpath.c_str()), OPENCL);
	if(lReturnCode!=0) 
	{
		cout << "Begin failed, return code=" << lReturnCode << endl;
        return;
	}

	cout << "Starting in-process and server ClearVolume\n";

	int lRendererID = 1;
	int lServerID = 2;

	// Creates an in-process renderer:
    cout << "Creating renderer... " << endl;
	if(createRenderer(lRendererID, 512, 512, 1, 512, 512)!=0) {
		cout << "ERROR while creating renderer \n";
        return;
    }
    cout << " done." << endl;

	// wait for the CV renderer to initialize
	this_thread::sleep_for(chrono::seconds(2));

	// Creates a network server:
    cout << "Creating server... ";
	if(createServer(lServerID)!=0)
		cout << "ERROR while creating server \n";
    cout << " done." << endl;

	// Sets the voxel dimensions in real units for in-process renderer
    cout << "Setting up volume units for renderer ... " << endl;
	if(setVoxelDimensionsInRealUnits(lRendererID,1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	// Sets the voxel dimensions in real units for in-process server
    cout << "Setting up volume units for server ... " << endl;
	if(setVoxelDimensionsInRealUnits(lServerID,  1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	// Information on volume sizes:
	int channel = 0;
	char* buffer;

    // read the tiff file
    TIFFReader *tr = new TIFFReader(tiff_file_path);

    cout << "Read tiff file \"" << tiff_file_path << "\" with dimensions ";
    for(unsigned int d: *(tr->getDimensions())) {
        cout << d << " ";
    }
    cout << endl;

    if(tr->getDimensions()->size() < 3) {
        cerr << "Sorry, but we need a TIFF stack, not just a 2D image!" << endl;
    }

    buffer = (char*)tr->getBuffer();
    cerr << "Buffer size is " << tr->getBufferSize() << endl;

    cout << "Starting sending volumes ... " << endl;

    // for demo purposes we say that the 'acquisition' periode is 100 ms: 
    double timeins = 1.0;

    // We set the current index and time for both in-process renderer and server:
    if(setVolumeIndexAndTime(lRendererID,1,timeins)!=0)
        cout << "ERROR while setting volume index and time in seconds (renderer)\n";
    if(setVolumeIndexAndTime(lServerID,  1,timeins)!=0)
        cout << "ERROR while setting volume index and time in seconds (server)\n";

    //We send the data to both the in-process renderer and server.
    if(send8bitUINTVolumeDataToSink(lRendererID, channel, buffer,
                tr->getBufferSize(),
                (*(tr->getDimensions()))[0],
                (*(tr->getDimensions()))[1],
                (*(tr->getDimensions()))[2]) != 0) {
        cout << "ERROR while sending volume! (renderer)\n";
    }
    if(send8bitUINTVolumeDataToSink(lServerID, channel, buffer,
                tr->getBufferSize(),
                (*(tr->getDimensions()))[0],
                (*(tr->getDimensions()))[1],
                (*(tr->getDimensions()))[2]) != 0) {
        cout << "ERROR while sending volume! (server)\n";
    }

    sleep(300);

    // we destroy both renderer and server
    if(destroyRenderer(lRendererID)!=0)
        cout << "ERROR while destroying renderer \n";
    if(destroyServer(lServerID)!=0)
        cout << "ERROR while destroying server \n";

	// closes the library
	endcvlib();
}

static void dummyCallback(void * info) {}

#ifdef __WINDOWS__
int _tmain(int argc, _TCHAR* argv[])
#else
int main(int argc, char** argv)
#endif
{
	if (argc >= 2) {
		tiff_file_path = argv[1];
	} else {
        fprintf(stderr, "Please give a TIFF file as argument!");
        return -1;
	}

    thread cv_thread(run_clearvolume);

#ifdef __APPLE__
    CFRunLoopRef loopRef = CFRunLoopGetCurrent();

    CFRunLoopSourceContext sourceContext = { 
        .version = 0, .info = NULL, .retain = NULL, .release = NULL,
        .copyDescription = NULL, .equal = NULL, .hash = NULL, 
        .schedule = NULL, .cancel = NULL, .perform = &dummyCallback };

    CFRunLoopSourceRef sourceRef = CFRunLoopSourceCreate(NULL, 0, &sourceContext);
    CFRunLoopAddSource(loopRef, sourceRef,  kCFRunLoopCommonModes);        
    CFRunLoopRun();
    CFRelease(sourceRef);
#endif

    cv_thread.join();

    return 0;
}

