#include "readline.h"
#include <ipc.h>
#include <log.h>
#include <errno.h>
#include "util.h"


JNIEXPORT jobject NATIVE_FUNCTION(interpreter_PythonInterpreter_waitForReadLineConnection)
                  (JNIEnv* env, jclass __unused cls) {
    static ipcConnection* connection = NULL;
    if (connection == NULL) {
        connection = createConnection("readLineAndroid", 0);
    }
    if (connection == NULL) {
        LOG_WARN("CreateConnection failed: %s!", strerror(errno));
        return NULL;
    }
    return createFileDescriptor(env, waitForClient(connection));
}
