package com.apython.python.pythonhost;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * An activity where the user can download and delete python versions and modules.
 *
 * Created by Sebastian on 09.08.2015.
 */

public class PythonDownloadCenterActivity extends Activity {

    private PythonVersionListAdapter pythonVersionListAdapter;
    volatile boolean isUpdateRunning = false;

    public static String indexPath = "index.json";

    private PythonDownloadServiceConnection downloadServiceConnection = new PythonDownloadServiceConnection();
    class PythonDownloadServiceConnection implements ServiceConnection {
        private PythonDownloadService        downloadService      = null;
        private boolean                      isBound              = false;
        private boolean                      displayNotification  = false;
        private Map<String, ProgressHandler> progressHandlerCache = new HashMap<>();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(MainActivity.TAG, "Service Connected.");
            isBound = true;
            downloadService = ((PythonDownloadService.PythonDownloadServiceBinder) service).getService();
            downloadService.showProgressAsNotification(displayNotification);
            for (String version : progressHandlerCache.keySet()) {
                downloadService.registerProgressHandler(version, progressHandlerCache.get(version));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(MainActivity.TAG, "Service Disconnected.");
            downloadService = null;
        }

        public void registerProgressHandler(String version, ProgressHandler progressHandler) {
            if (downloadService != null) {
                downloadService.registerProgressHandler(version, progressHandler);
            } else {
                progressHandlerCache.put(version, progressHandler);
            }
        }

        public Map<String, ProgressHandler> getProgressHandlerList() {
            if (downloadService != null) {
                return downloadService.getProgressHandlerList();
            } else {
                return progressHandlerCache;
            }
        }

        public void disconnect() {
            if (isBound) {
                unbindService(this);
                isBound = false;
                downloadService = null;
            }
        }

