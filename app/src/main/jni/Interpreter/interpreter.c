#include "interpreter.h"
#include <errno.h>
#include "log.h"
#include "py_utils.h"
#include "py_compatibility.h"
#include "terminal.h"

jobject jPyInterpreter = NULL;
static JavaVM *Jvm = NULL;
PseudoTerminal* pseudoTerminal = NULL;
int isInterrupted = 0;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    Jvm = vm;
    return JNI_VERSION_1_6;
}

/* Java functions */

void redirectOutputToJava(const char *string, size_t len) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env = NULL;
    jbyteArray jOutputByteArray = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;
    int detached = (*Jvm)->GetEnv(Jvm, (void *) &env, JNI_VERSION_1_6) == JNI_EDETACHED;
    if (detached) (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);

    if (cls == NULL) {
        cls = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/interpreter/PythonInterpreter'!");
        mid = (*env)->GetMethodID(env, cls, "addTextToOutput", "([B)V");
        ASSERT(mid, "Could not find the function 'addTextToOutput' in the Android class!");
    }
    jOutputByteArray = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, jOutputByteArray, 0, len, (const jbyte*) string);
    (*env)->CallVoidMethod(env, jPyInterpreter, mid, jOutputByteArray);
    (*env)->DeleteLocalRef(env, jOutputByteArray);
    if (detached) (*Jvm)->DetachCurrentThread(Jvm);
}

char* readLineFromJavaInput(FILE *sys_stdin, FILE *sys_stdout, const char *prompt) {
    if (jPyInterpreter == NULL) { return NULL; }
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
        line = (char*) call_PyMem_Malloc((1 + strlen(lineCopy)) * sizeof(char));
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
        if (isInterrupted) {
            // Remove the emulated input from stdin
            (void) fgetc(sys_stdin);
            line = (char*) call_PyMem_Malloc(1 * sizeof(char));
            line[0] = '\0';
        } else {
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
                        if (errno == EAGAIN) {
                            continue;
                        }
                        LOG_ERROR("Failed to read from input: %s\n", strerror(errno));
                        free(buffer);
                        (*Jvm)->DetachCurrentThread(Jvm);
                        return NULL;
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
            line = (char*) call_PyMem_Malloc((1 + strlen(buffer)) * sizeof(char));
            strcpy(line, buffer);
            free(buffer);
        }
    }
    isInterrupted = 0;
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
           jstring jDataPath, jstring jAppTag, jobjectArray jArgs, jboolean redirectOutput) {
    jPyInterpreter = (*env)->NewGlobalRef(env, obj);
    int i;

    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    setApplicationTag(appTag);

    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    if (!setPythonLibrary(pythonLibName)) { return 1; }
    if (redirectOutput == JNI_TRUE) {
        pseudoTerminal = createPseudoTerminal(redirectOutputToJava, redirectOutputToJava, NULL, NULL);
        set_PyOS_ReadlineFunctionPointer(readLineFromJavaInput);
    } else {
        pseudoTerminal = createPseudoTerminal(NULL, NULL, NULL, NULL);
    }
    if (pseudoTerminal == NULL) {
        LOG_ERROR("Failed to create a pseudo terminal"); // TODO: maybe fall back to the pipe method?
        return 1;
    }

    const char *programName    = (*env)->GetStringUTFChars(env, jProgramPath, 0);
    const char *pythonLibs     = (*env)->GetStringUTFChars(env, jLibPath, 0);
    const char *pythonHostLibs = (*env)->GetStringUTFChars(env, jPyHostLibPath, 0);
    const char *pythonHome     = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    const char *pythonTemp     = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    const char *xdgBasePath    = (*env)->GetStringUTFChars(env, jXDGBasePath, 0);
    const char *dataPath       = (*env)->GetStringUTFChars(env, jDataPath, 0);
    setupPython(programName, pythonLibs, pythonHostLibs, pythonHome, pythonTemp, xdgBasePath, dataPath);

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
    
    int result = runPythonInterpreter(pseudoTerminal, argc, argv);
    closePseudoTerminal(pseudoTerminal);

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
    (*env)->ReleaseStringUTFChars(env, jDataPath, dataPath);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    closePythonLibrary();
    for (i = 1; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    jPyInterpreter = NULL;
    return result;
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_dispatchKey(JNIEnv *env, jobject obj, jint character) {
    if (pseudoTerminal == NULL) {
        LOG_WARN("Tried to dispatch key event to the Python interpreter, but the pseudoTerminal is not initialized yet.");
        return;
    }
    writeToPseudoTerminal(pseudoTerminal, (const char *) &character, 1);
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_sendStringToStdin(JNIEnv *env, jobject obj, jstring jString) {
    if (pseudoTerminal == NULL) {
        LOG_WARN("Tried to write a string to stdin, but the pseudoTerminal is not initialized yet.");
        return;
    }
    const char* string = (*env)->GetStringUTFChars(env, jString, 0);
    writeToPseudoTerminal(pseudoTerminal, string, strlen(string));
    (*env)->ReleaseStringUTFChars(env, jString, string);
}

JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_getEnqueueInput(JNIEnv *env, jclass obj) {
    if (pseudoTerminal == NULL) return NULL;
    static const int INPUT_BUFFER_LENGTH = 8192;
    char* input = malloc(sizeof(char) * INPUT_BUFFER_LENGTH);
    if (input == NULL) {
        LOG_ERROR("Failed to read enqueued input: Out of memory!");
        return NULL;
    }
    readFromPseudoTerminalStdin(pseudoTerminal, input, INPUT_BUFFER_LENGTH);
    return (*env)->NewStringUTF(env, input);
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_interruptInterpreter(JNIEnv *env, jclass obj) {
    isInterrupted = 1;
    interruptPython();
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_stopInterpreter(JNIEnv *env, jclass obj) {
    terminatePython();
}
