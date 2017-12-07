package com.apython.python.pythonhost.interpreter.handles;

import android.util.Log;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.interpreter.PythonInterpreter;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * An interpreter handle that handles the I/O of the
 * Python interpreter with a pseudo-terminal.
 * 
 * Created by Sebastian on 21.10.2017.
 */
abstract class InterpreterPseudoTerminalIOHandle implements PythonInterpreterHandle {
    private IOHandler ioHandler;
    private FileDescriptor pythonProcessFd = null;
    private PythonInterpreter.ExitHandler exitHandler = null;
    private Thread outputListenerThread = null;
    private Thread readLineThread = null;
    String logTag = MainActivity.TAG;
    
    @Override
    public boolean startInterpreter(String pythonVersion, String[] args) {
        pythonProcessFd = PythonInterpreter.openPseudoTerminal();
        if (pythonProcessFd != null && ioHandler instanceof LineIOHandler) {
            
        }
        return pythonProcessFd != null;
    }
    
    @Override
    public boolean stopInterpreter() {
        if (pythonProcessFd != null) {
            PythonInterpreter.closePseudoTerminal(pythonProcessFd);
            pythonProcessFd = null;
        }
        return true;
    }

    @Override
    public void interrupt() {
        if (pythonProcessFd != null) {
            PythonInterpreter.interruptTerminal(pythonProcessFd);
        }
    }

    @Override
    public void sendInput(String input) {
        OutputStream pythonInput = new FileOutputStream(pythonProcessFd);
        try {
            pythonInput.write(input.getBytes("Utf-8"));
        } catch (UnsupportedEncodingException e) {
            Log.wtf(logTag, "Utf-8 encoding is not supported?!", e);
        } catch (IOException e) {
            Log.e(logTag, "Failed to write input to the python process", e);
        }
    }

    @Override
    public void setIOHandler(IOHandler handler) {
        this.ioHandler = handler;
    }

    @Override
    public void setLogTag(String tag) {
        logTag = tag;
    }

    @Override
    public void setExitHandler(PythonInterpreter.ExitHandler handler) {
        exitHandler = handler;
    }

    /**
     * Get the path to the slave side of the pseudo terminal that can
     * be used by the Python interpreter to connect to the pseudo-terminal.
     * 
     * @return The path to the pseudo-terminal.
     */
    String getPseudoTerminalPath() {
        if (pythonProcessFd == null) return null;
        return PythonInterpreter.getPseudoTerminalPath(pythonProcessFd);
    }

    /**
     * Start a thread that listens for the output of the python interpreter
     * and passes the output to the ioHandler.
     */
    void startOutputListener() {
        outputListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] buffer = new byte[256];
                int bytesRead;
                InputStream pythonOutput = new FileInputStream(pythonProcessFd);
                try {
                    while (!Thread.currentThread().isInterrupted()
                            && (bytesRead = pythonOutput.read(buffer)) != -1) {
                        final String text;
                        try {
                            text = new String(buffer, 0, bytesRead, "Utf-8");
                        } catch (UnsupportedEncodingException e) {
                            Log.wtf(logTag, "Utf-8 encoding is not supported?!", e);
                            break;
                        }
                        if (ioHandler != null) {
                            ioHandler.onOutput(text);
                        }
                        Log.d(logTag, text);
                    }
                } catch (IOException e) {
                    Log.w(logTag, "Reading from Python process failed", e);
                }
                if (exitHandler != null) {
                    Integer exitCode = getInterpreterResult(false);
                    exitHandler.onExit(exitCode == null ? 1 : exitCode);
                }
            }
        });
        outputListenerThread.setDaemon(true);
        outputListenerThread.start();
        readLineThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    FileDescriptor fd = PythonInterpreter.waitForReadLineConnection();
                    if (fd == null) continue;
                    if (ioHandler instanceof LineIOHandler) {
                        ((LineIOHandler) ioHandler).enableLineMode();
                    }
                    try {
                        // Wait for eof
                        new FileInputStream(fd).read();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (ioHandler instanceof LineIOHandler) {
                        ((LineIOHandler) ioHandler).stopLineMode();
                    }
                }
            }
        });
        readLineThread.setDaemon(true);
        readLineThread.start();
    }

    /**
     * Stop the thread that listens to the output of the python interpreter.
     */
    void stopOutputListener() {
        if (outputListenerThread != null && outputListenerThread.isAlive()) {
            outputListenerThread.interrupt();
            try {
                outputListenerThread.join(); // TODO: This will lock, need to interrupt the read
            } catch (InterruptedException ignored) {}
        }
        outputListenerThread = null;
        if (readLineThread != null && readLineThread.isAlive()) {
            readLineThread.interrupt();
            try {
                readLineThread.join(); // TODO: This will lock, need to interrupt the read
            } catch (InterruptedException ignored) {}
        }
        readLineThread = null;
    }
}
