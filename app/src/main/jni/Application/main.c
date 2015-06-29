#include "main.h"
#include "Python.h"
#include "Log/log.h"

JNIEXPORT jint JNICALL startApp(JNIEnv *env, jobject obj, jstring jPythonAppBase, jstring jPythonHome, jstring jPythonTemp, jstring jAppTag) {
    const char *appTag = (*env)->GetStringUTFChars(env, jAppTag, 0);
    int res;
    FILE *startFile = NULL;
    char* fileName = "main.py";
    const char* filePath = NULL;

    setApplicationTag(appTag);
    setupOutputRedirection();

    const char *pythonHome = (*env)->GetStringUTFChars(env, jPythonHome, 0);
    Py_SetPythonHome((char*) pythonHome);
    const char *pythonTemp = (*env)->GetStringUTFChars(env, jPythonTemp, 0);
    setenv("TMPDIR", pythonTemp, 1);
    (*env)->ReleaseStringUTFChars(env, jPythonTemp, pythonTemp);

    const char *pythonAppBase = (*env)->GetStringUTFChars(env, jPythonAppBase, 0);
    filePath = malloc(strlen(pythonAppBase) + 1 + strlen(fileName) + 1);
    ASSERT(filePath != NULL, "Could not create the path to Python start file: Out of memory!");
    strcpy((char*) filePath, pythonAppBase);
    strcat((char*) filePath, "/");
    strcat((char*) filePath, fileName);
    LOG(filePath);

//    Py_VerboseFlag = 1;
//    Py_DebugFlag = 1;

    //Py_SetProgramName("Test"); // Todo: Change
    Py_Initialize();

    PyObject *pyAppBase = PyString_FromString(pythonAppBase);
    PyObject *sys_path = PySys_GetObject("path");
    if ((sys_path == NULL) || (PyList_Insert(sys_path, 0, pyAppBase) == -1)) {
        LOG_WARN("Could not insert the appBase as sys.path[0]!");
    }

    startFile = fopen(filePath, "r");
    if (startFile == NULL) {
        LOG_ERROR("Could not open start file:");
        char* errorMsg = strerror(errno);
        LOG_ERROR(errorMsg);
        return -1;
    }

    LOG("Starting...");
    res = PyRun_SimpleFileEx(startFile, filePath, 1);
    fflush(stdout);
    fflush(stderr);
    if (res != 0) {
        LOG_ERROR("App threw an error!");
    } else {
        LOG("App executed normally!");
    }
    Py_Finalize();
    (*env)->ReleaseStringUTFChars(env, jPythonHome, pythonHome);
    (*env)->ReleaseStringUTFChars(env, jAppTag, appTag);
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
    {"startApp", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I", (void *)&startApp},
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
