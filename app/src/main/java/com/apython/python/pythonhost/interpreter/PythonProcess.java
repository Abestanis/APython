package com.apython.python.pythonhost.interpreter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.apython.python.pythonhost.MainActivity;

/**
 * Created by Sebastian on 03.10.2017.
 */
public class PythonProcess extends Service {
    public static final String PYTHON_VERSION_KEY       = "pythonVersion";
    public static final String PYTHON_ARGUMENTS_KEY     = "pythonArgs";
    public static final String PSEUDO_TERMINAL_PATH_KEY = "pseudoTerminalPath";
    
    
    /** Command to the interpreter to set the log tag. */
    public static final int SET_LOG_TAG = 1;

    /**
     * Handler of incoming messages from clients.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SET_LOG_TAG:
                String tag = msg.getData().getString("tag");
                interpreter.setLogTag(tag);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    PythonInterpreter interpreter = null;
    Thread interpreterThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (this.interpreter != null) {
            // TODO: Handle
        }
        String pythonVersion = intent.getStringExtra(PYTHON_VERSION_KEY);
        if (pythonVersion == null) {
            // TODO: Handle
        }
        String pseudoTerminalPath = intent.getStringExtra(PSEUDO_TERMINAL_PATH_KEY);
        String[] args = intent.getStringArrayExtra(PYTHON_ARGUMENTS_KEY);
        startPythonInterpreter(pythonVersion, pseudoTerminalPath, args);
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void startPythonInterpreter(String pythonVersion, String pseudoTerminalPath,
                                        String[] args) {
        interpreter = new PythonInterpreterRunnable(this, pythonVersion, pseudoTerminalPath, args);
        interpreter.setExitHandler(new PythonInterpreter.ExitHandler() {
            @Override
            public void onExit(int exitCode) {
                Log.d(MainActivity.TAG, "Python process will exit wit exit code " + exitCode);
                // TODO: Send the exit code
            }
        });
        interpreterThread = new Thread((PythonInterpreterRunnable) interpreter);
        interpreterThread.start();
    }
}
