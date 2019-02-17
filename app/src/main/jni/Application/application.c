#include "application.h"
#include <stdlib.h>
#include <string.h>

#define PY_HOST_PACKAGE_PATH            "com.apython.python.pythonhost"
#define APP_INTERPRETER_CLASS_PATH      PY_HOST_PACKAGE_PATH ".interpreter.app.AppInterpreter"
#define APP_INTERPRETER_CONSTRUCTOR_SIG "(Landroid/content/Context;Landroid/app/Activity;Ljava/lang/String;)V"
#define WRAPPER_CLASS_PATH_PROPERTY     "python.android.app.wrapper.class"

#define SET_WINDOW_DEF        "setWindow", "(ILandroid/view/ViewGroup;)Ljava/lang/Object;"
#define ON_ACTIVITY_LCE_DEF   "onActivityLifecycleEvent", "(I)V"
#define SET_LOG_TAG_DEF       "setLogTag", "(Ljava/lang/String;)V"
#define START_INTERPRETER_DEF "startInterpreter", "([Ljava/lang/String;)I"

#define ARRAY_LEN(array) (sizeof(array) / sizeof((array)[0]))

static JNINativeMethod nativeJMethods[] = {
    { "loadPythonHost", "(Landroid/app/Activity;Ljava/lang/String;)Z",
                        (void *)&loadPythonHost },
    { "setLogTag", "(Ljava/lang/String;)V", (void *)&setLogTag },
    { SET_WINDOW_DEF,        (void *)&setWindow },
    { ON_ACTIVITY_LCE_DEF,   (void *)&onActivityLifecycleEvent },
    { START_INTERPRETER_DEF, (void *)&startInterpreter },
};

jobject appInterpreter = NULL;

jstring getHostWrapperClassPath(JNIEnv *env) {
    jclass systemClass = (*env)->FindClass(env, "java/lang/System");
    jmethodID getPropertyMethod = (*env)->GetStaticMethodID(env, systemClass, "getProperty",
                                                            "(Ljava/lang/String;)Ljava/lang/String;");
    jstring propertyNameString = (*env)->NewStringUTF(env, WRAPPER_CLASS_PATH_PROPERTY);
    jstring jJavaClassPath = (*env)->CallStaticObjectMethod(env, systemClass, getPropertyMethod,
                                                            propertyNameString);
    (*env)->DeleteLocalRef(env, propertyNameString);
    ASSERT(jJavaClassPath != NULL, "System variable '%s' is not set! You must set this variable to "
                                   "point to the Python host wrapper class!", WRAPPER_CLASS_PATH_PROPERTY);
    return jJavaClassPath;
}

int appRegisterNativeMethods(JNIEnv *env) {
    jstring jJavaClassPath = getHostWrapperClassPath(env);
    const char* javaClasspath = (*env)->GetStringUTFChars(env, jJavaClassPath, 0);
    jclass jClass = NULL;
    LOG("Registering native methods for apps class '%s'.", javaClasspath);
    jClass = (*env)->FindClass(env, javaClasspath);
    ASSERT(jClass != NULL, "Was unable to find class '%s'!", javaClasspath);
    ASSERT((*env)->RegisterNatives(env, jClass, nativeJMethods, ARRAY_LEN(nativeJMethods)) == 0,
           "Failed to register native methods for the class!");
    (*env)->ReleaseStringUTFChars(env, jJavaClassPath, javaClasspath);
    return 1;
}

