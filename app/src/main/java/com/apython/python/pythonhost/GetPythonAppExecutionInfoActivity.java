package com.apython.python.pythonhost;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/*
 * This activity can be launched via an intent with the action
 * "com.python.pythonhost.PYTHON_APP_GET_EXECUTION_INFO".
 * It is used by the Python apps to execute their Python code.
 *
 * Created by Sebastian on 27.05.2015.
 */

public class GetPythonAppExecutionInfoActivity extends Activity {

    // The communication manager which handles the communication with the Python apps.
    private PythonAppCommunicationManager communicationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.communicationManager = new PythonAppCommunicationManager(this);
        if (this.communicationManager.parseArgs(this.getIntent())) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(GetPythonAppExecutionInfoActivity.this);
            final LinearLayout progressContainer = new LinearLayout(this.getApplicationContext());
            final TextView progressTextView = new TextView(this.getApplicationContext());
            final ProgressBar progressView = new ProgressBar(this.getApplicationContext(), null, android.R.attr.progressBarStyleHorizontal);

            dialogBuilder.setNegativeButton(
                    getText(R.string.run_in_background), (dialog, which) -> dialog.dismiss());

            progressContainer.setPadding(20, 20, 20, 20);
            progressContainer.setOrientation(LinearLayout.VERTICAL);
            progressContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            progressTextView.setSingleLine();
            progressTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            progressTextView.setTextColor(Color.WHITE);
            progressTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            progressContainer.addView(progressTextView);
            progressContainer.addView(progressView);

            dialogBuilder.setView(progressContainer);

            final AlertDialog dialog = dialogBuilder.create();
            dialog.setCancelable(false);

            final ProgressHandler progressHandler = ProgressHandler.Factory.create(
                    this, progressTextView, progressView, () -> {
                dialog.show();
                WindowManager.LayoutParams windowLayoutParams = new WindowManager.LayoutParams();
                Window window = dialog.getWindow();
                if (window != null) {
                    windowLayoutParams.copyFrom(window.getAttributes());
                    windowLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                    window.setAttributes(windowLayoutParams);
                }
            }, null);
            new Thread(() -> {
                communicationManager.startPythonApp(progressHandler);
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                finish();
            }).start();
        } else {
            finish();
        }
    }
}
