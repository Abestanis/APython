#ifndef LOGMODULE_H
#define LOGMODULE_H

#include "android/log.h"

int _log_write_(int priority, const char *text, ...);
void _assert_(const char*, const char*, int, const char*, ...);
void setApplicationTag(const char*);
void setStdoutRedirect(void (*f)(const char*, int));
void setStderrRedirect(void (*f)(const char*, int));
int startOutputCapturing(int);
int setupOutputRedirection(int pipe[2]);

#define ASSERT(EX, message, args...) (void)((EX) || (_assert_ (#EX, __FILE__, __LINE__, message, ##args),0))
#define LOG(x, args...)        _log_write_(ANDROID_LOG_DEBUG, (x), ##args)
#define LOG_INFO_(x, args...)  _log_write_(ANDROID_LOG_INFO,  (x), ##args)
#define LOG_WARN(x, args...)   _log_write_(ANDROID_LOG_WARN,  (x), ##args)
#define LOG_ERROR(x, args...)  _log_write_(ANDROID_LOG_ERROR, (x), ##args)

#endif // LOGMODULE_H //
