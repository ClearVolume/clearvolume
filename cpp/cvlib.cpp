// cvlib.cpp : Defines the exported functions for the cvlib DLL.
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
jmethodID getLastExceptionMessageID,createRendererID,destroyRendererID,send8bitUINTVolumeDataToSinkID,send16bitUINTVolumeDataToSinkID;

__declspec(dllexport) unsigned long __cdecl begincvlib(char* AutoPilotJarPath)
{
	try
	{
		clearError();

		const char* JAVAHOME  =getEnvWin("JAVA_HOME");
		cout << JAVAHOME << "\n";

		char JREFolderPath[1024];

		strcpy(JREFolderPath,JAVAHOME);
		strcat(JREFolderPath,"\\bin\\server\\jvm.dll");

		cout << JREFolderPath << "\n";

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
		strcat(lClassPathString,AutoPilotJarPath);

		//cout <<  lClassPathString << "\n";



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


		getLastExceptionMessageID = lJNIEnv->GetStaticMethodID(sClearVolumeClass, "getLastExceptionMessage", "()Ljava/lang/String;");
		createRendererID = lJNIEnv->GetStaticMethodID(sClearVolumeClass, "createRenderer", "(IIIIII)V");
		destroyRendererID = lJNIEnv->GetStaticMethodID(sClearVolumeClass, "destroyRenderer", "(I)V");
		send8bitUINTVolumeDataToSinkID = lJNIEnv->GetStaticMethodID(sClearVolumeClass, "send8bitUINTVolumeDataToSink", "(IJJJJJ)V");
		send16bitUINTVolumeDataToSinkID = lJNIEnv->GetStaticMethodID(sClearVolumeClass, "send16bitUINTVolumeDataToSink", "(IJJJJJ)V");
		

		if (   getLastExceptionMessageID == 0 
			|| createRendererID == 0
			|| destroyRendererID == 0
			|| send8bitUINTVolumeDataToSinkID == 0
			|| send16bitUINTVolumeDataToSinkID == 0)
		{
			return 5;
		}

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

__declspec(dllexport) int __cdecl createRenderer(	int pRendererId,
													int pWindowWidth,
													int pWindowHeight,
													int pBytesPerVoxel,
													int pMaxTextureWidth,
													int pMaxTextureHeight)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		lJNIEnv->CallStaticDoubleMethod(sClearVolumeClass,
										createRendererID,
										pRendererId, 
										pWindowWidth, 
										pWindowHeight, 
										pBytesPerVoxel,
										pMaxTextureWidth,
										pMaxTextureHeight);

		return 0;
	}
	catch (...)
	{
		sJavaLastError = "Error while creating Renderer";
		return -1;
	}
}

__declspec(dllexport) int __cdecl destroyRenderer(int pRendererId)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		lJNIEnv->CallStaticDoubleMethod(sClearVolumeClass,
										destroyRendererID,
										pRendererId);
		return 0;
	}
	catch (...)
	{
		sJavaLastError = "Error while destroying Renderer";
		return -1;
	}
}

__declspec(dllexport) int __cdecl send8bitUINTVolumeDataToSink( int pSinkId,
																 char *pBufferAddress,
																 long pBufferLength,
																 long pWidth,
																 long pHeight,
																 long pDepth)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		lJNIEnv->CallStaticDoubleMethod(sClearVolumeClass,
										send8bitUINTVolumeDataToSinkID,
										pSinkId,
										(long)pBufferAddress,
										pBufferLength,
										pWidth,
										pHeight,
										pDepth);
		return 0;
	}
	catch (...)
	{
		sJavaLastError = "Error while sending 8bit volume data";
		return -1;
	}
}

__declspec(dllexport) int __cdecl send16bitUINTVolumeDataToSink( int pSinkId,
																 short *pBufferAddress,
																 long pBufferLength,
																 long pWidth,
																 long pHeight,
																 long pDepth)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		lJNIEnv->CallStaticDoubleMethod(sClearVolumeClass,
										send16bitUINTVolumeDataToSinkID,
										pSinkId,
										(long)pBufferAddress,
										pBufferLength,
										pWidth,
										pHeight,
										pDepth);
		return 0;
	}
	catch (...)
	{
		sJavaLastError = "Error while sending 16bit volume data";
		return -1;
	}
}


