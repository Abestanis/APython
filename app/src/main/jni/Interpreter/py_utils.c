#include "py_utils.h"
#include <stdlib.h>
#include "Log/log.h"

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

    Py_SetPythonHome((char*) pythonHome);
    Py_SetProgramName((char*) pythonProgramPath);

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
    free((char*) dataHome);
}