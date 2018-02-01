#ifndef IPC_H
#define IPC_H
#ifdef __cplusplus
extern "C" {
#endif

typedef struct _ipcConnection ipcConnection;

ipcConnection* createConnection(const char* address);
int waitForClient(ipcConnection* connection);
int openConnection(const char* address, u_int8_t blocking);
void closeConnection(ipcConnection* connection);

#ifdef __cplusplus
}
#endif
#endif /* IPC_H */
