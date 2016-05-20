#include "py_utils.h"
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <libgen.h>
#include "Log/log.h"
#include "py_compatibility.h"

struct pythonThreadArguments {
    int argc;
    char** argv;
};
FILE *stdin_writer = NULL;
static int saved_stdout;
static int saved_stderr;
pthread_t pythonThread;

void setupPython(const char* pythonProgramPath, const char* pythonLibs, const char* pythonHostLibs,
                 const char* pythonHome, const char* pythonTemp, const char* xdgBasePath) {
    const char* value = (const char*) getenv("LD_LIBRARY_PATH");
    if (value == NULL) { value = ""; }
    if (strstr(value, pythonLibs) == NULL) { // Check if our Path is already in LD_LIBRARY_PATH
        const char* newValue = malloc(sizeof(char) * (strlen(pythonHostLibs) + strlen(pythonLibs) + strlen(value) + 3));
        ASSERT(newValue != NULL, "Not enough memory to change 'LD_LIBRARY_PATH'!");
        strcpy((char*) newValue, pythonHostLibs);
        strcat((char*) newValue, ":");
        strcat((char*) newValue, pythonLibs);
        strcat((char*) newValue, ":");
        strcat((char*) newValue, value);
        setenv("LD_LIBRARY_PATH", newValue, 1);
        free((char*) newValue);
    }

    char cwd[32];
    if (getcwd(cwd, sizeof(cwd)) == NULL || strcmp(cwd, "/") == 0) {
        // '/' is the default working dir for a java process, but it is unusable for the python program
        char* newCwd = dirname(pythonProgramPath);
        chdir(newCwd);
        free(newCwd);
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

    // TODO: Temporary
    setenv("TCL_LIBRARY", "/data/data/com.apython.python.pythonhost/files/data/tcl8.6.4/library", 1);
}

void setupStdinEmulation() {
    int p[2];
    ASSERT(pipe(p) != -1, "Could not create the input pipe to replace stdin: %s", strerror(errno));
    stdin_writer = fdopen(p[1], "w");
    ASSERT(dup2(p[0], fileno(stdin)) != -1, "Could not link the input pipe with stdin: %s", strerror(errno));
}

void readFromStdin(char* inputBuffer, int bufferSize) {
    int count = 0;
    int character;
    int flags = fcntl(fileno(stdin), F_GETFL);
    fcntl(fileno(stdin), F_SETFL, flags | O_NONBLOCK);
    while ((character = fgetc(stdin)) != EOF) {
        *inputBuffer++ = character;
        count++;
        if (count == bufferSize - 1) {
            break;
        }
    }
    fcntl(fileno(stdin), F_SETFL, flags);
    *inputBuffer++ = 0;
}

static void cleanupPythonThread(void* arg) {
    dup2(saved_stdout, fileno(stdout));
    dup2(saved_stderr, fileno(stderr));
}

void* startPythonInterpreter(void* arg) {
    pthread_cleanup_push(cleanupPythonThread, NULL);

    struct pythonThreadArguments *args = (struct pythonThreadArguments*) arg;

    LOG("Starting...");
    fflush(stdout);
    exit(call_Py_Main(args->argc, args->argv));
    pthread_cleanup_pop(0);
}

int runPythonInterpreter(int argc, char** argv) {
    int outputPipe[2];
    void *status = NULL;

    if (pipe(outputPipe) == -1) {
        LOG_ERROR("Could not create the pipe to redirect stdout and stderr to.");
        return 1;
    }

    saved_stderr = dup(fileno(stderr));
    saved_stdout = dup(fileno(stdout));
    setupOutputRedirection(outputPipe);
    setupStdinEmulation();

    struct pythonThreadArguments args;
    args.argc = argc;
    args.argv = argv;

    if (pthread_create(&pythonThread, NULL, startPythonInterpreter, (void *) &args) == -1) {
        LOG_ERROR("Could not create the Python thread!");
        return 1;
    }

    captureOutput(outputPipe[0]);
    pthread_join(pythonThread, &status);
    terminatePython();
    return (int) status;
}

void interruptPython() {
    pthread_kill(pythonThread, SIGINT);
}

void terminatePython() {
    LOG_WARN("Killing Python thread");
    pthread_kill(pythonThread, SIGTERM);
}
