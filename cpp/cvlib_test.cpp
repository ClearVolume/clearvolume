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
	long lReturnCode = begincvlib(".\\ClearVolume.jar");
	if(lReturnCode!=0) 
	{
		cout << "Begin failed, return code=" << lReturnCode;
		return lReturnCode;
	}

	cout << "TEST HAPPENS NOW\n";

	if(createRenderer(1,512, 512, 1, 512, 512)<0)
		cout << "ERROR while creating renderer \n";

	long width = 128;
	long height = 128;
	long depth = 128;
	long length =width*height*depth;
	char* buffer = new char[length];

	for(int i=0; i<10000; i++)
	{
		for(int z=0; z<depth; z++)
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++)
					buffer[x+width*y+width*height*z]=(char)((char)i^(char)x^(char)y^(char)z);

		cout << "send volume i=" << i << "\n";
		if(send8bitUINTVolumeDataToSink(1,buffer,length,width,height,depth)<0)
			cout << "ERROR while sending volume! \n";
	}

	if(destroyRenderer(1)<0)
		cout << "ERROR while destroying renderer \n";

	endcvlib();
	cout << "TEST END\n";
	return 0;
}