JNIEXPORT jint __unused JNI_OnLoad(JavaVM *vm, void __unused *reserved) {
    JNIEnv *env = NULL;
    if((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    appRegisterNativeMethods(env);
    return JNI_VERSION_1_6;
}

void syncLogTagToPyHost(JNIEnv *env) {
    if (appInterpreter != NULL) {
        static jmethodID mid = NULL;
        if (mid == NULL) {
            jclass *cls = (*env)->GetObjectClass(env, appInterpreter);
            ASSERT(cls, "Could not get an instance from the appInterpreter!");
            mid = (*env)->GetMethodID(env, cls, SET_LOG_TAG_DEF);
            ASSERT(mid, "Could not find the function '%s' (%s) in the Android class!", SET_LOG_TAG_DEF);
        }
        (*env)->CallVoidMethod(env, appInterpreter, mid, (*env)->NewStringUTF(env, logTag));
    }
}

/*
 * hostingAppActivity.createPackageContext(Util.class.getPackage().getName(), Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY)
 */
jobject getPythonHostContext(JNIEnv *env, jobject hostingAppActivity) {
    jclass activityCls = (*env)->GetObjectClass(env, hostingAppActivity);
    jclass contextCls  = (*env)->FindClass(env, "android/content/Context");
    ASSERT(contextCls != NULL, "Failed to find androids Context class!");
    #define CREATE_PACKAGE_CONTEXT_SIG "createPackageContext", "(Ljava/lang/String;I)Landroid/content/Context;"
    jmethodID createPackageContextMId = (*env)->GetMethodID(env, activityCls, CREATE_PACKAGE_CONTEXT_SIG);
    ASSERT(createPackageContextMId != NULL, "Failed to find method %s (%s) in androids Application class",
                                            CREATE_PACKAGE_CONTEXT_SIG);
    #undef CREATE_PACKAGE_CONTEXT_SIG
    
    jfieldID includeCodeFId = (*env)->GetStaticFieldID(env, contextCls, "CONTEXT_INCLUDE_CODE", "I");
    ASSERT(includeCodeFId != NULL, "Failed to find the static int field 'CONTEXT_INCLUDE_CODE' in "
                                   "Androids Context class");
    jfieldID ignoreSecurityFId = (*env)->GetStaticFieldID(env, contextCls, "CONTEXT_IGNORE_SECURITY", "I");
    ASSERT(ignoreSecurityFId != NULL, "Failed to find the static int field 'CONTEXT_IGNORE_SECURITY' in "
                                      "Androids Context class");
    int flags = (*env)->GetStaticIntField(env, contextCls, includeCodeFId) |
                (*env)->GetStaticIntField(env, contextCls, ignoreSecurityFId);
    jstring pyHostPackageName = (*env)->NewStringUTF(env, PY_HOST_PACKAGE_PATH);
    jobject pyHostContext = (*env)->CallObjectMethod(env, hostingAppActivity, createPackageContextMId,
                                                     pyHostPackageName, flags);
    (*env)->DeleteLocalRef(env, pyHostPackageName);
    return pyHostContext;
}

JNIEXPORT jboolean JNICALL loadPythonHost(
        JNIEnv *env, jobject __unused obj, jobject hostingAppActivity, jstring pythonVersion) {
    jmethodID loadClass;
    jmethodID constructor;
    jclass classLoaderCls;
    jobject appInterpreterClass;
    jobject pyHostContext;
    jobject classLoader;
    
    LOG("Loading Python host..."); // TODO: Remove
    pyHostContext = getPythonHostContext(env, hostingAppActivity);
    if (pyHostContext == NULL) {
        LOG_ERROR("Failed to load the Python host context!");
        return JNI_FALSE;
    }
    
    // Get the class loader
    #define GET_CLASSLOADER_SIG "getClassLoader", "()Ljava/lang/ClassLoader;"
    jmethodID getClassLoaderMId = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, pyHostContext),
                                                      GET_CLASSLOADER_SIG);
    ASSERT(getClassLoaderMId != NULL, "Failed to find method %s (%s) in androids Context class",
                                      GET_CLASSLOADER_SIG);
    #undef GET_CLASSLOADER_SIG
    classLoader = (*env)->CallObjectMethod(env, pyHostContext, getClassLoaderMId);
    if (classLoader == NULL) {
        LOG_ERROR("Failed to load the classloader for the Python host!");
        return JNI_FALSE;
    }
    
    // Load the interpreter class
    classLoaderCls = (*env)->GetObjectClass(env, classLoader);
    loadClass = (*env)->GetMethodID(env, classLoaderCls, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    ASSERT(loadClass != NULL, "Failed to find loadClass in the classLoader!");
    appInterpreterClass = (*env)->CallObjectMethod(env, classLoader, loadClass,
                                                   (*env)->NewStringUTF(env, APP_INTERPRETER_CLASS_PATH));
    if (appInterpreterClass == NULL) {
        (*env)->ExceptionClear(env);
        LOG_ERROR("Failed to find the app interpreter class at %s!", APP_INTERPRETER_CLASS_PATH);
        return JNI_FALSE;
    }
    constructor = (*env)->GetMethodID(env, appInterpreterClass, "<init>", APP_INTERPRETER_CONSTRUCTOR_SIG);
    ASSERT(constructor != NULL, "Failed to find the constructor (%s) in the app interpreter %s!",
           APP_INTERPRETER_CONSTRUCTOR_SIG, APP_INTERPRETER_CLASS_PATH);
    appInterpreter = (*env)->NewObject(env, appInterpreterClass, constructor, pyHostContext,
                                       hostingAppActivity, pythonVersion);
    if (appInterpreter == NULL) {
        (*env)->ExceptionClear(env);
        LOG_ERROR("Failed to call the constructor (%s) of the app interpreter class at %s!",
                  APP_INTERPRETER_CONSTRUCTOR_SIG, APP_INTERPRETER_CLASS_PATH);
        return JNI_FALSE;
    }
    appInterpreter = (*env)->NewGlobalRef(env, appInterpreter);
    syncLogTagToPyHost(env);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL setLogTag(JNIEnv *env, jobject __unused obj, jstring jTag) {
    const char* tag = (*env)->GetStringUTFChars(env, jTag, 0);
    strncpy(logTag, tag, sizeof(logTag) / sizeof(logTag[0]));
    (*env)->ReleaseStringUTFChars(env, jTag, tag);
    syncLogTagToPyHost(env);
}

JNIEXPORT jobject JNICALL setWindow(
        JNIEnv *env, jobject __unused obj, jint windowType, jobject parent) {
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass *cls = (*env)->GetObjectClass(env, appInterpreter);
        ASSERT(cls, "Could not get an instance from the appInterpreter!");
        mid = (*env)->GetMethodID(env, cls, SET_WINDOW_DEF);
        ASSERT(mid, "Could not find the function '%s' (%s) in the Android class!", SET_WINDOW_DEF);
    }
    return (*env)->CallObjectMethod(env, appInterpreter, mid, windowType, parent);
}

JNIEXPORT jint JNICALL startInterpreter(JNIEnv *env, jobject __unused obj, jobjectArray jArgs) {
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass *cls = (*env)->GetObjectClass(env, appInterpreter);
        ASSERT(cls, "Could not get an instance from the appInterpreter!");
        mid = (*env)->GetMethodID(env, cls, START_INTERPRETER_DEF);
        ASSERT(mid, "Could not find the function '%s' (%s) in the Android class!", START_INTERPRETER_DEF);
    }
    return (*env)->CallIntMethod(env, appInterpreter, mid, jArgs);
}

JNIEXPORT void JNICALL onActivityLifecycleEvent(JNIEnv *env, jobject __unused obj, jint eventId) {
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass *cls = (*env)->GetObjectClass(env, appInterpreter);
        ASSERT(cls, "Could not get an instance from the appInterpreter!");
        mid = (*env)->GetMethodID(env, cls, ON_ACTIVITY_LCE_DEF);
        ASSERT(mid, "Could not find the function '%s' (%s) in the Android class!", ON_ACTIVITY_LCE_DEF);
    }
    (*env)->CallVoidMethod(env, appInterpreter, mid, eventId);
}
