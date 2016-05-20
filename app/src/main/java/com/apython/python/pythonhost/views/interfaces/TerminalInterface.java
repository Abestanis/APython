package com.apython.python.pythonhost.views.interfaces;

import android.view.KeyEvent;

/**
 * Interface for the Terminal view.
 *
 * Created by Sebastian on 20.11.2015.
 */

public interface TerminalInterface {
    interface ProgramHandler {
        void notifyInput(String input);
        boolean dispatchKeyEvent(KeyEvent event);
    }

    void addOutput(String output);
    void enableInput(String prompt, String enqueuedInput);
    void disableInput();
    void registerInputHandler(ProgramHandler programHandler);
    boolean isInputEnabled();
}
