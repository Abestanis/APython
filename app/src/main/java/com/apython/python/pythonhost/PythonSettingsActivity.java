package com.apython.python.pythonhost;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.ArrayList;

/**
 * Display and change settings for this app.
 *
 * Created by Sebastian on 26.09.2015.
 */

public class PythonSettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PYTHON_VERSION_NOT_SELECTED = "";

    public static final String KEY_PYTHON_VERSION = "pref_key_default_python_version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ListPreference listPreference = (ListPreference) findPreference(KEY_PYTHON_VERSION);
        initializeVersionSetting(listPreference);

    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void initializeVersionSetting(ListPreference preference) {
        ArrayList<String> versions = PackageManager.getInstalledPythonVersions(this);
        versions.add("Always ask");
        String[] options = versions.toArray(new String[versions.size()]);
        String[] values  = new String[options.length];
        values[values.length - 1] = PYTHON_VERSION_NOT_SELECTED;
        System.arraycopy(options, 0, values, 0, options.length);
        preference.setEntries(options);
        preference.setEntryValues(values);
        preference.setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString(
                PythonSettingsActivity.KEY_PYTHON_VERSION,
                "Always Ask"
        ));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_PYTHON_VERSION.equals(key)) {
            Preference pyVersionPreference = findPreference(KEY_PYTHON_VERSION);
            pyVersionPreference.setSummary(sharedPreferences.getString(key, "Always Ask"));
        }
    }
}
