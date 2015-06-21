#ifndef LOGMODULE_H
#define LOGMODULE_H

#include "android/log.h"

int _log_write_(int priority, const char *text);
void _assert_(const char*, const char*, int, const char*);
void setApplicationTag(const char*);
void setStdoutRedirect(void (*f)(const char*));
void setStderrRedirect(void (*f)(const char*));
int setupOutputRedirection(void);

#define ASSERT(EX, message) (void)((EX) || (_assert_ (#EX, __FILE__, __LINE__, message),0))
#define LOG(x)        _log_write_(ANDROID_LOG_DEBUG, (x))
#define LOG_INFO_(x)  _log_write_(ANDROID_LOG_INFO,  (x))
#define LOG_ERROR(x)  _log_write_(ANDROID_LOG_ERROR, (x))

#endif // LOGMODULE_H //
