#include "interpreter.h"
#include <errno.h>
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

void redirectOutputToJava(const char *string, int len) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env = NULL;
    jbyteArray jOutputByteArray = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    int detached = (*Jvm)->GetEnv(Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;

    if (detached) {
        (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);
    }

    if (cls == NULL) {
        cls = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/interpreter/PythonInterpreter'!");
        mid = (*env)->GetMethodID(env, cls, "addTextToOutput", "([B)V");
        ASSERT(mid, "Could not find the function 'addTextToOutput' in the Android class!");
    }
    jOutputByteArray = (*env)->NewByteArray(env, len + 1);
    (*env)->SetByteArrayRegion(env, jOutputByteArray, 0, len + 1, (const jbyte*) string);
    (*env)->CallVoidMethod(env, jPyInterpreter, mid, jOutputByteArray);
    (*env)->DeleteLocalRef(env, jOutputByteArray);
    if (detached) {
        (*Jvm)->DetachCurrentThread(Jvm);
    }
}

char* readLineFromJavaInput(FILE *sys_stdin, FILE *sys_stdout, const char *prompt) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    char *line = NULL;

    int detached = (*Jvm)->GetEnv(Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) {
        (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);
    }

    if (cls == NULL) {
        cls = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/interpreter/PythonInterpreter'!");
        mid = (*env)->GetMethodID(env, cls, "readLine", "(Ljava/lang/String;Z)Ljava/lang/String;");
        ASSERT(mid, "Could not find the function 'readLine' in the Android class!");
    }
    PyOS_InputHookFunc PyOS_InputHook = get_PyOS_InputHook();
    jstring jLine = (*env)->CallObjectMethod(env, jPyInterpreter, mid, (*env)->NewStringUTF(env, prompt),
                                             PyOS_InputHook == NULL ? JNI_TRUE : JNI_FALSE);
    if (PyOS_InputHook == NULL) {
        // Java has blocked until we had an input
        if (jLine == NULL) {
            (*Jvm)->DetachCurrentThread(Jvm);
            return NULL;
        }
        const char* lineCopy = (*env)->GetStringUTFChars(env, jLine, 0);
        line = (char*) call_PyMem_Malloc(1 + strlen(lineCopy));
        if (line == NULL) {
            LOG("Could not copy string!");
            (*Jvm)->DetachCurrentThread(Jvm);
            return NULL;
        }
        strcpy(line, lineCopy);
        (*env)->ReleaseStringUTFChars(env, jLine, lineCopy);
    } else {
        // Java will not block, instead PYOS_InputHook does
        (void) PyOS_InputHook();
        static const int BUFFER_SIZE = 10;
        char* buffer = malloc(sizeof(char) * BUFFER_SIZE);
        char* bufferPointer = buffer;
        int strLen = 0;
        if (buffer == NULL) {
            LOG_ERROR("Out of memory: Could not allocate input buffer!\n");
            (*Jvm)->DetachCurrentThread(Jvm);
            return NULL;
        }
        do {
            clearerr(sys_stdin);
            if (fgets(bufferPointer, BUFFER_SIZE, sys_stdin) == NULL) {
                if (ferror(sys_stdin)) {
                    if (errno != EAGAIN) {
                        LOG_ERROR("Failed to read from input: %s\n", strerror(errno));
                        free(buffer);
                        (*Jvm)->DetachCurrentThread(Jvm);
                        return NULL;
                    }
                    *bufferPointer = '\0';
                }
            }
            strLen += strlen(bufferPointer);
            if (buffer[strLen - 1] == '\n') {
                break;
            }
            bufferPointer = malloc(sizeof(char) * (strLen + BUFFER_SIZE));
            if (bufferPointer == NULL) {
                LOG_ERROR("Failed to allocate space for the input buffer!\n");
                free(buffer);
                (*Jvm)->DetachCurrentThread(Jvm);
                return NULL;
            }
            memcpy(bufferPointer, buffer, sizeof(char) * strLen);
            free(buffer);
            buffer = bufferPointer;
            bufferPointer = buffer + strLen;
        } while (1);
        line = (char*) call_PyMem_Malloc(1 + strlen(buffer));
        strcpy(line, buffer);
        free(buffer);
    }
    (*Jvm)->DetachCurrentThread(Jvm);
    return line;
}

/* JNI functions */

JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_nativeGetPythonVersion(JNIEnv *env, jclass clazz, jstring jPythonLibName) {
    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    setPythonLibrary(pythonLibName);
    (*env)->ReleaseStringUTFChars(env, jPythonLibName, pythonLibName);
    return (*env)->NewStringUTF(env, getPythonVersion());
}

JNIEXPORT jint JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_runInterpreter(
           JNIEnv *env, jobject obj, jstring jPythonLibName, jstring jProgramPath, jstring jLibPath,
           jstring jPyHostLibPath, jstring jPythonHome, jstring jPythonTemp, jstring jXDGBasePath,
           jstring jAppTag, jobjectArray jArgs, jboolean redirectOutput) {
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

    const char *programName    = (*env)->GetStringUTFChars(env, jProgramPath, 0);
    const char *pythonLibs     = (*env)->GetStringUTFChars(env, jLibPath, 0);
    const char *pythonHostLibs = (*env)->GetStringUTFChars(env, jPyHostLibPath, 0);
    const char *pythonHome     = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    const char *pythonTemp     = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    const char *xdgBasePath    = (*env)->GetStringUTFChars(env, jXDGBasePath, 0);
    setupPython(programName, pythonLibs, pythonHostLibs, pythonHome, pythonTemp, xdgBasePath);

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
    (*env)->ReleaseStringUTFChars(env, jPyHostLibPath, pythonHostLibs);
    (*env)->ReleaseStringUTFChars(env, jProgramPath, programName);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    closePythonLibrary();
    jPyInterpreter = NULL;
    return result;
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_dispatchKey(JNIEnv *env, jobject obj, jint character) {
    if (stdin_writer == NULL) {
        LOG_WARN("Tried to dispatch key event to the Python interpreter, but the input pipe is not initialized yet.");
        return;
    }
    putc(character, stdin_writer);
    fflush(stdin_writer);
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_sendStringToStdin(JNIEnv *env, jobject obj, jstring jString) {
    if (stdin_writer == NULL) {
        LOG_WARN("Tried to write a string to stdin, but the input pipe is not initialized yet.");
        return;
    }
    const char* string = (*env)->GetStringUTFChars(env, jString, 0);
    fputs(string, stdin_writer);
    (*env)->ReleaseStringUTFChars(env, jString, string);
    fflush(stdin_writer);
}

JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_getEnqueueInput(JNIEnv *env, jclass obj) {
    static const int INPUT_BUFFER_LENGTH = 8192;
    char* input = malloc(sizeof(char) * INPUT_BUFFER_LENGTH);
    if (input == NULL) {
        LOG_ERROR("Failed to read enqueued input: Out of memory!");
        return NULL;
    }
    readFromStdin(input, INPUT_BUFFER_LENGTH);
    return (*env)->NewStringUTF(env, input);
}
