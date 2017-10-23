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
 * Created by Sebastian on 20.10.2017.
 */

public class PythonInterpreterProcessHandle extends InterpreterPseudoTerminalIOHandle {
    private class PythonServiceConnection implements ServiceConnection {
        private boolean isBound = false;
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
            if (pythonProcess != null) {
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

        boolean bindToService() {
            isBound = context.bindService(
                    getPythonProcessIntent(), pythonProcessConnection, Context.BIND_AUTO_CREATE |
                            Context.BIND_IMPORTANT | Context.BIND_ADJUST_WITH_ACTIVITY);
            return isBound;
        }
        
        void unbind() {
            if (isBound) {
                context.unbindService(pythonProcessConnection);
                isBound = false;
            }
        }
    }

    /**
     * Handler of incoming messages from the service.
     */
    private static class IncomingHandler extends Handler {
        private final PythonInterpreterProcessHandle processHandle;

        public IncomingHandler(PythonInterpreterProcessHandle processHandle) {
            super();
            this.processHandle = processHandle;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case PythonProcess.PROCESS_EXIT:
                Log.d(processHandle.logTag, "Python process is exiting with exit code " + msg.arg1);
                processHandle.exitCode = msg.arg1;
                synchronized (processHandle) {
                    processHandle.notifyAll();
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
            synchronized (this) {
                try {
                    this.wait();
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

    private Intent getPythonProcessIntent() {
        return new Intent(context, PythonProcess.class);
    }
    
    private void bindToInterpreter() {
        pythonProcessConnection.bindToService();
        startOutputListener();
        Message registerMsg = Message.obtain(null, PythonProcess.REGISTER_RESPONDER);
        registerMsg.replyTo = messageHandler;
        pythonProcessConnection.sendMessage(registerMsg);
    }
}
