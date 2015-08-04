#include "main.h"
#include "Python.h"
#include "Log/log.h"
#include "Interpreter/py_utils.h"


JNIEXPORT jint JNICALL startApp(JNIEnv *env, jobject obj, jstring jPythonExecutable, jstring jLibPath, jstring jPythonHome, jstring jPythonTemp, jstring jXDGBasePath, jstring jPythonAppBase, jstring jAppTag) {
    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    int res;
    FILE *startFile = NULL;
    char* fileName = "main.py";
    const char* filePath = NULL;

    setApplicationTag(appTag);

    const char *programName = (*env)->GetStringUTFChars(env, jPythonExecutable, 0);
    const char *pythonLibs  = (*env)->GetStringUTFChars(env, jLibPath, 0);
    const char *pythonHome  = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    const char *pythonTemp  = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    const char *xdgBasePath = (*env)->GetStringUTFChars(env, jXDGBasePath, 0);
    setupPython(programName, pythonLibs, pythonHome, pythonTemp, xdgBasePath);

    const char *pythonAppBase = (*env)->GetStringUTFChars(env, jPythonAppBase, 0);
    filePath = malloc(strlen(pythonAppBase) + 1 + strlen(fileName) + 1);
    ASSERT(filePath != NULL, "Could not create the path to Python start file: Out of memory!");
    strcpy((char*) filePath, pythonAppBase);
    strcat((char*) filePath, "/");
    strcat((char*) filePath, fileName);
    LOG(filePath);

//    Py_VerboseFlag = 1;
//    Py_DebugFlag = 1;

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
    (*env)->ReleaseStringUTFChars(env, jLibPath, pythonLibs);
    (*env)->ReleaseStringUTFChars(env, jPythonExecutable, programName);
    (*env)->ReleaseStringUTFChars(env, jXDGBasePath, xdgBasePath);
    (*env)->ReleaseStringUTFChars(env, jPythonAppBase, pythonAppBase);
    return res;
}

int jniRegisterNativeMethods(JNIEnv *env, const char *className, const JNINativeMethod *jMethods, int numMethods) {
    jclass jClass = NULL;
    LOG("Registering native methods for apps class:");
    LOG(className);
    jClass = (*env)->FindClass(env, className);
    ASSERT(jClass != NULL, "Was unable to find the class!");
    ASSERT((*env)->RegisterNatives(env, jClass, jMethods, numMethods) == 0, "Failed to register native methods for the class!");
}

static JNINativeMethod methods[] = {
    {"startApp", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)I", (void *)&startApp},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    const char* javaClasspath = getenv("JAVA_PYTHON_MAIN_CLASSPATH");
    ASSERT(javaClasspath != NULL, "Environment variable 'JAVA_PYTHON_MAIN_CLASSPATH' not set!");
    jniRegisterNativeMethods(env, javaClasspath, methods, sizeof(methods)/sizeof(methods[0]));
    return JNI_VERSION_1_6;
}
