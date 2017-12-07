#include "readline.h"
#include <ipc.h>
#include <log.h>
#include "util.h"


JNIEXPORT jobject NATIVE_FUNCTION(interpreter_PythonInterpreter_waitForReadLineConnection)
                  (JNIEnv* env, jclass __unused cls) {
    LOG_INFO_("%s\n", __func__);
    static ipcConnection* connection = NULL;
    if (connection == NULL) {
        connection = createConnection("readLineAndroid");
    }
    if (connection == NULL) {
        LOG_WARN("CreateConnection failed!");
        return NULL;
    }
    LOG_WARN("CreateConnection succeeded!");
    return createFileDescriptor(env, waitForClient(connection));
}
