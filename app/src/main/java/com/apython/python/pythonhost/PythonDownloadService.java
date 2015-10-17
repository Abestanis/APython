package com.apython.python.pythonhost;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service that downloads Python versions.
 *
 * Created by Sebastian on 13.09.2015.
 */

public class PythonDownloadService extends IntentService {

    private static final String TAG = PythonDownloadService.class.getSimpleName();

    private static final int WARNING_NOTIFICATION_ID = 0;
    private static final int NOTIFICATION_ID         = 1;

    private static final int PYTHON_LIBRARY_INDEX    = 0;
    private static final int PYTHON_MODULE_ZIP_INDEX = 1;

    private final IBinder binder = new PythonDownloadServiceBinder();

    private boolean displayNotification = false;
    NotificationManager notificationManager;

    private Map<String, ProgressHandler> progressHandlerList = new ConcurrentHashMap<>();

    private ProgressHandlerProxy currentProxy = null;
    private String currentPythonVersion = null;

    class PythonDownloadServiceBinder extends Binder {
        PythonDownloadService getService() {
            return PythonDownloadService.this;
        }
    }

    class ProgressHandlerProxy implements ProgressHandler {

        ProgressHandler progressHandler;
        String pythonVersion;
        int downloadSteps = 0;
        String text          = null;
        String progressText  = null;
        float  totalProgress = -1.0f;
        float  secProgress   = -1.0f;

        public ProgressHandlerProxy(ProgressHandler progressHandler, String pythonVersion, int downloadSteps) {
            this.progressHandler = progressHandler;
            this.pythonVersion = pythonVersion;
            this.downloadSteps = downloadSteps;
        }

        @Override
        public void enable(String text) {
            this.text = text;
            if (progressHandler != null && !showingNotification()) {
                progressHandler.enable(text);
            } else {
                displayOrUpdateNotification();
            }
        }

        @Override
        public void setText(String text) {
            this.text = text;
            if (progressHandler != null && !showingNotification()) {
                progressHandler.setText(text);
            } else {
                displayOrUpdateNotification();
            }
        }

        @Override
        public void setProgress(float progress) {
            saveProgress(progress, null);
            if (progressHandler != null && !showingNotification()) {
                progressHandler.setProgress(progress);
            } else {
                displayOrUpdateNotification();
            }
        }

        @Override
        public void setProgress(float progress, int bytesPerSecond, int remainingSeconds) {
            saveProgress(progress, Util.generateDownloadInfoText(PythonDownloadService.this, bytesPerSecond, remainingSeconds));
            if (progressHandler != null && !showingNotification()) {
                progressHandler.setProgress(progress, bytesPerSecond, remainingSeconds);
            } else {
                displayOrUpdateNotification();
            }
        }

        private void saveProgress(float progress, String text) {
            if (progress >= 0) {
                if (totalProgress < 0) { totalProgress = 0.0f; secProgress = 0.0f; }
                if (progress >= secProgress) {
                    totalProgress += progress - secProgress;
                } else {
                    totalProgress = Math.round(totalProgress) + progress;
                }
                secProgress = progress;
            }
            if (progressText != null || progress < 0) {
                this.progressText = text;
            }
        }

        @Override
        public void onComplete(boolean success) {
            if (progressHandler != null && !showingNotification()) {
                progressHandler.onComplete(success);
                notificationManager.cancel(NOTIFICATION_ID);
            } else {
                if (success) {
                    stopForeground(true);
                    notificationManager.cancel(NOTIFICATION_ID);
                } else {
                    displayWarningNotification();
                }
            }
        }

        public void setProgressHandler(Factory.TwoLevelProgressHandler progressHandler) {
            if (text != null) {
                progressHandler.enable(text);
                progressHandler.setTotalSteps(downloadSteps);
                for (int i = 0; i < Math.round(totalProgress); i++) {
                    progressHandler.setProgress(1.0f);
                    progressHandler.setProgress(-1.0f);
                }
                progressHandler.setProgress(secProgress, progressText);
            }
            this.progressHandler = progressHandler;
        }

        public Notification buildNotification() {
            Context context = PythonDownloadService.this;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            notificationBuilder.setSmallIcon(R.drawable.python_grey_icon);
            notificationBuilder.setWhen(System.currentTimeMillis());
            notificationBuilder.setContentTitle("Downloading Python Version " + pythonVersion);
            notificationBuilder.setContentText(text + (progressText != null ? "\n" + progressText : ""));
            notificationBuilder.setOngoing(true);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, PythonDownloadCenterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_STATUS);
            notificationBuilder.setProgress(downloadSteps * 100, (int) (totalProgress * 100), totalProgress == -1.0f);
            return notificationBuilder.build();
        }

        private void displayOrUpdateNotification() {
            notificationManager.notify(NOTIFICATION_ID, buildNotification());
        }

