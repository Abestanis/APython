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

import com.apython.python.pythonhost.downloadcenter.PythonDownloadCenterActivity;
import com.apython.python.pythonhost.interpreter.PythonInterpreterActivity;

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
//        ).apply();

        boolean skipSplashScreen = savedInstanceState != null || 
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                        PythonSettingsActivity.KEY_SKIP_SPLASH_SCREEN,
                        getResources().getBoolean(R.bool.pref_default_skip_splash_screen)
                );
        if (skipSplashScreen) {
            setupMainMenu();
        }
        runStartupTasks(!skipSplashScreen);
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

    /**
     * Set the content view of this activity to the main menu.
     */
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
    
    private void runStartupTasks(final boolean visible) {
        // Display the splash screen
        final int MIN_SPLASH_SCREEN_TIME = 4000;
        final long startSplashScreen = System.currentTimeMillis();
        final Handler UiThreadHandler = new Handler();
        final ProgressHandler progressHandler;
        
        if (visible) {
            RelativeLayout container = (RelativeLayout) findViewById(R.id.splash_container);
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupMainMenu();
                }
            });
            final LinearLayout progressContainer = (LinearLayout)
                    findViewById(R.id.startupProcessContainer);
            TextView startupProcessText = (TextView) findViewById(R.id.startupProcessDescription);
            ProgressBar startupProcessBar = (ProgressBar) findViewById(R.id.startupProgressBar);
            progressHandler = ProgressHandler.Factory.create(this, startupProcessText,
                                                             startupProcessBar, new Runnable() {
                        @Override
                        public void run() {
                            progressContainer.setVisibility(View.VISIBLE);
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            // TODO: Handle error
                        }
                    });
        } else {
            progressHandler = null;
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO: Check access rights of all installed files
                // TODO: Check checksum of all installed libraries
                // TODO: Check for updated libraries
                PackageManager.installPythonExecutable(getApplicationContext(), progressHandler);
                
                if (!visible) return;
                long timeLeft = (System.currentTimeMillis() - startSplashScreen) - MIN_SPLASH_SCREEN_TIME;
                UiThreadHandler.postDelayed(new Runnable() {
                    public void run() {
                        setupMainMenu();
                    }
                }, Math.max(0, timeLeft));
            }
        }).start();
    }
}
