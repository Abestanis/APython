#include "py_utils.h"
#include <stdlib.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <libgen.h>
#include <dirent.h>
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
                 const char* pythonHome, const char* pythonTemp, const char* xdgBasePath,
                 const char* dataDir) {
    const char* NO_VALUE = "";
    const char* value = (const char*) getenv("LD_LIBRARY_PATH");
    if (value == NULL) { value = NO_VALUE; }
    if (strstr(value, pythonLibs) == NULL) { // Check if our path is already in LD_LIBRARY_PATH
        size_t ldLibLen = strlen(pythonHostLibs) + strlen(pythonLibs) + strlen(value) + 3;
        const char* newValue = malloc(sizeof(char) * (ldLibLen));
        ASSERT(newValue != NULL, "Not enough memory to change 'LD_LIBRARY_PATH'!");
        snprintf((char*) newValue, ldLibLen, "%s:%s:%s", pythonHostLibs, pythonLibs, value);
        setenv("LD_LIBRARY_PATH", newValue, 1);
        free((char*) newValue);
    }

    char cwd[32];
    if (getcwd(cwd, sizeof(cwd)) == NULL || strcmp(cwd, "/") == 0) {
        // '/' is the default working dir for a java process, but it is unusable for the python program
        char* path = strdup(pythonProgramPath);
        chdir(dirname(path));
        free(path);
    }
    
    call_Py_SetPythonHome((char*) pythonHome);
    call_Py_SetProgramName((char*) pythonProgramPath);

    setenv("TMPDIR", pythonTemp, 1);
    setenv("XDG_CACHE_HOME", pythonTemp, 1);
    
    size_t dataHomeLen = strlen(xdgBasePath) + strlen("/.local/share") + 1;
    const char* dataHome = malloc(sizeof(char) * dataHomeLen);
    ASSERT(dataHome != NULL, "Not enough memory to construct 'XDG_DATA_HOME'!");
    snprintf((char*) dataHome, dataHomeLen, "%s/.local/share", xdgBasePath);
    setenv("XDG_DATA_HOME", dataHome, 1);
    free((char*) dataHome);
    
    size_t configHomeLen = strlen(xdgBasePath) + strlen("/.configy") + 1;
    const char* configHome = malloc(sizeof(char) * configHomeLen);
    ASSERT(configHome != NULL, "Not enough memory to construct 'XDG_CONFIG_HOME'!");
    snprintf((char*) configHome, configHomeLen, "%s/.configy", xdgBasePath);
    setenv("XDG_CONFIG_HOME", configHome, 1);
    free((char*) configHome);

    // Search dataDir for best 'tcl' dir
    char* tclDirName = NULL;
    size_t dirNameLen = 0, nameLen = 0;
    int majorVersion, maxMajorVersion = 0;
    int minorVersion, maxMinorVersion = 0;
    DIR* dir = opendir(dataDir);
    struct dirent* entry;
    if (dir != NULL) {
        while ((entry = readdir(dir)) != NULL) {
            if (strncmp(entry->d_name, "tcl", 3) == 0) {
                if (sscanf(entry->d_name, "tcl%d.%d", &majorVersion, &minorVersion) != 2) continue;
                if (majorVersion > maxMajorVersion || (majorVersion == maxMajorVersion && minorVersion > maxMinorVersion)) {
                    maxMajorVersion = majorVersion;
                    maxMinorVersion = minorVersion;
                    nameLen = strlen(entry->d_name);
                    if (nameLen > dirNameLen) {
                        if (tclDirName != NULL) free(tclDirName);
                        tclDirName = malloc(sizeof(char) * (nameLen + 1));
                        dirNameLen = nameLen;
                    }
                    strncpy(tclDirName, entry->d_name, nameLen + 1);
                }
            }
        }
        closedir(dir);
    } else {
        LOG_WARN("Failed to open the data directory: %s", dataDir);
    }
    if (tclDirName != NULL) {
        size_t tclDirPathLen = strlen(dataDir) + 1 + strlen(tclDirName) + strlen("/library") + 1;
        const char* tclLibDir = malloc(sizeof(char) * tclDirPathLen);
        ASSERT(tclLibDir != NULL, "Not enough memory to construct 'TCL_LIBRARY'!");
        snprintf((char*) tclLibDir, tclDirPathLen, "%s/%s/library", dataDir, tclDirName);
        setenv("TCL_LIBRARY", tclLibDir, 1);
        free((char*) tclLibDir);
    }
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
        *inputBuffer++ = (char) character;
        count++;
        if (count == bufferSize - 1) {
            break;
        }
    }
    fcntl(fileno(stdin), F_SETFL, flags);
    *inputBuffer = '\0';
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
    pthread_exit((void*) call_Py_Main(args->argc, args->argv));
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
