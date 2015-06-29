#include "interpreter.h"
#include "Python.h"
#include <unistd.h>
#include <stdio.h>
#include "Log/log.h"
#include "PythonPatch/redirects.h"

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

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_startInterpreter(JNIEnv *env, jobject obj, jstring jAppTag, jstring jPythonHome, jstring jPythonTemp) {
    jPyInterpreter = (*env)->NewGlobalRef(env, obj);
    jEnv = env;

    setupStdinEmulation();
    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    setApplicationTag(appTag);
    setupOutputRedirection();
    setStdoutRedirect(redirectToTextView);
    setStderrRedirect(redirectToTextView);
    PyOS_ReadlineFunctionPointer = readLineFromJavaInput;

    const char *pythonHome = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    Py_SetPythonHome((char*) pythonHome);
//
//    sleep(1);
//    LOG(pythonHome);
//    printf("%d", chdir(pythonHome));
//    int bufsize = 1024;
//    char tmpbuf[1024];
//    char *res = NULL;
//    res = getcwd(tmpbuf, bufsize - 1);
//    tmpbuf[bufsize] = 0;
//    printf("res: %s, tmpbuf: %s\n", res, tmpbuf);
//    fflush(stdout);

    const char *pythonTemp = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    setenv("TMPDIR", pythonTemp, 1);
    (*env)->ReleaseStringUTFChars(env, jPythonTemp, pythonTemp);
    //Py_VerboseFlag = 1;
    //Py_DebugFlag = 1;
    const int argc = 1;
    char* argv[] = {"TestName"};
    LOG("1");
    Py_Main(argc, argv);
    LOG("2");
    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);
    (*env)->DeleteGlobalRef(env, jPyInterpreter);
    jPyInterpreter = NULL;
}

JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_dispatchKey(JNIEnv *env, jobject obj, jint character) {
    putc(character, stdin_writer);
    fflush(stdin_writer);
}
