package com.apython.python.pythonhost;

import android.app.Activity;
import android.os.Bundle;

/*
 * This activity can be launched via an intent with the action
 * "com.python.pythonhost.PYTHON_APP_GET_EXECUTION_INFO".
 * It is used by the Python apps to execute their Python code.
 *
 * Created by Sebastian on 27.05.2015.
 */

public class GetPythonAppExecutionInfoActivity extends Activity {

    // The communication manager which handles the communication with the Python apps.
    PythonAppCommunicationManager communicationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.communicationManager = new PythonAppCommunicationManager(this);
        if (this.communicationManager.parseArgs(this.getIntent())) {
            this.communicationManager.startPythonApp();
        }
    }
}
