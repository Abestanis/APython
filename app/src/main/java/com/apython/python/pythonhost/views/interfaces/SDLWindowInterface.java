package com.apython.python.pythonhost.views.interfaces;

import android.view.KeyEvent;

import com.apython.python.pythonhost.views.sdl.SDLWindowFragment;

/**
 * Interface for the SDL view.
 *
 * Created by Sebastian on 21.11.2015.
 */

public interface SDLWindowInterface {
    interface WindowManager {
        interface ActivityEventsListener {
            void onPause();
            void onResume();
            void onLowMemory();
            void onDestroy();
        }
        SDLWindowFragment createWindow();
        void destroyWindow(SDLWindowFragment window);
        void setWindowName(SDLWindowFragment window, String name);
        void setActivityEventsListener(ActivityEventsListener eventsListener);
    }
    boolean dispatchKeyEvent(KeyEvent event);
}
