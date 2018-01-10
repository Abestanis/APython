#include <malloc.h>
#include <memory.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include "ipc.h"

struct _ipcConnection {
    int fd;
};

int makeAddress(const char* name, struct sockaddr_un* address, socklen_t* sockLen) {
    char nameBuff[256];
    snprintf(nameBuff, 256, "local.%s.socket", name);
    int nameLen = strlen(nameBuff);
    if (nameLen >= (int) sizeof(address->sun_path) -1) {
        return -1;
    }
    address->sun_path[0] = '\0';
    strcpy(address->sun_path + 1, nameBuff);
    address->sun_family = AF_LOCAL;
    *sockLen = 1 + nameLen + offsetof(struct sockaddr_un, sun_path);
    return 0;
}

ipcConnection* createConnection(const char* address) {
    struct sockaddr_un sockAddress;
    socklen_t sockLen;
    if (makeAddress(address, &sockAddress, &sockLen) < 0) {
        return NULL;
    }
    ipcConnection* connection = malloc(sizeof(struct _ipcConnection));
    if (connection == NULL) { return NULL; }
    if ((connection->fd = socket(AF_LOCAL, SOCK_STREAM, PF_UNIX)) < 0) {
        free(connection);
        return NULL;
    }
    if (bind(connection->fd, (const struct sockaddr*) &sockAddress, sockLen) == 0) {
        if (listen(connection->fd, 5) == 0) {
            return connection;
        }
    }
    close(connection->fd);
    free(connection);
    return NULL;
}

int waitForClient(ipcConnection* connection) {
    return accept(connection->fd, NULL, NULL);
}

int openConnection(const char* address) {
    struct sockaddr_un sockAddress;
    socklen_t sockLen;
    int fd;
    if (makeAddress(address, &sockAddress, &sockLen) < 0) {
        return NULL;
    }
    if ((fd = socket(AF_LOCAL, SOCK_STREAM, PF_UNIX)) < 0) {
        return -1;
    }
    if (connect(fd, (const struct sockaddr*) &sockAddress, sockLen) < 0) {
        close(fd);
        return -1;
        
    }
    return fd;
}

void closeConnection(ipcConnection* connection) {
    close(connection->fd);
    free(connection);
}