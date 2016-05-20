package com.apython.python.pythonhost.views.terminalwm;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;
import com.apython.python.pythonhost.views.interfaces.TerminalWindowManagerInterface;
import com.apython.python.pythonhost.views.sdl.SDLWindowFragment;
import com.apython.python.pythonhost.views.terminal.TerminalFragment;

import java.util.ArrayList;

/**
 * A window manager fragment designed to hold multiple "windows" as tabs.
 * The first "window" will always be a {@link TerminalFragment}.
 *
 * Created by Sebastian on 08.01.2016.
 */

public class TerminalWindowManagerFragment extends Fragment implements TerminalWindowManagerInterface,
        SDLWindowInterface.WindowManager {
    private static final String TAG = "TerminalWindowManager";
    private WindowManagerFragmentTabHost tabHost;
    private              ActivityEventsListener        activityEventsListener = null;
    private static final String                        DEFAULT_UNTITLED_NAME  = "Untitled";
    private              ArrayList<String>             windowNames            = new ArrayList<>(5);
    private              ArrayList<SDLWindowInterface> windows                = new ArrayList<>(5);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SDLWindowFragment.initLibraries(getActivity().getApplicationContext())) {
            SDLWindowFragment.setWindowManager(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.terminal_window_manager_layout, container, false);
        tabHost = (WindowManagerFragmentTabHost) root.findViewById(android.R.id.tabhost);
        tabHost.setFragmentManager(getActivity().getSupportFragmentManager());
        windowNames.add("Python");
        windows.add(null);
        tabHost.addTab(tabHost.getFragmentTabSpec("Python")
                               .setFragmentClass(TerminalFragment.class)
                               .setIcon(getResources().getDrawable(R.drawable.python_launcher_icon))
                               .setTitle("Python"));
        return root;
    }

    @Override
    public Fragment getCurrentWindow() {
        return getActivity().getSupportFragmentManager().findFragmentByTag(tabHost.getCurrentTabTag());
    }

    private int count = 0;

    @Override
    public SDLWindowFragment createWindow() {
        final Object lock = new Object();
        final Object[] result = {Boolean.FALSE, null};
        synchronized (lock) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        String windowName = getUnusedWindowName();
                        String windowTag = "window " + count++;
                        windowNames.add(windowName);
                        tabHost.addTab(tabHost.getFragmentTabSpec(windowTag)
                                               .setFragmentClass(SDLWindowFragment.class)
                                               .setTitle(windowName));
                        tabHost.setCurrentTab(windowTag);
                        getActivity().getSupportFragmentManager().executePendingTransactions();
                        result[0] = Boolean.TRUE;
                        result[1] = windowTag;
                        lock.notify();
                    }
                }
            });
            if (result[0] == Boolean.FALSE) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    Log.e(TAG, "Was interrupted while waiting for the window Fragment creation!", ex);
                }
            }
        }
        SDLWindowFragment window = (SDLWindowFragment) getActivity().getSupportFragmentManager().findFragmentByTag((String) result[1]);
        if (window != null) windows.add(window);
        return window;
    }

    @Override
    public void destroyWindow(SDLWindowFragment window) {
        windows.remove(window);
        windowNames.remove(tabHost.getTabTitle(window.getTag()));
        tabHost.removeTab(window.getTag());
    }

    @Override
    public void setWindowName(SDLWindowFragment window, String name) {
        int windowIndex = getWindowIndex(window);
        if (windowIndex != -1) {
            if (name == null) {
                windowNames.set(windowIndex, null);
                name = getUnusedWindowName();
            }
            windowNames.set(windowNames.indexOf(tabHost.getTabTitle(window.getTag())), name);
            tabHost.setTabTitle(window.getTag(), name);
        }
    }

    @Override
    public void setWindowIcon(SDLWindowFragment window, Drawable icon) {
        tabHost.setTabIcon(window.getTag(), icon);
    }

    @Override
    public void setActivityEventsListener(ActivityEventsListener eventsListener) {
        activityEventsListener = eventsListener;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (activityEventsListener != null) {
            activityEventsListener.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (activityEventsListener != null) {
            activityEventsListener.onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activityEventsListener != null) {
            activityEventsListener.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (activityEventsListener != null) {
            activityEventsListener.onLowMemory();
        }
    }

    private int getWindowIndex(SDLWindowInterface window) {
        return windows.indexOf(window);
    }

    private String getUnusedWindowName() {
        return getUnusedWindowName(DEFAULT_UNTITLED_NAME);
    }

    private String getUnusedWindowName(String proposedName) {
        String name = proposedName;
        int i = 1;
        while (windowNames.contains(name)) {
            name = proposedName + " " + i;
            i++;
        }
        return name;
    }
}
