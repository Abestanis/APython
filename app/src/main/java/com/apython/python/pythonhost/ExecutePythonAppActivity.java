package com.apython.python.pythonhost;

import android.app.Activity;
import android.os.Bundle;

/*
 * This activity can be launched via an intent with the action
 * "com.python.pythonhost.PYTHON_APP_EXECUTE".
 * It is used by the Python apps to execute their Python code.
 *
 * Created by Sebastian on 27.05.2015.
 */

public class ExecutePythonAppActivity extends Activity {

    // The communication manager to handle the communication to the Python apps.
    PythonAppCommunicationManager communicationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.communicationManager = new PythonAppCommunicationManager(this);
        if (this.communicationManager.parseArgs(this.getIntent())) {
            this.communicationManager.connectToApp();
            // TODO: Maybe display a bootstrap image from the python App here if specified.
        }
    }

    @Override
    public void onBackPressed() {
        // Handle this event to the Python code
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.communicationManager.closeConnection();
    }
}
