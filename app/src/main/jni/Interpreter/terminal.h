//
// Created by Sebastian on 25.11.2016.
//

#ifndef TERMINAL_H
#define TERMINAL_H

#include <stdlib.h>
#include <termios.h>

int createPseudoTerminal();
char* getPseudoTerminalSlavePath(int masterFd);
int openSlavePseudoTerminal(const char* path);
void closePseudoTerminal(int masterFd);
void setAsControllingTerminal(int slaveFd);
void disconnectFromPseudoTerminal(int slaveFd);
void writeToPseudoTerminal(int masterFd, const char* input, size_t len);
size_t readFromPseudoTerminalStdin(int masterFd, char* buff, size_t buffLen);
int getPseudoTerminalAttributes(int slaveFd, struct termios* attributes);
int setPseudoTerminalAttributes(int slaveFd, int op, struct termios* attributes);

#endif //TERMINAL_H
