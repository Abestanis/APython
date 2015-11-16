package com.apython.python.pythonhost.interpreter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
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

    private TerminalInput pythonInput;
    private TerminalAdapter pythonOutput;
    private PythonInterpreterRunnable interpreter;

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
        ListView scrollContainer = (ListView)    findViewById(R.id.scrollContainer);
        this.pythonOutput        = new TerminalAdapter(getApplicationContext());
        this.pythonInput         = (TerminalInput) LayoutInflater.from(getApplicationContext())
                                   .inflate(R.layout.terminal_input, scrollContainer, false);
        scrollContainer.addFooterView(this.pythonInput);
        scrollContainer.setAdapter(this.pythonOutput);
        scrollContainer.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        scrollContainer.setItemsCanFocus(true);
        scrollContainer.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector detector = new GestureDetector(getApplicationContext(), new GestureDetector.OnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public void onShowPress(MotionEvent e) {
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) { return false; }
                    if (e1.getPointerCount() == 2 && e2.getPointerCount() == 2) {
                        if (Math.abs(e1.getY() - e2.getY()) > 30) {
                            if (e1.getY() - e2.getY() < 0) {
                                pythonInput.loadNextCommand();
                            } else {
                                pythonInput.loadLastCommand();
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO: This intercepts the scrolling of the listView
                // return detector.onTouchEvent(event);
                return false;
            }
        });

        this.pythonInput.setCommitHandler(new TerminalInput.OnCommitHandler() {
            @Override
            public void onCommit(TerminalInput terminalInput) {
                String[] inputList = terminalInput.popCurrentInput();
                String prompt = inputList[0], input = inputList[1];
                int splitIndex = input.indexOf('\n') + 1;
                pythonOutput.addOutput(prompt + input.substring(0, splitIndex));
                interpreter.notifyInput(input.substring(0, splitIndex));
                // Anything after the first newline must be send to stdin
                for (char character : input.substring(splitIndex).toCharArray()) {
                    interpreter.dispatchKey(character);
                }
            }

            @Override
            public void onKeyEventWhileDisabled(KeyEvent event) {
                dispatchKeyEvent(event);
            }
        });

        this.interpreter = new PythonInterpreterRunnable(this, pythonVersion, new PythonInterpreter.IOHandler() {
            @Override
            public void addOutput(String text) {
                pythonOutput.addOutput(text);
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
        if (!pythonInput.isInputEnabled() && interpreter != null) {
            // input via stdin pipe
            return interpreter.dispatchKeyEvent(event);
        } else {
            // Normal input (with delete, copy, paste etc.)
            return super.dispatchKeyEvent(event);
        }
    }
}
