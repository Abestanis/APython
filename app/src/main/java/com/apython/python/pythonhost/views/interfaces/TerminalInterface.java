package com.apython.python.pythonhost.views.interfaces;

import android.view.KeyEvent;

/**
 * Interface for the Terminal view.
 *
 * Created by Sebastian on 20.11.2015.
 */

public interface TerminalInterface extends WindowManagerInterface.Window {
    interface ProgramHandler {
        void notifyInput(String input);
        boolean dispatchKeyEvent(KeyEvent event);
        void terminate();
    }
    void addOutput(String output);
    void enableInput(String prompt, String enqueuedInput);
    void disableInput();
    void setProgramHandler(ProgramHandler programHandler);
    boolean isInputEnabled();
}
