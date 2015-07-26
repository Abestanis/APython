package com.apython.python.pythonhost;

import android.content.Context;
import android.os.Handler;

/*
 * A Python Interpreter which can run in an different thread.
 *
 * Created by Sebastian on 22.07.2015.
 */

public class PythonInterpreterRunnable extends PythonInterpreter implements Runnable {

    PythonInterpreterRunnable(Context context, final IOHandler ioHandler, final Handler handler) {
        super(context);
        this.ioHandler = new IOHandler() {
            @Override
            public void addOutput(final String text) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ioHandler.addOutput(text);
                    }
                });
            }

            @Override
            public void setupInput(final String prompt) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ioHandler.setupInput(prompt);
                    }
                });
            }
        };
    }

    @Override
    public void run() {
        this.runPythonInterpreter(null);
    }
}
