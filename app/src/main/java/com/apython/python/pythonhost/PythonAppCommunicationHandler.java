package com.apython.python.pythonhost;

/*
 * Handles the communication between the python app and us.
 *
 * Created by Sebastian on 26.05.2015.
 */


import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class PythonAppCommunicationHandler extends Handler {

    private Messenger self = new Messenger(this);

    public static final int PROTOCOL_VERSION = 0;

    private enum MessageIdentifiers {
        PROTOCOL_VERSION_HANDSHAKE(0);

        private int id;

        MessageIdentifiers(final int _id) {
            id = _id;
        }

        private static Map<Integer, MessageIdentifiers> map = new HashMap<>();

        static {
            for (MessageIdentifiers identifier : MessageIdentifiers.values()) {
                map.put(identifier.id, identifier);
            }
        }

        public static MessageIdentifiers valueOf(int identifier) {
            return map.get(identifier);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        Log.d(MainActivity.TAG, "Got Message!");
        switch (MessageIdentifiers.valueOf(msg.what)) {
        case PROTOCOL_VERSION_HANDSHAKE:
            Log.d(MainActivity.TAG, "Got PROTOCOL_VERSION_HANDSHAKE!");
            Log.d(MainActivity.TAG, "Client has protocol version: " + msg.arg1);
            replyVersionHandshake(msg.replyTo);
            break;
        default:
            Log.w(MainActivity.TAG, "Got unexpected message identifier " + msg.what);
            break;
        }

    }

    private void sendMessage(Messenger messenger, Message msg) {
        msg.replyTo = self;
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            // TODO: What now?
        }
    }

    private void replyVersionHandshake(Messenger messenger) {
        Message msg = Message.obtain(null, MessageIdentifiers.PROTOCOL_VERSION_HANDSHAKE.id);
        msg.arg1 = PROTOCOL_VERSION;
        //msg.obj = Python version
        this.sendMessage(messenger, msg);
    }

}
