package com.apython.python.pythonhost.interpreter.handles;

import android.util.Log;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.Util;
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
public abstract class InterpreterPseudoTerminalIOHandle implements PythonInterpreterHandle {
    private IOHandler ioHandler;
    private FileDescriptor pythonProcessFd = null;
    private PythonInterpreter.ExitHandler exitHandler = null;
    private Thread outputListenerThread = null;
    private Thread readLineThread = null;
    String logTag = MainActivity.TAG;
    
    @Override
    public boolean startInterpreter(String pythonVersion, String[] args) {
        pythonProcessFd = PythonInterpreter.openPseudoTerminal();
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
            pythonInput.write(input.getBytes(Util.UTF_8));
        } catch (UnsupportedEncodingException e) {
            Log.wtf(logTag, "Utf-8 encoding is not supported?!", e);
        } catch (IOException e) {
            Log.e(logTag, "Failed to write input to the Python process", e);
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
     * Start a thread that listens for the output of the Python interpreter
     * and passes the output to the ioHandler.
     */
    void startOutputListener() {
        if (pythonProcessFd == null) { return; }
        outputListenerThread = new Thread(() -> {
            final byte[] buffer = new byte[256];
            int bytesRead;
            InputStream pythonOutput = new FileInputStream(pythonProcessFd);
            try {
                while (!Thread.currentThread().isInterrupted()
                        && (bytesRead = pythonOutput.read(buffer)) != -1) {
                    final String text = new String(buffer, 0, bytesRead, Util.UTF_8);
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
        });
        outputListenerThread.setDaemon(true);
        outputListenerThread.start();
        if (!(ioHandler instanceof LineIOHandler)) { return; }
        readLineThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                FileDescriptor fd = PythonInterpreter.waitForReadLineConnection();
                if (fd == null) continue;
                try (FileInputStream inputStream = new FileInputStream(fd)) {
                    int bytesRead;
                    byte[] buffer = new byte[64];
                    StringBuilder prompt = new StringBuilder();
                    do {
                        bytesRead = inputStream.read(buffer);
                        if (bytesRead == 0) {
                            continue;
                        }
                        if (bytesRead == -1) {
                            break;
                        } // eof
                        if (buffer[bytesRead - 1] == '\0') {
                            prompt.append(new String(buffer, 0, bytesRead - 1, Util.UTF_8));
                            if (ioHandler instanceof LineIOHandler) {
                                ((LineIOHandler) ioHandler).enableLineMode(prompt.toString());
                            }
                            prompt.setLength(0);
                        } else {
                            prompt.append(new String(buffer, 0, bytesRead, Util.UTF_8));
                        }
                    } while (true);
                } catch (IOException error) {
                    Log.d(logTag, "Error listening to program output", error);
                }
                if (ioHandler instanceof LineIOHandler) {
                    ((LineIOHandler) ioHandler).stopLineMode();
                }
            }
        });
        readLineThread.setDaemon(true);
        readLineThread.start();
    }

    /**
     * Stop the thread that listens to the output of the Python interpreter.
     */
    void stopOutputListener() {
        if (outputListenerThread != null && outputListenerThread.isAlive()) {
//            outputListenerThread.interrupt();
//            try {
//                outputListenerThread.join(); // TODO: This will lock, need to interrupt the read
//            } catch (InterruptedException ignored) {}
        }
        outputListenerThread = null;
        if (readLineThread != null && readLineThread.isAlive()) {
//            readLineThread.interrupt();
//            try {
//                readLineThread.join(); // TODO: This will lock, need to interrupt the read
//            } catch (InterruptedException ignored) {}
        }
        readLineThread = null;
    }

    /**
     * Tell the pseudo terminal that the terminal window has a new size. 
     * 
     * @param width  The width of the terminal window in characters.
     * @param height The height of the terminal window in characters.
     * @param pixelWidth  The width of the terminal window in pixel.
     * @param pixelHeight The height of the terminal window in pixel.
     */
    public void setTerminalSize(int width, int height, int pixelWidth, int pixelHeight) {
        if (pythonProcessFd == null) return;
        Log.i(MainActivity.TAG, "Terminal layout changed: " + width + "x" + height + " (" + pixelWidth + "x" + pixelHeight + ")");
        PythonInterpreter.setPseudoTerminalSize(pythonProcessFd, width, height,
                                                pixelWidth, pixelHeight);
    }
}
