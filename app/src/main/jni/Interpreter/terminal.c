//
// Created by Sebastian on 25.11.2016.
//

#include "terminal.h"
#include "log.h"
#include "py_utils.h"

#include <fcntl.h>
#include <unistd.h>

struct PseudoTerminal {
    int masterFd;
    int slaveFd;
    char* slaveName;
    int slaveStdStreams[3];
    void (*onOutput)(const char*, size_t);
    void (*onError)(const char*, size_t);
};

#define STDIN_INDEX 0
#define STDOUT_INDEX 1
#define STDERR_INDEX 2

PseudoTerminal* createPseudoTerminal(void (*onOutput)(const char*, size_t),
                                     void (*onError)(const char*, size_t),
                                     struct termios *termp, struct winsize *winp) {
    PseudoTerminal* terminal = malloc(sizeof(PseudoTerminal));
    if (terminal == NULL) {
        LOG_ERROR("Failed to create a pseudoTerminal: Out of memory");
        return NULL;
    }
    terminal->onError = onError;
    terminal->onOutput = onOutput;
    
    terminal->masterFd = getpt(); // open("/dev/ptmx", O_RDWR | O_NONBLOCK);
    if (terminal->masterFd == -1) {
        LOG_ERROR("Fail to open master");
        free(terminal);
        return terminal;
    }
    
    // Remove the signal handler for SIGCHLD if there is one
    // The behavior of grantpt is undefined with a SIGCHLD installed
    __sighandler_t oldSignalHandler = setSignalHandler(SIGCHLD, SIG_DFL);
    
    /* change permission of slave */
    if (grantpt(terminal->masterFd) < 0) {
        setSignalHandler(SIGCHLD, oldSignalHandler);
        close(terminal->masterFd);
        free(terminal);
        return NULL;
    }
    
    /* unlock slave */
    if (unlockpt(terminal->masterFd) < 0) {
        setSignalHandler(SIGCHLD, oldSignalHandler);
        close(terminal->masterFd);
        free(terminal);
        return NULL;
    }
    setSignalHandler(SIGCHLD, oldSignalHandler);
    if ((terminal->slaveName = ptsname(terminal->masterFd)) == NULL) { /* get name of slave */
        close(terminal->masterFd);
        free(terminal);
        return NULL;
    }
    LOG("openpty: slave name %s", terminal->slaveName);
    if ((terminal->slaveFd = open(terminal->slaveName, O_RDWR | O_NOCTTY)) < 0) { /* open slave */
        close(terminal->masterFd);
        free(terminal);
        return NULL;
    }
    
    struct termios attrs; // Enable line oriented input and erase and kill processing. 
    tcgetattr(terminal->masterFd, &attrs);
    attrs.c_lflag &= ~ECHO;
    attrs.c_lflag &= ~ICANON;
    tcsetattr(terminal->masterFd, TCSAFLUSH, &attrs);
    
    if (termp) tcsetattr(terminal->slaveFd, TCSAFLUSH, termp);
    if (winp) ioctl(terminal->slaveFd, TIOCSWINSZ, winp);
    
    return terminal;
}

void closePseudoTerminal(PseudoTerminal* terminal) {
    close(terminal->masterFd);
    if (terminal->slaveFd != -1) close(terminal->slaveFd);
    free(terminal);
}

void setAsControllingTerminal(PseudoTerminal* terminal) {
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
    
    ioctl(terminal->slaveFd, TIOCSCTTY, 1);
    
    if ((fd = open(terminal->slaveName, O_RDWR)) < 0) {
        LOG_WARN("%s: %s", terminal->slaveName, strerror(errno));
    } else {
        close(terminal->slaveFd);
        terminal->slaveFd = fd;
    }
    
    if ((fd = open(TTY_PATH, O_RDWR | O_NOCTTY)) >= 0) {
        close(fd);
    } else {
        LOG_WARN("Open %s failed - could not set controlling tty: %s", TTY_PATH, strerror(errno));
    }
}

