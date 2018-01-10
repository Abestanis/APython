package com.apython.python.pythonhost.views.terminal;

import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;

import com.apython.python.pythonhost.MainActivity;

public class TerminalInputConnection extends BaseInputConnection {
    
    public TerminalInputConnection(TerminalInput terminalInput) {
        super(terminalInput, true);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        Log.d(MainActivity.TAG, "sendKeyEvent " + event);
        return super.sendKeyEvent(event);
    }
    
    
    
    
}
