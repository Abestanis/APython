package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String TAG = "PythonHost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get the python version.

        if (!PackageManager.ensurePythonInstallation(this.getApplicationContext())) {
            Log.e(TAG, "Python installation is not complete!");
            finish();
            return;
        }

        TextView pythonVersionView = (TextView) findViewById(R.id.pythonVersionText);
        pythonVersionView.setText("Python Version " + PythonInterpreter.getPythonVersion());

        // TODO: Provide a set of actions.

        this.startActivity(new Intent(this, PythonInterpreterActivity.class));
    }
}