/*


__declspec(dllexport) double __cdecl dcts16bit(short* pBuffer, int pWidth, int pHeight, double pPSFSupportDiameter)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);
		jobject lByteBuffer = lJNIEnv->NewDirectByteBuffer(pBuffer,pWidth*pHeight*2);
		double dcts = lJNIEnv->CallStaticDoubleMethod(sAutoPilotClass,dcts16bitID,lByteBuffer, pWidth, pHeight, pPSFSupportDiameter);
		return dcts;
	}
	catch (...)
	{
		sJavaLastError = "Error while computing dcts focus measure";
		return -1;
	}
}

__declspec(dllexport) double __cdecl tenengrad16bit(short* pBuffer, int pWidth, int pHeight, double pPSFSupportDiameter)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);
		jobject lByteBuffer = lJNIEnv->NewDirectByteBuffer(pBuffer,pWidth*pHeight*2);
		double tenengrad = lJNIEnv->CallStaticDoubleMethod(sAutoPilotClass,tenengrad16bitID, lByteBuffer, pWidth, pHeight, pPSFSupportDiameter);
		return tenengrad;
	}
	catch (...)
	{
		sJavaLastError = "Error while computing dcts focus measure";
		return -1;
	}
}


__declspec(dllexport) int __cdecl l2solveSSP(
		bool pAnchorDetection,
		bool pSymmetricAnchor,
		int pNumberOfWavelengths,
		int pNumberOfPlanes,
		int pSyncPlaneIndex,
		double* pOldStateVector,
		double* pObservationsVector,
		bool* pMissingObservations,
		double* pNewStateVector)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		bool lAddExtraDOF = true;
		int lStateVectorLength = pNumberOfWavelengths*pNumberOfPlanes*2*(1+(lAddExtraDOF?4:1));
		int lObservationVectorLength = pNumberOfWavelengths*pNumberOfPlanes*(2*2+2*(lAddExtraDOF?3:0));

		jdoubleArray lOldStateJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lOldStateJArray, 0, lStateVectorLength, (jdouble*)pOldStateVector);

		jdoubleArray lObservationJArray = lJNIEnv->NewDoubleArray(lObservationVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lObservationJArray, 0, lObservationVectorLength, (jdouble*)pObservationsVector);

		jbooleanArray lMissingJArray = lJNIEnv->NewBooleanArray(lObservationVectorLength);
		lJNIEnv->SetBooleanArrayRegion(lMissingJArray, 0, lObservationVectorLength, (jboolean*)pMissingObservations);

		jdoubleArray lNewStateJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lNewStateJArray, 0, lStateVectorLength, (jdouble*)pNewStateVector);

		int lReturnValue = lJNIEnv->CallStaticIntMethod(sAutoPilotClass,l2solveIDSSP, pAnchorDetection, pSymmetricAnchor, pNumberOfWavelengths,pNumberOfPlanes,pSyncPlaneIndex,lOldStateJArray,lObservationJArray,lMissingJArray,lNewStateJArray);

		jdouble* lArray = lJNIEnv->GetDoubleArrayElements(lNewStateJArray, NULL);
		for (int i=0; i<lStateVectorLength; i++)
			pNewStateVector[i] = lArray[i];
		lJNIEnv->ReleaseDoubleArrayElements(lNewStateJArray, lArray, NULL);

		return lReturnValue;
	}
	catch (...)
	{
		sJavaLastError = "Error while running L2 solver";
		return -2;
	}
}


__declspec(dllexport) int __cdecl l2solve(
		bool pAnchorDetection,
		bool pSymmetricAnchor,
		int pNumberOfWavelengths,
		int pNumberOfPlanes,
		bool* pSyncPlaneIndices,
		double* pOldStateVector,
		double* pObservationsVector,
		bool* pMissingObservations,
		double* pNewStateVector)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		bool lAddExtraDOF = true;
		int lStateVectorLength = pNumberOfWavelengths*pNumberOfPlanes*2*(1+(lAddExtraDOF?4:1));
		int lObservationVectorLength = pNumberOfWavelengths*pNumberOfPlanes*(2*2+2*(lAddExtraDOF?3:0));
		
		jbooleanArray lSyncPlanesIndicesJArray = lJNIEnv->NewBooleanArray(pNumberOfWavelengths*pNumberOfPlanes);
		lJNIEnv->SetBooleanArrayRegion(lSyncPlanesIndicesJArray, 0, pNumberOfWavelengths*pNumberOfPlanes, (jboolean*)pSyncPlaneIndices);

		jdoubleArray lOldStateJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lOldStateJArray, 0, lStateVectorLength, (jdouble*)pOldStateVector);

		jdoubleArray lObservationJArray = lJNIEnv->NewDoubleArray(lObservationVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lObservationJArray, 0, lObservationVectorLength, (jdouble*)pObservationsVector);

		jbooleanArray lMissingJArray = lJNIEnv->NewBooleanArray(lObservationVectorLength);
		lJNIEnv->SetBooleanArrayRegion(lMissingJArray, 0, lObservationVectorLength, (jboolean*)pMissingObservations);

		jdoubleArray lNewStateJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lNewStateJArray, 0, lStateVectorLength, (jdouble*)pNewStateVector);

		int lReturnValue = lJNIEnv->CallStaticIntMethod(sAutoPilotClass,l2solveID, pAnchorDetection, pSymmetricAnchor, pNumberOfWavelengths,pNumberOfPlanes,lSyncPlanesIndicesJArray,lOldStateJArray,lObservationJArray,lMissingJArray,lNewStateJArray);

		jdouble* lArray = lJNIEnv->GetDoubleArrayElements(lNewStateJArray, NULL);
		for (int i=0; i<lStateVectorLength; i++)
			pNewStateVector[i] = lArray[i];
		lJNIEnv->ReleaseDoubleArrayElements(lNewStateJArray, lArray, NULL);

		return lReturnValue;
	}
	catch (...)
	{
		sJavaLastError = "Error while running L2 solver";
		return -2;
	}
}


__declspec(dllexport) int __cdecl qpsolve(
		bool pAnchorDetection,
		bool pSymmetricAnchor,
		int pNumberOfWavelengths,
		int pNumberOfPlanes,
		bool* pSyncPlaneIndices,
		double* pOldStateVector,
		double* pObservationsVector,
		bool* pMissingObservations,
		double* pMaxCorrections,
		double* pNewStateVector)
{
	try
	{
		clearError();
		JNIEnv *lJNIEnv;
		sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

		bool lAddExtraDOF = true;
		int lStateVectorLength = pNumberOfWavelengths*pNumberOfPlanes*2*(1+(lAddExtraDOF?4:1));
		int lObservationVectorLength = pNumberOfWavelengths*pNumberOfPlanes*(2*2+2*(lAddExtraDOF?3:0));
		
		jbooleanArray lSyncPlanesIndicesJArray = lJNIEnv->NewBooleanArray(pNumberOfWavelengths*pNumberOfPlanes);
		lJNIEnv->SetBooleanArrayRegion(lSyncPlanesIndicesJArray, 0, pNumberOfWavelengths*pNumberOfPlanes, (jboolean*)pSyncPlaneIndices);

		jdoubleArray lOldStateJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lOldStateJArray, 0, lStateVectorLength, (jdouble*)pOldStateVector);

		jdoubleArray lObservationJArray = lJNIEnv->NewDoubleArray(lObservationVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lObservationJArray, 0, lObservationVectorLength, (jdouble*)pObservationsVector);

		jbooleanArray lMissingJArray = lJNIEnv->NewBooleanArray(lObservationVectorLength);
		lJNIEnv->SetBooleanArrayRegion(lMissingJArray, 0, lObservationVectorLength, (jboolean*)pMissingObservations);

		jdoubleArray lMaxCorrectionsJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lMaxCorrectionsJArray, 0, lStateVectorLength, (jdouble*)pMaxCorrections);

		jdoubleArray lNewStateJArray = lJNIEnv->NewDoubleArray(lStateVectorLength);
		lJNIEnv->SetDoubleArrayRegion(lNewStateJArray, 0, lStateVectorLength, (jdouble*)pNewStateVector);

		int lReturnValue = lJNIEnv->CallStaticIntMethod(sAutoPilotClass,qpsolveID, pAnchorDetection, pSymmetricAnchor, pNumberOfWavelengths,pNumberOfPlanes,lSyncPlanesIndicesJArray,lOldStateJArray,lObservationJArray,lMissingJArray,lMaxCorrectionsJArray,lNewStateJArray);

		jdouble* lArray = lJNIEnv->GetDoubleArrayElements(lNewStateJArray, NULL);
		for (int i=0; i<lStateVectorLength; i++)
			pNewStateVector[i] = lArray[i];
		lJNIEnv->ReleaseDoubleArrayElements(lNewStateJArray, lArray, NULL);

		return lReturnValue;
	}
	catch (...)
	{
		sJavaLastError = "Error while running L2 solver";
		return -2;
	}
}

__declspec(dllexport) void __cdecl freePointer(void* pPointer)
{
	free(pPointer);
}


/**/



