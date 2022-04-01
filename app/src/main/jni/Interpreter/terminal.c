//
// Created by Sebastian on 25.11.2016.
//

#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>

#include "terminal.h"
#include "log.h"
#include "py_utils.h"
#include "util.h"

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
    
    struct termios attrs; // Enable line oriented input and erase and signal processing. 
    if (tcgetattr(masterFd, &attrs) < 0) {
        LOG_ERROR("Failed to get the terminal attributes");
    } else {
        attrs.c_iflag |= IUTF8;
        attrs.c_lflag |= ICANON | PENDIN | ECHO | ECHOCTL | ISIG;
        if (tcsetattr(masterFd, TCSAFLUSH, &attrs) < 0) {
            LOG_ERROR("Failed to update the terminal attributes");
        }
    }
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
    
    while ((dup2(slaveFd, STDIN_FILENO)  == -1) && (errno == EINTR)) {}
    while ((dup2(slaveFd, STDOUT_FILENO) == -1) && (errno == EINTR)) {}
    while ((dup2(slaveFd, STDERR_FILENO) == -1) && (errno == EINTR)) {}
    
    setvbuf(stdout, NULL, _IOLBF, 0);
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
    if (setsid() < 0 && errno != EPERM) {
        LOG_WARN("setsid failed: %s", strerror(errno));
    }
    ioctl(slaveFd, TIOCSCTTY, 1);
}

void disconnectFromPseudoTerminal(int slaveFd) {
    close(slaveFd);
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

int getPseudoTerminalAttributes(int slaveFd, struct termios* attributes) {
    return tcgetattr(slaveFd, attributes);
}

int setPseudoTerminalAttributes(int slaveFd, int op, struct termios* attributes) {
    return tcsetattr(slaveFd, op, attributes);
}
