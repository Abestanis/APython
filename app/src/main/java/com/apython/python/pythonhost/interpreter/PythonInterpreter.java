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
    
    private       String  logTag        = MainActivity.TAG;
    private       String  inputLine     = null;
    protected     boolean blockingInput = true;
    protected final Context context;
    protected final String  pythonVersion;

    protected IOHandler ioHandler;

    private boolean running   = false;
    private boolean inStartup = false;
    public interface IOHandler {
        void addOutput(String text);
        void setupInput(String prompt);
    }
    public PythonInterpreter(Context context, String pythonVersion) {
        this(context, pythonVersion, null);
    }

    public PythonInterpreter(Context context, String pythonVersion, IOHandler ioHandler) {
        PackageManager.loadDynamicLibrary(context, "pythonPatch");
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
        PackageManager.loadDynamicLibrary(context, "python" + pythonVersion);
        PackageManager.loadAdditionalLibraries(context);
        this.context = context;
        this.pythonVersion = pythonVersion;
        this.ioHandler = ioHandler;
    }

    public String getPythonVersion() {
        return nativeGetPythonVersion(System.mapLibraryName("python" + this.pythonVersion));
    }

    public void setIoHandler(IOHandler ioHandler) {
        this.ioHandler = ioHandler;
    }
    
    public void setLogTag(String logTag) {
        this.logTag = logTag;
    }

    public int runPythonInterpreter(String[] interpreterArgs) {
        running = inStartup = true;
        int res = this.runInterpreter(System.mapLibraryName("python" + this.pythonVersion),
                                      PackageManager.getPythonExecutable(this.context).getAbsolutePath(),
                                      PackageManager.getDynamicLibraryPath(this.context).getAbsolutePath(),
                                      PackageManager.getSharedLibrariesPath(this.context).getAbsolutePath(),
                                      this.context.getFilesDir().getAbsolutePath(),
                                      PackageManager.getTempDir(this.context).getAbsolutePath(),
                                      PackageManager.getXDGBase(this.context).getAbsolutePath(),
                                      PackageManager.getDataPath(this.context).getAbsolutePath(),
                                      logTag,
                                      interpreterArgs,
                                      this.ioHandler != null);
        running = false;
        return res;
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

    public boolean isRunning() {
        return running;
    }

    @Override
    public void terminate() {
        stopInterpreter();
    }

    public void interrupt() {
        if (inStartup) return;
        interruptInterpreter();
        // When we are waiting for input, the following will
        // cause the python interpreter to exit.
        if (blockingInput) {
            synchronized (this) {
                this.inputLine = "";
                this.notifyAll();
            }
        } else {
            dispatchKey('\0'); // Emulate input availability
        }
        
    }

    public void notifyInput(String input) {
        if (blockingInput) {
            synchronized (this) {
                if (this.inputLine != null) {
                    Log.w(MainActivity.TAG, "Interpreter still has input queued up!");
                    input = this.inputLine + input;
                }
                this.inputLine = input;
                this.notifyAll();
            }
        } else {
            sendStringToStdin(input);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int unicodeChar = event.getUnicodeChar();
            if (unicodeChar == 0) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    unicodeChar = '\b';
                }
                // TODO: Handle more special keys
            }
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
        inStartup = false;
        this.blockingInput = blockingInput;
        if (ioHandler != null) {
            ioHandler.setupInput(prompt);
        }
        if (!blockingInput) {
            return null; // Do not wait for input
        }
        // Wait for line
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return "";
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
    public  native String getEnqueueInput();
    private native int    runInterpreter(String pythonLibName, String executable, String libPath, String pyHostLibPath, String pythonHome, String pythonTemp, String xdcBasePath, String dataPath, String appTag, String[] interpreterArgs, boolean redirectOutput);
    private native void   interruptInterpreter();
    private native void   stopInterpreter();
}
