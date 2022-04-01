#include <malloc.h>
#include <memory.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include "ipc.h"

#define BUFF_SIZE 256
#define MAX_SOCKET_BACKLOG 5

struct _ipcConnection {
    int fd;
};

int makeAddress(const char* name, struct sockaddr_un* address, socklen_t* sockLen, int addressTyp) {
    char nameBuff[BUFF_SIZE];
    snprintf(nameBuff, BUFF_SIZE * sizeof(nameBuff[0]), "local.%s.socket", name);
    int nameLen = strlen(nameBuff);
    if (nameLen >= (int) sizeof(address->sun_path) -1) {
        return -1;
    }
    address->sun_path[0] = '\0';
    strcpy(address->sun_path + 1, nameBuff);
    address->sun_family = (__kernel_sa_family_t) addressTyp;
    *sockLen = 1 + nameLen + offsetof(struct sockaddr_un, sun_path);
    return 0;
}

ipcConnection* createConnection(const char* address, u_int8_t flags) {
    struct sockaddr_un sockAddress;
    int socketType = AF_LOCAL;
    int protocol = PF_LOCAL;
    if (flags & (~ALLOW_SEND_FD)) {
        socketType = AF_UNIX;
        protocol = PF_UNIX;
    }
    socklen_t sockLen;
    if (makeAddress(address, &sockAddress, &sockLen, socketType) < 0) {
        return NULL;
    }
    ipcConnection* connection = malloc(sizeof(struct _ipcConnection));
    if (connection == NULL) { return NULL; }
    if ((connection->fd = socket(socketType, SOCK_STREAM | SOCK_CLOEXEC, protocol)) < 0) {
        free(connection);
        return NULL;
    }
    if (bind(connection->fd, (const struct sockaddr*) &sockAddress, sockLen) == 0) {
        if (listen(connection->fd, MAX_SOCKET_BACKLOG) == 0) {
            return connection;
        }
    }
    close(connection->fd);
    free(connection);
    return NULL;
}

int waitForClient(ipcConnection* connection) {
#if __ANDROID_API__ >= 21
    return accept4(connection->fd, NULL, NULL, SOCK_CLOEXEC);
#else
    return accept(connection->fd, NULL, NULL); // NOLINT(android-cloexec-accept)
#endif /* __ANDROID_API__ >= 21 */
}

int openConnection(const char* address, u_int8_t blocking, u_int8_t flags) {
    struct sockaddr_un sockAddress;
    socklen_t sockLen;
    int socketSetting = 0;
    int fd;
    int socketType = AF_LOCAL;
    int protocol = PF_LOCAL;
    if (flags & (~ALLOW_SEND_FD)) {
        socketType = AF_UNIX;
        protocol = PF_UNIX;
    }
    if (makeAddress(address, &sockAddress, &sockLen, socketType) < 0) {
        return -1;
    }
    if ((fd = socket(socketType, SOCK_STREAM | SOCK_CLOEXEC, protocol)) < 0) {
        return -1;
    }
    if (!blocking) {
        socketSetting = fcntl(fd, F_GETFL);
        fcntl(fd, F_SETFL, socketSetting | O_NONBLOCK);
    }
    if (connect(fd, (const struct sockaddr*) &sockAddress, sockLen) < 0) {
        close(fd);
        return -1;
        
    }
    if (!blocking) {
        fcntl(fd, F_SETFL, socketSetting & ~O_NONBLOCK);
    }
    return fd;
}

void closeConnection(ipcConnection* connection) {
    close(connection->fd);
    free(connection);
}