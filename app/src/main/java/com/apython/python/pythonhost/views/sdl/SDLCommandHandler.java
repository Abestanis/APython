package com.apython.python.pythonhost.views.sdl;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Sebastian on 02.02.2018.
 */
public class SDLCommandHandler extends Handler {
    private final static String TAG = "SDLCommandHandler";
    // Messages from SDL
    static final int COMMAND_CHANGE_TITLE = 1;
    static final int COMMAND_UNUSED = 2;
    static final int COMMAND_TEXTEDIT_HIDE = 3;
    static final int COMMAND_SET_KEEP_SCREEN_ON = 5;
    protected static final int COMMAND_USER = 0x8000;
    
    @Override
    public void handleMessage(Message msg) {
        switch (msg.arg1) {
//        case COMMAND_CHANGE_TITLE:
//            if (context instanceof Activity) {
//                ((Activity) context).setTitle((String)msg.obj);
//            } else {
//                Log.e(TAG, "error handling message, getContext() returned no Activity");
//            }
//            break;
//        case COMMAND_TEXTEDIT_HIDE:
//            if (mTextEdit != null) {
//                // Note: On some devices setting view to GONE creates a flicker in landscape.
//                // Setting the View's sizes to 0 is similar to GONE but without the flicker.
//                // The sizes will be set to useful values when the keyboard is shown again.
//                mTextEdit.setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
//
//                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
//
//                mScreenKeyboardShown = false;
//            }
//            break;
//        case COMMAND_SET_KEEP_SCREEN_ON:
//        {
//            if (context instanceof Activity) {
//                Window window = ((Activity) context).getWindow();
//                if (window != null) {
//                    if ((msg.obj instanceof Integer) && (((Integer) msg.obj).intValue() != 0)) {
//                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//                    } else {
//                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//                    }
//                }
//            }
//            break;
//        }
        default:
            if (!onUnhandledMessage(msg.arg1, msg.obj)) {
                Log.e(TAG, "Error handling message, command is " + msg.arg1);
            }
        }
    }

    /**
     * This method is called by SDL if SDL did not handle a message itself.
     * This happens if a received message contains an unsupported command.
     * Method can be overwritten to handle Messages in a different class.
     * @param command the command of the message.
     * @param param the parameter of the message. May be null.
     * @return if the message was handled in overridden method.
     */
    protected boolean onUnhandledMessage(int command, Object param) {
        return false;
    }
}
