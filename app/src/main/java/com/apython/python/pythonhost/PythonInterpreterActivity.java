package com.apython.python.pythonhost;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/*
 * This Activity starts and displays a Python interpreter.
 *
 * Created by Sebastian on 08.06.2015.
 */

public class PythonInterpreterActivity extends Activity {

    // A boolean that is required in order to show the soft keyboard at any time.
    // If we would call pythonInput.setEnabled(false), we would hide the soft keyboard every time.
    // Instead, we don't disable pythonInput, but we handle the input after it has occurred.
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
        this.interpreter = new PythonInterpreterRunnable(this, new PythonInterpreter.IOHandler() {
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
        // Make the keyboard always visible
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Start the interpreter thread
        new Thread(this.interpreter).start();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!this.pythonInputEnabled) {
            // input via stdin pipe
            return this.interpreter.dispatchKeyEvent(event);
        } else {
            // Normal input (with delete, copy, paste etc.)
            return super.dispatchKeyEvent(event);
        }
    }
}
