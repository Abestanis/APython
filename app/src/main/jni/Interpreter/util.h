//
// Created by Sebastian on 29.11.2017.
//

#ifndef UTIL_H
#define UTIL_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_apython_python_pythonhost_Util
 * Method:    nativeCreateSymlink
 * Signature: (Ljava/lang/String;Ljava/lang/String;)z
 */
JNIEXPORT jboolean JNICALL Java_com_apython_python_pythonhost_Util_nativeCreateSymlink
        (JNIEnv *, jclass, jstring, jstring);

#ifdef __cplusplus
}
#endif
#endif //UTIL_H
