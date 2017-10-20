package com.apython.python.pythonhost.interpreter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.apython.python.pythonhost.MainActivity;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by Sebastian on 20.10.2017.
 */

public class PythonInterpreterHandle {
    private class PythonServiceConnection implements ServiceConnection {
        private Messenger      pythonProcess = null;
        private Queue<Message> messageQueue  = new ArrayDeque<>();
        public void onServiceConnected(ComponentName className, IBinder service) {
            Message message;
            pythonProcess = new Messenger(service);
            while ((message = messageQueue.poll()) != null) {
                try {
                    pythonProcess.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            pythonProcess = null;
        }

        boolean sendMessage(Message message) {
            if (connected()) {
                try {
                    pythonProcess.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                messageQueue.add(message);
            }
            return true;
        }

        boolean connected() {
            return pythonProcess != null;
        }
    }

    public interface IOHandler {
        void addOutput(String text);
        void setupInput(String prompt);
    }
    
    private Context context;
    private IOHandler ioHandler;
    private PythonServiceConnection pythonProcessConnection = new PythonServiceConnection();
    private FileDescriptor pythonProcessFd = null;
    private boolean pendingBind = false;
    
    public PythonInterpreterHandle(Context context) {
        this.context = context;
    }
    
    public boolean startInterpreter(String pythonVersion) {
        final FileDescriptor pythonProcessFd = PythonInterpreter.openPseudoTerminal();
        this.pythonProcessFd = pythonProcessFd;
        String pseudoTerminalPath = PythonInterpreter.getPseudoTerminalPath(pythonProcessFd);
        Intent pythonProcessIntent = getPythonProcessIntent();
        pythonProcessIntent.putExtra(PythonProcess.PYTHON_VERSION_KEY, pythonVersion);
        pythonProcessIntent.putExtra(PythonProcess.PSEUDO_TERMINAL_PATH_KEY, pseudoTerminalPath);
        context.startService(pythonProcessIntent);
        if (pendingBind) {
            pendingBind = false;
            bindToInterpreter();
        }
        return true;
    }
    
    public boolean attach() {
        if (pythonProcessFd == null) {
            pendingBind = true;
        } else {
            bindToInterpreter();
        }
        return true;
    }
    
    public boolean detach() {
        if (pythonProcessConnection.connected()) {
            context.unbindService(pythonProcessConnection);
        }
        return true;
    }

    public boolean stopInterpreter() {
        // TODO: Close pseudoterminal
        return context.stopService(getPythonProcessIntent());
    }

    public void interrupt() {
        if (pythonProcessFd != null) {
            PythonInterpreter.interruptTerminal(pythonProcessFd);
        }
    }
    
    public void sendInput(String input) {
        OutputStream pythonInput = new FileOutputStream(pythonProcessFd);
        try {
            pythonInput.write(input.getBytes("Utf-8"));
        } catch (UnsupportedEncodingException e) {
            Log.wtf(MainActivity.TAG, "Utf-8 encoding is not supported?!", e);
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to write input to the python process", e);
        }
    }
    
    public int getInterpreterResult() {
        return 1;
    }
    
    public void setIOHandler(IOHandler handler) {
        this.ioHandler = handler;
    }
    
    private Intent getPythonProcessIntent() {
        return new Intent(context, PythonProcess.class);
    }
    
    private void bindToInterpreter() {
        context.bindService(getPythonProcessIntent(), pythonProcessConnection,
                            Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT
                                    | Context.BIND_ADJUST_WITH_ACTIVITY); // TODO: Handle return value
        new Thread(new Runnable() {
            @Override
            public void run() {
                final byte[] buffer = new byte[256];
                int bytesRead;
                InputStream pythonOutput = new FileInputStream(pythonProcessFd);
                try {
                    while ((bytesRead = pythonOutput.read(buffer)) != -1) {
                        final String text;
                        try {
                            text = new String(buffer, 0, bytesRead, "Utf-8");
                        } catch (UnsupportedEncodingException e) {
                            Log.wtf(MainActivity.TAG, "Utf-8 encoding is not supported?!", e);
                            break;
                        }
                        if (ioHandler != null) {
                            ioHandler.addOutput(text);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
