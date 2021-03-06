package com.apython.python.pythonhost;

import android.app.Activity;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * An interface to show some process on the UI.
 *
 * Created by Sebastian on 09.08.2015.
 */
public interface ProgressHandler {

    interface TwoLevelProgressHandler extends ProgressHandler {
        void setTotalSteps(int totalSteps);
    }
    class Factory {

        static class SimpleProgressHandler implements ProgressHandler {
            protected boolean enabled = false;
            protected String text     = null;
            String progressText       = null;
            protected Activity activity;
            private   TextView output;
            private   ProgressBar progressBar;
            private   Runnable onEnable;
            private   Runnable onSuccess;
            private   Runnable onFailure;
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
                activity.runOnUiThread(() -> {
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
                });
            }

            @Override
            public void setText(final String text) {
                this.text = text;
                activity.runOnUiThread(() -> {
                    if (output != null) {
                        output.setText(text + (progressText != null ? "\n" + progressText : ""));
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
                activity.runOnUiThread(() -> {
                    if (progress < 0) {
                        progressBar.setIndeterminate(true);
                    } else {
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress((int) (progress * 100));
                    }
                    if (output != null) {
                        output.setText(text + (progressText != null ? "\n" + progressText : ""));
                    }
                });
            }

            @Override
            public void onComplete(final boolean success) {
                if ((success && onSuccess != null) || (!success && onFailure != null)) {
                    activity.runOnUiThread(() -> {
                        if (success) {
                            onSuccess.run();
                        } else {
                            onFailure.run();
                        }
                    });
                }
                enabled = false;
            }

        }
        public static class SimpleTwoLevelProgressHandler extends SimpleProgressHandler
                implements TwoLevelProgressHandler {

            private float lastProgress  = 0.0f;
            private float totalProgress = 0.0f;
            private ProgressBar totalProgressBar;
            private int totalSteps = 1;
            SimpleTwoLevelProgressHandler(Activity activity, TextView output,
                                          ProgressBar totalProgressBar,
                                          ProgressBar secondaryProgressBar, Runnable onEnable,
                                          Runnable onSuccess, Runnable onFailure) {
                super(activity, output, secondaryProgressBar, onEnable, onSuccess, onFailure);
                this.totalProgressBar = totalProgressBar;
            }

            @Override
            public void setTotalSteps(final int totalSteps) {
                this.totalSteps = totalSteps;
                activity.runOnUiThread(() -> totalProgressBar.setMax(100 * totalSteps));
            }

            @Override
            public void enable(String text) {
                if (!enabled) {
                    activity.runOnUiThread(() -> {
                        totalProgressBar.setIndeterminate(true);
                        totalProgressBar.setMax(100 * totalSteps);
                    });
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
                    activity.runOnUiThread(() -> {
                        totalProgressBar.setIndeterminate(false);
                        totalProgressBar.setProgress((int) (totalProgress * 100));
                    });
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
            return new SimpleProgressHandler(activity, output, progressBar, onEnable,
                                             onSuccess, onFailure);
        }

        public static TwoLevelProgressHandler createTwoLevel(Activity activity, TextView output,
                                                             ProgressBar totalProgressBar,
                                                             ProgressBar progressBar,
                                                             Runnable onEnable, Runnable onSuccess,
                                                             Runnable onFailure) {
            return new SimpleTwoLevelProgressHandler(activity, output, totalProgressBar, progressBar,
                                               onEnable, onSuccess, onFailure);
        }

    }
    
    void enable(String text);
    void setText(String text);
    void setProgress(float progress);
    void setProgress(float progress, int bytesPerSecond, int remainingSeconds);
    void onComplete(boolean success);
}
