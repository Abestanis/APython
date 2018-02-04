package com.apython.python.pythonhost.views.sdl;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;

/**
 * An input connection that sends it's input to the SDL event queue.
 * 
 * Created by Sebastian on 21.11.2015.
 */
class SDLInputConnection extends BaseInputConnection {

    private final SDLWindowFragment sdlWindow;

    SDLInputConnection(View targetView, boolean fullEditor, SDLWindowFragment sdlWindow) {
        super(targetView, fullEditor);
        this.sdlWindow = sdlWindow;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        /*
         * This handles the keycodes from soft keyboard (and IME-translated input from hardkeyboard)
         */
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (isTextInputEvent(event)) {
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
        nativeCommitText(text.toString(), newCursorPosition);
        return super.commitText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        nativeSetComposingText(text.toString(), newCursorPosition);
        return super.setComposingText(text, newCursorPosition);
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        // Workaround to capture backspace key. Ref: http://stackoverflow.com/questions/14560344/android-backspace-in-webview-baseinputconnection
        // and https://bugzilla.libsdl.org/show_bug.cgi?id=2265
        if (beforeLength > 0 && afterLength == 0) {
            boolean ret = true;
            // backspace(s)
            while (beforeLength-- > 0) {
               boolean ret_key = sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                              && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
               ret = ret && ret_key; 
            }
            return ret;
        }
        return super.deleteSurroundingText(beforeLength, afterLength);
    }
    
    @SuppressLint("ObsoleteSdkInt")
    static boolean isTextInputEvent(KeyEvent event) {
        // Key pressed with Ctrl should be sent as SDL_KEYDOWN/SDL_KEYUP and not SDL_TEXTINPUT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (event.isCtrlPressed()) {
                return false;
            }
        }
        return event.isPrintingKey() || event.getKeyCode() == KeyEvent.KEYCODE_SPACE;
    }

    public native void nativeCommitText(String text, int newCursorPosition);
    public native void nativeSetComposingText(String text, int newCursorPosition);
}
