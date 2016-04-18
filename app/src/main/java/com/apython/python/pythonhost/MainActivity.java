package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

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
//        ).commit();

        boolean skipSplashScreen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PythonSettingsActivity.KEY_SKIP_SPLASH_SCREEN,
                getResources().getBoolean(R.bool.pref_default_skip_splash_screen)
        );
        if (skipSplashScreen) {
            setupMainMenu();
        } else {
            // Display the splash screen
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
            PackageManager.installPythonExecutable(getApplicationContext(), null);
        }
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
}
