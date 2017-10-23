package com.apython.python.pythonhost.interpreter;

/*
 * The Python interpreter.
 *
 * Created by Sebastian on 10.06.2015.
 */

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

import com.apython.python.pythonhost.CalledByNative;
import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class PythonInterpreter {
    
    static {
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
    }
    
    protected final Context context;
    protected final String  pythonVersion;
    private String logTag = MainActivity.TAG;
    private boolean running   = false;
    private String pseudoTerminalPath = null;
    
    public PythonInterpreter(Context context, String pythonVersion, String pseudoTerminalPath) {
        this(context, pythonVersion);
        this.pseudoTerminalPath = pseudoTerminalPath;
    }

    public PythonInterpreter(Context context, String pythonVersion) {
        PackageManager.loadDynamicLibrary(context, "python" + pythonVersion);
        PackageManager.loadAdditionalLibraries(context);
        this.context = context;
        this.pythonVersion = pythonVersion;
    }

    public String getPythonVersion() {
        return nativeGetPythonVersion(System.mapLibraryName("python" + this.pythonVersion));
    }
    
    public void setLogTag(String logTag) {
        this.logTag = logTag;
    }

    public int runPythonInterpreter(String[] interpreterArgs) {
        running = true;
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
                                      this.pseudoTerminalPath);
        running = false;
        // TODO: Show result and pause for a few seconds (add as config)
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

    public int runPythonString(String command, String[] args) {
        return this.runPythonInterpreter(Util.mergeArrays(new String[] {"-c", command}, args));
    }

    public boolean isRunning() {
        return running;
    }

    private native String nativeGetPythonVersion(String pythonLibName);
    private native int    runInterpreter(String pythonLibName, String executable, String libPath,
                                         String pyHostLibPath, String pythonHome, String pythonTemp,
                                         String xdcBasePath, String dataPath, String appTag,
                                         String[] interpreterArgs, String pseudoTerminalPath);
    public native void   stopInterpreter();
    public static native void interruptTerminal(FileDescriptor fd);
    public static native FileDescriptor openPseudoTerminal(); // TODO: Close it when done.
    public static native String getPseudoTerminalPath(FileDescriptor fd);
    public static native String getEnqueueInput(FileDescriptor fd);
}
