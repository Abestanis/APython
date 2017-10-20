//
// Created by Sebastian on 25.11.2016.
//

#include "terminal.h"
#include "log.h"
#include "py_utils.h"

#include <fcntl.h>
#include <unistd.h>

#define STDIN_INDEX 0
#define STDOUT_INDEX 1
#define STDERR_INDEX 2

int createPseudoTerminal() {
    int masterFd;
    if ((masterFd = getpt()) == -1) {
        LOG_ERROR("Fail to open master");
        return -1;
    }
    
    // Remove the signal handler for SIGCHLD if there is one
    // The behavior of grantpt is undefined with a SIGCHLD installed
    __sighandler_t oldSignalHandler = setSignalHandler(SIGCHLD, SIG_DFL);
    
    /* change permission of slave */
    if (grantpt(masterFd) < 0) {
        setSignalHandler(SIGCHLD, oldSignalHandler);
        close(masterFd);
        return -1;
    }
    
    /* unlock slave */
    if (unlockpt(masterFd) < 0) {
        setSignalHandler(SIGCHLD, oldSignalHandler);
        close(masterFd);
        return -1;
    }
    setSignalHandler(SIGCHLD, oldSignalHandler);
    
    struct termios attrs; // Enable line oriented input and erase and kill processing. 
    tcgetattr(masterFd, &attrs);
    attrs.c_lflag &= ~ECHO;
    attrs.c_lflag &= ~ICANON;
    tcsetattr(masterFd, TCSAFLUSH, &attrs);
    
    return masterFd;
}

char* getPseudoTerminalSlavePath(int masterFd) {
    return ptsname(masterFd);
}

int openSlavePseudoTerminal(const char* path) {
    int slaveFd;
    if ((slaveFd = open(path, O_RDWR | O_NOCTTY)) < 0) {
        LOG_ERROR("Failed to attach to pseudo terminal at %s: %s", path), strerror(errno);
        return -1;
    }
    setAsControllingTerminal(slaveFd);
    
//    terminal->slaveStdStreams[STDIN_INDEX]  = dup(fileno(stdin));
//    terminal->slaveStdStreams[STDOUT_INDEX] = dup(fileno(stdout));
//    terminal->slaveStdStreams[STDERR_INDEX] = dup(fileno(stderr));
    
    while ((dup2(slaveFd, fileno(stdin))  == -1) && (errno == EINTR)) {}
    while ((dup2(slaveFd, fileno(stdout)) == -1) && (errno == EINTR)) {}
    while ((dup2(slaveFd, fileno(stderr)) == -1) && (errno == EINTR)) {}
    
    setvbuf(stdin, 0, _IOFBF, 0);
    setvbuf(stdout, 0, _IOLBF, 0);
//    setvbuf(stderr, 0, _IONBF, 0);
    return slaveFd;
}

//void setPseudoTerminalAttr(struct termios *termp, struct winsize *winp) {
//    if (termp) tcsetattr(terminal->slaveFd, TCSAFLUSH, termp);
//    if (winp) ioctl(terminal->slaveFd, TIOCSWINSZ, winp);
//}

void closePseudoTerminal(int masterFd) {
    close(masterFd);
}

void setAsControllingTerminal(int slaveFd) {
    static const char* TTY_PATH = "/dev/tty";
    int fd;
    if ((fd = open(TTY_PATH, O_RDWR | O_NOCTTY)) >= 0) {
        (void) ioctl(fd, TIOCNOTTY, NULL);
        close(fd);
    }
    
    if (setsid() < 0) {
        LOG_WARN("setsid failed: %s", strerror(errno));
    }
    
    fd = open(TTY_PATH, O_RDWR | O_NOCTTY);
    if (fd >= 0) {
        LOG_WARN("Failed to disconnect from controlling tty.");
        close(fd);
    }
    
    ioctl(slaveFd, TIOCSCTTY, 1);
    
//    if ((fd = open(terminal->slaveName, O_RDWR)) < 0) {
//        LOG_WARN("%s: %s", terminal->slaveName, strerror(errno));
//    } else {
//        close(terminal->slaveFd);
//        terminal->slaveFd = fd;
//    }
    
    if ((fd = open(TTY_PATH, O_RDWR | O_NOCTTY)) >= 0) {
        close(fd);
    } else {
        LOG_WARN("Open %s failed - could not set controlling tty: %s", TTY_PATH, strerror(errno));
    }
}

void disconnectFromPseudoTerminal(int slaveFd) {
    close(slaveFd);
//    terminal->slaveFd = -1;
//    
//    while ((dup2(terminal->slaveStdStreams[STDIN_INDEX],  fileno(stdin))  == -1) && (errno == EINTR)) {}
//    while ((dup2(terminal->slaveStdStreams[STDOUT_INDEX], fileno(stdout)) == -1) && (errno == EINTR)) {}
//    while ((dup2(terminal->slaveStdStreams[STDERR_INDEX], fileno(stderr)) == -1) && (errno == EINTR)) {}
}

void writeToPseudoTerminal(int masterFd, const char* input, size_t len) {
    ssize_t bytesWritten;
    while ((bytesWritten = write(masterFd, input, len)) < len) {
        if (bytesWritten == -1) {
            if (errno != EAGAIN && errno != EINTR) {
                LOG_WARN("Failed to write input to the pseudo terminal: %s", strerror(errno));
                break;
            }
        } else {
            input += bytesWritten;
            len -= bytesWritten;
        }
    }
}

size_t readFromPseudoTerminalStdin(int masterFd, char* buff, size_t buffLen) {
    size_t bytesRead = 0;
    ssize_t readByte;
    // TODO: Open the slave of the pseudoterminal and read all input;
//    int flags = fcntl(masterFd, F_GETFL);
//    fcntl(masterFd, F_SETFL, flags | O_NONBLOCK);
//    while (bytesRead < buffLen && ((readByte = read(masterFd, &buff[bytesRead], 1)) != -1 || errno == EINTR)) {
//        if (readByte != -1) bytesRead += readByte;
//    }
    buff[bytesRead] = '\0';
    return bytesRead;
}

