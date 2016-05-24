package com.apython.python.pythonhost.downloadcenter;

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
import android.widget.Toast;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.PythonSettingsActivity;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.downloadcenter.items.Dependency;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * An activity which allows the user to download and delete python versions and modules.
 *
 * Created by Sebastian on 09.08.2015.
 */

public class PythonDownloadCenterActivity extends Activity {

    public static final String INDEX_PATH = "index.json";

    private PythonVersionListAdapter pythonVersionListAdapter;
    private volatile boolean isUpdateRunning = false;
    private ImageButton refreshButton;

    public class ServiceActionRunnable {
        private Dependency                              dependency;
        private ProgressHandler.TwoLevelProgressHandler progressHandler;

        public ServiceActionRunnable(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler) {
            super();
            this.dependency = dependency;
            this.progressHandler = progressHandler;
        }

        public Dependency getDependency() {
            return dependency;
        }

        public ProgressHandler.TwoLevelProgressHandler getProgressHandler() {
            return progressHandler;
        }

        public boolean applyAction(ProgressHandler.TwoLevelProgressHandler progressHandler) {
            return dependency.applyAction(progressHandler);
        }
    }

    private PythonDownloadServiceConnection downloadServiceConnection = new PythonDownloadServiceConnection();
    class PythonDownloadServiceConnection implements ServiceConnection {
        private PythonDownloadCenterService      downloadService     = null;
        private boolean                          isBound             = false;
        private boolean                          displayNotification = false;
        private ArrayList<ServiceActionRunnable> actionQueue         = new ArrayList<>();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(MainActivity.TAG, "Service Connected.");
            isBound = true;
            downloadService = ((PythonDownloadCenterService.PythonDownloadServiceBinder) service).getService();
            downloadService.showProgressAsNotification(displayNotification);
            for (ServiceActionRunnable action : actionQueue) {
                downloadService.enqueueAction(action);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(MainActivity.TAG, "Service Disconnected.");
            downloadService = null;
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

        public void addPendingActionToQueue(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler) {
            ServiceActionRunnable action = new ServiceActionRunnable(dependency, progressHandler);
            if (downloadService == null) {
                actionQueue.add(action);
            } else {
                downloadService.enqueueAction(action);
            }
        }

        public ArrayList<ServiceActionRunnable> getActionQueue() {
            return downloadService == null ? actionQueue : downloadService.getActionQueue();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_download_center);

        final ListView pythonVersionsContainer = (ListView) findViewById(R.id.pythonVersions_scrollContainer);
        final EditText searchInput = (EditText) findViewById(R.id.search_input);
        Spinner orderSelector = (Spinner) findViewById(R.id.order_selector);
        refreshButton = (ImageButton) findViewById(R.id.refresh_button);

        pythonVersionListAdapter = new PythonVersionListAdapter(this);
        pythonVersionListAdapter.setActionHandler(new PythonVersionListAdapter.ActionHandler() {
            @Override
            public void onAction(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler) {
                executeActionInService(dependency, progressHandler);
            }
        });
        pythonVersionsContainer.setAdapter(pythonVersionListAdapter);
        TextView emptyTextView = new TextView(getApplicationContext());
        emptyTextView.setText(R.string.downloadManager_no_matches);
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
                pythonVersionListAdapter.setFilter(searchInput.getText().toString());
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
        if (!downloadServiceConnection.isBound) {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (PythonDownloadCenterService.class.getName().equals(service.service.getClassName())) {
                    bindService(new Intent(this, PythonDownloadCenterService.class), downloadServiceConnection, 0);
                }
            }
        }
        // TODO: Update the version views from the action queue
        downloadServiceConnection.showProgressAsNotification(false);
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

    private void handleVersionListUpdateFinished(boolean success) {
        isUpdateRunning = false;
        if (!success) {
            if (false) {
                // TODO: Check for connection and display dialog
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.downloadManager_update_failed,
                                       Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        // TODO: Stop rotation of the refresh button
        pythonVersionListAdapter.checkInstalledVersions();
    }

    public void updatePythonVersions() {
        if (this.isUpdateRunning) {
            return;
        }
        this.isUpdateRunning = true;
        // TODO: Make the refresh button rotate
        new Thread(new Runnable() {
            @Override
            public void run() {
                String indexString;
                // Update installed libraries
                pythonVersionListAdapter.updateInstalledLibraries();
                // Update remote versions
                URLConnection connection = Util.connectToUrl(getDownloadServerUrl() + "/" + INDEX_PATH, 10000);
                if (connection == null) {
                    handleVersionListUpdateFinished(false);
                    return;
                }
                try {
                    InputStream input = connection.getInputStream();
                    indexString = Util.convertStreamToString(input);
                    input.close();
                } catch (IOException e) {
                    Log.e(MainActivity.TAG, "Failed to download the version data", e);
                    handleVersionListUpdateFinished(false);
                    return;
                }
                JSONObject data;
                try {
                    data = new JSONObject(indexString);
                } catch (JSONException e) {
                    Log.e(MainActivity.TAG, "Parsing the version data failed", e);
                    handleVersionListUpdateFinished(false);
                    return;
                }
                handleVersionListUpdateFinished(pythonVersionListAdapter.parseJSONData(data, getDownloadServerUrl()));
            }
        }).start();
    }

    private void executeActionInService(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler) {
        // TODO: Display licences?
        final Intent serviceIntent = new Intent(this, PythonDownloadCenterService.class);
        startService(serviceIntent);
        bindService(serviceIntent, downloadServiceConnection, 0);
        downloadServiceConnection.addPendingActionToQueue(dependency, progressHandler);
    }
}
