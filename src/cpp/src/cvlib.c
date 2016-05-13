/**
 * cvlib.cpp 
 * 
 * Defines the exported functions for the cvlib native binding library to ClearVolume.
 *
 * @author Loic Royer, Ulrik Guenther 2014
 *
 */

#include "cvlib.h"
#include <stdbool.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define cErrorNone "No Error"

#ifdef _WIN32
const char * getEnvWin(const char * name)
{
    const DWORD buffSize = 1024;
    static char buffer[1024];
    if (GetEnvironmentVariableA(name, &buffer[0], buffSize))
    {
        return buffer;
    }
    else
    {
        return "";
    }
}
#endif

typedef jint (JNICALL *CreateJavaVM)(JavaVM **pvm, void **penv, void *args);
static JavaVM *sJVM; /* denotes a Java VM */
JavaVMInitArgs sJVMArgs; /* JDK/JRE 6 VM initialization arguments */
char* sJavaLastError = cErrorNone;

jclass sClearVolumeClass;
static jmethodID 	getLastExceptionMessageID,
            createRendererID,
            destroyRendererID,
            createServerID,
            destroyServerID,
            setVoxelDimensionsInRealUnitsID,
            setVolumeIndexAndTimeID,
            send8bitUINTVolumeDataToSinkID,
            send16bitUINTVolumeDataToSinkID,
            setChannelNameID,
            setChannelColorID;

#ifndef _WIN32
#include <signal.h>

static struct sigaction old_sa[NSIG];