void connectToPseudoTerminalFromChild(PseudoTerminal* terminal) {
    setAsControllingTerminal(terminal);
    
    terminal->slaveStdStreams[STDIN_INDEX]  = dup(fileno(stdin));
    terminal->slaveStdStreams[STDOUT_INDEX] = dup(fileno(stdout));
    terminal->slaveStdStreams[STDERR_INDEX] = dup(fileno(stderr));
    
    while ((dup2(terminal->slaveFd, fileno(stdin))  == -1) && (errno == EINTR)) {}
    while ((dup2(terminal->slaveFd, fileno(stdout)) == -1) && (errno == EINTR)) {}
    while ((dup2(terminal->slaveFd, fileno(stderr)) == -1) && (errno == EINTR)) {}
    
    setvbuf(stdin, 0, _IOFBF, 0);
    setvbuf(stdout, 0, _IOLBF, 0);
//    setvbuf(stderr, 0, _IONBF, 0);
}

void disconnectFromPseudoTerminal(PseudoTerminal* terminal) {
    close(terminal->slaveFd);
    terminal->slaveFd = -1;
    
    while ((dup2(terminal->slaveStdStreams[STDIN_INDEX],  fileno(stdin))  == -1) && (errno == EINTR)) {}
    while ((dup2(terminal->slaveStdStreams[STDOUT_INDEX], fileno(stdout)) == -1) && (errno == EINTR)) {}
    while ((dup2(terminal->slaveStdStreams[STDERR_INDEX], fileno(stderr)) == -1) && (errno == EINTR)) {}
}

void writeToPseudoTerminal(PseudoTerminal* terminal, const char* input, size_t len) {
    ssize_t bytesWritten;
    size_t bytesToWrite = len;
    while ((bytesWritten = write(terminal->masterFd, input, bytesToWrite)) < bytesToWrite) {
        if (bytesWritten == -1) {
            if (errno != EAGAIN && errno != EINTR) {
                LOG_WARN("Failed to write input to the pseudo terminal: %s", strerror(errno));
                break;
            }
        } else {
            input += bytesWritten;
            bytesToWrite -= bytesWritten;
        }
    }
}

size_t readFromPseudoTerminalStdin(PseudoTerminal* terminal, char* buff, size_t buffLen) {
    size_t bytesRead = 0;
    ssize_t readByte;
    int flags = fcntl(terminal->slaveFd, F_GETFL);
    fcntl(terminal->slaveFd, F_SETFL, flags | O_NONBLOCK);
    while (bytesRead < buffLen && ((readByte = read(terminal->slaveFd, &buff[bytesRead], 1)) != -1 || errno == EINTR)) {
        if (readByte != -1) bytesRead += readByte;
    }
    buff[bytesRead] = '\0';
    return bytesRead;
}

void handlePseudoTerminalOutput(PseudoTerminal* terminal) {
    ssize_t outputSize;
    char buffer[4096];
    size_t MAX_BUFFER_SIZE = (sizeof(buffer) / sizeof(buffer[0])) - 1;
    while ((outputSize = read(terminal->masterFd, buffer, MAX_BUFFER_SIZE)) != 0) {
        if (outputSize == -1) {
            if (errno != EINTR && errno != EAGAIN) {
                if (terminal->slaveFd != -1) {
                    LOG_WARN("Failed to read from output pipe:");
                    LOG_WARN(strerror(errno));
                    LOG_WARN("Stop reading from output (output no longer valid).");
                } // else: Slave closed pipe
                break;
            }
            LOG_WARN("Failed to read from pseudo terminal output pipe: %s", strerror(errno));
            continue;
        }
        buffer[outputSize] = '\0'; // add null-terminator
        if (terminal->onOutput != NULL) {
            terminal->onOutput(buffer, (size_t) outputSize);
        } //else {
            LOG("%s", buffer);
        //}
    }
    LOG_WARN("Returning from %s", __func__);
}

int getPseudoTerminalAttributes(PseudoTerminal* terminal, struct termios* attributes) {
    return tcgetattr(terminal->slaveFd, attributes);
}

int setPseudoTerminalAttributes(PseudoTerminal* terminal, int op, struct termios* attributes) {
    return tcsetattr(terminal->slaveFd, op, attributes);
}
