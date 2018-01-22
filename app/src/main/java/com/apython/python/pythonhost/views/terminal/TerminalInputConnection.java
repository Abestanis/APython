package com.apython.python.pythonhost.views.terminal;

import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import com.apython.python.pythonhost.MainActivity;

public class TerminalInputConnection extends InputConnectionWrapper {
    
    public TerminalInputConnection(InputConnection target) {
        super(target, true);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        Log.d(MainActivity.TAG, "sendKeyEvent " + event);
        return super.sendKeyEvent(event);
    }
}
