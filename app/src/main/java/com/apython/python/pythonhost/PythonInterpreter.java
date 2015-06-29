package com.apython.python.pythonhost;

/*
 * The Python interpreter with his own thread.
 *
 * Created by Sebastian on 10.06.2015.
 */

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

public class PythonInterpreter implements Runnable {

    static {
        System.loadLibrary("python2.7.2");
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
    }

    public final Object inputUpdater = new Object();
    public String inputLine = null;
    private Handler  handler;
    private Activity activity;
    private IOHandler ioHandler;
    interface IOHandler {
        void addOutput(String text);
        void setupInput(String prompt);
    }

    public PythonInterpreter(Handler handler, Activity activity, IOHandler ioHandler) {
        this.handler   = handler;
        this.activity  = activity;
        this.ioHandler = ioHandler;
    }


    @Override
    public void run() {
        Context context = this.activity.getApplicationContext();
        this.startInterpreter(MainActivity.TAG, context.getFilesDir().getAbsolutePath(), context.getCacheDir().getAbsolutePath());
        this.activity.finish();
    }

    public boolean notifyInput(String input) {
        synchronized (this.inputUpdater) {
            if (this.inputLine != null) {
                Log.w(MainActivity.TAG, "Interpreter still has input queued up!");
                return false;
            }
            this.inputLine = input;
            this.inputUpdater.notify();
        }
        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            this.dispatchKey(event.getUnicodeChar());
            return true;
        }
        return true; // TODO: What happens if we say false?
    }

    @SuppressWarnings("unused")
    protected void addTextToOutput(final String text) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                ioHandler.addOutput(text);
            }
        });
    }

    @SuppressWarnings("unused")
    protected String readLine(final String prompt) {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                ioHandler.setupInput(prompt);
            }
        });

        // Wait for line
        synchronized (inputUpdater) {
            try {
                inputUpdater.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        String line = this.inputLine;
        this.inputLine = null;
        if (line == null) {
            Log.w(MainActivity.TAG, "Did not receive input!");
            return null;
        }
        line = line.substring(prompt.length());
        return line + "\n";
    }

    public  native static String getPythonVersion();
    private native        void   startInterpreter(String appTag, String pythonHome, String pythonTemp);
    private native        void   dispatchKey(int character);
}
