// dllmain.cpp : Defines the entry point for the DLL application.
#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
#include <windows.h>
#include <jni.h>       /* where everything is defined */
#include <iostream>

#include "AutoPilot.h"

    

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
					 )
{
	switch (ul_reason_for_call)
	{
	case DLL_PROCESS_ATTACH:
	{

		
		
	}

	case DLL_THREAD_ATTACH:
	case DLL_THREAD_DETACH:
	case DLL_PROCESS_DETACH:
		break;
	}
	return TRUE;
}


/* invoke the Main.test method using the JNI 
		jclass cls = env->FindClass("Main");
		jmethodID mid = env->GetStaticMethodID(cls, "test", "(I)V");
		env->CallStaticVoidMethod(cls, mid, 100);
		/* We are done. */
/**/








