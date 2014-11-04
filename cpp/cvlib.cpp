/**
 * cvlib.cpp 
 * 
 * Defines the exported functions for the cvlib native binding library to ClearVolume.
 *
 * @author Loic Royer 2014
 *
 */

// cvlib.cpp : 
//

#include "cvlib.h"
#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
#include <windows.h>
#include "jvmlib/jni.h"       /* where everything is defined */
#include <iostream>
#include <string.h>

using namespace std;

#define cErrorNone "No Error"

const char * getEnvWin(const char * name)
{
    const DWORD buffSize = 65535;
    static char buffer[buffSize];
    if (GetEnvironmentVariableA(name, buffer, buffSize))
    {
        return buffer;
    }
    else
    {
        return 0;
    }
}

typedef jint (JNICALL *CreateJavaVM)(JavaVM **pvm, void **penv, void *args);
JavaVM *sJVM; /* denotes a Java VM */
JavaVMInitArgs sJVMArgs; /* JDK/JRE 6 VM initialization arguments */
char* sJavaLastError = cErrorNone;

jclass sClearVolumeClass;
jmethodID 	getLastExceptionMessageID,
			createRendererID,
			destroyRendererID,
			createServerID,
			destroyServerID,
			setVoxelDimensionsInRealUnitsID,
			setVolumeIndexAndTimeID,
			send8bitUINTVolumeDataToSinkID,
			send16bitUINTVolumeDataToSinkID;


__declspec(dllexport) unsigned long __cdecl begincvlib(char* pClearVolumeJarPath)
{
	try
	{
		clearError();

		const char* JAVAHOME  =getEnvWin("JAVA_HOME");
		char JREFolderPath[1024];
		strcpy(JREFolderPath,JAVAHOME);
		strcat(JREFolderPath,"\\bin\\server\\jvm.dll");

		HINSTANCE lDLLInstance = LoadLibraryA(JREFolderPath);
		if( lDLLInstance == 0)
		{
			sJavaLastError = "Cannot load Jvm.dll (wrong path given, should be jre folder inside of autopilot folder)";
			return 1;
		}
		CreateJavaVM lCreateJavaVM = (CreateJavaVM)GetProcAddress(lDLLInstance, "JNI_CreateJavaVM");
		if (lCreateJavaVM == NULL )
		{
			sJavaLastError = "Cannot load Jvm.dll (wrong path given)";
			return 2;
		}

		size_t lJREFolderPathLength= strlen(JREFolderPath);

		char lClassPathPrefix[] = "-Djava.class.path=";
		size_t lClassPathPrefixLength= strlen(lClassPathPrefix);
		
		char lClassPathString[1024];

		strcpy(lClassPathString,lClassPathPrefix);
		strcat(lClassPathString,pClearVolumeJarPath);


		JavaVMOption options[3];
		options[0].optionString = "-Xmx4G";
		options[1].optionString = lClassPathString;
		options[2].optionString = "-verbose";
		sJVMArgs.version = JNI_VERSION_1_6;
		sJVMArgs.nOptions = 2;
		sJVMArgs.options = options;
		sJVMArgs.ignoreUnrecognized = false;

		JNIEnv *lJNIEnv;
		jint res = lCreateJavaVM(&sJVM, (void **)&lJNIEnv, &sJVMArgs);
		if (res < 0)
		{
			return 3;
		}

		sClearVolumeClass = lJNIEnv->FindClass("clearvolume/interfaces/ClearVolumeC");

		if (sClearVolumeClass == 0)
		{
			return 4;
		}

		/*
		 B = byte
		 C = char
		 D = double
		 F = float
		 I = int
		 J = long
		 S = short
		 V = void
		 Z = boolean
		 Lfully-qualified-class = fully qualified class
		 [type = array of type
		 (argument types)return type = method type. If no arguments, use empty argument types: (). 
		 If return type is void (or constructor) use (argument types)V.
		 Observe that the ; is needed after the class name in all situations. 
		 This won't work "(Ljava/lang/String)V" but this will "(Ljava/lang/String;)V". 
		 /**/


		getLastExceptionMessageID		= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "getLastExceptionMessage", "()Ljava/lang/String;");
		createRendererID 				= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "createRenderer", "(IIIIII)I");
		destroyRendererID 				= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "destroyRenderer", "(I)I");
		createServerID    				= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "createServer", "(I)I");
		destroyServerID 				= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "destroyServer", "(I)I");
		setVoxelDimensionsInRealUnitsID	= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "setVoxelDimensionsInRealUnits", "(IDDD)I");
		setVolumeIndexAndTimeID 		= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "setVolumeIndexAndTime", "(IID)I");
		send8bitUINTVolumeDataToSinkID 	= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "send8bitUINTVolumeDataToSink", "(IIJJIII)I");
		send16bitUINTVolumeDataToSinkID = lJNIEnv->GetStaticMethodID(sClearVolumeClass, "send16bitUINTVolumeDataToSink", "(IIJJIII)I");
		
		if (getLastExceptionMessageID == 0) return 101;
		if (createRendererID == 0) return 102;
		if (destroyRendererID == 0) return 103;
		if (createServerID == 0) return 104;
		if (destroyServerID == 0) return 105;
		if (setVoxelDimensionsInRealUnitsID == 0) return 106;
		if (setVolumeIndexAndTimeID == 0) return 107;
		if (send8bitUINTVolumeDataToSinkID == 0) return 108;
		if (send16bitUINTVolumeDataToSinkID == 0) return 109;

		return 0;
	}
	catch(...)
	{
		sJavaLastError = "Error while creating Java JVM";
		return 100;
	}
}

