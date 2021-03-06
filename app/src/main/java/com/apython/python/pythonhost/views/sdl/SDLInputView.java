package com.apython.python.pythonhost.views.sdl;

import android.content.Context;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * A view that acts as an input target for the SDL surface.
 * 
 * Created by Sebastian on 21.11.2015.
 */
class SDLInputView extends View implements View.OnKeyListener {
    private InputConnection   inputConnection;
    private SDLWindowFragment sdlWindow;

    public SDLInputView(Context context) {
        super(context);
        setFocusableInTouchMode(true);
        setFocusable(true);
        setOnKeyListener(this);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // This handles the hardware keyboard input
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (SDLInputConnection.isTextInputEvent(event)) {
                inputConnection.commitText(String.valueOf((char) event.getUnicodeChar()), 1);
            }
            sdlWindow.onNativeKeyDown(keyCode);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            sdlWindow.onNativeKeyUp(keyCode);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // As seen on StackOverflow: http://stackoverflow.com/questions/7634346/keyboard-hide-event
        // FIXME: Discussion at http://bugzilla.libsdl.org/show_bug.cgi?id=1639
        // FIXME: This is not a 100% effective solution to the problem of detecting if the keyboard is showing or not
        // FIXME: A more effective solution would be to assume our Layout to be RelativeLayout or LinearLayout
        // FIXME: And determine the keyboard presence doing this: http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        // FIXME: An even more effective way would be if Android provided this out of the box, but where would the fun be in that :)
        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            if (getVisibility() == View.VISIBLE) {
                sdlWindow.onNativeKeyboardFocusLost();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        inputConnection = new SDLInputConnection(this, true, sdlWindow);

        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_FULLSCREEN /* API 11 */;
        
        return inputConnection;
    }

    public void setSDLWindow(SDLWindowFragment sdlWindow) {
        this.sdlWindow = sdlWindow;
    }
}
