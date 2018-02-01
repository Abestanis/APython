package com.apython.python.pythonhost.views.interfaces;

/**
 * Interface for the Terminal view.
 *
 * Created by Sebastian on 20.11.2015.
 */

public interface TerminalInterface extends WindowManagerInterface.Window {
    interface ProgramHandler {
        void sendInput(String input);
        void interrupt();
        void terminate();
        void onTerminalSizeChanged(int newWidth, int newHeight, int pixelWidth, int pixelHeight);
    }
    void addOutput(String output);
    void enableLineInput(String prompt);
    void disableLineInput();
    void setProgramHandler(ProgramHandler programHandler);
    boolean isInputEnabled();
}
