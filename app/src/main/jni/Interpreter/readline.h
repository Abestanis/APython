#ifndef PYTHON_HOST_READLINE_H
#define PYTHON_HOST_READLINE_H

#include <jni.h>
#include "util.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_apython_python_pythonhost_interpreter_PythonInterpreter
 * Method:    waitForReadLineConnection
 * Signature: ()Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject NATIVE_FUNCTION(interpreter_PythonInterpreter_waitForReadLineConnection)
                  (JNIEnv *, jclass __unused);

#ifdef __cplusplus
}
#endif

#endif /* PYTHON_HOST_READLINE_H */
