package com.apython.python.pythonhost;

/*
 * This class holds the connection to the python app communication service.
 *
 * Created by Sebastian on 31.05.2015.
 */

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

public class PythonAppConnection implements ServiceConnection {

    // Messenger for communicating with the python app service.
    private Messenger service = null;

    // True if we have called bind on the python app service.
    private boolean bound = false;

    // A listener to implement a callback when the service successfully connects.
    private ConnectionListener connectionListener;
    public interface ConnectionListener {
        void onConnected(Messenger messenger);
    }

    public PythonAppConnection(ConnectionListener connectionListener) {
        super();
        this.connectionListener = connectionListener;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(MainActivity.TAG, "onServiceConnected");
        this.service = new Messenger(service);
        this.bound   = true;
        this.connectionListener.onConnected(this.service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(MainActivity.TAG, "onServiceDisconnected");
        this.service = null;
        this.bound   = false;
    }

    public boolean isBound() {
        return this.bound;
    }

    public void setDisconnected() {
        this.bound = false;
    }
}
