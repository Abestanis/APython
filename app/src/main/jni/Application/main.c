#include "main.h"
#include "Python.h"
#include "Log/log.h"


JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonExecute_nativeTest(JNIEnv *env, jobject obj) {
    Py_Initialize();
    initLog();
    PyRun_SimpleString(
        "print 'HELLO!!!'\n" \
        "1/0\n");
    Py_Finalize();
}