void cv_sigaction(int signal, siginfo_t *info, void *reserved)
{
    printf("Signal caught: %i\n", signal);

    JNIEnv* lJNIEnv;
    jthrowable exc;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

        exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
        if (exc) {
            jclass newExcCls;
            (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
            (*lJNIEnv)->ExceptionClear(lJNIEnv);

        }

    if(signal == 10 || signal == 11) {
        endcvlib();
    }

	old_sa[signal].sa_handler(signal);
}
#endif

char* get_cv_jars(const char* path, char* buffer) {
    DIR* jar_dir;
    struct dirent* entry;
    int error;

    char* tmp = (char*)malloc(1);
    memset(tmp, 0, 1);

    if ((jar_dir = opendir(path)) != NULL) {
        while ((entry = readdir(jar_dir)) != NULL) {
            if(strncmp(".jar", entry->d_name+(strlen(entry->d_name))-4, 4) == 0) {
                char* new;
                unsigned long new_size = strlen(path) + strlen(entry->d_name) + strlen("/") + strlen(JAR_SEPARATOR) + strlen("\0");
                if((new = malloc(new_size)) != NULL) {
                    snprintf(new, new_size+1, "%s/%s%s", path, entry->d_name, JAR_SEPARATOR);

                    tmp = realloc(tmp, strlen(tmp)+strlen(new)+1);
                    //fprintf(stderr, "%s new=%lu, tmp is now %lu bytes vs %lu\n", new, strlen(new)+1, strlen(tmp)+strlen(new)+1, strlen(tmp));
                    memset(tmp+strlen(tmp), 0, strlen(new) + 1);
                    strncat(tmp, new, strlen(new));
                } else {
                    fprintf(stderr, "malloc failed, out of memory?\n");
                    return NULL;
                }
            }
        }
        
        closedir (jar_dir);

        return tmp;

    } else {
        /* could not open directory */
        perror ("");
        return NULL;
    }
}

__declspec(dllexport) unsigned long __cdecl begincvlib(char* pClearVolumeJarPath, backend_t backend)
{
    clearError();
    JNIEnv* lJNIEnv;
#ifdef _WIN32
    const char* JAVAHOME = getEnvWin("JAVA_HOME");
    printf("JAVAHOME=%s\n", JAVAHOME);

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
    char* lClassPathPrefix = "-Djava.class.path=";

    char* lClassPathString;
    char path[1024];
    char* jars;

    if(strlen(pClearVolumeJarPath) > 0) {
        strcpy(path, pClearVolumeJarPath);
    } else {
        strcpy(path, "./jars");
    }

    if((jars = get_cv_jars(path, jars)) > 0) {
        printf("Getting CV jars ...");
        printf("%s\n", jars);
    } else {
        jars = (char*)malloc(1);
        jars[0] = '\0';
    }

    lClassPathString = malloc(strlen(lClassPathPrefix) + strlen(jars) + 1);
    strcpy(lClassPathString, lClassPathPrefix);
    strcat(lClassPathString, jars);

    memset(&sJVMArgs, 0, sizeof(sJVMArgs));

    JavaVMOption options[3];
    options[0].optionString = (char*)"-Xmx12G";
    options[1].optionString = (char*)lClassPathString;

    switch(backend) {
        case CUDA:
            printf("Forcing CUDA backend.\n");
            options[2].optionString = "-DClearVolume.disableOpenCL";
            sJVMArgs.nOptions = 3;
            break;
        case OPENCL:
            printf("Forcing OpenCL backend.\n");
            options[2].optionString = "-DClearVolume.disableCUDA";
            sJVMArgs.nOptions = 3;
            break;
        case USEBEST:
        default:
            printf("No backend preference selected.\n");
            sJVMArgs.nOptions = 2;
    }

    sJVMArgs.version = JNI_VERSION_1_6;
    sJVMArgs.options = options;
    sJVMArgs.ignoreUnrecognized = 0;

#ifdef _WIN32
    jint res = lCreateJavaVM(&sJVM, (void **)&lJNIEnv, &sJVMArgs);
#else
    jint res = JNI_CreateJavaVM(&sJVM, (void**)&lJNIEnv, &sJVMArgs);
#endif
    if (res == JNI_ERR)
    {
        return 3;
    } else if (res == JNI_OK) {
        fprintf(stderr, "JVM created.\n");
    }

    sClearVolumeClass = (*lJNIEnv)->FindClass(lJNIEnv, "clearvolume/interfaces/ClearVolumeC");

    if (sClearVolumeClass == 0)
    {
        fprintf(stderr, "Unable to locate class %s in %s\n", "clearvolume/interfaces/ClearVolumeC", pClearVolumeJarPath);
        fprintf(stderr, "Java class path was %s\n", options[1].optionString);

        jthrowable exc;
        exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
        if (exc) {
            jclass newExcCls;
            (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
            (*lJNIEnv)->ExceptionClear(lJNIEnv);

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
       */


    getLastExceptionMessageID		= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "getLastExceptionMessage", "()Ljava/lang/String;");
    createRendererID 				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "createRenderer", "(IIIIIIZZ)I");
    destroyRendererID 				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "destroyRenderer", "(I)I");
    createServerID    				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "createServer", "(I)I");
    destroyServerID 				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "destroyServer", "(I)I");
    setVoxelDimensionsInRealUnitsID	= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "setVoxelDimensionsInRealUnits", "(IDDD)I");
    setVolumeIndexAndTimeID 		= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "setVolumeIndexAndTime", "(IID)I");
    send8bitUINTVolumeDataToSinkID 	= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "send8bitUINTVolumeDataToSink", "(IIJJIII)I");
    send16bitUINTVolumeDataToSinkID = (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "send16bitUINTVolumeDataToSink", "(IIJJIII)I");
    setChannelNameID                = (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "setChannelName", "(ILjava/lang/String;)I");
    setChannelColorID               = (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "setChannelColor","(I[F)I");


    if (getLastExceptionMessageID == 0) return 101;
    if (createRendererID == 0) return 102;
    if (destroyRendererID == 0) return 103;
    if (createServerID == 0) return 104;
    if (destroyServerID == 0) return 105;
    if (setVoxelDimensionsInRealUnitsID == 0) return 106;
    if (setVolumeIndexAndTimeID == 0) return 107;
    if (send8bitUINTVolumeDataToSinkID == 0) return 108;
    if (send16bitUINTVolumeDataToSinkID == 0) return 109;
    if (setChannelNameID == 0) return 110;
    if (setChannelColorID == 0) return 111;

    jthrowable exc;
    exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
    if (exc) {
        jclass newExcCls;
        (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
        (*lJNIEnv)->ExceptionClear(lJNIEnv);

    }

#ifndef _WIN32
       struct sigaction handler;
	memset(&handler, 0, sizeof(sigaction));
	handler.sa_sigaction = cv_sigaction;
	handler.sa_flags = SA_RESETHAND;
#define CATCHSIG(X) sigaction(X, &handler, &old_sa[X])
	CATCHSIG(SIGILL);
	CATCHSIG(SIGABRT);
//	CATCHSIG(SIGBUS);
	CATCHSIG(SIGFPE);
//	CATCHSIG(SIGSEGV);
	CATCHSIG(SIGPIPE);
#endif

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
    JNIEnv* lJNIEnv;

    clearError();
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    if(sLastJavaExceptionMessageJString!=NULL && sLastJavaExceptionMessage != NULL)
    {
        (*lJNIEnv)->ReleaseStringUTFChars(lJNIEnv, sLastJavaExceptionMessageJString, sLastJavaExceptionMessage);
    }

    sLastJavaExceptionMessageJString = (jstring)(*lJNIEnv)->CallStaticObjectMethod(lJNIEnv, sClearVolumeClass,getLastExceptionMessageID);
    sLastJavaExceptionMessage = NULL;

    if(sLastJavaExceptionMessageJString!=NULL)
    {
        sLastJavaExceptionMessage = (char*)(*lJNIEnv)->GetStringUTFChars(lJNIEnv, sLastJavaExceptionMessageJString, NULL);
    }
    return sLastJavaExceptionMessage;
}

