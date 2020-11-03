package com.apython.python.pythonhost

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.apython.python.pythonhost.downloadcenter.PythonDownloadCenterActivity
import com.apython.python.pythonhost.interpreter.PythonInterpreterActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val skipSplashScreen = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
            PythonSettingsActivity.KEY_SKIP_SPLASH_SCREEN,
            resources.getBoolean(R.bool.pref_default_skip_splash_screen)
        )
        if (skipSplashScreen || savedInstanceState != null) {
            setupMainMenu()
        } else {
            // Display the splash screen
            val handler = Handler()
            handler.postDelayed(this::setupMainMenu, 4000)
            val container = findViewById<RelativeLayout>(R.id.splash_container)
            container.setOnClickListener { setupMainMenu() }
            PackageManager.installPythonExecutable(applicationContext, null)
        }
    }

    // Button callbacks
    fun onPythonInterpreterButtonClick(v: View?) {
        startActivity(Intent(this, PythonInterpreterActivity::class.java))
    }

    fun onDownloadVersionsButtonClick(v: View?) {
        startActivity(Intent(this, PythonDownloadCenterActivity::class.java))
    }

    fun onSettingsButtonClick(v: View?) {
        startActivity(Intent(this, PythonSettingsActivity::class.java))
    }

    /**
     * Set the content view of this activity to the main menu.
     */
    private fun setupMainMenu() {
        setContentView(R.layout.main_menu)
        val interpreterButtonContainer =
            findViewById<LinearLayout>(R.id.main_menu_interpreter_button_container)
        interpreterButtonContainer.setOnLongClickListener {
            val interpreterIntent = Intent(this@MainActivity, PythonInterpreterActivity::class.java)
            interpreterIntent.putExtra(
                "pythonVersion",
                PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED
            )
            startActivity(interpreterIntent)
            true
        }
    }

    companion object {
        const val TAG = "PythonHost"
    }
}