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
static string tiff_dir_path;

vector<string>* get_timepoints_from_directory(string path, string name_filter = "") {
    vector<string>* files = new vector<string>();

    DIR* dir;
    struct dirent* entry;
    int error;

    if ((dir = opendir(path.c_str())) != NULL) {
        while ((entry = readdir(dir)) != NULL) {
            string filename = entry->d_name;
            if(filename.find(".tif") != string::npos) {
                if(name_filter != "" && filename.find(name_filter) == string::npos) {
                    continue;
                }
                files->push_back(path + "/" + entry->d_name);
            }
        }
        
        closedir(dir);
        return files;

    } else {
        /* could not open directory */
        perror ("");
        return files;
    }
}

long to_microseconds(struct timeval& time) {
    return ((unsigned long long)time.tv_sec * 1000000) + time.tv_usec;
}

bool timepoint_sort(const string &lhs, const string &rhs) {
    string file_lhs = lhs.substr(lhs.find_last_of("/")+1);
    string file_rhs = rhs.substr(rhs.find_last_of("/")+1);

    string lhs_tp = file_lhs.substr(file_lhs.find_first_of("TP")+2, file_lhs.find_last_of("_Ch")-file_lhs.find_first_of("TP"));
    string rhs_tp = file_rhs.substr(file_rhs.find_first_of("TP")+2, file_rhs.find_last_of("_Ch")-file_rhs.find_first_of("TP"));

    return stoi(lhs_tp) < stoi(rhs_tp);
}

void run_clearvolume()
{
    chrono::time_point<chrono::high_resolution_clock> start, end;
    chrono::duration<double, milli> elapsed_ms;

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
	if(createRendererWithTimeShiftAndChannels(lRendererID, 1024, 1024, 2, 1024, 1024, false, true)!=0) {
		cout << "ERROR while creating renderer \n";
        return;
    }
    cout << " done." << endl;

	// wait for the CV renderer to initialize
	this_thread::sleep_for(chrono::seconds(2));

	// Sets the voxel dimensions in real units for in-process renderer
    cout << "Setting up volume units for renderer ... " << endl;
	if(setVoxelDimensionsInRealUnits(lRendererID,1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	// Information on volume sizes:
	int channel = 0;
    int index = 1;
	short* buffer;

    unsigned int counter = 0;
    
    float green_channel_color[] = {0.0f, 1.0f, 0.0f, 1.0f};
    float red_channel_color[] = {1.0f, 0.0f, 0.0f, 1.0f};

    setChannelColor(lRendererID, 0, green_channel_color);
    //setChannelColor(lRendererID, 2, red_channel_color);

    vector<string> files = *get_timepoints_from_directory(tiff_dir_path, "green");
    sort(files.begin(), files.end(), timepoint_sort);

    cout << "File list:" << endl;
    for(string s: files) {
        cout << "\t" << s << endl;
    }

    for(string tiff_file_path: files) {
        counter++;

        if(counter%2 == 0) {
            channel = 1;
            cerr << tiff_file_path << ", red" << endl;
            index++;
        } else {
            cerr << tiff_file_path << ", green" << endl;
            channel = 0;
        }

        //cout << "Current file: " << tiff_file_path << endl;
        start = chrono::high_resolution_clock::now();
        // read the tiff file
        TIFFReader *tr = new TIFFReader(tiff_file_path);

        cerr << "Read tiff file \"" << tiff_file_path << "\" with dimensions ";
        for(unsigned int d: *(tr->getDimensions())) {
            cout << d << " ";
        }
        cout << endl;

        if(tr->getDimensions()->size() < 3) {
            cerr << "Sorry, but we need a TIFF stack, not just a 2D image!" << endl;
        }

        buffer = (short*)tr->getBuffer();
        end = chrono::high_resolution_clock::now();
        elapsed_ms = end-start;

        cout << "R: " << elapsed_ms.count() << "ms" << endl;
        cerr << "Buffer size is " << tr->getBufferSize()/1024.0f/1024.0f << endl;

        start = chrono::high_resolution_clock::now();
        // for demo purposes we say that the 'acquisition' periode is 100 ms: 
        double timeins = 1.0;

        // We set the current index and time for both in-process renderer and server:
        if(setVolumeIndexAndTime(lRendererID, index, timeins)!=0)
            cout << "ERROR while setting volume index and time in seconds (renderer)\n";

        //We send the data to both the in-process renderer and server.
        if(send16bitUINTVolumeDataToSink(lRendererID, channel, buffer,
                    tr->getBufferSize(),
                    (*(tr->getDimensions()))[0],
                    (*(tr->getDimensions()))[1],
                    (*(tr->getDimensions()))[2]) != 0) {
            cout << "ERROR while sending volume! (renderer)\n";
        }
        end = chrono::high_resolution_clock::now();
        elapsed_ms = end-start;

        cout << "S: " << elapsed_ms.count() << "ms" << endl;

        delete tr;

        setChannelColor(lRendererID, 0, green_channel_color);
//        setChannelColor(lRendererID, 1, green_channel_color);
//
        if(index == 1) {
            sleep(10);
        }
    }

    sleep(500);

    // we destroy both renderer and server
    if(destroyRenderer(lRendererID)!=0)
        cout << "ERROR while destroying renderer \n";
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
		tiff_dir_path = argv[1];
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

