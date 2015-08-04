#ifndef OUTPUT_REDIRECT_H
#define OUTPUT_REDIRECT_H

//extern int (*androidMoreCommandPointer)(const char* command);

int redirectedIsATty(int fd);
int redirectedIOCtl(int fd, int request, ...);
void __attribute__((noreturn)) redirectedExit(int code);
//int redirectedSystem(const char *command);

#endif // OUTPUT_REDIRECT_H //
