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

#include <windows.h>
#include <tchar.h>
#include <iostream>
#include <stdio.h>
#include <tchar.h>
#include "cvlib.h"

using namespace std;


int _tmain(int argc, _TCHAR* argv[])
{
	cout << "TEST BEGIN\n";
	int lReturnCode = begincvlib(".\\ClearVolume.jar");
	if(lReturnCode!=0) 
	{
		cout << "Begin failed, return code=" << lReturnCode;
		return lReturnCode;
	}

	cout << "Starting in-process ClearVolume\n";

	int lRendererID = 1;
	int lServerID = 2;

	if(createRenderer(lRendererID,512, 512, 1, 512, 512)!=0)
		cout << "ERROR while creating renderer \n";

	if(createServer(lServerID)!=0)
		cout << "ERROR while creating server \n";

	if(setVoxelDimensionsInRealUnits(lRendererID,1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	if(setVoxelDimensionsInRealUnits(lServerID,  1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	int channel = 0;
	int width = 512;
	int height = width+1;
	int depth = width+3;
	size_t length =width*height*depth;
	char* buffer = new char[length];

	for(int i=0; i<500; i++)
	{
		double timeins = 0.1*i;
		if(setVolumeIndexAndTime(lRendererID,i,timeins)!=0)
			cout << "ERROR while setting volume index and time in seconds (renderer)\n";
		if(setVolumeIndexAndTime(lServerID,  i,timeins)!=0)
			cout << "ERROR while setting volume index and time in seconds (server)\n";

		for(int z=0; z<depth; z++)
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++)
					buffer[x+width*y+width*height*z]=(char)((char)i^(char)x^(char)y^(char)z);

		cout << "send volume i=" << i << " on channel " << channel << "\n";
		//cout << "width=" << width << "\n";
		//cout << "height=" << height << "\n";
		//cout << "depth=" << depth << "\n";
		if(send8bitUINTVolumeDataToSink(lRendererID,channel,buffer,length,width,height,depth)!=0)
			cout << "ERROR while sending volume! (renderer)\n";
		if(send8bitUINTVolumeDataToSink(lServerID,  channel,buffer,length,width,height,depth)!=0)
			cout << "ERROR while sending volume! (server)\n";
	}

	if(destroyRenderer(lRendererID)!=0)
		cout << "ERROR while destroying renderer \n";

	if(destroyServer(lServerID)!=0)
		cout << "ERROR while destroying server \n";

	endcvlib();
	cout << "TEST END\n";
	return 0;
}


