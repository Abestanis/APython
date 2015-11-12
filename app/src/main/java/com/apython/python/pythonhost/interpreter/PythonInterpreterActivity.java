package com.apython.python.pythonhost.interpreter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.PythonSettingsActivity;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;

import java.util.ArrayList;

/*
 * This Activity starts and displays a Python interpreter.
 *
 * Created by Sebastian on 08.06.2015.
 */

public class PythonInterpreterActivity extends Activity {

    TextView      pythonOutput;
    TerminalInput pythonInput;
    ScrollView    scrollContainer;
    PythonInterpreterRunnable interpreter;

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
        this.pythonOutput    = (TextView)      findViewById(R.id.pythonOutput);
        this.pythonInput     = (TerminalInput) findViewById(R.id.pythonInput);
        this.scrollContainer = (ScrollView)    findViewById(R.id.scrollContainer);
        this.pythonInput.setCommitHandler(new TerminalInput.onCommitHandler() {
            @Override
            public void onCommit(TerminalInput terminalInput) {
                String[] input = pythonInput.popCurrentInput();
                pythonInput.disableInput();
                int splitIndex = input[1].indexOf('\n') + 1;
                pythonOutput.append(input[0] + input[1].substring(0, splitIndex));
                interpreter.notifyInput(input[1].substring(0, splitIndex));
                // Anything after the first newline must be send to stdin
                for (char character : input[1].substring(splitIndex).toCharArray()) {
                    interpreter.dispatchKey(character);
                }
            }
        });

        this.interpreter = new PythonInterpreterRunnable(this, pythonVersion, new PythonInterpreter.IOHandler() {
            @Override
            public void addOutput(String text) {
                scrollContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollContainer.fullScroll(View.FOCUS_DOWN);
                    }
                }, 100);
                int specialCharacterIndex = text.indexOf(String.valueOf('\r'));
                if (specialCharacterIndex == -1) {
                    pythonOutput.append(text);
                    return;
                }
                String output = pythonOutput.getText().toString();
                int lastLineStart = output.lastIndexOf("\n");
                String line = output.substring(lastLineStart + 1) + text.substring(0, specialCharacterIndex);
                output = output.substring(0, lastLineStart + 1);
                text = text.substring(specialCharacterIndex);
                while (specialCharacterIndex != -1) {
                    char specialCharacter = text.charAt(0);
                    text = text.substring(1);
                    specialCharacterIndex = text.indexOf(String.valueOf('\r'));
                    switch (specialCharacter) {
                    case '\r':
                        String str = text.substring(0, specialCharacterIndex == -1 ? text.length() : specialCharacterIndex);
                        text = text.substring(str.length());
                        line = str + line.substring(Math.min(str.length(), line.length()));
                        break;
                    default:
                        break;
                    }
                }
                output += line;
                pythonOutput.setText(output);
            }

            @Override
            public void setupInput(String prompt) {
                String enqueuedInput = interpreter.getEnqueueInputTillNewLine();
                if (enqueuedInput == null) {
                    // TODO: Handle such a situation?
                    enqueuedInput = "";
                }
                pythonInput.enableInput(prompt, enqueuedInput);
            }
        }, this);

        // Make the keyboard always visible
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Start the interpreter thread
        new Thread(this.interpreter).start();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!this.pythonInput.isEnabled() && this.interpreter != null) {
            // input via stdin pipe
            return this.interpreter.dispatchKeyEvent(event);
        } else {
            // Normal input (with delete, copy, paste etc.)
            return super.dispatchKeyEvent(event);
        }
    }
}
