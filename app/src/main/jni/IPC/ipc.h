#ifndef IPC_H
#define IPC_H
#ifdef __cplusplus
extern "C" {
#endif

typedef struct _ipcConnection ipcConnection;

#define ALLOW_SEND_FD (1U << 0U)

ipcConnection* createConnection(const char* address, u_int8_t flags);
int waitForClient(ipcConnection* connection);
int openConnection(const char* address, u_int8_t blocking, u_int8_t flags);
void closeConnection(ipcConnection* connection);

#ifdef __cplusplus
}
#endif
#endif /* IPC_H */
