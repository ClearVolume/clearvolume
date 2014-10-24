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
	long lReturnCode = begin("C:\\Program Files\\Java\\jre8\\bin\\server\\jvm.dll", ".\\ClearVolume.jar");
	if(lReturnCode!=0) 
	{
		cout << "Begin failed, return code=" << lReturnCode;
		return 1;
	}

	cout << "TEST HAPPENS HERE\n";
	
	end();
	cout << "TEST END\n";
	return 0;
}


