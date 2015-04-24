/**
 * ClearVolume native interface example test
 * 
 * This is an example of how to instanciate and use a ClearVolume renderer from C/C++.
 * In addition this example also instanciates a ClearVolume server. You can connect
 * to the server using the ClearVolume client: ClearVolume.exe.jar -c localhost
 *
 * @author Loic Royer 2014
 *
 */

#ifdef __WINDOWS__
    #include <windows.h>
    #include <tchar.h>
#elif __APPLE__

#endif
#include <iostream>
#include <stdio.h>
#include "cvlib.h"

using namespace std;

#ifdef __WINDOWS__
int _tmain(int argc, _TCHAR* argv[])
#else
int main(int argc, char** argv)
#endif
{
	cout << "TEST BEGIN\n";

	// First we initialize the library and provide the location of the ClearVolume jar file.
	// the JVM location is determined automatically using the JAVA_HOME env var.
	int lReturnCode = begincvlib("./ClearVolume.jar");
	if(lReturnCode!=0) 
	{
		cout << "Begin failed, return code=" << lReturnCode << endl;
		return lReturnCode;
	}

	cout << "Starting in-process and server ClearVolume\n";

	int lRendererID = 1;
	int lServerID = 2;

	// Creates an in-process renderer:
	if(createRenderer(lRendererID,512, 512, 1, 512, 512)!=0)
		cout << "ERROR while creating renderer \n";

	// Creates a network server:
	if(createServer(lServerID)!=0)
		cout << "ERROR while creating server \n";

	// Sets the voxel dimensions in real units for in-process renderer
	if(setVoxelDimensionsInRealUnits(lRendererID,1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	// Sets the voxel dimensions in real units for in-process server
	if(setVoxelDimensionsInRealUnits(lServerID,  1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	// Information on volume sizes:
	int channel = 0;
	int width = 512;
	int height = width+1;
	int depth = width+3;
	size_t length =width*height*depth;
	char* buffer = new char[length];

	// We will repeatedly send 500 volumes:
	for(int i=0; i<500; i++)
	{
		// for demo purposes we say that the 'acquisition' periode is 100 ms: 
		double timeins = 0.1*i;

		// We set the current index and time for both in-process renderer and server:
		if(setVolumeIndexAndTime(lRendererID,i,timeins)!=0)
			cout << "ERROR while setting volume index and time in seconds (renderer)\n";
		if(setVolumeIndexAndTime(lServerID,  i,timeins)!=0)
			cout << "ERROR while setting volume index and time in seconds (server)\n";

		// We fill in the buffer with some data:
		for(int z=0; z<depth; z++)
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++)
					buffer[x+width*y+width*height*z]=(char)((char)i^(char)x^(char)y^(char)z);

		//We send the data to both the in-process renderer and server.
		cout << "send volume i=" << i << " on channel " << channel << "\n";
		if(send8bitUINTVolumeDataToSink(lRendererID,channel,buffer,length,width,height,depth)!=0)
			cout << "ERROR while sending volume! (renderer)\n";
		if(send8bitUINTVolumeDataToSink(lServerID,  channel,buffer,length,width,height,depth)!=0)
			cout << "ERROR while sending volume! (server)\n";
	}

	// we destroy both renderer and server
	if(destroyRenderer(lRendererID)!=0)
		cout << "ERROR while destroying renderer \n";
	if(destroyServer(lServerID)!=0)
		cout << "ERROR while destroying server \n";

	// closes the library
	endcvlib();
	cout << "TEST END\n";
	return 0;
}


