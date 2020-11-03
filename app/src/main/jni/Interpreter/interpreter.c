#include "interpreter.h"
#include <errno.h>
#include <string.h>
#include "log.h"
#include "py_utils.h"
#include "py_compatibility.h"
#include "terminal.h"

jobject jPyInterpreter = NULL;
static JavaVM *Jvm = NULL;

jint __unused JNI_OnLoad(JavaVM *vm, void __unused *reserved) {
    Jvm = vm;
    return JNI_VERSION_1_6;
}

/* Helper functions */

int getFdFromFileDescriptor(JNIEnv *env, jobject fileDescriptor) {
    static jclass *fileDescriptorClass = NULL;
    static jfieldID fdFieldId = NULL;
    
    // TODO: Initialize these once.
    // TODO: https://developer.android.com/training/articles/perf-jni.html#jclass_jmethodID_and_jfieldID
    if (fileDescriptorClass == NULL) {
        fileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");
        ASSERT(fileDescriptorClass, "Could not find class 'java/io/FileDescriptor'!");
        fdFieldId = (*env)->GetFieldID(env, fileDescriptorClass, "descriptor", "I");
        ASSERT(fdFieldId, "Could not find the 'descriptor' field of the FileDescriptor class!");
    }
    return (*env)->GetIntField(env, fileDescriptor, fdFieldId);
}

/* JNI to JAVA functions */

void exitHandler(int exitCode) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env = NULL;
    static jclass *interpreterClass = NULL;
    static jmethodID setExitCodeMethodId;
    int detached = (*Jvm)->GetEnv(Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) { (*Jvm)->AttachCurrentThread(Jvm, &env, NULL); }
    
    if (interpreterClass == NULL) {
        interpreterClass = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(interpreterClass, "Could not get an instance from class 'com/apython/python/pythonhost/interpreter/PythonInterpreter'!");
        setExitCodeMethodId = (*env)->GetMethodID(env, interpreterClass, "setExitCode", "(I)V");
        ASSERT(setExitCodeMethodId, "Could not find the function 'setExitCode' in the Android class!");
    }
    (*env)->CallVoidMethod(env, jPyInterpreter, setExitCodeMethodId, exitCode);
    if (detached) { (*Jvm)->DetachCurrentThread(Jvm); }
}

/* JNI functions */

JNIEXPORT jstring NATIVE_FUNCTION(interpreter_PythonInterpreter_nativeGetPythonVersion)(
        JNIEnv *env, jobject __unused obj, jstring jPythonLibName) {
    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    setPythonLibrary(pythonLibName);
    (*env)->ReleaseStringUTFChars(env, jPythonLibName, pythonLibName);
    return (*env)->NewStringUTF(env, getPythonVersion());
}

JNIEXPORT jint NATIVE_FUNCTION(interpreter_PythonInterpreter_runInterpreter)(
           JNIEnv *env, jobject obj, jstring jPythonLibName, jstring jProgramPath, jstring jLibPath,
           jstring jPyHostLibPath, jstring jPythonHome, jstring jPythonTemp, jstring jXDGBasePath,
           jstring jDataPath, jstring jAppTag, jobjectArray jArgs, jstring jPseudoTerminalPath) {
    int i;
    int result;
    int pseudoTerminalFd = -1;
    jPyInterpreter = (*env)->NewGlobalRef(env, obj);

    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    setApplicationTag(appTag);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);

    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    result = setPythonLibrary(pythonLibName);
    (*env)->ReleaseStringUTFChars(env, jPythonLibName, pythonLibName);
    if (!result) { return 1; }
    if (!call_setExitHandler(exitHandler)) { 
        LOG_ERROR("Failed to set an exit handler for the Python interpreter!");
    }
    if (jPseudoTerminalPath != NULL) {
        const char *pseudoTerminalPath = (*env)->GetStringUTFChars(env, jPseudoTerminalPath, 0);
        pseudoTerminalFd = openSlavePseudoTerminal(pseudoTerminalPath);
        (*env)->ReleaseStringUTFChars(env, jPseudoTerminalPath, pseudoTerminalPath);
        if (pseudoTerminalFd < 0) {
            LOG_ERROR("Failed to create a pseudo terminal"); // TODO: maybe fall back to the pipe method?
            return 1;
        }
    }
    
    const char *programName    = (*env)->GetStringUTFChars(env, jProgramPath, 0);
    const char *pythonLibs     = (*env)->GetStringUTFChars(env, jLibPath, 0);
    const char *pythonHostLibs = (*env)->GetStringUTFChars(env, jPyHostLibPath, 0);
    const char *pythonHome     = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    const char *pythonTemp     = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    const char *xdgBasePath    = (*env)->GetStringUTFChars(env, jXDGBasePath, 0);
    const char *dataPath       = (*env)->GetStringUTFChars(env, jDataPath, 0);
    setupPython(programName, pythonLibs, pythonHostLibs, pythonHome, pythonTemp, xdgBasePath, dataPath);
    (*env)->ReleaseStringUTFChars(env, jLibPath, pythonLibs);
    (*env)->ReleaseStringUTFChars(env, jPyHostLibPath, pythonHostLibs);
    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jPythonTemp, pythonTemp);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->ReleaseStringUTFChars(env, jDataPath, dataPath);

    jsize argc = 1;
    if (jArgs != NULL) {
        argc += (*env)->GetArrayLength(env, jArgs);
    }
    char** argv = NULL;
    argv = malloc(sizeof(char*) * argc);
    if (argv == NULL) {
        LOG_ERROR("Failed to allocate space for the argument list!");
        return 1;
    }

    argv[0] = (char*) programName;
    if (jArgs != NULL) {
        for (i = 1; i < argc; i++) {
            jstring jArgument = (*env)->GetObjectArrayElement(env, jArgs, i - 1);
            const char *argument = (*env)->GetStringUTFChars(env, jArgument, 0);
            argv[i] = strdup(argument);
            (*env)->ReleaseStringUTFChars(env, jArgument, argument);
            if (argv[i] == NULL) {
                LOG_ERROR("Failed to allocate space for argument %d ('%s')!", i, argument);
                for (i--; i > 1; i--) {
                    free(argv[i]);
                }
                free(argv);
                return 1; // TODO: Cleanup
            }
        }
    }
    
    result = runPythonInterpreter(argc, argv);
    if (pseudoTerminalFd >= 0) {
        disconnectFromPseudoTerminal(pseudoTerminalFd);
    }

    int detached = (*Jvm)->GetEnv (Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) {
        (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);
    }
    
    (*env)->ReleaseStringUTFChars(env, jProgramPath, programName);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    jPyInterpreter = NULL;
    closePythonLibrary();
    for (i = 1; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    return result;
}

