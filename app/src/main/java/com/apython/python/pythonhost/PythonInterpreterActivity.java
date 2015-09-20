package com.apython.python.pythonhost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

/*
 * This Activity starts and displays a Python interpreter.
 *
 * Created by Sebastian on 08.06.2015.
 */

public class PythonInterpreterActivity extends Activity {

    /**
     * A boolean that is required in order to show the soft keyboard at any time.
     * If we would call {@code pythonInput.setEnabled(false)}, we would hide the soft keyboard every time.
     * Instead, we don't disable pythonInput, but we handle the input after it has occurred.
     */
    boolean    pythonInputEnabled = false;
    TextView   pythonOutput;
    EditText   pythonInput;
    ScrollView scrollContainer;
    PythonInterpreterRunnable interpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_python_interpreter);
        this.pythonOutput    = (TextView)   findViewById(R.id.pythonOutput);
        this.pythonInput     = (EditText)   findViewById(R.id.pythonInput);
        this.scrollContainer = (ScrollView) findViewById(R.id.scrollContainer);
        this.pythonInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (input.length() > 0) {
                    if (!pythonInputEnabled) {
                        interpreter.dispatchKey(input.charAt(input.length() - 1));
                        pythonInput.setText(input.substring(0, input.length() - 1));
                    } else if (input.charAt(input.length() - 1) == '\n') {
                        pythonOutput.append(input);
                        pythonInput.setText("");
                        pythonInput.setCursorVisible(false);
                        pythonInputEnabled = false;
                        interpreter.notifyInput(input);
                    }
                }
            }
        });

        if (getIntent().getStringExtra("pythonVersion") != null) {
            this.startInterpreter(Util.getMajorMinorVersionPart(getIntent().getStringExtra("pythonVersion")));
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final ArrayList<String> versions = PackageManager.getInstalledPythonVersions(getApplicationContext());
            if (versions.size() <= 1) {
                if (versions.size() == 1) {
                    startInterpreter(versions.get(0));
                    return;
                }
                Log.i(MainActivity.TAG, "No Python version installed. Please download a version to use the interpreter.");
                return;
            }
            String[] items = new String[versions.size()];
            for (int i = 0; i < versions.size(); i++) {
                items[i] = "Python " + versions.get(i);
            }
            builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int n) {
                    d.dismiss();
                    startInterpreter(versions.get(n));
                }

            });
            builder.setCancelable(false);
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ListView listView = ((AlertDialog) dialog).getListView();
                    startInterpreter(versions.get(listView.getCheckedItemPosition()));
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
                pythonInputEnabled = true;
                pythonInput.append(prompt);
                pythonInput.setSelection(prompt.length());
                pythonInput.setCursorVisible(true);
            }
        }, this);

        // Make the keyboard always visible
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Start the interpreter thread
        new Thread(this.interpreter).start();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!this.pythonInputEnabled && this.interpreter != null) {
            // input via stdin pipe
            return this.interpreter.dispatchKeyEvent(event);
        } else {
            // Normal input (with delete, copy, paste etc.)
            return super.dispatchKeyEvent(event);
        }
    }
}
