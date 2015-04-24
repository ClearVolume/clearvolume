/**
 * cvlib.cpp 
 * 
 * Defines the exported functions for the cvlib native binding library to ClearVolume.
 *
 * @author Loic Royer, Ulrik Guenther 2014
 *
 */

#include "cvlib.h"
#ifdef __APPLE__
// any includes needed?
#define __cdecl __attribute__((__cdecl__))
#define __declspec(dllexport)
#define __int64 unsigned long long
#elif __WINDOWS__
#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
#include <windows.h>
#endif

//#include "jvmlib/jni.h"       /* where everything is defined */
#include <stdio.h>
#include <string.h>

#define cErrorNone "No Error"

#ifdef __WINDOWS__
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
#endif

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
    clearError();
#ifdef __WINDOWS__
    const char* JAVAHOME  =getEnvWin("JAVA_HOME");
    printf("JAVAHOME=%s\n");

    char JREFolderPath[1024];

    strcpy(JREFolderPath,JAVAHOME);
    strcat(JREFolderPath,"\\bin\\server\\jvm.dll");
    printf("trying to load from a JRE path: %s", JREFolderPath);

    HINSTANCE lDLLInstance = LoadLibraryA(JREFolderPath);
    if( lDLLInstance == 0)
    {
        printf( "JAVAHOME=%s\n", JAVAHOME);
        strcpy(JREFolderPath,JAVAHOME);
        strcat(JREFolderPath,"\\jre\\bin\\server\\jvm.dll");
        printf( "trying to load from a JDK path:%s\n ", JREFolderPath);

        lDLLInstance = LoadLibraryA(JREFolderPath);
        if( lDLLInstance == 0)
        {
            sJavaLastError = "Cannot load Jvm.dll (wrong path given)";
            return 1;
        }
    }
    CreateJavaVM lCreateJavaVM = (CreateJavaVM)GetProcAddress(lDLLInstance, "JNI_CreateJavaVM");
    if (lCreateJavaVM == NULL )
    {
        sJavaLastError = "Cannot load Jvm.dll (wrong path given)";
        return 2;
    }

    size_t lJREFolderPathLength= strlen(JREFolderPath);

#endif
    char lClassPathPrefix[] = "-Djava.class.path=./jars";
    size_t lClassPathPrefixLength= strlen(lClassPathPrefix);

    char lClassPathString[1024];

    strcpy(lClassPathString,lClassPathPrefix);
    //strcat(lClassPathString,pClearVolumeJarPath);


    JavaVMOption options[4];
    options[0].optionString = (char*)"-Xmx4G";
    options[1].optionString = (char*)"-Djava.class.path=./jars;./jars/ClearVolume-0.9.0.jar;.";
    options[2].optionString = (char*)"-verbose";
    options[3].optionString = (char*)"-Xdebug";

    memset(&sJVMArgs, 0, sizeof(sJVMArgs));
    sJVMArgs.version = JNI_VERSION_1_6;
    sJVMArgs.options = options;
    sJVMArgs.nOptions = 4;
    sJVMArgs.ignoreUnrecognized = false;

    JNIEnv *lJNIEnv;
#ifdef __WINDOWS
    jint res = lCreateJavaVM(&sJVM, (void **)&lJNIEnv, &sJVMArgs);
#else
    jint res = JNI_CreateJavaVM(&sJVM, (void**)&lJNIEnv, &sJVMArgs);
#endif
    if (res == JNI_ERR)
    {
        return 3;
    } else if (res == JNI_OK) {
        cerr << "JVM created." << endl;
    }

    sClearVolumeClass = lJNIEnv->FindClass("clearvolume/interfaces/ClearVolumeC");

    if (sClearVolumeClass == 0)
    {
        cerr << "Unable to locate class " << "clearvolume/interfaces/ClearVolumeC" << " in " << pClearVolumeJarPath << endl;
        cerr << "Java class path was " << lClassPathString << endl;

        jthrowable exc;
        exc = lJNIEnv->ExceptionOccurred();
        if (exc) {
            jclass newExcCls;
            lJNIEnv->ExceptionDescribe();
            lJNIEnv->ExceptionClear();

        }

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
    createRendererID 				= lJNIEnv->GetStaticMethodID(sClearVolumeClass, "createRenderer", "(IIIIIIZZ)I");
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

__declspec(dllexport) unsigned long __cdecl endcvlib()
{
    clearError();
    // This hangs the system for no good reason:
    //sJVM->DetachCurrentThread();
    //sJVM->DestroyJavaVM();
    return 0;
}

__declspec(dllexport)  void __cdecl clearError()
{
    sJavaLastError=cErrorNone;
}



jstring sLastJavaExceptionMessageJString = NULL;
char* sLastJavaExceptionMessage = NULL;

__declspec(dllexport) char* __cdecl getLastJavaExceptionMessage()
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

__declspec(dllexport) char* __cdecl getLastError()
{
    char* lLastJavaExceptionMessage = getLastJavaExceptionMessage();
    if(lLastJavaExceptionMessage!=NULL) return lLastJavaExceptionMessage;
    else return sJavaLastError;
}

__declspec(dllexport) jint __cdecl createRenderer(				long pRendererId,
        long pWindowWidth,
        long pWindowHeight,
        long pBytesPerVoxel,
        long pMaxTextureWidth,
        long pMaxTextureHeight)
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
            pMaxTextureHeight,
            false,
            false);
}

__declspec(dllexport) jint __cdecl createRenderer(	jint pRendererId,
        jint pWindowWidth,
        jint pWindowHeight,
        jint pBytesPerVoxel,
        jint pMaxTextureWidth,
        jint pMaxTextureHeight,
        jboolean pTimeShift,
        jboolean pChannelSelector)
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
            pMaxTextureHeight,
            pTimeShift,
            pChannelSelector);

}

__declspec(dllexport) jint __cdecl destroyRenderer(jint pRendererId)
{
    clearError();
    JNIEnv *lJNIEnv;
    sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

    return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
            destroyRendererID,
            pRendererId);
}

__declspec(dllexport) jint __cdecl createServer(	jint pServerId)
{
    clearError();
    JNIEnv *lJNIEnv;
    sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

    return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
            createServerID,
            pServerId);
}

__declspec(dllexport) jint __cdecl destroyServer(jint pServerId)
{
    clearError();
    JNIEnv *lJNIEnv;
    sJVM->AttachCurrentThread((void**)&lJNIEnv, NULL);

    return lJNIEnv->CallStaticIntMethod(sClearVolumeClass,
            destroyServerID,
            pServerId);
}


__declspec(dllexport) long __cdecl setVoxelDimensionsInRealUnits( 	jint pSinkId,
        jdouble pVoxelWidthInRealUnits,
        jdouble pVoxelHeightInRealUnits,
        jdouble pVoxelDepthInRealUnits)
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
    sJavaLastError = "Error while setting voxel dimensions in real units";
    return -1;
}



__declspec(dllexport) jint __cdecl setVolumeIndexAndTime( 			jint pSinkId,
        jint pVolumeIndex,
        jdouble pVolumeTimeInSeconds)
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


__declspec(dllexport) jint __cdecl send8bitUINTVolumeDataToSink( 	jint pSinkId,
        jint pChannelId,
        char *pBufferAddress,
        __int64 pBufferLength,																 	
        jint pWidth,
        jint pHeight,
        jint pDepth)
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

__declspec(dllexport) jint __cdecl send16bitUINTVolumeDataToSink( 	jint pSinkId,
        jint pChannelId,
        short *pBufferAddress,
        __int64 pBufferLength,
        jint pWidth,
        jint pHeight,
        jint pDepth)
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

