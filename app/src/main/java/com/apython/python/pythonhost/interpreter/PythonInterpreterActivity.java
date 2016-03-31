package com.apython.python.pythonhost.interpreter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.Toast;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.PythonSettingsActivity;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;
import com.apython.python.pythonhost.views.interfaces.TerminalWindowManagerInterface;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;

import java.util.ArrayList;

/*
 * This Activity starts and displays a Python interpreter.
 *
 * Created by Sebastian on 08.06.2015.
 */

public class PythonInterpreterActivity extends FragmentActivity {

    private PythonInterpreterRunnable      interpreter;
    private TerminalInterface              terminalView;
    private TerminalWindowManagerInterface terminalWindowManager;
    private String enqueuedOutput = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pyVersion = getIntent().getStringExtra("pythonVersion");
        if (pyVersion == null) {
            pyVersion = PreferenceManager.getDefaultSharedPreferences(this).getString(
                    PythonSettingsActivity.KEY_PYTHON_VERSION,
                    PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED
            );
        }
        if (!PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED.equals(pyVersion) && PackageManager.isPythonVersionInstalled(this, pyVersion)) {
            this.startInterpreter(Util.getMainVersionPart(pyVersion));
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final ArrayList<String> versions = PackageManager.getInstalledPythonVersions(getApplicationContext());
            if (versions.size() <= 1) {
                if (versions.size() == 1) {
                    startInterpreter(versions.get(0));
                    return;
                }
                Log.i(MainActivity.TAG, "No Python version installed. Please download a version to use the interpreter.");
                Toast.makeText(
                        PythonInterpreterActivity.this,
                        "No Python version installed. Please download a version to use the interpreter.",
                        Toast.LENGTH_SHORT
                ).show(); // TODO: Open the download activity?
                finish();
                return;
            }
            String[] items = new String[versions.size()];
            for (int i = 0; i < versions.size(); i++) {
                items[i] = "Python " + versions.get(i);
            }
            builder.setSingleChoiceItems(items, 0, null);
            builder.setNegativeButton("Just once", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ListView listView = ((AlertDialog) dialog).getListView();
                    startInterpreter(versions.get(listView.getCheckedItemPosition()));
                }
            });
            builder.setPositiveButton("Set as default", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ListView listView = ((AlertDialog) dialog).getListView();
                    String version = versions.get(listView.getCheckedItemPosition());
                    PreferenceManager.getDefaultSharedPreferences(PythonInterpreterActivity.this)
                            .edit().putString(PythonSettingsActivity.KEY_PYTHON_VERSION, version).commit();
                    startInterpreter(version);
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.setTitle("Choose a Python version");
            builder.show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void startInterpreter(String pythonVersion) {
        this.setContentView(R.layout.activity_python_interpreter);
        this.interpreter = new PythonInterpreterRunnable(this, pythonVersion, new PythonInterpreter.IOHandler() {
            @Override
            public void addOutput(String text) {
                if (terminalView == null) {
                    enqueuedOutput += text;
                } else {
                    terminalView.addOutput(text);
                }
            }

            @Override
            public void setupInput(String prompt) {
                String enqueuedInput = PythonInterpreterActivity.this.interpreter.getEnqueueInput();
                if (enqueuedInput == null) {
                    // TODO: Handle such a situation?
                    enqueuedInput = "";
                }
                terminalView.enableInput(prompt, enqueuedInput);
            }
        }, this);

        // Start the interpreter thread
        new Thread(this.interpreter).start();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof TerminalInterface) {
            terminalView = (TerminalInterface) fragment;
            terminalView.registerInputHandler(interpreter);
            if (!enqueuedOutput.equals("")) {
                terminalView.addOutput(enqueuedOutput);
            }
        } else if (fragment instanceof TerminalWindowManagerInterface) {
            terminalWindowManager = (TerminalWindowManagerInterface) fragment;
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        Fragment currentWindow = terminalWindowManager.getCurrentWindow();
        if (currentWindow instanceof TerminalInterface) {
            if (!terminalView.isInputEnabled()) {
                // input via stdin pipe
                if (interpreter.dispatchKeyEvent(event)) {
                    return true;
                }
            }
        } else if (currentWindow instanceof SDLWindowInterface) {
            if (((SDLWindowInterface) currentWindow).dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
