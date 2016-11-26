#ifndef APPLICATION_MODULE_H
#define APPLICATION_MODULE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include "android/log.h"

char logTag[128] = "PythonApp";

#define LOG(msg, args...) __android_log_print(ANDROID_LOG_INFO, logTag, (msg), ##args)
#define LOG_ERROR(msg, args...) __android_log_print(ANDROID_LOG_ERROR, logTag, (msg), ##args)
#define LOG_WARN(msg, args...) __android_log_print(ANDROID_LOG_WARN, logTag, (msg), ##args)
#define ASSERT(expression, msg, args...) (void)((expression) || (__android_log_assert(#expression, logTag, \
                 "Assertion failed (%s) at %s, line %i: " msg, #expression, __FILE__, __LINE__, ##args), 0))

JNIEXPORT jboolean JNICALL loadPythonHost(JNIEnv*, jobject, jobject, jstring);
JNIEXPORT void JNICALL setLogTag(JNIEnv*, jobject, jstring);
JNIEXPORT jobject JNICALL setWindow(JNIEnv*, jobject, jint, jobject);
JNIEXPORT jint JNICALL startInterpreter(JNIEnv*, jobject, jobjectArray);
JNIEXPORT void JNICALL onActivityLifecycleEvent(JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif

#endif // APPLICATION_MODULE_H
