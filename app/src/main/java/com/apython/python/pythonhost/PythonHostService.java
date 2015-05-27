package com.apython.python.pythonhost;

/*
 * This service will serve as a communication entry for the python apps.
 *
 * Created by Sebastian on 26.05.2015.
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Messenger;

public class PythonHostService extends Service {

    final Messenger communicationHandler = new Messenger(new PythonAppCommunicationHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return communicationHandler.getBinder();
    }
}
