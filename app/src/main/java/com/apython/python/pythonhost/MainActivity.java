package com.apython.python.pythonhost;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    public static final String TAG = "PythonHost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO: Get the python version
    }
}
