package com.apython.python.pythonhost.interpreter.handles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.interpreter.PythonInterpreter;
import com.apython.python.pythonhost.interpreter.PythonProcess;

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

public class PythonInterpreterProcessHandle extends InterpreterPseudoTerminalIOHandle {
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
    private Context context;
    private PythonServiceConnection pythonProcessConnection = new PythonServiceConnection();
    
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
        if (pythonProcessConnection.connected()) {
            context.unbindService(pythonProcessConnection);
        }
        return true;
    }

    @Override
    public boolean stopInterpreter() {
        return super.stopInterpreter() && context.stopService(getPythonProcessIntent());
        
    }

    @Override
    public Integer getInterpreterResult(boolean block) {
        return null; // TODO: Implement
    }

    @Override
    public void setLogTag(String tag) {
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
        context.bindService(getPythonProcessIntent(), pythonProcessConnection,
                            Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT
                                    | Context.BIND_ADJUST_WITH_ACTIVITY); // TODO: Handle return value
        startOutputListener();
    }
}
