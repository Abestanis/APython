package com.apython.python.pythonhost.interpreter.handles;

import com.apython.python.pythonhost.interpreter.PythonInterpreter;

/**
 * A Handle to the python interpreter.
 * 
 * Created by Sebastian on 21.10.2017.
 */
public interface PythonInterpreterHandle {
    /**
     * A Handler for IO from the interpreter.
     */
    interface IOHandler {
        /**
         * Handle output from the interpreter.
         * This may not be called on the main app thread.
         * 
         * @param output The output received from the interpreter.
         */
        void onOutput(String output);
    }


    /**
     * A Handler for IO from the interpreter that supports a "read line" mode.
     */
    interface LineIOHandler extends IOHandler {
        /**
         * Enable the line mode.
         * 
         * @param prompt An optional prompt.
         */
        void enableLineMode(String prompt);

        /**
         * Interrupt reading in a line and go back to normal send character mode.
         */
        void stopLineMode();
    }

    /**
     * Start the Python interpreter.
     * 
     * @param pythonVersion The python version to start.
     * @param args Optional arguments to the Python interpreter.
     * @return false, if an error occurred while starting the interpreter.
     */
    boolean startInterpreter(String pythonVersion, String[] args);

    /**
     * Attach to the interpreter to allow to communicate with it.
     * 
     * @return true, if we successfully attached to the interpreter.
     */
    boolean attach();

    /**
     * Detach from the interpreter.
     * 
     * @return true, if the detach was successful.
     */
    boolean detach();

    /**
     * Stop / kill the Python interpreter.
     * 
     * @return false, if there was an error while stopping the interpreter.
     */
    boolean stopInterpreter();

    /**
     * Interrupt the interpreter. (Send SIGINT)
     */
    void interrupt();

    /**
     * Send input to the interpreter.
     * 
     * @param input The input to send to the interpreter.
     */
    void sendInput(String input);

    /**
     * Get the exit code of the interpreter.
     * If the interpreter has not exited yet and block is true, wait until
     * the interpreter has exited or until the wait has been interrupted.
     * If block false and the interpreter has not exited yet, return null.
     * 
     * @param block If true, block until we have the interpreter result.
     * @return The exit code of the interpreter or null.
     */
    Integer getInterpreterResult(boolean block);

    /**
     * Set the input/output handler for the IO of the interpreter.
     * 
     * @param handler The new IO-handler.
     */
    void setIOHandler(IOHandler handler);

    /**
     * Set the tag the interpreter should use for logging.
     * 
     * @param tag The new log tag.
     */
    void setLogTag(String tag);

    /**
     * Set the exit handler for the interpreter.
     * 
     * @param handler The new exit handler.
     */
    void setExitHandler(PythonInterpreter.ExitHandler handler);
}
