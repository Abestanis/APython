package com.apython.python.pythonhost.views.sdl;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

/**
 * An input connection that sends it's input to the SDL event queue.
 * 
 * Created by Sebastian on 21.11.2015.
 */
public class SDLInputConnection extends BaseInputConnection {

    private SDLWindowFragment sdlWindow;

    public SDLInputConnection(View targetView, boolean fullEditor, SDLWindowFragment sdlWindow) {
        super(targetView, fullEditor);
        this.sdlWindow = sdlWindow;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        /*
         * This handles the key-codes from soft keyboard (and IME-translated
         * input from hard-keyboard)
         */
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.isPrintingKey()) {
                commitText(String.valueOf((char) event.getUnicodeChar()), 1);
            }
            sdlWindow.onNativeKeyDown(keyCode);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {

            sdlWindow.onNativeKeyUp(keyCode);
            return true;
        }
        return super.sendKeyEvent(event);
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        sdlWindow.nativeCommitText(text.toString(), newCursorPosition);
        return super.commitText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        sdlWindow.nativeSetComposingText(text.toString(), newCursorPosition);
        return super.setComposingText(text, newCursorPosition);
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        // Workaround to capture backspace key. Ref: http://stackoverflow.com/questions/14560344/android-backspace-in-webview-baseinputconnection
        if (beforeLength == 1 && afterLength == 0) {
            // backspace
            return super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    && super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }
        return super.deleteSurroundingText(beforeLength, afterLength);
    }
}
