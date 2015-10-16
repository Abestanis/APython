package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String TAG = "PythonHost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This is just for debugging on localhost
//        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(
//                PythonSettingsActivity.KEY_PYTHON_DOWNLOAD_URL,
//                "http://10.0.2.2:8000"
//        ).commit();

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

    public void onSettingsButtonClick(View v) {
        startActivity(new Intent(this, PythonSettingsActivity.class));
    }

    private void setupMainMenu() {
        setContentView(R.layout.main_menu);
        LinearLayout interpreterButtonContainer = (LinearLayout) findViewById(R.id.main_menu_interpreter_button_container);
        interpreterButtonContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent interpreterIntent = new Intent(MainActivity.this, PythonInterpreterActivity.class);
                interpreterIntent.putExtra("pythonVersion", PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED);
                startActivity(interpreterIntent);
                return true;
            }
        });
    }

    protected void onInstallationCheckFinished(boolean result) {
//        if (!result) {
//            Log.e(TAG, "Python installation is not complete!");
//            Toast.makeText(this.getApplicationContext(), "An error occurred during the Python installation.", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
        findViewById(R.id.progressContainer).setVisibility(View.GONE);
        boolean skipSplashScreen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PythonSettingsActivity.KEY_SKIP_SPLASH_SCREEN,
                getResources().getBoolean(R.bool.pref_default_skip_splash_screen)
        );
        if (skipSplashScreen) {
            setupMainMenu();
            return;
        }

        // Get the python version.
        TextView pythonVersionView = (TextView) findViewById(R.id.pythonVersionText);
        pythonVersionView.setText("Python");// Version " + new PythonInterpreter(getApplicationContext(), PackageManager.pythonVersion).getPythonVersion());
        pythonVersionView.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setupMainMenu();
            }
        }, 4000);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.splash_container);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupMainMenu();
            }
        });
    }
}
