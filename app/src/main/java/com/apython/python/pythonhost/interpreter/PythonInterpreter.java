package com.apython.python.pythonhost.interpreter;

/*
 * The Python interpreter.
 *
 * Created by Sebastian on 10.06.2015.
 */

import android.content.Context;

import com.apython.python.pythonhost.CalledByNative;
import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;

import java.io.FileDescriptor;

public class PythonInterpreter {
    
    static {
        System.loadLibrary("pyLog");
        System.loadLibrary("pyInterpreter");
    }

    /**
     * A handler for the exit of the Python interpreter.
     */
    public interface ExitHandler {
        /**
         * Handle the exit of the Python interpreter.
         * This may not be called on the main thread.
         * 
         * @param exitCode The exit code of the interpreter.
         */
        void onExit(int exitCode);
    }
    
    protected final Context context;
    protected final String  pythonVersion;
    private String logTag             = MainActivity.TAG;
    private boolean running           = false;
    private String pseudoTerminalPath = null;
    private ExitHandler exitHandler   = null;
    private Integer exitCode          = null;
    
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
    
    String getLogTag() {
        return logTag;
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
        if (exitCode == null) {
            setExitCode(res);
        }
        // TODO: Show result and pause for a few seconds (add as config)
        return res;
    }

    public boolean isRunning() {
        return running;
    }
    
    public void setExitHandler(ExitHandler handler) {
        exitHandler = handler;
    }
    
    @CalledByNative
    protected void setExitCode(int exitCode) {
        this.exitCode = exitCode;
        if (exitHandler != null) {
            exitHandler.onExit(exitCode);
        }
    }

    private native String nativeGetPythonVersion(String pythonLibName);
    private native int runInterpreter(String pythonLibName, String executable, String libPath,
                                      String pyHostLibPath, String pythonHome, String pythonTemp,
                                      String xdcBasePath, String dataPath, String appTag,
                                      String[] interpreterArgs, String pseudoTerminalPath);
    public static native void interruptTerminal(FileDescriptor fd);
    public static native FileDescriptor openPseudoTerminal();
    public static native void closePseudoTerminal(FileDescriptor fd);
    public static native String getPseudoTerminalPath(FileDescriptor fd);
    public static native String getEnqueueInput(FileDescriptor fd);
    public static native FileDescriptor waitForReadLineConnection();
}
