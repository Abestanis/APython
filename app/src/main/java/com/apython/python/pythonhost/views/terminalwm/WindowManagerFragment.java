package com.apython.python.pythonhost.views.terminalwm;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.views.ActivityLifecycleEventListener;
import com.apython.python.pythonhost.views.PythonFragment;
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;
import com.apython.python.pythonhost.views.sdl.SDLServer;
import com.apython.python.pythonhost.views.terminal.TerminalFragment;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * A window manager fragment designed to hold multiple "windows" as tabs.
 * The first "window" will always be a {@link TerminalFragment}.
 *
 * Created by Sebastian on 08.01.2016.
 */

public class WindowManagerFragment extends PythonFragment implements 
        WindowManagerInterface, ActivityLifecycleEventListener {
    private static final String TAG = "TerminalWindowManager";
    private WindowManagerFragmentTabHost tabHost;
    private static final String                 DEFAULT_UNTITLED_NAME  = "Untitled";
    private final        ArrayList<String>      windowNames            = new ArrayList<>(5);
    private final        ArrayList<Window>      windows                = new ArrayList<>(5);
    private final        SDLServer              sdlServer;

    public WindowManagerFragment(Activity activity, String tag) {
        super(activity, tag);
        sdlServer = new SDLServer(activity, this);
    }

    @Override
    public View createView(ViewGroup container) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                                                 | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        Context context = getActivity().getBaseContext();
        View root = LayoutInflater.from(context).inflate(context.getResources().getLayout(
                R.layout.terminal_window_manager_layout), container, false);
        tabHost = root.findViewById(android.R.id.tabhost);
        return root;
    }

    @Override
    public PythonFragment getCurrentWindow() {
        return tabHost.getCurrentFragment();
    }

    private int count = 0;

    @Override
    public <T extends PythonFragment & Window> T createWindow(final Class<T> windowClass) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String windowName = getUnusedWindowName();
        final String windowTag = "window " + count++;
        windowNames.add(windowName);
        getActivity().runOnUiThread(() -> {
                tabHost.addTab(tabHost.getFragmentTabSpec(getActivity(), windowTag)
                                       .setFragmentClass(windowClass)
                                       .setTitle(windowName));
                tabHost.setCurrentTab(windowTag);
                countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Was interrupted while waiting for the window Fragment creation!", e);
        }
        //noinspection unchecked
        final T window = (T) tabHost.getCurrentFragment();
        if (window != null) {
            windows.add(window);
            tabHost.getTabWidget(window.getTag()).setOnCloseListener(tabWidget -> window.close());
        }
        return window;
    }

    @Override
    public void destroyWindow(Window window) {
        windows.remove(window);
        windowNames.remove(tabHost.getTabTitle(window.getTag()));
        tabHost.removeTab(window.getTag());
    }

    @Override
    public void setWindowName(Window window, String name) {
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
    public void setWindowIcon(Window window, Drawable icon) {
        tabHost.setTabIcon(window.getTag(), icon);
    }
    
    @Override
    public void onPause() {
        sdlServer.onActivityPause();
    }

    @Override
    public void onResume() {
        sdlServer.onActivityResume();
    }

    @Override
    public void onDestroy() {
        sdlServer.onActivityDestroyed();
    }

    @Override
    public void onLowMemory() {
        sdlServer.onActivityLowMemory();
    }

    private int getWindowIndex(Window window) {
        return windows.indexOf(window);
    }

    private String getUnusedWindowName() {
        return getUnusedWindowName(DEFAULT_UNTITLED_NAME);
    }

    private String getUnusedWindowName(
            @SuppressWarnings("SameParameterValue") String proposedName) {
        String name = proposedName;
        int i = 1;
        while (windowNames.contains(name)) {
            name = proposedName + " " + i;
            i++;
        }
        return name;
    }
}
