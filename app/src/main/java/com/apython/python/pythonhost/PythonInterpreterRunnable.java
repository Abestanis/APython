package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/*
 * A Python Interpreter which can run in an different thread.
 *
 * Created by Sebastian on 22.07.2015.
 */

public class PythonInterpreterRunnable extends PythonInterpreter implements Runnable {

    Activity activity;

    PythonInterpreterRunnable(Context context, String pythonVersion, final IOHandler ioHandler, final Activity activity) {
        super(context, pythonVersion);
        this.activity = activity;
        this.ioHandler = new IOHandler() {
            @Override
            public void addOutput(final String text) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ioHandler.addOutput(text);
                    }
                });
            }

            @Override
            public void setupInput(final String prompt) {
                activity.runOnUiThread(new Runnable() {
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
        int res = this.runPythonInterpreter(null);
        Log.d(MainActivity.TAG, "Python interpreter exited with result " + res);
        activity.finish();
    }
}
