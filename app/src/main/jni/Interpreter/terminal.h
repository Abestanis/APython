//
// Created by Sebastian on 25.11.2016.
//

#ifndef TERMINAL_H
#define TERMINAL_H

#include <stdlib.h>
#include <termios.h>

struct PseudoTerminal;
typedef struct PseudoTerminal PseudoTerminal;

PseudoTerminal* createPseudoTerminal(void (*onOutput)(const char*, size_t),
                                     void (*onError)(const char*, size_t),
                                     struct termios *termp, struct winsize *winp);
void closePseudoTerminal(PseudoTerminal* terminal);
void connectToPseudoTerminalFromChild(PseudoTerminal* terminal);
void disconnectFromPseudoTerminal(PseudoTerminal* terminal);
void writeToPseudoTerminal(PseudoTerminal* terminal, const char* input, size_t len);
size_t readFromPseudoTerminalStdin(PseudoTerminal* terminal, char* buff, size_t buffLen);
void handlePseudoTerminalOutput(PseudoTerminal* terminal);

int getPseudoTerminalAttributes(PseudoTerminal* terminal, struct termios* attributes);
int setPseudoTerminalAttributes(PseudoTerminal* terminal, int op, struct termios* attributes);


#endif //TERMINAL_H
