#include "redirects.h"
#include <unistd.h>
#include <stdio.h>
#include <stdarg.h>
#include <termios.h>

int redirectedIsATty(int fd) {
    return fd == fileno(stdin) || fd == fileno(stdout) || fd == fileno(stderr) || isatty(fd);
}

// TODO: Somebody who has a better understanding of ttys must look at this.
int redirectedIOCtl(int fd, int request, ...) {
    va_list args;
    if (fd == fileno(stdin) || fd == fileno(stdout) || fd == fileno(stderr)) {
        if (request == TCGETS) { // This is tcgetattr
            struct termios *termios_p;
            va_start(args, request);
            termios_p = va_arg(args, struct termios*);
            termios_p->c_iflag    = IGNBRK;      /* input modes */
            termios_p->c_oflag    = OPOST;      /* output modes */
            termios_p->c_cflag    = 0;      /* control modes */
            termios_p->c_lflag    = ECHO;      /* local modes */
            /* special characters */
            cc_t* characters = termios_p->c_cc;
            //characters[VEOF] = EOF;
            //characters[VEOL] = "\n";
            //characters[VMIN] = EOF;
            //characters[VTIME] = "\n";
            va_end(args);
            return 0;
        } else if (request == TCSAFLUSH) { // This is tcsetattr
            // TODO: Does this matter? See tty.py
            return 0;
        } else {
            return ioctl(fd, request, args);
        }
    } else {
        return ioctl(fd, request, args);
    }
}

void redirectedExit(int code) {
    int *ret = malloc(sizeof(int));
    if (ret == NULL) {
        printf("Could not allocate memory for the return value (%d)!", code);
    } else {
        *ret = code;
    }
    pthread_exit(ret);
}

// because: https://github.com/awong-dev/ndk/blob/master/sources/android/support/src/locale/setlocale.c
char* redirectedSetLocale(int category, const char *locale) {
    if (locale == NULL) {
        return "C";
    }
    return setlocale(category, locale);
}

// If we ever want to emulate a command, we can do this here:
//int (*androidMoreCommandPointer)(const char* command) = NULL;

// Don't forget -D system=redirectedSystem
//int redirectedSystem(const char *command) {
//    if ((strncmp(command, "more ", 5) == 0) && androidMoreCommandPointer != NULL) {
//        return (*androidMoreCommandPointer)(command);
//    } else {
//        return system(command);
//    }
//}
