#ifndef UTIL_H
#define UTIL_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#define ANDROID_JAVA_PACKAGE_PATH com_apython_python_pythonhost
#define CONCATENATE_NATIVE_FUNCTION_NAME(pkg, name) Java_ ## pkg ## _ ## name
#define CREATE_NATIVE_FUNCTION_NAME(pkg, name) CONCATENATE_NATIVE_FUNCTION_NAME(pkg, name)
#define NATIVE_FUNCTION(name) JNICALL CREATE_NATIVE_FUNCTION_NAME(ANDROID_JAVA_PACKAGE_PATH, name)

/*
 * Class:     com_apython_python_pythonhost_Util
 * Method:    nativeCreateSymlink
 * Signature: (Ljava/lang/String;Ljava/lang/String;)z
 */
JNIEXPORT jboolean NATIVE_FUNCTION(Util_nativeCreateSymlink)
    (JNIEnv *, jclass __unused, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif //UTIL_H
