package com.apython.python.pythonhost.interpreter.handles;

import android.content.Context;

import com.apython.python.pythonhost.interpreter.PythonInterpreterRunnable;

/**
 * Created by Sebastian on 21.10.2017.
 */

public class PythonInterpreterThreadHandle extends InterpreterPseudoTerminalIOHandle {
    private Context context;
    private PythonInterpreterRunnable interpreter = null;
    private Integer exitCode = null;
    String logTag = null;

    public PythonInterpreterThreadHandle(Context context) {
        this.context = context;
    }

    @Override
    public boolean startInterpreter(String pythonVersion, String[] args) {
        super.startInterpreter(pythonVersion, args);
        interpreter = new PythonInterpreterRunnable(context, pythonVersion,
                                                    getPseudoTerminalPath(), args) {
            @Override
            protected void onPythonInterpreterFinished(int result) {
                super.onPythonInterpreterFinished(result);
                exitCode = result;
                synchronized (this) {
                    this.notifyAll();
                }
            }
        };
        startOutputListener();
        new Thread(interpreter).start();
        if (logTag != null) {
            interpreter.setLogTag(logTag);
        }
        return false;
    }

    @Override
    public boolean attach() {
        return true;
    }

    @Override
    public boolean detach() {
        return false;
    }

    @Override
    public Integer getInterpreterResult(boolean block) {
        if (block) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        return exitCode;
    }

    @Override
    public void setLogTag(String tag) {
        logTag = tag;
        if (interpreter != null) {
            interpreter.setLogTag(tag);
        }
    }
}
