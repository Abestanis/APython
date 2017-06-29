#include "log.h"
#include <string.h>
#include <assert.h>
#include <stdio.h>
#include <malloc.h>

// The Log system //

char appTag[64] = "Python";

int _log_write_(int priority, const char *text, ...) {
    va_list argP;
    va_start(argP, text);
    int res = __android_log_vprint(priority, appTag, text, argP);
    va_end(argP);
    return res;
}

void _assert_(const char* expression, const char* file, int line, const char* errorString, ...) {
    va_list argP;
    char* message = malloc(sizeof(char) * (strlen(errorString) + 2048));
    if (message == NULL) {
        LOG_ERROR("Failed to allocate memory for assert message (format = '%s')", errorString);
        assert(message != NULL);
    }
    va_start(argP, errorString);
    vsprintf(message, errorString, argP);
    va_end(argP);
    __android_log_assert(expression, appTag, "Assertion failed (%s) at %s, line %i: %s", expression, file, line, message);
}

void setApplicationTag(const char* newAppTag) {
    strncpy(appTag, newAppTag, sizeof(appTag) / sizeof(appTag[0])); 
}
