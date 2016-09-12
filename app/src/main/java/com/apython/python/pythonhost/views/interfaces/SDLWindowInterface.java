package com.apython.python.pythonhost.views.interfaces;

import android.view.KeyEvent;

/**
 * Interface for the SDL view.
 *
 * Created by Sebastian on 21.11.2015.
 */

public interface SDLWindowInterface extends WindowManagerInterface.Window {
    boolean dispatchKeyEvent(KeyEvent event);
}
