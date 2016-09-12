package com.apython.python.pythonhost.views.interfaces;

import android.graphics.drawable.Drawable;

import com.apython.python.pythonhost.views.PythonFragment;

/**
 * Interface for the window manager.
 *
 * Created by Sebastian on 08.01.2016.
 */

public interface WindowManagerInterface {

    interface Window {
        String getTag();
    }
    interface ActivityEventsListener {
        void onPause();
        void onResume();
        void onLowMemory();
        void onDestroy();
    }
    <T extends PythonFragment & Window> T createWindow(Class<T> windowClass);
    void destroyWindow(Window window);
    void setWindowName(Window window, String name);
    void setWindowIcon(Window window, Drawable icon);
    void setActivityEventsListener(ActivityEventsListener eventsListener);
    PythonFragment getCurrentWindow();
}
