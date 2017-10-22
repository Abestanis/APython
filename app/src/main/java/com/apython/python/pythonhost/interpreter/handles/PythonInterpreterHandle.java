package com.apython.python.pythonhost.interpreter.handles;

/**
 * Created by Sebastian on 21.10.2017.
 */

public interface PythonInterpreterHandle {
    interface IOHandler {
        void addOutput(String text);
    }
    
    boolean startInterpreter(String pythonVersion, String[] args);
    boolean attach();
    boolean detach();
    boolean stopInterpreter();
    void interrupt();
    void sendInput(String input);
    Integer getInterpreterResult(boolean block);
    void setIOHandler(IOHandler handler);
    void setLogTag(String tag);
}
