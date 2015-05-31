package com.apython.python.pythonhost;

/*
 * Provides an address for the Python app to respond to.
 *
 * Created by Sebastian on 26.05.2015.
 */

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class PythonAppCommunicationHandler extends Handler {

    // A messenger of this class, used to indicate where the Python app communication service should answer to.
    private Messenger self = new Messenger(this);

    // An interface to implement a callback function to handle an incoming message.
    private ConnectionManager connectionManager;
    public interface ConnectionManager {
        void onMessage(Message message);
    }

    public PythonAppCommunicationHandler(ConnectionManager manager) {
        this.connectionManager = manager;
    }

    @Override
    public void handleMessage(Message message) {
        this.connectionManager.onMessage(message);
    }

    // Sends the message 'message' to the Python app communication service via the messenger 'service'.
    public void sendMessage(Messenger service, Message message) {
        message.replyTo = self;
        try {
            service.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            // TODO: What now?
        }
    }
}
