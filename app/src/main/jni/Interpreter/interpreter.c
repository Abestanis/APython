#include "interpreter.h"
#include "Python.h"
#include <unistd.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include "Log/log.h"
#include "py_utils.h"

jobject jPyInterpreter = NULL;
static JavaVM *Jvm = NULL;
JNIEnv *jEnv = NULL;
FILE *stdin_writer;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    Jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_getPythonVersion(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, PY_VERSION);
}

void redirectToTextView(const char* string) {
    if (jPyInterpreter == NULL) { return; }
    JNIEnv* env = NULL;
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;

    (*Jvm)->AttachCurrentThread(Jvm, &env, NULL);

    if (cls == NULL) {
        cls = (*env)->GetObjectClass(env, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/PythonInterpreter'!");
        mid = (*env)->GetMethodID(env, cls, "addTextToOutput", "(Ljava/lang/String;)V");
        ASSERT(mid, "Could not find the function 'addTextToOutput' in the Android class!");
    }
    (*env)->CallVoidMethod(env, jPyInterpreter, mid, (*env)->NewStringUTF(env, string));
    (*Jvm)->DetachCurrentThread(Jvm);
}

void setupStdinEmulation() {
    int p[2];

    // error return checks omitted
    pipe(p);
    dup2(p[0], fileno(stdin));

    stdin_writer = fdopen(p[1], "w");
}

char* readLineFromJavaInput(FILE *sys_stdin, FILE *sys_stdout, char *prompt) {
    if (jEnv == NULL || jPyInterpreter == NULL) { return; }
    static jclass *cls   = NULL;
    static jmethodID mid = NULL;

    if (cls == NULL) {
        cls = (*jEnv)->GetObjectClass(jEnv, jPyInterpreter);
        ASSERT(cls, "Could not get an instance from class 'com/apython/python/pythonhost/PythonInterpreter'!");
        mid = (*jEnv)->GetMethodID(jEnv, cls, "readLine", "(Ljava/lang/String;)Ljava/lang/String;");
        ASSERT(mid, "Could not find the function 'readLine' in the Android class!");
    }
    jstring jLine = (*jEnv)->CallObjectMethod(jEnv, jPyInterpreter, mid, (*jEnv)->NewStringUTF(jEnv, prompt));
    if (jLine == NULL) {
        return NULL;
    }

    const char *line = (*jEnv)->GetStringUTFChars(jEnv, jLine, 0);
    char *lineCopy = (char*) PyMem_Malloc(1 + strlen(line)); // TODO: Use PyMem_RawMalloc from python version 3.4
    if (lineCopy == NULL) {
        LOG("Could not copy string!");
        return NULL;
    }
    strcpy(lineCopy, line);
    (*jEnv)->ReleaseStringUTFChars(jEnv, jLine, line);
    return lineCopy;
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_runInterpreter(
           JNIEnv *env, jobject obj, jstring jProgramPath, jstring jLibPath, jstring jPythonHome,
           jstring jPythonTemp, jstring jXDGBasePath, jstring jAppTag, jobjectArray jArgs, jboolean redirectOutput) {
    jPyInterpreter = (*env)->NewGlobalRef(env, obj);
    jEnv = env;
    int i;

    setupStdinEmulation();
    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    setApplicationTag(appTag);
    setupOutputRedirection();
    if (redirectOutput == JNI_TRUE) {
        setStdoutRedirect(redirectToTextView);
        setStderrRedirect(redirectToTextView);
    }
    PyOS_ReadlineFunctionPointer = readLineFromJavaInput;

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
    argv = malloc(sizeof(char) * (argc + 1)); // TODO: Fix me!
    if (argv == NULL) {
        LOG_ERROR("Failed to allocate space for the argument list!");
        return;
    }

    argv[0] = (char*) programName;
    if (jArgs != NULL) {
        for (i = 0; i < argc; i++) {
            jstring jArgument = (*env)->GetObjectArrayElement(env, jArgs, i);
            const char *argument = (*env)->GetStringUTFChars(env, jArgument, 0);
            char *arg = malloc(sizeof(char) * (strlen(argument) + 1));
            if (argv == NULL) {
                LOG_ERROR("Failed to allocate space for an argument!");
                return;
            }
            arg = strcpy(arg, argument);
            argv[i + 1] = arg;
            (*env)->ReleaseStringUTFChars(env, jArgument, argument);
        }
        argc++;
    }

    //Py_VerboseFlag = 1;
    //Py_DebugFlag = 1;
    LOG("Starting...");
    Py_Main(argc, argv);

    for (i = 1; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);
    (*env)->ReleaseStringUTFChars(env, jLibPath, pythonLibs);
    (*env)->ReleaseStringUTFChars(env, jProgramPath, programName);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    jPyInterpreter = NULL;
}

JNIEXPORT int JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_runInterpreterForResult(
           JNIEnv *env, jobject obj, jstring jProgramPath, jstring jLibPath, jstring jPythonHome,
           jstring jPythonTemp, jstring jXDGBasePath, jstring jAppTag, jobjectArray jArgs) {
    jPyInterpreter = (*env)->NewGlobalRef(env, obj);
    jEnv = env;
    int i;

    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    setApplicationTag(appTag);
    setupOutputRedirection();

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
    argv = malloc(sizeof(char) * (argc + 1)); // TODO: Fix me!
    if (argv == NULL) {
        LOG_ERROR("Failed to allocate space for the argument list!");
        return -1;
    }

    argv[0] = (char*) programName;
    if (jArgs != NULL) {
        for (i = 0; i < argc; i++) {
            jstring jArgument = (*env)->GetObjectArrayElement(env, jArgs, i);
            const char *argument = (*env)->GetStringUTFChars(env, jArgument, 0);
            char *arg = malloc(sizeof(char) * (strlen(argument) + 1));
            if (argv == NULL) {
                LOG_ERROR("Failed to allocate space for an argument!");
                return -1;
            }
            arg = strcpy(arg, argument);
            argv[i + 1] = arg;
            (*env)->ReleaseStringUTFChars(env, jArgument, argument);
        }
        argc++;
    }

    pid_t pid = fork();

    if (pid == 0) {
        //Py_VerboseFlag = 1;
        //Py_DebugFlag = 1;
        LOG("Starting...");
        exit(Py_Main(argc, argv));
    }
    int status;
    waitpid(pid, &status, 0);

    for (i = 1; i < argc; i++) {
        free(argv[i]);
    }
    //free(argv); // TODO: Investigate why it crashes here
    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);
    (*env)->ReleaseStringUTFChars(env, jLibPath, pythonLibs);
    (*env)->ReleaseStringUTFChars(env, jProgramPath, programName);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    jPyInterpreter = NULL;
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    return 1;
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_dispatchKey(JNIEnv *env, jobject obj, jint character) {
    putc(character, stdin_writer);
    fflush(stdin_writer);
}
