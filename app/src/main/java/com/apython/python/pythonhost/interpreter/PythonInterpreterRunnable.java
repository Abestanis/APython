package com.apython.python.pythonhost.interpreter;

import android.content.Context;

/*
 * A Python Interpreter which can run in an different thread.
 *
 * Created by Sebastian on 22.07.2015.
 */
class PythonInterpreterRunnable extends PythonInterpreter implements Runnable {
    PythonInterpreterRunnable(Context context, String pythonVersion, String pseudoTerminalPath) {
        super(context, pythonVersion, pseudoTerminalPath);
    }

    @Override
    public void run() {
        int result = this.runPythonInterpreter(null);
        onPythonInterpreterFinished(result);
    }
    
    protected void onPythonInterpreterFinished(int result) {}
}
