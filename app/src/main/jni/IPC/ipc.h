#ifndef IPC_H
#define IPC_H

typedef struct _ipcConnection ipcConnection;

ipcConnection* createConnection(const char* address);
int waitForClient(ipcConnection* connection);
int openConnection(const char* address);
void closeConnection(ipcConnection* connection);

#endif /* IPC_H */
