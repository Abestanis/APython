package com.apython.python.pythonhost.interpreter;

/*
 * The Python interpreter.
 *
 * Created by Sebastian on 10.06.2015.
 */

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;

import java.io.File;

public class PythonInterpreter {

    static {
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
    }

    public final Object inputUpdater = new Object();
    public       String inputLine    = null;
    protected Context   context;
    protected String    pythonVersion;
    protected IOHandler ioHandler;

    public interface IOHandler {
        void addOutput(String text);
        void setupInput(String prompt);
    }

    public PythonInterpreter(Context context, String pythonVersion) {
        this(context, pythonVersion, null);
    }

    public PythonInterpreter(Context context, String pythonVersion, IOHandler ioHandler) {
        PackageManager.loadDynamicLibrary(context, "pythonPatch");
        PackageManager.loadDynamicLibrary(context, "python" + pythonVersion);
        PackageManager.loadAdditionalLibraries(context);
        this.context = context;
        this.pythonVersion = pythonVersion;
        this.ioHandler = ioHandler;
    }

    public String getPythonVersion() {
        return nativeGetPythonVersion(System.mapLibraryName("python" + this.pythonVersion));
    }

    public int runPythonInterpreter(String[] interpreterArgs) {
        return this.runInterpreter(System.mapLibraryName("python" + this.pythonVersion),
                                   PackageManager.getPythonExecutable(this.context).getAbsolutePath(),
                                   PackageManager.getStandardLibPath(this.context).getAbsolutePath(),
                                   this.context.getFilesDir().getAbsolutePath(),
                                   PackageManager.getTempDir(this.context).getAbsolutePath(),
                                   PackageManager.getXDGBase(this.context).getAbsolutePath(),
                                   MainActivity.TAG,
                                   interpreterArgs,
                                   this.ioHandler != null);
    }

    public int runPythonFile(File file, String[] args) {
        return this.runPythonFile(file.getAbsolutePath(), args);
    }

    public int runPythonFile(String filePath, String[] args) {
        return this.runPythonInterpreter(Util.mergeArrays(new String[] {filePath}, args));
    }

    public int runPythonModule(String module, String[] args) {
        return this.runPythonInterpreter(Util.mergeArrays(new String[] {"-m", module}, args));
    }

    @SuppressWarnings("unused")
    public int runPythonString(String command, String[] args) {
        return this.runPythonInterpreter(Util.mergeArrays(new String[] {"-c", command}, args));
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
        }
        return true;
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
        return line;
    }

    private native String nativeGetPythonVersion(String pythonLibName);
    public  native void   dispatchKey(int character);
    private native int    runInterpreter(String pythonLibName, String executable, String libPath, String pythonHome, String pythonTemp, String xdcBasePath, String appTag, String[] interpreterArgs, boolean redirectOutput);
}
