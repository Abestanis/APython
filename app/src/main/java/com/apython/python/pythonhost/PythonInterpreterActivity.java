package com.apython.python.pythonhost;

import android.app.Activity;
import android.os.*;
import android.support.annotation.NonNull;
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

    TextView pythonOutput;
    EditText pythonInput;
    ScrollView scrollContainer;
    PythonInterpreter interpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PackageManager.installStandardModules(getApplicationContext());
        this.setContentView(R.layout.activity_python_interpreter);
        this.pythonOutput    = (TextView)   findViewById(R.id.pythonOutput);
        this.pythonInput     = (EditText)   findViewById(R.id.pythonInput);
        this.scrollContainer = (ScrollView) findViewById(R.id.scrollContainer);
        this.interpreter = new PythonInterpreter(new Handler(), this, new PythonInterpreter.IOHandler() {
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
                pythonInput.append(prompt);
                pythonInput.setSelection(prompt.length());
                pythonInput.setCursorVisible(true);
                pythonInput.setEnabled(true);
            }
        });
        this.pythonInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    String input = v.getText().toString();
                    pythonOutput.append(input + "\n");
                    v.setText("");
                    v.setCursorVisible(false);
                    v.setEnabled(false);
                    interpreter.notifyInput(input);
                    return true;
                }
                return false;
            }
        });
        // Make the keyboard always visible
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Start the interpreter thread
        new Thread(this.interpreter).start();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!this.pythonInput.isEnabled()) {
            // input via stdin pipe
            return this.interpreter.dispatchKeyEvent(event);
        } else {
            // Normal input (with delete, copy, paste etc.)
            return super.dispatchKeyEvent(event);
        }
    }
}
