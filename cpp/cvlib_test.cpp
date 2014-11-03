//
// ClearVolume native interface example test
//
// Loic Royer, 2014
//

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

	cout << "TEST HAPPENS NOW\n";

	int lRendererID = 1;

	if(createRenderer(lRendererID,512, 512, 1, 512, 512)!=0)
		cout << "ERROR while creating renderer \n";

	if(setVoxelDimensionsInRealUnits(lRendererID,1,1,1)!=0)
		cout << "ERROR while setting dimensions in real units(um)) \n";

	int channel = 0;
	int width = 512;
	int height = width+1;
	int depth = width+3;
	size_t length =width*height*depth;
	char* buffer = new char[length];

	for(int i=0; i<10000; i++)
	{
		double timeins = 0.1*i;
		if(setVolumeIndexAndTime(lRendererID,i,timeins)!=0)
			cout << "ERROR while setting volume index and time in seconds \n";

		for(int z=0; z<depth; z++)
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++)
					buffer[x+width*y+width*height*z]=(char)((char)i^(char)x^(char)y^(char)z);

		cout << "send volume i=" << i << " on channel " << channel << "\n";
		//cout << "width=" << width << "\n";
		//cout << "height=" << height << "\n";
		//cout << "depth=" << depth << "\n";
		if(send8bitUINTVolumeDataToSink(lRendererID,channel,buffer,length,width,height,depth)!=0)
			cout << "ERROR while sending volume! \n";
	}

	if(destroyRenderer(lRendererID)!=0)
		cout << "ERROR while destroying renderer \n";

	endcvlib();
	cout << "TEST END\n";
	return 0;
}


