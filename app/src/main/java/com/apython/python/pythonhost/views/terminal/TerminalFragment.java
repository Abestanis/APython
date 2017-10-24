package com.apython.python.pythonhost.views.terminal;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.method.TextKeyListener;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;

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
    private View rootView = null;
    private FrameLayout rootLayout = null;
    private String outputBuffer = null;

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
            ListView scrollContainer = (ListView) rootView.findViewById(R.id.terminalView);
            this.pythonOutput = new TerminalAdapter(context);
            if (outputBuffer != null) {
                pythonOutput.addOutput(outputBuffer);
                outputBuffer = null;
            }
            this.pythonInput = (TerminalInput) layoutInflater.inflate(
                    context.getResources().getLayout(R.layout.terminal_input), scrollContainer, false);
            scrollContainer.addFooterView(this.pythonInput);
            scrollContainer.setAdapter(this.pythonOutput);
            scrollContainer.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
            scrollContainer.setItemsCanFocus(true);
            pythonInput.requestFocus();
            this.pythonInput.setOnTouchListener(new View.OnTouchListener() {
                final GestureDetector detector = new GestureDetector(
                        context, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent firstEvent, MotionEvent lastEvent,
                                           float velocityX, float velocityY) {
                        if (firstEvent != null && lastEvent != null &&
                                Math.abs(firstEvent.getX() - lastEvent.getX()) > 30) {
                            if (firstEvent.getX() - lastEvent.getX() > 0) {
                                pythonInput.loadNextCommand();
                            } else {
                                pythonInput.loadLastCommand();
                            }
                            return true;
                        }
                        return false;
                    }
                });

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return pythonInput.isInputEnabled() && detector.onTouchEvent(event);
                }
            });
            this.pythonInput.setCommitHandler(new TerminalInput.OnCommitHandler() {
                TextKeyListener keyInputListener = new TextKeyListener(TextKeyListener.Capitalize.NONE, false);
                Editable keyInput = Editable.Factory.getInstance().newEditable("");
                
                @Override
                public void onCommit(TerminalInput terminalInput) {
                    String[] inputList = terminalInput.popCurrentInput();
                    String prompt = inputList[0], input = inputList[1];
                    int splitIndex = input.indexOf('\n') + 1;
                    pythonOutput.addOutput(prompt + input.substring(0, splitIndex));
                    if (programHandler == null) return;
                    programHandler.sendInput(input);
                }

                @Override
                public void onKeyEventWhileDisabled(KeyEvent event) {
                    switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        keyInputListener.onKeyDown(null, keyInput, event.getKeyCode(), event);
                        break;
                    case KeyEvent.ACTION_UP:
                        keyInputListener.onKeyUp(null, keyInput, event.getKeyCode(), event);
                        break;
                    default:
                        keyInputListener.onKeyOther(null, keyInput, event);
                    }
                    if (programHandler != null) {
                        String input = null;
                        if (keyInput.length() > 0) {
                            input = keyInput.toString();
                            keyInput.clear();
                        } else if (event.getAction() == KeyEvent.ACTION_DOWN
                                && event.getKeyCode() == KeyEvent.KEYCODE_DEL) { // TODO: Handle more special keys
                            input = "\u007F";
                        }
                        if (input != null) {
                            programHandler.sendInput(input);
                        }
                    }
                }
            });
        } else {
            rootLayout.removeView(rootView);
            rootLayout = new FrameLayout(getActivity().getApplicationContext());
            rootLayout.addView(rootView);
        }

        // Make the keyboard always visible
        this.getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                                                      | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return rootLayout;
    }

    @Override
    public boolean isInputEnabled() {
        return pythonInput.isInputEnabled();
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
    public void enableInput(String prompt, String enqueuedInput) {
        pythonInput.enableInput(prompt, enqueuedInput != null ? enqueuedInput : "");
    }

    @Override
    public void setProgramHandler(ProgramHandler programHandler) {
        this.programHandler = programHandler;
    }

    @Override
    public void disableInput() {
        pythonInput.setEnabled(false);
    }

    @Override
    public void close() {
        if (programHandler != null) {
            programHandler.terminate();
        }
    }
}
