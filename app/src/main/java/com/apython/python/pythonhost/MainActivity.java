package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    public static final String TAG = "PythonHost";

    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LinearLayout progressContainer = (LinearLayout) findViewById(R.id.progressContainer);
        final TextView progressTextView = (TextView) findViewById(R.id.updateText);
        final ProgressBar progressView = (ProgressBar) findViewById(R.id.progressBar);

        onInstallationCheckFinished(true);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                final boolean result = PackageManager.ensurePythonInstallation(
//                        getApplicationContext(),
//                        ProgressHandler.Factory.create(MainActivity.this, progressTextView, progressView, new Runnable() {
//                            @Override
//                            public void run() {
//                                progressContainer.setVisibility(View.VISIBLE);
//                            }
//                        }, null));
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        onInstallationCheckFinished(result);
//                    }
//                });
//            }
//        }).start();
    }

    // Button callbacks

    public void onPythonInterpreterButtonClick(View v) {
        startActivity(new Intent(this, PythonInterpreterActivity.class));
    }

    public void onDownloadVersionsButtonClick(View v) {
        startActivity(new Intent(this, PythonDownloadCenterActivity.class));
    }

    protected void onInstallationCheckFinished(boolean result) {
//        if (!result) {
//            Log.e(TAG, "Python installation is not complete!");
//            Toast.makeText(this.getApplicationContext(), "An error occurred during the Python installation.", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
        findViewById(R.id.progressContainer).setVisibility(View.GONE);

        // Get the python version.
        TextView pythonVersionView = (TextView) findViewById(R.id.pythonVersionText);
        pythonVersionView.setText("Python Version " + new PythonInterpreter(getApplicationContext(), PackageManager.pythonVersion).getPythonVersion());
        pythonVersionView.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setContentView(R.layout.main_menu);
            }
        }, 4000);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.splash_container);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.main_menu);
            }
        });
    }
}