        public void showProgressAsNotification(boolean showNotification) {
            displayNotification = showNotification;
            if (downloadService != null) {
                downloadService.showProgressAsNotification(showNotification);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_download_center);

        final ListView pythonVersionsContainer = (ListView) findViewById(R.id.pythonVersions_scrollContainer);
        final EditText searchInput = (EditText) findViewById(R.id.search_input);
        Spinner orderSelector = (Spinner) findViewById(R.id.order_selector);
        ImageButton refreshButton = (ImageButton) findViewById(R.id.refresh_button);

        pythonVersionListAdapter = new PythonVersionListAdapter(this);
        pythonVersionListAdapter.setActionHandler(new PythonVersionListAdapter.ActionHandler() {
            @Override
            public void onDownload(String version, String[] downloadUrls, String[] md5Hashes,
                                   int numRequirements, int numDependencies, int numModules,
                                   ProgressHandler progressHandler) {
                download(version, downloadUrls, md5Hashes, numRequirements, numDependencies, numModules, progressHandler);
            }

            @Override
            public void onUpdateProgressHandler(String pythonVersion, ProgressHandler progressHandler) {
                downloadServiceConnection.registerProgressHandler(pythonVersion, progressHandler);
            }
        });
        pythonVersionsContainer.setAdapter(pythonVersionListAdapter);
        TextView emptyTextView = new TextView(getApplicationContext());
        emptyTextView.setText("No matches");
        ((ViewGroup) pythonVersionsContainer.getParent()).addView(emptyTextView);
        pythonVersionsContainer.setEmptyView(emptyTextView);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePythonVersions();
            }
        });

        searchInput.requestFocus();
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null) {
                    Log.i(MainActivity.TAG, "Selected " + pythonVersionsContainer.getItemAtPosition(0));
                    return true;
                }
                return false;
            }
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            boolean doAutocomplete = true;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                doAutocomplete = after != 0;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (!doAutocomplete) {
                    doAutocomplete = true;
                } else if (!text.equals("")) {
                    if (text.endsWith(" ") && "python".startsWith(text.substring(0, text.length() - 1).toLowerCase())) {
                        secureSetText("Python ");
                        searchInput.setSelection(searchInput.getText().length());
                    } else if ("python".startsWith(text.toLowerCase())) {
                        secureSetText("Python");
                        searchInput.setSelection(text.length(), "Python".length());
                    }
                }
                pythonVersionListAdapter.filter(searchInput.getText().toString());
            }

            private void secureSetText(String text) {
                searchInput.removeTextChangedListener(this);
                searchInput.setText(text);
                searchInput.addTextChangedListener(this);
            }
        });
        updatePythonVersions();
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PythonDownloadService.class.getName().equals(service.service.getClassName())) {
                Intent serviceIntent = new Intent(this, PythonDownloadService.class);
                if (!downloadServiceConnection.isBound) {
                    bindService(serviceIntent, downloadServiceConnection, 0);
                }
                updateProgressHandler();
                downloadServiceConnection.showProgressAsNotification(false);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        downloadServiceConnection.showProgressAsNotification(true);
        downloadServiceConnection.disconnect();
    }

    private String getDownloadServerUrl() {
        return PreferenceManager.getDefaultSharedPreferences(PythonDownloadCenterActivity.this)
                .getString(PythonSettingsActivity.KEY_PYTHON_DOWNLOAD_URL,
                           getString(R.string.pref_default_python_download_url));
    }

    public void updatePythonVersions() {
        if (this.isUpdateRunning) {
            return;
        }
        this.isUpdateRunning = true;
        pythonVersionListAdapter.invalidateVersionList();
        // Update installed versions
        pythonVersionListAdapter.updateInstalledVersionsList();
        // Update remote versions
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpResponse response = Util.connectToUrl(getDownloadServerUrl() + "/" + indexPath, 15);
                String indexString;
                if (response == null) {
                    isUpdateRunning = false;
                    return;
                }
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    Log.w(MainActivity.TAG, "Updating python versions failed with status code "
                            + response.getStatusLine().getStatusCode() + ": "
                            + response.getStatusLine().getReasonPhrase());
                    isUpdateRunning = false; // TODO: Add Dialog for failures
                    return;
                }
                try {
                    InputStream input = response.getEntity().getContent();
                    indexString = Util.convertStreamToString(input);
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    isUpdateRunning = false;
                    return;
                }
                final JSONObject data;
                try {
                    data = new JSONObject(indexString);
                } catch (JSONException e) {
                    e.printStackTrace();
                    isUpdateRunning = false;
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: Update inner GridView adapter.
                        pythonVersionListAdapter.parseJSONData(data);
                        pythonVersionListAdapter.notifyDataSetChanged();
                        updateProgressHandler();
                        isUpdateRunning = false;
                    }
                });
            }
        }).start();
    }

    private void updateProgressHandler() {
        for (String activeVersion : downloadServiceConnection.getProgressHandlerList().keySet()) {
            ProgressHandler progressHandler = this.pythonVersionListAdapter.getProgressHandler(activeVersion);
            if (progressHandler != null) {
                downloadServiceConnection.registerProgressHandler(activeVersion, progressHandler);
            }
        }
    }



    protected void download(String version, String[] downloadUrls, String[] md5Hashes,
                            int numRequirements, int numDependencies, int numModules,
                            final ProgressHandler progressHandler) {
        final Intent serviceIntent = new Intent(this, PythonDownloadService.class);
        serviceIntent.putExtra("waitForProgressHandler", true);
        serviceIntent.putExtra("version", version);
        serviceIntent.putExtra("serverUrl", getDownloadServerUrl());
        serviceIntent.putExtra("downloadUrls", downloadUrls);
        serviceIntent.putExtra("md5Hashes", md5Hashes);
        serviceIntent.putExtra("numRequirements", numRequirements);
        serviceIntent.putExtra("numDependencies", numDependencies);
        serviceIntent.putExtra("numModules", numModules);

        startService(serviceIntent);
        bindService(serviceIntent, downloadServiceConnection, 0);
        downloadServiceConnection.registerProgressHandler(Util.getMainVersionPart(version), progressHandler);
    }

}