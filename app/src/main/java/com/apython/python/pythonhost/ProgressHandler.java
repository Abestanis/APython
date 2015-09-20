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

    class Factory {
        static class ProgressHandlerStub implements ProgressHandler {
            protected boolean enabled         = false;
            private   Activity activity       = null;
            private   TextView output         = null;
            private   ProgressBar progressBar = null;
            private   Runnable onEnable       = null;
            private   Runnable onSuccess      = null;
            private   Runnable onFailure      = null;

            ProgressHandlerStub(Activity activity, TextView output, ProgressBar progressBar,
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
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (output != null) {
                            output.setText(text);
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
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (output != null) {
                            output.setText(text);
                        }
                    }
                });
            }

            @Override
            public void setProgress(final float progress) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progress < 0) {
                            progressBar.setIndeterminate(true);
                        } else {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress((int) (progress * 100));
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
            }
        }

        static class TwoLevelProgressHandler extends ProgressHandlerStub {

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
            public void setProgress(float progress) {
                super.setProgress(progress);
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
        }

        public static ProgressHandler create(Activity activity, TextView output,
                                             ProgressBar progressBar, Runnable onEnable,
                                             Runnable onComplete) {
            return create(activity, output, progressBar, onEnable, onComplete, onComplete);
        }

        public static ProgressHandler create(Activity activity, TextView output,
                                             ProgressBar progressBar, Runnable onEnable,
                                             Runnable onSuccess, Runnable onFailure) {
            return new ProgressHandlerStub(activity, output, progressBar, onEnable, onSuccess, onFailure);
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
    void onComplete(boolean success);
}
