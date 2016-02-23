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
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class PythonInterpreter implements TerminalInterface.ProgramHandler {

    static {
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
    }

    private final Object  inputUpdater  = new Object();
    private       String  inputLine     = null;
    protected     boolean blockingInput = true;
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

    public void notifyInput(String input) {
        if (blockingInput) {
            synchronized (this.inputUpdater) {
                if (this.inputLine != null) {
                    Log.w(MainActivity.TAG, "Interpreter still has input queued up!");
                    input = this.inputLine + input;
                }
                this.inputLine = input;
                this.inputUpdater.notify();
            }
        } else {
            sendStringToStdin(input);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int unicodeChar = event.getUnicodeChar();
            // TODO: Handle special events (like delete)
            if (unicodeChar != 0) {
                this.dispatchKey(unicodeChar);
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    protected void addTextToOutput(byte[] text) {
        if (ioHandler != null) {
            try {
                ioHandler.addOutput(new String(text, "Utf-8"));
            } catch (UnsupportedEncodingException e) {
                Log.wtf(MainActivity.TAG, "Utf-8 encoding is not supported?!", e);
            }
        }
    }

    @SuppressWarnings("unused")
    protected String readLine(String prompt, boolean blockingInput) {
        this.blockingInput = blockingInput;
        if (ioHandler != null) {
            ioHandler.setupInput(prompt);
        }

        if (!blockingInput) {
            return null; // Do not wait for input
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
        return line;
    }

    private native String nativeGetPythonVersion(String pythonLibName);
    public  native void   dispatchKey(int character);
    public  native void   sendStringToStdin(String string);
    public  native String getEnqueueInputTillNewLine();
    private native int    runInterpreter(String pythonLibName, String executable, String libPath, String pythonHome, String pythonTemp, String xdcBasePath, String appTag, String[] interpreterArgs, boolean redirectOutput);
}
