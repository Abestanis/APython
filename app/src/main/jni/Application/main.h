#include <jni.h>
#ifndef APPLICATION_MODULE_H
#define APPLICATION_MODULE_H
#ifdef __cplusplus
extern "C" {
#endif

int startApp(JNIEnv *, jobject, jstring, jstring, jstring, jstring, jstring, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif // APPLICATION_MODULE_H
