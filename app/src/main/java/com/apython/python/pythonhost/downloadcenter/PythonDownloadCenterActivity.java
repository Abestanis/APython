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
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.PythonSettingsActivity;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.downloadcenter.items.Dependency;

import java.util.ArrayList;

/**
 * An activity which allows the user to download and delete python versions and modules.
 *
 * Created by Sebastian on 09.08.2015.
 */

public class PythonDownloadCenterActivity extends Activity {

    private DownloadServer downloadServer;
    private PythonVersionListAdapter pythonVersionListAdapter;
    private volatile boolean isUpdateRunning = false;
    SwipeRefreshLayout refreshLayout;

    class ServiceActionRunnable {
        private final Dependency                              dependency;
        private final ProgressHandler.TwoLevelProgressHandler progressHandler;

        ServiceActionRunnable(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler) {
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

        boolean applyAction(ProgressHandler.TwoLevelProgressHandler progressHandler) {
            return dependency.applyAction(progressHandler);
        }
    }

    private PythonDownloadServiceConnection downloadServiceConnection = new PythonDownloadServiceConnection();
    private class PythonDownloadServiceConnection implements ServiceConnection {
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

        void showProgressAsNotification(boolean showNotification) {
            displayNotification = showNotification;
            if (downloadService != null) {
                downloadService.showProgressAsNotification(showNotification);
            }
        }

        void addPendingActionToQueue(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler) {
            ServiceActionRunnable action = new ServiceActionRunnable(dependency, progressHandler);
            if (downloadService == null) {
                actionQueue.add(action);
            } else {
                downloadService.enqueueAction(action);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_download_center);
        
        try {
            this.downloadServer = new DownloadServer(this, getDownloadServerUrl());
        } catch (IllegalArgumentException error) {
            throw error; // TODO: Handle
        }

        final ListView pythonVersionsContainer = (ListView) findViewById(R.id.pythonVersions_scrollContainer);
        final EditText searchInput = (EditText) findViewById(R.id.search_input);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);

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
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updatePythonVersions();
            }
        });
        
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
        refreshLayout.setRefreshing(true);
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

    private void handleVersionListUpdateFinished(final boolean success) {
        pythonVersionListAdapter.checkInstalledVersions();
        isUpdateRunning = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
                if (!success) {
                    Log.w(MainActivity.TAG, "Failed to update the download versions");
                    if (false) {
                        // TODO: Check for connection and display dialog
                    } else {
                        Toast.makeText(getApplicationContext(),
                                       R.string.downloadManager_update_failed, Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        });
    }

    public void updatePythonVersions() {
        if (this.isUpdateRunning) {
            return;
        }
        this.isUpdateRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                // Update installed libraries
                pythonVersionListAdapter.updateInstalledLibraries();
                DownloadServer.ServerDownloads downloads = downloadServer.getDownloads();
                if (downloads != null) {
                    success = pythonVersionListAdapter.onUpdatedDownloads(downloads);
                }
                handleVersionListUpdateFinished(success);
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