        private void displayWarningNotification() {
            Context context = PythonDownloadService.this;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            notificationBuilder.setSmallIcon(R.drawable.python_grey_icon); // TODO: Warning icon
            notificationBuilder.setWhen(System.currentTimeMillis());
            notificationBuilder.setContentTitle("Download failed!");
            notificationBuilder.setContentText("Downloading Python Version " + pythonVersion + " failed!");
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, PythonDownloadCenterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_ERROR);
            notificationManager.notify(WARNING_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    public PythonDownloadService() {
        super(PythonDownloadService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean interrupted = false;
        String pythonVersion = intent.getStringExtra("version");
        String serverUrl = intent.getStringExtra("serverUrl");
        String[] urls = intent.getStringArrayExtra("downloadUrls");
        String[] md5Hashes = intent.getStringArrayExtra("md5Hashes");
        if (pythonVersion == null || serverUrl == null
                || urls == null || md5Hashes == null) {
            if (pythonVersion == null) {
                Log.e(TAG, "Failed to download Python version: Python version to download not given!");
            }
            if (serverUrl == null) {
                Log.e(TAG, "Failed to download Python version " + pythonVersion + ": Url of the download server not given!");
            }
            if (urls == null) {
                Log.e(TAG, "Failed to download Python version " + pythonVersion + ": Urls to download not given!");
            }
            if (md5Hashes == null) {
                Log.e(TAG, "Failed to download Python version " + pythonVersion + ": Md5 hashes not given!");
            }
            return;
        }
        currentPythonVersion = pythonVersion;
        ProgressHandler progressHandler = progressHandlerList.get(Util.getMainVersionPart(pythonVersion));
        if (intent.getBooleanExtra("waitForProgressHandler", false) && progressHandler == null) {
            for (int i = 0; i < 10; i++) {
                progressHandler = progressHandlerList.get(Util.getMainVersionPart(pythonVersion));
                if (progressHandler != null) { break; }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Waiting for progressHandler was interrupted");
                    interrupted = true;
                    break;
                }
            }
        }
        if (progressHandler == null) {
            displayNotification = true;
        }
        currentProxy = new ProgressHandlerProxy(progressHandler, pythonVersion, urls.length);
        boolean success = !interrupted && download(intent.getStringExtra("version"), // TODO: Check
                                                   intent.getStringExtra("serverUrl"),
                                                   urls,
                                                   intent.getStringArrayExtra("md5Hashes"),
                                                   intent.getIntExtra("numRequirements", 0),
                                                   intent.getIntExtra("numDependencies", 0),
                                                   intent.getIntExtra("numModules", 0),
                                                   currentProxy);
        currentProxy.onComplete(success);
        progressHandlerList.remove(Util.getMainVersionPart(pythonVersion));
        currentPythonVersion = null;
        currentProxy = null;
    }

    private boolean download(String version, String serverUrl,
                             String[] downloadUrls, String[] md5Hashes,
                             int numRequirements, int numDependencies, int numModules,
                             ProgressHandler progressHandler) {
        Log.d(MainActivity.TAG, "Downloading Python " + version);
        if (progressHandler != null) {
            progressHandler.enable("Downloading Python " + version);
        }
        File libraryFile = new File(PackageManager.getDynamicLibraryPath(getApplicationContext()), System.mapLibraryName("python" + Util.getMainVersionPart(version)));
        File moduleZipFile = new File(PackageManager.getStandardLibPath(getApplicationContext()), "python" + Util.getMainVersionPart(version).replace(".", "") + ".zip");
        if (progressHandler != null) {
            progressHandler.setText("Downloading Python library " + libraryFile.getName() + "...");
        }
        boolean success = Util.downloadFile(serverUrl + "/" + downloadUrls[PYTHON_LIBRARY_INDEX], libraryFile, md5Hashes[PYTHON_LIBRARY_INDEX], progressHandler);
        if (success && progressHandler != null) {
            progressHandler.setText("Downloading Python modules...");
        }
        success = success && Util.downloadFile(serverUrl + "/" + downloadUrls[PYTHON_MODULE_ZIP_INDEX], moduleZipFile, md5Hashes[PYTHON_MODULE_ZIP_INDEX], progressHandler);
        if (!success) {
            Log.e(MainActivity.TAG, "Failed to download the Python library and module for version " + version + ".");
            if (libraryFile.exists()) {
                if (!libraryFile.delete()) {
                    Log.e(MainActivity.TAG, "Failed to clean up the Python library file at '" + libraryFile.getAbsolutePath() + "'!");
                }
            }
            return false;
        }
        for (int i = 0; i < numRequirements; i++) {
            String requirement = Util.getLibraryName(new File(downloadUrls[PYTHON_MODULE_ZIP_INDEX + 1 + i]));
            if (!PackageManager.isAdditionalLibraryInstalled(getApplicationContext(), requirement)) {
                if (progressHandler != null) {
                    progressHandler.setText("Downloading requirement " + requirement + "...");
                }
                if (!Util.downloadFile(serverUrl + "/" + downloadUrls[PYTHON_MODULE_ZIP_INDEX + 1 + i],
                                       new File(PackageManager.getDynamicLibraryPath(getApplicationContext()), System.mapLibraryName(requirement)),
                                       md5Hashes[PYTHON_MODULE_ZIP_INDEX + 1 + i],
                                       progressHandler)) {
                    Log.e(MainActivity.TAG, "Failed to download a required library for version " + version + ": " + requirement + "!");
                    if (!libraryFile.delete()) {
                        Log.e(MainActivity.TAG, "Failed to clean up the Python library file at '" + libraryFile.getAbsolutePath() + "'!");
                    }
                    if (!moduleZipFile.delete()) {
                        Log.e(MainActivity.TAG, "Failed to clean up the module zip file at '" + moduleZipFile.getAbsolutePath() + "'!");
                    }
                    return false;
                }
            } else if (progressHandler != null) {
                progressHandler.enable("Skipping requirement " + requirement);
                progressHandler.setProgress( 0.0f);
                progressHandler.setProgress( 1.0f);
                progressHandler.setProgress(-1.0f);
            }
        }

        for (int i = 0; i < numDependencies; i++) {
            String dependency = Util.getLibraryName(new File(downloadUrls[PYTHON_MODULE_ZIP_INDEX + numRequirements + 1 + i]));
            if (!PackageManager.isAdditionalLibraryInstalled(getApplicationContext(), dependency)) {
                if (progressHandler != null) {
                    progressHandler.setText("Downloading dependency " + dependency + "...");
                }
                if (!Util.downloadFile(serverUrl + "/" + downloadUrls[PYTHON_MODULE_ZIP_INDEX + numRequirements + 1 + i],
                                       new File(PackageManager.getDynamicLibraryPath(getApplicationContext()), System.mapLibraryName(dependency)),
                                       md5Hashes[PYTHON_MODULE_ZIP_INDEX + numRequirements + 1 + i],
                                       progressHandler)) {
                    Log.e(MainActivity.TAG, "Failed to download dependency '" + dependency + "'!");
                    return false;
                }
            } else if (progressHandler != null) {
                progressHandler.enable("Skipping dependency " + dependency);
                progressHandler.setProgress(0.0f);
                progressHandler.setProgress(1.0f);
                progressHandler.setProgress(-1.0f);
            }
        }
        for (int i = 0; i < numModules; i++) {
            String downloadUrl = downloadUrls[PYTHON_MODULE_ZIP_INDEX + numRequirements + numDependencies + 1 + i];
            String moduleFileName = new File(downloadUrl).getName();
            String moduleName = moduleFileName.substring(0, moduleFileName.lastIndexOf('.'));
            if (progressHandler != null) {
                progressHandler.setText("Downloading module " + moduleName + "...");
            }
            if (!Util.downloadFile(serverUrl + "/" + downloadUrl,
                                   new File(PackageManager.getLibDynLoad(getApplicationContext(), Util.getMainVersionPart(version)), moduleFileName),
                                   md5Hashes[PYTHON_MODULE_ZIP_INDEX + numRequirements + numDependencies + 1 + i],
                                   progressHandler)) {
                Log.w(MainActivity.TAG, "Failed to install module '" + moduleName + "'.");
            }
        }
        Util.makeFileAccessible(getApplicationContext().getFilesDir(), true);
        return true;
    }

    public void registerProgressHandler(String version, ProgressHandler progressHandler) {
        ProgressHandler previousProgressHandler = progressHandlerList.put(version, progressHandler);
        if (previousProgressHandler != progressHandler &&
                currentPythonVersion != null &&
                version.equals(Util.getMainVersionPart(currentPythonVersion))
                && currentProxy != null) {
            currentProxy.setProgressHandler((ProgressHandler.Factory.TwoLevelProgressHandler) progressHandler);
        }
    }

    public Map<String, ProgressHandler> getProgressHandlerList() {
        return progressHandlerList;
    }

    public synchronized boolean showingNotification() {
        return displayNotification;
    }

    public void showProgressAsNotification(boolean showAsNotification) {
        if (displayNotification == showAsNotification) { return; }
        displayNotification = showAsNotification;
        if (!showAsNotification) {
            stopForeground(true);
            notificationManager.cancel(NOTIFICATION_ID);
            if (currentPythonVersion != null && currentProxy != null) {
                ProgressHandler progressHandler = progressHandlerList.get(Util.getMainVersionPart(currentPythonVersion));
                if (progressHandler != null) {
                    currentProxy.setProgressHandler((ProgressHandler.Factory.TwoLevelProgressHandler) progressHandler);
                }
            }
        } else if (currentProxy != null) {
            startForeground(NOTIFICATION_ID, currentProxy.buildNotification());
        }
    }
}
