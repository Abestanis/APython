#include "py_utils.h"
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <libgen.h>
#include <dirent.h>
#include "log.h"
#include "py_compatibility.h"

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
    
    // These must not be freed before the interpreter exits.
    char* pyHomeCopy = strdup(pythonHome);
    ASSERT(pyHomeCopy != NULL, "Not enough memory to copy the Python home path!");    
    char* pyProgramPathCopy = strdup(pythonHome);
    ASSERT(pyProgramPathCopy != NULL, "Not enough memory to copy the Python program path!");
    call_Py_SetPythonHome(pyHomeCopy);
    call_Py_SetProgramName(pyProgramPathCopy);

    setenv("TMPDIR", pythonTemp, 1);
    setenv("XDG_CACHE_HOME", pythonTemp, 1);
    
    size_t dataHomeLen = strlen(xdgBasePath) + strlen("/.local/share") + 1;
    char* dataHome = malloc(sizeof(char) * dataHomeLen);
    ASSERT(dataHome != NULL, "Not enough memory to construct 'XDG_DATA_HOME'!");
    snprintf(dataHome, dataHomeLen, "%s/.local/share", xdgBasePath);
    setenv("XDG_DATA_HOME", dataHome, 1);
    free(dataHome);
    
    size_t configHomeLen = strlen(xdgBasePath) + strlen("/.config") + 1;
    char* configHome = malloc(sizeof(char) * configHomeLen);
    ASSERT(configHome != NULL, "Not enough memory to construct 'XDG_CONFIG_HOME'!");
    snprintf(configHome, configHomeLen, "%s/.config", xdgBasePath);
    setenv("XDG_CONFIG_HOME", configHome, 1);
    free(configHome);

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
        char* tclLibDir = malloc(sizeof(char) * tclDirPathLen);
        ASSERT(tclLibDir != NULL, "Not enough memory to construct 'TCL_LIBRARY'!");
        snprintf(tclLibDir, tclDirPathLen, "%s/%s/library", dataDir, tclDirName);
        setenv("TCL_LIBRARY", tclLibDir, 1);
        free(tclLibDir);
    }
    
    DIR* termInfoDir;
    size_t termInfoPathLen = strlen(dataDir) + strlen("/terminfo") + 1;
    char* termInfoDirPath = malloc(sizeof(char) * termInfoPathLen);
    ASSERT(termInfoDirPath != NULL, "Not enough memory to construct 'TERMINFO'!");
    snprintf(termInfoDirPath, termInfoPathLen, "%s/terminfo", dataDir);
    if ((termInfoDir = opendir(termInfoDirPath))) {
        closedir(termInfoDir);
        setenv("TERMINFO", termInfoDirPath, 0);
    }
    free(termInfoDirPath);
    
    setenv("TERM", "xterm-256color", 0);
}

int runPythonInterpreter(int argc, char** argv) {
    LOG_INFO_("Running Python interpreter...");
    return call_Py_Main(argc, argv);
}
