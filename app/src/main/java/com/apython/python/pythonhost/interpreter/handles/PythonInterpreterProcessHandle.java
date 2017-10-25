package com.apython.python.pythonhost.interpreter.handles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.apython.python.pythonhost.interpreter.PythonProcess;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * An interpreter handle that communicates with
 * the Python interpreter in a separate process.
 * 
 * Created by Sebastian on 20.10.2017.
 */
public class PythonInterpreterProcessHandle extends InterpreterPseudoTerminalIOHandle {
    /**
     * ServiceConnection with the Python process.
     */
    private class PythonServiceConnection implements ServiceConnection {
        private boolean isBound = false;
        private Messenger      pythonProcess = null;
        private Queue<Message> messageQueue  = new ArrayDeque<>();
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Message message;
            pythonProcess = new Messenger(service);
            while ((message = messageQueue.poll()) != null) {
                _sendMessage(message);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            pythonProcess = null;
            exitCode = exitCode == null ? 1 : exitCode;
            synchronized (exitLocker) {
                exitLocker.notifyAll();
            }
        }

        /**
         * Send a message to the Python process or put it in the message queue
         * and send it as soon as the python process is connected.
         * 
         * @param message The message to send.
         * @return true if the message was send or stored to the queue successfully.
         */
        boolean sendMessage(Message message) {
            if (pythonProcess != null) {
                return _sendMessage(message);
            } else {
                messageQueue.add(message);
            }
            return true;
        }

        /**
         * Bind to the Python process.
         * 
         * @return true if the bind was successful.
         */
        boolean bindToService() {
            isBound = context.bindService(
                    getPythonProcessIntent(), pythonProcessConnection, Context.BIND_AUTO_CREATE |
                            Context.BIND_IMPORTANT | Context.BIND_ADJUST_WITH_ACTIVITY);
            return isBound;
        }

        /**
         * Unbind from the Python process.
         */
        void unbind() {
            if (isBound) {
                context.unbindService(pythonProcessConnection);
                isBound = false;
            }
        }

        /**
         * Send the message to the Python process.
         *
         * @param message The message to send.
         * @return true if the message was send successfully.
         */
        private boolean _sendMessage(Message message) {
            try {
                pythonProcess.send(message);
                return true;
            } catch (RemoteException e) {
                Log.w(logTag, "Failed to send message " + message, e);
            }
            return false;
        }
    }

    /**
     * Handler of incoming messages from the Python process.
     */
    private static class IncomingHandler extends Handler {
        private final PythonInterpreterProcessHandle processHandle;

        IncomingHandler(PythonInterpreterProcessHandle processHandle) {
            super();
            this.processHandle = processHandle;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PythonProcess.PROCESS_EXIT:
                Log.d(processHandle.logTag, "Python process is exiting with exit code " + msg.arg1);
                processHandle.exitCode = msg.arg1;
                synchronized (processHandle.exitLocker) {
                    processHandle.exitLocker.notifyAll();
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    private Context context;
    private PythonServiceConnection pythonProcessConnection = new PythonServiceConnection();
    private final Messenger messageHandler = new Messenger(new IncomingHandler(this));
    private final Object exitLocker = new Object();
    private Integer exitCode = null;
    
    public PythonInterpreterProcessHandle(Context context) {
        this.context = context;
    }

    @Override
    public boolean startInterpreter(String pythonVersion, String[] args) {
        super.startInterpreter(pythonVersion, args);
        Intent pythonProcessIntent = getPythonProcessIntent();
        pythonProcessIntent.putExtra(PythonProcess.PYTHON_VERSION_KEY, pythonVersion);
        pythonProcessIntent.putExtra(PythonProcess.PYTHON_ARGUMENTS_KEY, args);
        pythonProcessIntent.putExtra(PythonProcess.PSEUDO_TERMINAL_PATH_KEY,
                                     getPseudoTerminalPath());
        context.startService(pythonProcessIntent);
        return true;
    }

    @Override
    public boolean attach() {
        bindToInterpreter();
        return true;
    }

    @Override
    public boolean detach() {
        pythonProcessConnection.unbind();
        return true;
    }

    @Override
    public boolean stopInterpreter() {
        return super.stopInterpreter() && context.stopService(getPythonProcessIntent());
        
    }

    @Override
    public Integer getInterpreterResult(boolean block) {
        if (exitCode == null && block) {
            synchronized (exitLocker) {
                try {
                    exitLocker.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        return exitCode;
    }

    @Override
    public void setLogTag(String tag) {
        super.setLogTag(tag);
        Message logTagMessage = Message.obtain(null, PythonProcess.SET_LOG_TAG);
        Bundle data = new Bundle();
        data.putString("tag", tag);
        logTagMessage.setData(data);
        pythonProcessConnection.sendMessage(logTagMessage);
    }

    /**
     * @return The intent for the Python process.
     */
    private Intent getPythonProcessIntent() {
        return new Intent(context, PythonProcess.class);
    }

    /**
     * Initialize communication with the Python process.
     */
    private void bindToInterpreter() {
        pythonProcessConnection.bindToService();
        startOutputListener();
        Message registerMsg = Message.obtain(null, PythonProcess.REGISTER_RESPONDER);
        registerMsg.replyTo = messageHandler;
        pythonProcessConnection.sendMessage(registerMsg);
    }
}
