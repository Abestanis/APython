package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/*
 * This manager handles the entire communication between the Python app and us.
 *
 * Created by Sebastian on 31.05.2015.
 */

public class PythonAppCommunicationManager {

    // The tag used by the Python host.
    public static final String PYTHON_HOST_TAG = MainActivity.TAG;
    // The tag used by the Python app.
    public              String PYTHON_APP_TAG;
    // The newest protocol version this Python host can understand.
    public static final int    MAX_PROTOCOL_VERSION = 0;
    // The actual protocol version used in the current communication.
    public              int    PROTOCOL_VERSION;

    // The current activity started by the Python app.
    private Activity activity;
    // The package and service name of the Python app communication service. These values must be
    // delivered via the intent that started this activity.
    private String pyAppPackageName, pyAppServiceName;
    // The connection to the communication service of the Python app.
    private PythonAppConnection connection;
    // A handler who receives all incoming messages from the Python app communication service.
    private PythonAppCommunicationHandler communicationHandler;

    public PythonAppCommunicationManager(Activity activity) {
        this.activity = activity;
        this.connection = new PythonAppConnection(new PythonAppConnection.ConnectionListener() {
            @Override
            public void onConnected(Messenger messenger) {
                new PythonExecute().start(); // TODO: Start here
            }
        });
        this.communicationHandler = new PythonAppCommunicationHandler(new PythonAppCommunicationHandler.ConnectionManager() {
            @Override
            public void onMessage(Message message) {
                handleAppMessage(message);
            }
        });
    }

    public boolean parseArgs(Intent args) {
        PROTOCOL_VERSION = args.getIntExtra("protocolVersion", -1);
        switch (PROTOCOL_VERSION) {
        case 0:
            // TODO: Handle if value is not present
            PYTHON_APP_TAG        = args.getStringExtra("appTag");
            this.pyAppPackageName = args.getStringExtra("packageName");
            this.pyAppServiceName = args.getStringExtra("serviceName");
            break;
        case -1:
            Log.e(PYTHON_HOST_TAG, "Client did not send protocol version!");
            exitWithError("Missing intent field: protocolVersion");
            return false;
        default:
            Log.w(PYTHON_HOST_TAG, "Client uses an unknown protocol version (" + PROTOCOL_VERSION
                                 + "), maximum supported protocol version is " + MAX_PROTOCOL_VERSION);
            exitWithError("Unknown protocol version");
            return false;
        }
        return true;
    }

    public void exitWithError(String errorMessage) {// TODO: Make this better or make some version handling.
        Intent result = new Intent();
        result.putExtra("errorMessage", errorMessage);
        this.activity.setResult(Activity.RESULT_CANCELED, result);
        this.activity.finish();
    }

    public void connectToApp() {
        // Create intent to connect to the python app
        Intent intent = new Intent();
        intent.setClassName(this.pyAppPackageName, this.pyAppServiceName);
        // Bind to the service
        Log.d(PYTHON_HOST_TAG, "Connecting to Python app service: " + intent);
        try {
            this.activity.bindService(intent, this.connection, Context.BIND_AUTO_CREATE);
        } catch (SecurityException se) {
            Log.e(PYTHON_HOST_TAG, "Could not connect to the python host.");
            se.printStackTrace();
            // TODO: Handle this, does this lead to a leaked Service?
            exitWithError("Binding failed");
        }
    }

    public void closeConnection() {
        Log.d(PYTHON_HOST_TAG, "bound: " + this.connection.isBound());
        // Unbind from the service
        if (this.connection.isBound()) {
            this.activity.unbindService(this.connection);
            this.connection.setDisconnected();
        }
    }

    public void handleAppMessage(Message message) {
        // TODO: Implement
    }
}
