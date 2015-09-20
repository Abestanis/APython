#include "interpreter.h"
#include "Log/log.h"
#include "py_utils.h"
#include "py_compatibility.h"

jobject jPyInterpreter = NULL;
static JavaVM *Jvm = NULL;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    Jvm = vm;
    return JNI_VERSION_1_6;
}

/* Java functions */

void redirectOutputToJava(const char *string) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    int detached = (*Jvm)->GetEnv(Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;

    if (detached) {
        (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);
    }

    if (cls == NULL) {
        cls = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/PythonInterpreter'!");
        mid = (*env)->GetMethodID(env, cls, "addTextToOutput", "(Ljava/lang/String;)V");
        ASSERT(mid, "Could not find the function 'addTextToOutput' in the Android class!");
    }
    (*env)->CallVoidMethod(env, jPyInterpreter, mid, (*env)->NewStringUTF(env, string));
    if (detached) {
        (*Jvm)->DetachCurrentThread(Jvm);
    }
}

char* readLineFromJavaInput(FILE *sys_stdin, FILE *sys_stdout, char *prompt) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;

    int detached = (*Jvm)->GetEnv(Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) {
        (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);
    }

    if (cls == NULL) {
        cls = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/PythonInterpreter'!");
        mid = (*env)->GetMethodID(env, cls, "readLine", "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(mid, "Could not find the function 'readLine' in the Android class!");
    }
    jstring jLine = (*env)->CallObjectMethod(env, jPyInterpreter, mid, (*env)->NewStringUTF(env, prompt));
    if (jLine == NULL) {
        return NULL;
    }

    const char *line = (*env)->GetStringUTFChars(env, jLine, 0);
    char *lineCopy = (char*) call_PyMem_Malloc(1 + strlen(line));
    if (lineCopy == NULL) {
        LOG("Could not copy string!");
        return NULL;
    }
    strcpy(lineCopy, line);
    (*env)->ReleaseStringUTFChars(env, jLine, line);
    (*Jvm)->DetachCurrentThread(Jvm);
    return lineCopy;
}

/* JNI functions */


JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_nativeGetPythonVersion(JNIEnv *env, jclass clazz, jstring jPythonLibName) {
    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    setPythonLibrary(pythonLibName);
    (*env)->ReleaseStringUTFChars(env, jPythonLibName, pythonLibName);
    return (*env)->NewStringUTF(env, getPythonVersion());
}

JNIEXPORT jint JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_runInterpreter(
           JNIEnv *env, jobject obj, jstring jPythonLibName, jstring jProgramPath, jstring jLibPath, jstring jPythonHome,
           jstring jPythonTemp, jstring jXDGBasePath, jstring jAppTag, jobjectArray jArgs, jboolean redirectOutput) {
    jPyInterpreter = (*env)->NewGlobalRef(env, obj);
    int i;

    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    setApplicationTag(appTag);

    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    if (!setPythonLibrary(pythonLibName)) { return 1; }
    if (redirectOutput == JNI_TRUE) {
        setStdoutRedirect(redirectOutputToJava);
        setStderrRedirect(redirectOutputToJava);
        set_PyOS_ReadlineFunctionPointer(readLineFromJavaInput);
    }

    const char *programName = (*env)->GetStringUTFChars(env, jProgramPath, 0);
    const char *pythonLibs  = (*env)->GetStringUTFChars(env, jLibPath, 0);
    const char *pythonHome  = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    const char *pythonTemp  = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    const char *xdgBasePath = (*env)->GetStringUTFChars(env, jXDGBasePath, 0);
    setupPython(programName, pythonLibs, pythonHome, pythonTemp, xdgBasePath);

    jsize argc = 1;
    if (jArgs != NULL) {
        argc = (*env)->GetArrayLength(env, jArgs);
    }
    char** argv = NULL;
    argv = malloc(sizeof(char) * (argc + 1));
    if (argv == NULL) {
        LOG_ERROR("Failed to allocate space for the argument list!");
        return 1;
    }

    argv[0] = (char*) programName;
    if (jArgs != NULL) {
        for (i = 0; i < argc; i++) {
            jstring jArgument = (*env)->GetObjectArrayElement(env, jArgs, i);
            const char *argument = (*env)->GetStringUTFChars(env, jArgument, 0);
            char *arg = malloc(sizeof(char) * (strlen(argument) + 1));
            if (argv == NULL) {
                LOG_ERROR("Failed to allocate space for argument %d ('%s')!", i, argument);
                return 1;
            }
            arg = strcpy(arg, argument);
            argv[i + 1] = arg;
            (*env)->ReleaseStringUTFChars(env, jArgument, argument);
        }
        argc++;
    }

    int result = runPythonInterpreter(argc, argv);

    int detached = (*Jvm)->GetEnv (Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) {
        (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);
    }

    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);
    (*env)->ReleaseStringUTFChars(env, jPythonLibName, pythonLibName);
    (*env)->ReleaseStringUTFChars(env, jLibPath, pythonLibs);
    (*env)->ReleaseStringUTFChars(env, jProgramPath, programName);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    closePythonLibrary();
    jPyInterpreter = NULL;
    return result;
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_dispatchKey(JNIEnv *env, jobject obj, jint character) {
    if (stdin_writer == NULL) {
        LOG_WARN("Tried to dispatch kex event to the Python interpreter, but the input pipe is not initialized yet.");
        return;
    }
    putc(character, stdin_writer);
    fflush(stdin_writer);
}
