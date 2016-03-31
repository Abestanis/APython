package com.apython.python.pythonhost.views.terminal;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;

/**
 * This fragment displays a terminal. It is designed to be usable from the Python
 * App via the {@link TerminalInterface}.
 *
 * Created by Sebastian on 20.11.2015.
 */

public class TerminalFragment extends Fragment implements TerminalInterface {

    private Activity hostingActivity = null;

    private TerminalInput   pythonInput;
    private TerminalAdapter pythonOutput;
    private ProgramHandler  programHandler;
    private View rootView = null;
    private FrameLayout rootLayout = null;

    public TerminalFragment() {
        super();
        setArguments(new Bundle());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        hostingActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        hostingActivity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null || rootLayout == null) {
            rootLayout = new FrameLayout(getActivity().getApplicationContext());
            rootView = inflater.inflate(R.layout.view_terminal_layout, container, false);
            rootLayout.addView(rootView);
            ListView scrollContainer = (ListView) rootView.findViewById(R.id.terminalView);
            final Context context = hostingActivity.getApplicationContext();
            this.pythonOutput = new TerminalAdapter(context);
            this.pythonInput = (TerminalInput) LayoutInflater.from(context)
                    .inflate(R.layout.terminal_input, scrollContainer, false);
            scrollContainer.addFooterView(this.pythonInput);
            scrollContainer.setAdapter(this.pythonOutput);
            scrollContainer.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            scrollContainer.setItemsCanFocus(true);
            this.pythonInput.setOnTouchListener(new View.OnTouchListener() {
                GestureDetector detector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
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
                        if (e1 != null && e2 != null && Math.abs(e1.getX() - e2.getX()) > 30) {
                            if (e1.getX() - e2.getX() > 0) {
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
                    if (!pythonInput.isInputEnabled()) return false;
                    return detector.onTouchEvent(event);
                }
            });
            this.pythonInput.setCommitHandler(new TerminalInput.OnCommitHandler() {
                @Override
                public void onCommit(TerminalInput terminalInput) {
                    String[] inputList = terminalInput.popCurrentInput();
                    String prompt = inputList[0], input = inputList[1];
                    int splitIndex = input.indexOf('\n') + 1;
                    pythonOutput.addOutput(prompt + input.substring(0, splitIndex));
                    programHandler.notifyInput(input.substring(0, splitIndex));
                    // Anything after the first newline must be send to stdin
                    for (KeyEvent event : Util.stringToKeyEvents(input.substring(splitIndex))) {
                        programHandler.dispatchKeyEvent(event);
                    }
                }

                @Override
                public void onKeyEventWhileDisabled(KeyEvent event) {
                    programHandler.dispatchKeyEvent(event);
                }
            });
        } else {
            rootLayout.removeView(rootView);
            rootLayout = new FrameLayout(getActivity().getApplicationContext());
            rootLayout.addView(rootView);
        }

        // Make the keyboard always visible
        this.hostingActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return rootLayout;
    }

    @Override
    public boolean isInputEnabled() {
        return pythonInput.isInputEnabled();
    }

    @Override
    public void addOutput(String output) {
        pythonOutput.addOutput(output);
    }

    @Override
    public void enableInput(String prompt, String enqueuedInput) {
        pythonInput.enableInput(prompt, enqueuedInput != null ? enqueuedInput : "");
    }

    @Override
    public void registerInputHandler(ProgramHandler programHandler) {
        this.programHandler = programHandler;
    }
}
