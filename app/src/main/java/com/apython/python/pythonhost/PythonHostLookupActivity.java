package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/*
 * This activity can be launched via an intent with the action
 * "com.apython.python.pythonhost.PYTHON_APP_EXECUTE".
 * The activity provides the python app with the required information to bind to our host service.
 *
 * Created by Sebastian on 27.05.2015.
 */

public class PythonHostLookupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent result = new Intent();
        Log.d(MainActivity.TAG, this.getPackageName());
        result.putExtra("package_name", this.getPackageName());
        Log.d(MainActivity.TAG, PythonHostService.class.getName());
        result.putExtra("service_name", PythonHostService.class.getName());
        this.setResult(Activity.RESULT_OK, result);
        this.finish();
    }
}
