package com.apython.python.pythonhost;

/*
 * The Python interpreter.
 *
 * Created by Sebastian on 10.06.2015.
 */

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import java.io.File;

public class PythonInterpreter {

    static {
        System.loadLibrary("python2.7.2");
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
    }

    public final Object inputUpdater = new Object();
    public       String inputLine    = null;
    protected Context   context;
    protected IOHandler ioHandler;

    interface IOHandler {
        void addOutput(String text);
        void setupInput(String prompt);
    }

    public PythonInterpreter(Context context) {
        this(context, null);
    }

    public PythonInterpreter(Context context, IOHandler ioHandler) {
        this.context = context;
        this.ioHandler = ioHandler;
    }

    public void runPythonInterpreter(String[] interpreterArgs) {
        this.runInterpreter(PackageManager.getPythonExecutable(this.context).getAbsolutePath(),
                                   PackageManager.getSharedLibrariesPath(this.context).getAbsolutePath(),
                                   this.context.getFilesDir().getAbsolutePath(),
                                   PackageManager.getTempDir(this.context).getAbsolutePath(),
                                   PackageManager.getXDCBase(this.context).getAbsolutePath(),
                                   MainActivity.TAG,
                                   interpreterArgs,
                                   this.ioHandler != null);
    }

    public int runPythonInterpreterForResult(String[] interpreterArgs) {
        return this.runInterpreterForResult(PackageManager.getPythonExecutable(this.context).getAbsolutePath(),
                                            PackageManager.getSharedLibrariesPath(this.context).getAbsolutePath(),
                                            this.context.getFilesDir().getAbsolutePath(),
                                            PackageManager.getTempDir(this.context).getAbsolutePath(),
                                            PackageManager.getXDCBase(this.context).getAbsolutePath(),
                                            MainActivity.TAG,
                                            interpreterArgs);
    }

    public int runPythonFile(File file, String[] args) {
        return this.runPythonFile(file.getAbsolutePath(), args);
    }

    public int runPythonFile(String filePath, String[] args) {
        return this.runPythonInterpreterForResult(Util.mergeArrays(new String[] {filePath}, args));
    }

    public int runPythonModule(String module, String[] args) {
        return this.runPythonInterpreterForResult(Util.mergeArrays(new String[] {"-m", module}, args));
    }

    @SuppressWarnings("unused")
    public int runPythonString(String command, String[] args) {
        return this.runPythonInterpreterForResult(Util.mergeArrays(new String[] {"-c", command}, args));
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
    protected void addTextToOutput(String text) {
        if (ioHandler != null) {
            ioHandler.addOutput(text);
        }
    }

    @SuppressWarnings("unused")
    protected String readLine(String prompt) {
        if (ioHandler != null) {
            ioHandler.setupInput(prompt);
        }

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
    private native        void   runInterpreter(String executable, String libPath, String pythonHome, String pythonTemp, String xdcBasePath, String appTag, String[] interpreterArgs, boolean redirectOutput);
    private native        int    runInterpreterForResult(String executable, String libPath, String pythonHome, String pythonTemp, String xdcBasePath, String appTag, String[] interpreterArgs);
    private native        void   dispatchKey(int character);
}
