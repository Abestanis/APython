package com.apython.python.pythonhost.views.terminal;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.views.PythonFragment;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;

/**
 * This fragment displays a terminal. It is designed to be usable from the Python
 * App via the {@link TerminalInterface}.
 *
 * Created by Sebastian on 20.11.2015.
 */

public class TerminalFragment extends PythonFragment implements TerminalInterface {
    private TerminalInput   pythonInput;
    private TerminalAdapter pythonOutput;
    private ProgramHandler  programHandler;
    private View        rootView        = null;
    private FrameLayout rootLayout      = null;
    private String      outputBuffer    = null;
    private ListView    scrollContainer = null;
    private int terminalCharWith = 0, terminalCharHeight = 0;

    public TerminalFragment(Activity activity, String tag) {
        super(activity, tag);
    }

    @Override
    public View createView(ViewGroup container) {
        if (rootView == null || rootLayout == null) {
            final Context context = getActivity().getBaseContext();
            rootLayout = new FrameLayout(context);
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            rootView = layoutInflater.inflate(context.getResources().getLayout(R.layout.view_terminal_layout),
                                        container, false);
            rootLayout.addView(rootView);
            rootLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    updateTerminalMetrics();
                }
            });
            scrollContainer = (ListView) rootView.findViewById(R.id.terminalView);
            this.pythonOutput = new TerminalAdapter(context);
            if (outputBuffer != null) {
                pythonOutput.addOutput(outputBuffer);
                outputBuffer = null;
            }
            this.pythonInput = (TerminalInput) layoutInflater.inflate(
                    context.getResources().getLayout(R.layout.terminal_input), scrollContainer, false);
            this.pythonOutput.setInputView(this.pythonInput);
            scrollContainer.setFocusable(false);
            scrollContainer.setAdapter(this.pythonOutput);
            scrollContainer.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
            scrollContainer.setItemsCanFocus(true);
            pythonInput.requestFocus();
            rootView.setOnTouchListener(new View.OnTouchListener() {
                final GestureDetector detector = new GestureDetector(
                        context, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent firstEvent, MotionEvent lastEvent,
                                           float velocityX, float velocityY) {
                        if (programHandler != null && firstEvent != null && lastEvent != null &&
                                Math.abs(firstEvent.getY() - lastEvent.getY()) < 50 &&
                                Math.abs(firstEvent.getX() - lastEvent.getX()) > 30) {
                            if (firstEvent.getX() - lastEvent.getX() > 0) {
                                programHandler.sendInput("\033[B");
                            } else {
                                programHandler.sendInput("\033[A");
                            }
                            return true;
                        }
                        return false;
                    }
                });

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return detector.onTouchEvent(event);
                }
            });
            this.pythonInput.setCommitHandler(new TerminalInput.OnCommitHandler() {
                TextKeyListener keyInputListener = new TextKeyListener(TextKeyListener.Capitalize.NONE, false);
                Editable keyInput = Editable.Factory.getInstance().newEditable("");
                
                @Override
                public void onCommit(TerminalInput terminalInput) {
                    String input = terminalInput.popCurrentInput();
                    if (programHandler != null) {
                        programHandler.sendInput(input);
                    }
                }

                @Override
                public boolean onKeyEventWhileDisabled(KeyEvent event) {
                    int keyCode = event.getKeyCode();
                    boolean result;
                    switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        result = keyInputListener.onKeyDown(null, keyInput, keyCode, event);
                        break;
                    case KeyEvent.ACTION_UP:
                        result = keyInputListener.onKeyUp(null, keyInput, keyCode, event);
                        break;
                    default:
                        result = keyInputListener.onKeyOther(null, keyInput, event);
                    }
                    if (programHandler != null) {
                        String input = null;
                        if (keyInput.length() > 0) {
                            input = keyInput.toString();
                            keyInput.clear();
                        } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            if (keyCode == KeyEvent.KEYCODE_DEL) {
                                input = "\u007F";
                            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                                programHandler.interrupt();
                                return result;
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                input = "\033[A";
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                input = "\033[B";
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                input = "\033[D";
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                input = "\033[C";
                            } else if (keyCode == KeyEvent.KEYCODE_TAB) {
                                input = "\t";
                            }
                        }
                        if (input != null) {
                            programHandler.sendInput(input);
                        }
                    }
                    return result;
                }
            });
        } else {
            rootLayout.removeView(rootView);
            rootLayout = new FrameLayout(getActivity().getApplicationContext());
            rootLayout.addView(rootView);
        }
        rootLayout.setFocusable(true);
        rootLayout.setFocusableInTouchMode(true);
        rootLayout.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return pythonInput.getCommitHandler().onKeyEventWhileDisabled(event);
            }
        });

        // Make the keyboard always visible
        this.getActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE |
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return rootLayout;
    }

    @Override
    public boolean isInputEnabled() {
        return pythonInput.isLineInputEnabled();
    }

    @Override
    public void addOutput(String output) {
        if (pythonOutput == null) {
            outputBuffer = (outputBuffer == null ? "" : outputBuffer) + output;
        } else {
            pythonOutput.addOutput(output);
        }
    }

    @Override
    public void setProgramHandler(ProgramHandler programHandler) {
        this.programHandler = programHandler;
    }

    @Override
    public void enableLineInput(String prompt) {
        pythonOutput.enableLineInput(prompt);
    }

    @Override
    public void disableLineInput() {
        pythonOutput.disableLineInput();
        if (rootLayout.requestFocus()) {
            InputMethodManager inputManager = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.showSoftInput(rootLayout, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    @Override
    public void close() {
        if (programHandler != null) {
            programHandler.terminate();
        }
    }
    
    private void updateTerminalMetrics() {
        StringBuilder testString = new StringBuilder();
        for (int i = pythonInput.getWidth(); i > 0; i -= 30) testString.append("MMM" + "MMM" + "MMM"); // TODO: This is ridicules
        int charWidth = pythonInput.getPaint().breakText(testString, 0, testString.length(),
                                                         true, pythonInput.getWidth(), null);
        int charHeight = (int) Math.floor(rootLayout.getHeight() / pythonInput.getHeight());
        if (charHeight != terminalCharHeight || charWidth != terminalCharWith) {
            Log.i(MainActivity.TAG, "Terminal layout changed: " + charWidth + "x" + charHeight);
            // TODO
        }
        terminalCharWith = charWidth;
        terminalCharHeight = charHeight;
    }
}
