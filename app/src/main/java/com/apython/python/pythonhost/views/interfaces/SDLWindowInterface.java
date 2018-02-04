package com.apython.python.pythonhost.views.interfaces;

import android.support.annotation.Nullable;
import android.view.KeyEvent;

/**
 * Interface for the SDL view.
 *
 * Created by Sebastian on 21.11.2015.
 */

public interface SDLWindowInterface extends WindowManagerInterface.Window {
    /**
     * Callback for the dispatch key function.
     * 
     * @param event The key event.
     * @return The return value of the activities dispatchKeyEvent or null to indicate
     *         that it should call the super function.
     */
    @Nullable
    Boolean dispatchKeyEvent(KeyEvent event);
}
