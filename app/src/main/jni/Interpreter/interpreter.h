#include <jni.h>
#include "util.h"
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
JNIEXPORT jstring NATIVE_FUNCTION(interpreter_PythonInterpreter_nativeGetPythonVersion)(
        JNIEnv *, jobject __unused, jstring
);

/*
 * Class:     com_apython_python_pythonhost_interpreter_PythonInterpreter
 * Method:    runInterpreter
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;
 *             Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;
 *             [Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)I
 */
JNIEXPORT jint NATIVE_FUNCTION(interpreter_PythonInterpreter_runInterpreter)(
        JNIEnv *, jobject, jstring, jstring, jstring, jstring, jstring,
        jstring, jstring, jstring, jstring, jobjectArray, jstring
);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    interruptTerminal
 * Signature: (Ljava/io/FileDescriptor;)V
 */
JNIEXPORT void NATIVE_FUNCTION(interpreter_PythonInterpreter_interruptTerminal)(
        JNIEnv *, jclass __unused, jobject
);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    openPseudoTerminal
 * Signature: (V)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject NATIVE_FUNCTION(interpreter_PythonInterpreter_openPseudoTerminal)(
        JNIEnv *, jclass
__unused);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    closePseudoTerminal
 * Signature: (Ljava/io/FileDescriptor;)V
 */
JNIEXPORT void NATIVE_FUNCTION(interpreter_PythonInterpreter_closePseudoTerminal)(
        JNIEnv *, jclass __unused, jobject
);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    getPseudoTerminalPath
 * Signature: (Ljava/io/FileDescriptor;)Ljava/lang/String;
 */
JNIEXPORT jstring NATIVE_FUNCTION(interpreter_PythonInterpreter_getPseudoTerminalPath)(
        JNIEnv *, jclass __unused, jobject
);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    getEnqueueInput
 * Signature: (Ljava/io/FileDescriptor;)Ljava/lang/String;
 */
JNIEXPORT jstring NATIVE_FUNCTION(interpreter_PythonInterpreter_getEnqueueInput)(
        JNIEnv *, jclass __unused, jobject
);

/*
 * Class:     com_apython_python_pythonhost_PythonInterpreter
 * Method:    setPseudoTerminalSize
 * Signature: (Ljava/io/FileDescriptor;IIII)V
 */
JNIEXPORT void NATIVE_FUNCTION(interpreter_PythonInterpreter_setPseudoTerminalSize)(
        JNIEnv *, jclass __unused, jobject, jint, jint, jint, jint
);

#ifdef __cplusplus
}
#endif
#endif // _Included_com_apython_python_pythonhost_interpreter_PythonInterpreter
