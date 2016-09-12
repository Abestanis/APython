package com.apython.python.pythonhost.downloadcenter;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;

import java.util.ArrayList;

/**
 * A service that downloads Python versions.
 *
 * Created by Sebastian on 13.09.2015.
 */

public class PythonDownloadCenterService extends IntentService {

    private static final int WARNING_NOTIFICATION_ID = 0;
    private static final int NOTIFICATION_ID         = 1;

    private final IBinder binder = new PythonDownloadServiceBinder();

    private boolean displayNotification = false;
    private NotificationManager notificationManager;

    private ProgressHandlerProxy currentProxy = null;
    private final ArrayList<PythonDownloadCenterActivity.ServiceActionRunnable> actionQueue  = new ArrayList<>();

    public void enqueueAction(PythonDownloadCenterActivity.ServiceActionRunnable action) {
        synchronized (actionQueue) {
            actionQueue.add(action);
            actionQueue.notifyAll();
        }
    }

    public ArrayList<PythonDownloadCenterActivity.ServiceActionRunnable> getActionQueue() {
        return actionQueue;
    }

    class PythonDownloadServiceBinder extends Binder {
        PythonDownloadCenterService getService() {
            return PythonDownloadCenterService.this;
        }
    }

    class ProgressHandlerProxy implements ProgressHandler.TwoLevelProgressHandler {

        TwoLevelProgressHandler progressHandler;
        String text            = null;
        String progressText    = null;
        float  totalProgress   = -1.0f;
        float  secProgress     = -1.0f;
        private int totalSteps = 1;

        public ProgressHandlerProxy(TwoLevelProgressHandler progressHandler) {
            this.progressHandler = progressHandler;
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
            saveProgress(progress, Util.generateDownloadInfoText(PythonDownloadCenterService.this, bytesPerSecond, remainingSeconds));
            if (progressHandler != null && !showingNotification()) {
                progressHandler.setProgress(progress, bytesPerSecond, remainingSeconds);
            } else {
                displayOrUpdateNotification();
            }
        }
        
        private void saveProgress(float progress, String text) {
            if (progress >= 0) {
                if (totalProgress < 0) {
                    totalProgress = 0.0f;
                    secProgress = 0.0f;
                }
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

        @Override
        public void setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
            if (progressHandler != null) {
                progressHandler.setTotalSteps(totalSteps);
            }
        }
        
        public void setProgressHandler(ProgressHandler.TwoLevelProgressHandler progressHandler) {
            if (text != null) {
                progressHandler.enable(text);
                progressHandler.setTotalSteps(totalSteps);
                for (int i = 0; i < Math.round(totalProgress); i++) {
                    progressHandler.setProgress(1.0f);
                    progressHandler.setProgress(-1.0f);
                }
                progressHandler.setProgress(secProgress);
                progressHandler.setText(progressText);
            }
            this.progressHandler = progressHandler;
        }

        public Notification buildNotification() {
            Context context = PythonDownloadCenterService.this;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            notificationBuilder.setSmallIcon(R.drawable.python_grey_icon);
            notificationBuilder.setWhen(System.currentTimeMillis());
            notificationBuilder.setContentTitle(text);
            if (progressText != null){
                notificationBuilder.setContentText(progressText);
            }
            notificationBuilder.setOngoing(true);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(
                    context, 0, new Intent(context, PythonDownloadCenterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_STATUS);
            notificationBuilder.setProgress(totalSteps * 100, (int) (totalProgress * 100), totalProgress == -1.0f);
            return notificationBuilder.build();
        }

        private void displayOrUpdateNotification() {
            notificationManager.notify(NOTIFICATION_ID, buildNotification());
        }

        private void displayWarningNotification() {
            Context context = PythonDownloadCenterService.this;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            notificationBuilder.setSmallIcon(R.drawable.python_grey_icon); // TODO: Warning icon
            notificationBuilder.setWhen(System.currentTimeMillis());
            notificationBuilder.setContentTitle("Download failed!");
            notificationBuilder.setAutoCancel(true);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(
                    context, 0, new Intent(context, PythonDownloadCenterActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_ERROR);
            notificationManager.notify(WARNING_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    public PythonDownloadCenterService() {
        super(PythonDownloadCenterService.class.getSimpleName());
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
        synchronized (actionQueue) {
            if (actionQueue.size() == 0) {
                try {
                    actionQueue.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
        while (actionQueue.size() != 0) {
            PythonDownloadCenterActivity.ServiceActionRunnable currentAction = actionQueue.remove(actionQueue.size() - 1);
            ProgressHandler.TwoLevelProgressHandler progressHandler = new ProgressHandlerProxy(currentAction.getProgressHandler());
            progressHandler.setTotalSteps(currentAction.getDependency().getActionSteps(new ArrayList<String>()));
            boolean success = currentAction.applyAction(progressHandler);
            progressHandler.onComplete(success);
        }
        Util.makePathAccessible(PackageManager.getDynamicLibraryPath(getApplicationContext()), getFilesDir().getParentFile());
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
        } else if (currentProxy != null) {
            startForeground(NOTIFICATION_ID, currentProxy.buildNotification());
        }
    }
}
