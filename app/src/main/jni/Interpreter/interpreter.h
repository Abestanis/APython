/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_apython_python_pythonhost_interpreter_PythonInterpreter */

#ifndef _Included_com_apython_python_pythonhost_interpreter_PythonInterpreter
#define _Included_com_apython_python_pythonhost_interpreter_PythonInterpreter
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_apython_python_pythonhost_interpreter_PythonInterpreter
 * Method:    nativeGetPythonVersion
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_nativeGetPythonVersion
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_apython_python_pythonhost_interpreter_PythonInterpreter
 * Method:    runInterpreter
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_runInterpreter
  (JNIEnv *, jobject, jstring, jstring, jstring, jstring, jstring, jstring, jstring, jobjectArray, jboolean);

/*
 * Class:     com_apython_python_pythonhost_interpreter_PythonInterpreter
 * Method:    dispatchKey
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_dispatchKey
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_apython_python_pythonhost_interpreter_PythonInterpreter
 * Method:    sendStringToStdin
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_apython_python_pythonhos_interpretert_PythonInterpreter_sendStringToStdin
(JNIEnv *, jobject, jstring);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    getEnqueueInputTillNewLine
 * Signature: (V)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_apython_python_pythonhost_interpreter_PythonInterpreter_getEnqueueInputTillNewLine
        (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif // _Included_com_apython_python_pythonhost_interpreter_PythonInterpreter
