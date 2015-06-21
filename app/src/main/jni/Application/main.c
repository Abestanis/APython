#include "main.h"
#include "Python.h"
#include "Log/log.h"


JNIEXPORT void JNICALL Java_com_apython_python_pythonhost_PythonExecute_nativeTest(JNIEnv *env, jobject obj) {
    Py_Initialize();

    setApplicationTag("PythonApp"); // TODO: set from app
    //setupPythonOutputRedirection();
    setupOutputRedirection();
    LOG(PY_VERSION);
//    PyRun_SimpleString(
//        "print 'HELLO!!!'\n" \
//        "import sys\n" \
//        "print sys.version\n" \
//        "print sys.version_info\n" \
//        "1/0\n"
//    );
    char* argv[2] = { "TestName", "-v" };
    Py_Main(2, argv);
    Py_Finalize();
}