__declspec(dllexport) char* __cdecl getLastError()
{
    char* lLastJavaExceptionMessage = getLastJavaExceptionMessage();
    if(lLastJavaExceptionMessage!=NULL) return lLastJavaExceptionMessage;
    else return sJavaLastError;
}

__declspec(dllexport) long __cdecl createRenderer(				int pRendererId,
        int pWindowWidth,
        int pWindowHeight,
        int pBytesPerVoxel,
        int pMaxTextureWidth,
        int pMaxTextureHeight)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);
    jint envRes = (*sJVM)->GetEnv(sJVM, (void**)&lJNIEnv, JNI_VERSION_1_6);

    if(envRes != JNI_OK) {
        fprintf(stderr, "Error attaching JRE to current thread! %d\n", envRes);
        return 1;
    }

    jthrowable exc;
    exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
    if (exc) {
        jclass newExcCls;
        (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
        (*lJNIEnv)->ExceptionClear(lJNIEnv);

    }

    printf("Creating renderer now...\n");
    int res = (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            createRendererID,
            pRendererId, 
            pWindowWidth, 
            pWindowHeight, 
            pBytesPerVoxel,
            pMaxTextureWidth,
            pMaxTextureHeight,
            0,
            0);

    printf("Checking for exceptions...\n");
    exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
    if (exc) {
        jclass newExcCls;
        (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
        (*lJNIEnv)->ExceptionClear(lJNIEnv);
        return 0;
    } else {
        return res;
    }
}

__declspec(dllexport) long __cdecl createRendererWithTimeShiftAndChannels(	long pRendererId,
        long pWindowWidth,
        long pWindowHeight,
        long pBytesPerVoxel,
        long pMaxTextureWidth,
        long pMaxTextureHeight,
        bool pTimeShift,
        bool pChannelSelector)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
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

__declspec(dllexport) long __cdecl destroyRenderer(long pRendererId)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            destroyRendererID,
            pRendererId);
}

__declspec(dllexport) long __cdecl createServer(	long pServerId)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            createServerID,
            pServerId);
}

__declspec(dllexport) long __cdecl destroyServer(long pServerId)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            destroyServerID,
            pServerId);
}


__declspec(dllexport) long __cdecl setVoxelDimensionsInRealUnits( 	long pSinkId,
        double pVoxelWidthInRealUnits,
        double pVoxelHeightInRealUnits,
        double pVoxelDepthInRealUnits)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            setVoxelDimensionsInRealUnitsID,
            pSinkId,
            pVoxelWidthInRealUnits,
            pVoxelHeightInRealUnits,
            pVoxelDepthInRealUnits);
    sJavaLastError = "Error while setting voxel dimensions in real units";
    return -1;
}



__declspec(dllexport) long __cdecl setVolumeIndexAndTime( 			long pSinkId,
        long pVolumeIndex,
        double pVolumeTimeInSeconds)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            setVolumeIndexAndTimeID,
            pSinkId,
            pVolumeIndex,
            pVolumeTimeInSeconds);
}


__declspec(dllexport) long __cdecl send8bitUINTVolumeDataToSink( 	long pSinkId,
        long pChannelId,
        char *pBufferAddress,
        __int64 pBufferLength,																 	
        long pWidth,
        long pHeight,
        long pDepth)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            send8bitUINTVolumeDataToSinkID,
            pSinkId,
            pChannelId,
            (__int64)pBufferAddress,
            (__int64)pBufferLength,
            pWidth,
            pHeight,
            pDepth);
}

__declspec(dllexport) long __cdecl send16bitUINTVolumeDataToSink( 	long pSinkId,
        long pChannelId,
        short *pBufferAddress,
        __int64 pBufferLength,
        long pWidth,
        long pHeight,
        long pDepth)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            send16bitUINTVolumeDataToSinkID,
            pSinkId,
            pChannelId,
            (__int64)pBufferAddress,
            (__int64)pBufferLength,
            pWidth,
            pHeight,
            pDepth);
}

__declspec(dllexport) long __cdecl setChannelName( 	long pSinkId,
        long pChannelId,
        const char* channelName)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            setChannelNameID,
            pSinkId,
            pChannelId,
            channelName);
}

__declspec(dllexport) long __cdecl setChannelColor( 	long pSinkId,
        long pChannelId,
        float* color)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    jfloatArray jArray = (*lJNIEnv)->NewFloatArray(lJNIEnv, 4*4);

    if(jArray != NULL) {
        (*lJNIEnv)->SetFloatArrayRegion(lJNIEnv, jArray, 0, 4, color);

        return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
                setChannelColorID,
                pSinkId,
                pChannelId,
                &jArray);
    } else {
        fprintf(stderr, "Array was null, returning -1\n");
        return -1;
    }
}

