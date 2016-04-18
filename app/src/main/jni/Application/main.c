#include "main.h"
#include <stdlib.h>
#include <string.h>
#include "Log/log.h"
#include "Interpreter/py_utils.h"


JNIEXPORT jint JNICALL startApp(JNIEnv *env, jobject obj, jstring jPythonLibName, jstring jPythonExecutable, jstring jLibPath, jstring jPyHostLibPath, jstring jPythonHome, jstring jPythonTemp, jstring jXDGBasePath, jstring jPythonAppBase, jstring jAppTag) {
    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    const char *pythonLibName = (*env)->GetStringUTFChars(env, jPythonLibName, 0);
    int res;
    FILE *startFile = NULL;
    char* fileName = "main.py";
    const char* filePath = NULL;

    setApplicationTag(appTag);
    if (!setPythonLibrary(pythonLibName)) { return 1; }

    const char *programName    = (*env)->GetStringUTFChars(env, jPythonExecutable, 0);
    const char *pythonLibs     = (*env)->GetStringUTFChars(env, jLibPath, 0);
    const char *pythonHostLibs = (*env)->GetStringUTFChars(env, jPyHostLibPath, 0);
    const char *pythonHome     = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    const char *pythonTemp     = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    const char *xdgBasePath    = (*env)->GetStringUTFChars(env, jXDGBasePath, 0);
    setupPython(programName, pythonLibs, pythonHostLibs, pythonHome, pythonTemp, xdgBasePath);

    const char *pythonAppBase = (*env)->GetStringUTFChars(env, jPythonAppBase, 0);
    filePath = malloc(strlen(pythonAppBase) + 1 + strlen(fileName) + 1);
    if (filePath == NULL) {
        LOG_ERROR("Could not create the path to Python start file: Out of memory!");
        return 1;
    }
    strcpy((char*) filePath, pythonAppBase);
    strcat((char*) filePath, "/");
    strcat((char*) filePath, fileName);
    LOG(filePath);

//    Py_VerboseFlag = 1;
//    Py_DebugFlag = 1;

//    PyObject *pyAppBase = PyString_FromString(pythonAppBase);
//    PyObject *sys_path = PySys_GetObject("path");
//    if ((sys_path == NULL) || (PyList_Insert(sys_path, 0, pyAppBase) == -1)) {
//        LOG_WARN("Could not insert the appBase as sys.path[0]!");
//    }
//
//    startFile = fopen(filePath, "r");
//    if (startFile == NULL) {
//        LOG_ERROR("Could not open start file:");
//        char* errorMsg = strerror(errno);
//        LOG_ERROR(errorMsg);
//        return -1;
//    }
    int argc = 2;

    char** argv = malloc(sizeof(char) * (argc + 1));
    if (argv == NULL) {
        LOG_ERROR("Failed to allocate space for the argument list!");
        return 1;
    }

    // TODO: Temp
    argv[0] = (char*) programName;
    argv[1] = (char*) filePath;


    res = runPythonInterpreter(argc, argv);
    fflush(stdout);
    fflush(stderr);
    if (res != 0) {
        LOG_ERROR("App threw an error!");
    } else {
        LOG("App executed normally!");
    }

    free((char*) filePath);
    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);
    (*env)->ReleaseStringUTFChars(env, jPythonLibName, pythonLibName);
    (*env)->ReleaseStringUTFChars(env, jLibPath, pythonLibs);
    (*env)->ReleaseStringUTFChars(env, jPyHostLibPath, pythonHostLibs);
    (*env)->ReleaseStringUTFChars(env, jPythonExecutable, programName);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->ReleaseStringUTFChars(env, jPythonAppBase, pythonAppBase);
    closePythonLibrary();
    return res;
}

int jniRegisterNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *jMethods, int numMethods) {
    jclass jClass = NULL;
    LOG("Registering native methods for apps class '%s'.", className);
    jClass = (*env)->FindClass(env, className);
    ASSERT(jClass != NULL, "Was unable to find class '%s'!", className);
    ASSERT((*env)->RegisterNatives(env, jClass, jMethods, numMethods) == 0, "Failed to register native methods for the class!");
}

static JNINativeMethod methods[] = {
    {"startApp", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)I", (void *)&startApp},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    const char* javaClasspath = NULL;
    static const char* propertyName = "python.android.entry.class";

    if((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }

    jclass systemClass = (*env)->FindClass(env, "java/lang/System");
    jmethodID getPropertyMethod = (*env)->GetStaticMethodID(env, systemClass, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
    jstring propertyNameString = (*env)->NewStringUTF(env, propertyName);
    jstring jJavaClassPath = (*env)->CallStaticObjectMethod(env, systemClass, getPropertyMethod, propertyNameString);
    ASSERT(jJavaClassPath != NULL, "System variable '%s' is not set! You must set this variable to point to the java class of your app that implements the native 'startApp' function.", propertyName);
    javaClasspath = (*env)->GetStringUTFChars(env, jJavaClassPath, 0);
    jniRegisterNativeMethods(env, javaClasspath, methods, sizeof(methods)/sizeof(methods[0]));
    (*env)->ReleaseStringUTFChars(env, jJavaClassPath, javaClasspath);
    return JNI_VERSION_1_6;
}