__declspec(dllexport) unsigned long __cdecl endcvlib()
{
	try
	{
		clearError();
		// This hangs the system for no good reason:
		//sJVM->DetachCurrentThread();
		//sJVM->DestroyJavaVM();
		return 0;
	}
	catch(...)
	{
		sJavaLastError = "Error while destroying Java JVM";
		return 1;
	}
}

__declspec(dllexport)  void __cdecl clearError()
{
	sJavaLastError=cErrorNone;
}



jstring sLastJavaExceptionMessageJString = NULL;
char* sLastJavaExceptionMessage = NULL;

__declspec(dllexport) char* __cdecl getLastJavaExceptionMessage()
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		if(sLastJavaExceptionMessageJString!=NULL && sLastJavaExceptionMessage != NULL)
		{
			lJNIEnv->ReleaseStringUTFChars(sLastJavaExceptionMessageJString, sLastJavaExceptionMessage);
		}

		sLastJavaExceptionMessageJString = (jstring)lJNIEnv->CallStaticObjectMethod(sClearVolumeClass,getLastExceptionMessageID);
		sLastJavaExceptionMessage = NULL;

		if(sLastJavaExceptionMessageJString!=NULL)
		{
			sLastJavaExceptionMessage = (char*)lJNIEnv->GetStringUTFChars(sLastJavaExceptionMessageJString, NULL);
		}
		return sLastJavaExceptionMessage;
	}
	catch (...)
	{
		return "Error while obtaining the Java exception string";
	}
}

__declspec(dllexport) char* __cdecl getLastError()
{
	char* lLastJavaExceptionMessage = getLastJavaExceptionMessage();
	if(lLastJavaExceptionMessage!=NULL) return lLastJavaExceptionMessage;
	else return sJavaLastError;
}

__declspec(dllexport) jint __cdecl createRenderer(	jint pRendererId,
													jint pWindowWidth,
													jint pWindowHeight,
													jint pBytesPerVoxel,
													jint pMaxTextureWidth,
													jint pMaxTextureHeight)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
										createRendererID,
										pRendererId, 
										pWindowWidth, 
										pWindowHeight, 
										pBytesPerVoxel,
										pMaxTextureWidth,
										pMaxTextureHeight);

	}
	catch (...)
	{
		sJavaLastError = "Error while creating Renderer";
		return -1;
	}
}

__declspec(dllexport) jint __cdecl destroyRenderer(jint pRendererId)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											destroyRendererID,
											pRendererId);
	}
	catch (...)
	{
		sJavaLastError = "Error while destroying Renderer";
		return -1;
	}
}

__declspec(dllexport) jint __cdecl createServer(	jint pServerId)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											createServerID,
											pServerId);

	}
	catch (...)
	{
		sJavaLastError = "Error while creating Server";
		return -1;
	}
}

__declspec(dllexport) jint __cdecl destroyServer(jint pServerId)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											destroyServerID,
											pServerId);
	}
	catch (...)
	{
		sJavaLastError = "Error while destroying Server";
		return -1;
	}
}


__declspec(dllexport) long __cdecl setVoxelDimensionsInRealUnits( 	jint pSinkId,
																	jdouble pVoxelWidthInRealUnits,
																	jdouble pVoxelHeightInRealUnits,
																	jdouble pVoxelDepthInRealUnits)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											setVoxelDimensionsInRealUnitsID,
											pSinkId,
											pVoxelWidthInRealUnits,
											pVoxelHeightInRealUnits,
											pVoxelDepthInRealUnits);
	}
	catch (...)
	{
		sJavaLastError = "Error while setting voxel dimensions in real units";
		return -1;
	}
}



__declspec(dllexport) jint __cdecl setVolumeIndexAndTime( 			jint pSinkId,
																	jint pVolumeIndex,
																	jdouble pVolumeTimeInSeconds)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											setVolumeIndexAndTimeID,
											pSinkId,
											pVolumeIndex,
											pVolumeTimeInSeconds);
	}
	catch (...)
	{
		sJavaLastError = "Error while setting volume index and time dimensions in real units";
		return -1;
	}
}


__declspec(dllexport) jint __cdecl send8bitUINTVolumeDataToSink( 	jint pSinkId,
																	jint pChannelId,
																 	char *pBufferAddress,
																 	__int64 pBufferLength,																 	
																 	jint pWidth,
																 	jint pHeight,
																 	jint pDepth)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											send8bitUINTVolumeDataToSinkID,
											pSinkId,
											pChannelId,
											(__int64)pBufferAddress,
											(__int64)pBufferLength,
											pWidth,
											pHeight,
											pDepth);
	}
	catch (...)
	{
		sJavaLastError = "Error while sending 8bit volume data";
		return -1;
	}
}

__declspec(dllexport) jint __cdecl send16bitUINTVolumeDataToSink( 	jint pSinkId,
																	jint pChannelId,
																 	short *pBufferAddress,
																 	__int64 pBufferLength,
																 	jint pWidth,
																 	jint pHeight,
																 	jint pDepth)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
											send16bitUINTVolumeDataToSinkID,
											pSinkId,
											pChannelId,
											(__int64)pBufferAddress,
											(__int64)pBufferLength,
											pWidth,
											pHeight,
											pDepth);
	}
	catch (...)
	{
		sJavaLastError = "Error while sending 16bit volume data";
		return -1;
	}
}

