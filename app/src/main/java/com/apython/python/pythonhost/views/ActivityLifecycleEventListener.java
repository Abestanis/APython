package com.apython.python.pythonhost.views;

/**
 * Indicates that this class needs to be informed of the
 * lifecycle events of an activity.
 * 
 * Created by Sebastian on 11.11.2016.
 */
public interface ActivityLifecycleEventListener {
    void onDestroy();
    void onResume();
    void onPause();
    void onLowMemory();
}
