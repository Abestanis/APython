package com.apython.python.pythonhost.views.sdl;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

/**
 * Generic motion handler for {@link SDLSurfaceView}s.
 * <p>
 * Created by Sebastian on 16.11.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
class SDLGenericMotionListener implements View.OnGenericMotionListener {
    private final SDLServer sdlServer;

    SDLGenericMotionListener(SDLServer sdlServer) {
        super();
        this.sdlServer = sdlServer;
    }

    // Generic Motion (mouse hover, joystick...) events go here
    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        SDLSurfaceView window = (SDLSurfaceView) v;
        float x, y;
        int action;

        switch (event.getSource()) {
        case InputDevice.SOURCE_JOYSTICK:
        case InputDevice.SOURCE_GAMEPAD:
        case InputDevice.SOURCE_DPAD:
            return sdlServer.getControllerManager().getJoystickHandler().handleMotionEvent(event);

        case InputDevice.SOURCE_MOUSE:
            if (!sdlServer.separateMouseAndTouch()) {
                break;
            }
            action = event.getActionMasked();
            switch (action) {
            case MotionEvent.ACTION_SCROLL:
                x = event.getAxisValue(MotionEvent.AXIS_HSCROLL, 0);
                y = event.getAxisValue(MotionEvent.AXIS_VSCROLL, 0);
                window.sdlWindow.onNativeMouse(0, action, x, y);
                return true;

            case MotionEvent.ACTION_HOVER_MOVE:
                x = event.getX(0);
                y = event.getY(0);

                window.sdlWindow.onNativeMouse(0, action, x, y);
                return true;

            default:
                break;
            }
            break;

        default:
            break;
        }

        // Event was not managed
        return false;
    }
}
