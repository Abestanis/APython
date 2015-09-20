#include "py_utils.h"
#include <stdlib.h>
#include <pthread.h>
#include "Log/log.h"
#include "py_compatibility.h"

struct pythonThreadArguments {
    int argc;
    char** argv;
};
FILE *stdin_writer = NULL;
static int saved_stdout;
static int saved_stderr;

void setupPython(const char* pythonProgramPath, const char* pythonLibs, const char* pythonHome, const char* pythonTemp, const char* xdgBasePath) {

    const char* value = (const char*) getenv("LD_LIBRARY_PATH");
    if (strstr(value, pythonLibs) == NULL) { // Check if our Path is already in LD_LIBRARY_PATH
        const char* newValue = malloc(sizeof(char) * (strlen(pythonLibs) + strlen(value) + 2));
        ASSERT(newValue != NULL, "Not enough memory to change 'LD_LIBRARY_PATH'!");
        strcpy((char*) newValue, pythonLibs);
        strcat((char*) newValue, ":");
        strcat((char*) newValue, value);
        setenv("LD_LIBRARY_PATH", newValue, 1);
        free((char*) newValue);
    }

    call_Py_SetPythonHome((char*) pythonHome);
    call_Py_SetProgramName((char*) pythonProgramPath);

    setenv("TMPDIR", pythonTemp, 1);
    setenv("XDG_CACHE_HOME", pythonTemp, 1);

    const char* dataAppendix = "/.local/share";
    const char* configAppendix = "/.config";

    const char* dataHome = malloc(sizeof(char) * (strlen(xdgBasePath) + strlen(dataAppendix) + 1));
    ASSERT(dataHome != NULL, "Not enough memory to construct 'XDG_DATA_HOME'!");
    strcpy((char*) dataHome, xdgBasePath);
    strcat((char*) dataHome, dataAppendix);
    setenv("XDG_DATA_HOME", dataHome, 1);
    free((char*) dataHome);

    const char* configHome = malloc(sizeof(char) * (strlen(xdgBasePath) + strlen(configAppendix) + 1));
    ASSERT(configHome != NULL, "Not enough memory to construct 'XDG_CONFIG_HOME'!");
    strcpy((char*) configHome, xdgBasePath);
    strcat((char*) configHome, configAppendix);
    setenv("XDG_CONFIG_HOME", configHome, 1);
    free((char*) configHome);
}

int setupStdinEmulation() {
    int p[2];

    // error return checks omitted
    pipe(p);

    stdin_writer = fdopen(p[1], "w");

    return  p[0];
}

static void cleanupPythonThread(void* arg) {
    dup2(saved_stdout, fileno(stdout));
    dup2(saved_stderr, fileno(stderr));
}

void* startPythonInterpreter(void* arg) {
    pthread_cleanup_push(cleanupPythonThread, NULL);

    struct pythonThreadArguments *args = (struct pythonThreadArguments *)arg;

    //Py_VerboseFlag = 1;
    //Py_DebugFlag = 1;
    LOG("Starting...");
    fflush(stdout);
    exit(call_Py_Main(args->argc, args->argv));
    pthread_cleanup_pop(0);
}

int runPythonInterpreter(int argc, char** argv) {
    int outputPipe[2];
    int redirectedStdInFD;
    void *status = NULL;
    pthread_t pythonThread;

    if (pipe(outputPipe) == -1) {
        LOG_ERROR("Could not create the pipe to redirect stdout and stderr to.");
        return 1;
    }

    saved_stderr = dup(fileno(stderr));
    saved_stdout = dup(fileno(stdout));
    setupOutputRedirection(outputPipe);
    redirectedStdInFD = setupStdinEmulation();
    dup2(redirectedStdInFD, fileno(stdin));

    struct pythonThreadArguments args;
    args.argc = argc;
    args.argv = argv;

    if (pthread_create(&pythonThread, NULL, startPythonInterpreter, (void *) &args) == -1) {
        LOG_ERROR("Could not create the Python thread!");
        return 1;
    }

    captureOutput(outputPipe[0]);
    pthread_join(pythonThread, &status);
    return *((int*) status);
}