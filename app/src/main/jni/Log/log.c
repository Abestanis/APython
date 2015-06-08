#include "log.h"
#include "android/log.h"

// The Log system //

#define LOG(x) __android_log_write(ANDROID_LOG_DEBUG, "Python", (x)) // TODO: Use Application tag
#define LOG_INFO(x) __android_log_write(ANDROID_LOG_INFO, "Python", (x))
#define LOG_ERROR(x) __android_log_write(ANDROID_LOG_ERROR, "Python", (x))

static PyObject *androidlog_info(PyObject *self, PyObject *args) {
    char *logString = NULL;
    if (!PyArg_ParseTuple(args, "s", &logString)) {
        return NULL;
    }
    LOG_INFO(logString);
    Py_RETURN_NONE;
}

static PyObject *androidlog_error(PyObject *self, PyObject *args) {
    char *logString = NULL;
    if (!PyArg_ParseTuple(args, "s", &logString)) {
        return NULL;
    }
    LOG_ERROR(logString);
    Py_RETURN_NONE;
}

static PyMethodDef AndroidLogMethods[] = {
    {"info",  androidlog_info,  METH_VARARGS, "Write the string 'logString' to the Android log as an info."},
    {"error", androidlog_error, METH_VARARGS, "Write the string 'logString' to the Android log as an error."},
    {NULL, NULL, 0, NULL}
};

PyMODINIT_FUNC initLogModule(void) {
    (void) Py_InitModule("androidlog", AndroidLogMethods);
}

void initLog() {
    LOG("Initializing log module...");
    initLogModule();
    PyRun_SimpleString(
        "import sys\n" \
        "import androidlog\n" \
        "class LogFile(object):\n" \
        "    def __init__(self, logFunc):\n" \
        "        self.buffer = ''\n" \
        "        self.logFunc = logFunc\n" \
        "    def write(self, s):\n" \
        "        s = self.buffer + s\n" \
        "        lines = s.split(\"\\n\")\n" \
        "        for line in lines[:-1]:\n" \
        "            self.logFunc(line)\n" \
        "        self.buffer = lines[-1]\n" \
        "    def flush(self):\n" \
        "        return\n" \
        "sys.stdout = LogFile(androidlog.info)\n" \
        "sys.stderr = LogFile(androidlog.error)\n" \
    );
}
