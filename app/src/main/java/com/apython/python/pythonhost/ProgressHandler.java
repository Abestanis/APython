package com.apython.python.pythonhost;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * An interface to show some process on the UI.
 *
 * Created by Sebastian on 09.08.2015.
 */
public interface ProgressHandler {

    class Factory {
        static class SimpleProgressHandler implements ProgressHandler {
            protected boolean enabled         = false;
            protected String text             = null;
            protected String progressText     = null;
            private   Activity activity       = null;
            private   TextView output         = null;
            private   ProgressBar progressBar = null;
            private   Runnable onEnable       = null;
            private   Runnable onSuccess      = null;
            private   Runnable onFailure      = null;

            SimpleProgressHandler(Activity activity, TextView output, ProgressBar progressBar,
                                  Runnable onEnable, Runnable onSuccess, Runnable onFailure) {
                this.activity = activity;
                this.output = output;
                this.progressBar = progressBar;
                this.onEnable = onEnable;
                this.onSuccess = onSuccess;
                this.onFailure = onFailure;
            }

            @Override
            public void enable(final String text) {
                this.text = text;
                activity.runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        if (output != null) {
                            output.setText(text + (progressText != null ? "\n" + progressText : ""));
                        }
                        if (enabled) {
                            return;
                        }
                        enabled = true;
                        if (onEnable != null) {
                            onEnable.run();
                        }
                        progressBar.setIndeterminate(true);
                        progressBar.setMax(100);
                    }
                });
            }

            @Override
            public void setText(final String text) {
                this.text = text;
                activity.runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        if (output != null) {
                            output.setText(text + (progressText != null ? "\n" + progressText : ""));
                        }
                    }
                });
            }

            @Override
            public void setProgress(float progress) {
                setProgress(progress, null);
            }

            @Override
            public void setProgress(float progress, int bytesPerSecond, int remainingSeconds) {
                setProgress(progress, Util.generateDownloadInfoText(activity, bytesPerSecond, remainingSeconds));
            }

            protected void setProgress(final float progress, String progressTextString) {
                if (progressTextString != null || progress < 0) {
                    this.progressText = progressTextString;
                }
                activity.runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        if (progress < 0) {
                            progressBar.setIndeterminate(true);
                        } else {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress((int) (progress * 100));
                        }
                        if (output != null) {
                            output.setText(text + (progressText != null ? "\n" + progressText : ""));
                        }
                    }
                });
            }

            @Override
            public void onComplete(final boolean success) {
                if ((success && onSuccess != null) || (!success && onFailure != null)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                onSuccess.run();
                            } else {
                                onFailure.run();
                            }
                        }
                    });
                }
                enabled = false;
            }
        }

        static class TwoLevelProgressHandler extends SimpleProgressHandler {

            private float lastProgress  = 0.0f;
            private float totalProgress = 0.0f;
            private ProgressBar totalProgressBar;
            private int totalSteps = 0;

            TwoLevelProgressHandler(Activity activity, TextView output,
                                    ProgressBar totalProgressBar, ProgressBar secondaryProgressBar,
                                    int totalSteps , Runnable onEnable,
                                    Runnable onSuccess, Runnable onFailure) {
                super(activity, output, secondaryProgressBar, onEnable, onSuccess, onFailure);
                this.totalProgressBar = totalProgressBar;
                this.totalSteps = totalSteps;
            }

            public void setTotalSteps(int totalSteps) {
                this.totalSteps = totalSteps;
                totalProgressBar.setMax(100 * totalSteps);
            }

            @Override
            public void enable(String text) {
                if (!enabled) {
                    totalProgressBar.setIndeterminate(true);
                    totalProgressBar.setMax(100 * totalSteps);
                }
                super.enable(text);
            }

            @Override
            public void setProgress(float progress, String text) {
                super.setProgress(progress, text);
                if (progress >= 0) {
                    if (progress >= lastProgress) {
                        totalProgress += progress - lastProgress;
                    } else {
                        totalProgress = Math.round(totalProgress) + progress;
                    }
                    totalProgressBar.setIndeterminate(false);
                    totalProgressBar.setProgress((int) (totalProgress * 100));
                    lastProgress = progress;
                }
            }

            @Override
            public void onComplete(boolean success) {
                super.onComplete(success);
                lastProgress = 0.0f;
                totalProgress = 0.0f;
            }
        }

        public static ProgressHandler create(Activity activity, TextView output,
                                             ProgressBar progressBar, Runnable onEnable,
                                             Runnable onComplete) {
            return create(activity, output, progressBar, onEnable, onComplete, onComplete);
        }

        public static ProgressHandler create(Activity activity, TextView output,
                                             ProgressBar progressBar, Runnable onEnable,
                                             Runnable onSuccess, Runnable onFailure) {
            return new SimpleProgressHandler(activity, output, progressBar, onEnable, onSuccess, onFailure);
        }

        public static ProgressHandler createTwoLevel(Activity activity, TextView output,
                                                     ProgressBar totalProgressBar,
                                                     ProgressBar progressBar, int totalSteps,
                                                     Runnable onEnable, Runnable onSuccess, Runnable onFailure) {
            return new TwoLevelProgressHandler(activity, output, totalProgressBar, progressBar,
                                               totalSteps, onEnable, onSuccess, onFailure);
        }
    }


    void enable(String text);
    void setText(String text);
    void setProgress(float progress);
    void setProgress(float progress, int bytesPerSecond, int remainingSeconds);
    void onComplete(boolean success);
}
