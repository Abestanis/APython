package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String TAG = "PythonHost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LinearLayout progressContainer = (LinearLayout) findViewById(R.id.progressContainer);
        final TextView progressTextView = (TextView) findViewById(R.id.updateText);
        final ProgressBar progressView = (ProgressBar) findViewById(R.id.progressBar);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean result = PackageManager.ensurePythonInstallation(getApplicationContext(), new PackageManager.ProgressHandler() {
                    boolean enabled = false;

                    @Override
                    public void enable(final String text) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressTextView.setText(text);
                                if (enabled) {
                                    return;
                                }
                                enabled = true;
                                progressContainer.setVisibility(View.VISIBLE);
                                progressView.setIndeterminate(true);
                                progressView.setMax(100);
                            }
                        });
                    }

                    @Override
                    public void setText(final String text) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressTextView.setText(text);
                            }
                        });
                    }

                    @Override
                    public void setProgress(final float progress) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progress < 0) {
                                    progressView.setIndeterminate(true);
                                } else {
                                    progressView.setIndeterminate(false);
                                    progressView.setProgress((int) (progress * 100));
                                }
                            }
                        });
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onInstallationCheckFinished(result);
                    }
                });
            }
        }).start();
    }

    protected void onInstallationCheckFinished(boolean result) {
        if (!result) {
            Log.e(TAG, "Python installation is not complete!");
            finish();
            return;
        }
        findViewById(R.id.progressContainer).setVisibility(View.GONE);

        // Get the python version.
        TextView pythonVersionView = (TextView) findViewById(R.id.pythonVersionText);
        pythonVersionView.setText("Python Version " + PythonInterpreter.getPythonVersion());
        pythonVersionView.setVisibility(View.VISIBLE);

        // TODO: Provide a set of actions.

        this.startActivity(new Intent(this, PythonInterpreterActivity.class));
    }
}
