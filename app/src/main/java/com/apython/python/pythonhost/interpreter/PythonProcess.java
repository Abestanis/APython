package com.apython.python.pythonhost.interpreter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
    
    /** Command to set the messenger that allows communication back to the host. */
    public static final int REGISTER_RESPONDER = 1;
    /** Command to the interpreter to set the log tag. */
    public static final int SET_LOG_TAG        = 2;
    /** Send back to the host indicating that the process will exit with the specified exit code. */
    public static final int PROCESS_EXIT       = 3;

    /**
     * Handler of incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
        private PythonProcess process;
        
        public IncomingHandler(PythonProcess process) {
            super();
            this.process = process;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case REGISTER_RESPONDER:
                process.responseMessenger = msg.replyTo;
                break;
            case SET_LOG_TAG:
                String tag = msg.getData().getString("tag");
                process.interpreter.setLogTag(tag);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    final Messenger messageHandler = new Messenger(new IncomingHandler(this));
    private Messenger responseMessenger = null;
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
        return messageHandler.getBinder();
    }

    private void startPythonInterpreter(String pythonVersion, String pseudoTerminalPath,
                                        String[] args) {
        interpreter = new PythonInterpreterRunnable(this, pythonVersion, pseudoTerminalPath, args);
        interpreter.setExitHandler(new PythonInterpreter.ExitHandler() {
            @Override
            public void onExit(int exitCode) {
                Message exitCodeMessage = Message.obtain(null, PROCESS_EXIT);
                exitCodeMessage.arg1 = exitCode;
                try {
                    responseMessenger.send(exitCodeMessage);
                } catch (RemoteException e) {
                    e.printStackTrace(); // TODO: Handle
                }
            }
        });
        interpreterThread = new Thread((PythonInterpreterRunnable) interpreter);
        interpreterThread.start();
    }
}
