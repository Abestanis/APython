/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_apython_python_pythonhost_PythonInterpreter */

#ifndef _Included_com_apython_python_pythonhost_PythonInterpreter
#define _Included_com_apython_python_pythonhost_PythonInterpreter
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    getPythonVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_getPythonVersion
  (JNIEnv *, jclass);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    startInterpreter
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_startInterpreter
  (JNIEnv *, jobject, jstring, jstring, jstring);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    dispatchKey
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonInterpreter_dispatchKey
  (JNIEnv *, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif