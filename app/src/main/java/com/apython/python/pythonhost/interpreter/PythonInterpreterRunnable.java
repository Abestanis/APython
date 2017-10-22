package com.apython.python.pythonhost.interpreter;

import android.content.Context;

/*
 * A Python Interpreter which can run in an different thread.
 *
 * Created by Sebastian on 22.07.2015.
 */
public class PythonInterpreterRunnable extends PythonInterpreter implements Runnable {
    String[] args;
    
    public PythonInterpreterRunnable(Context context, String pythonVersion,
                                     String pseudoTerminalPath, String[] args) {
        super(context, pythonVersion, pseudoTerminalPath);
        this.args = args;
    }

    @Override
    public void run() {
        int result = this.runPythonInterpreter(args);
        onPythonInterpreterFinished(result);
    }
    
    protected void onPythonInterpreterFinished(int result) {}
}