JNIEXPORT void NATIVE_FUNCTION(interpreter_PythonInterpreter_interruptTerminal)(
        JNIEnv *env, jclass __unused cls, jobject fileDescriptor) {
    int masterFd = getFdFromFileDescriptor(env, fileDescriptor);
#ifdef TIOCSIGNAL
    ioctl(masterFd, TIOCSIGNAL, SIGINT);
#else
    struct termios terminalAttributes;
    tcgetattr(masterFd, &terminalAttributes);
    writeToPseudoTerminal(masterFd, (const char *) &terminalAttributes.c_cc[VINTR], sizeof(cc_t));
#endif /* defined TIOCSIGNAL */
}

JNIEXPORT jobject NATIVE_FUNCTION(interpreter_PythonInterpreter_openPseudoTerminal)(
        JNIEnv *env, jclass __unused cls) {
    int masterFd;
    if ((masterFd = createPseudoTerminal()) < 0) {
        return NULL;
    }
    return createFileDescriptor(env, masterFd);
}

JNIEXPORT void NATIVE_FUNCTION(interpreter_PythonInterpreter_closePseudoTerminal)(
        JNIEnv *env, jclass __unused cls, jobject fileDescriptor) {
    int masterFd = getFdFromFileDescriptor(env, fileDescriptor);
    closePseudoTerminal(masterFd);
}

JNIEXPORT jstring NATIVE_FUNCTION(interpreter_PythonInterpreter_getPseudoTerminalPath)(
        JNIEnv *env, jclass __unused cls, jobject fileDescriptor) {
    char* slavePath;
    int masterFd = getFdFromFileDescriptor(env, fileDescriptor);
    if ((slavePath = getPseudoTerminalSlavePath(masterFd)) == NULL) {
        return NULL;
    }
    return (*env)->NewStringUTF(env, slavePath);
}

JNIEXPORT jstring NATIVE_FUNCTION(interpreter_PythonInterpreter_getEnqueueInput)(
        JNIEnv *env, jclass __unused cls, jobject fileDescriptor) {
    static const int INPUT_BUFFER_LENGTH = 8192;
    int masterFd;
    char* input = malloc(sizeof(char) * INPUT_BUFFER_LENGTH);
    if (input == NULL) {
        LOG_ERROR("Failed to read enqueued input: Out of memory!");
        return NULL;
    }
    masterFd = getFdFromFileDescriptor(env, fileDescriptor);
    readFromPseudoTerminalStdin(masterFd, input, INPUT_BUFFER_LENGTH);
    jstring jInput = (*env)->NewStringUTF(env, input);
    free(input);
    return jInput;
}

JNIEXPORT void NATIVE_FUNCTION(interpreter_PythonInterpreter_setPseudoTerminalSize)(
        JNIEnv *env, jclass __unused cls, jobject fileDescriptor,
        jint width, jint height, jint pixelWidth, jint pixelHeight) {
    int masterFd;
    struct winsize size = {
            (unsigned short) height, (unsigned short) width,
            (unsigned short) pixelWidth, (unsigned short) pixelHeight
    };
    masterFd = getFdFromFileDescriptor(env, fileDescriptor);
    ioctl(masterFd, TIOCSWINSZ, &size);
}
