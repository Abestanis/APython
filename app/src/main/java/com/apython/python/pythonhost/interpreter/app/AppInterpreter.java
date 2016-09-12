package com.apython.python.pythonhost.interpreter.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.apython.python.pythonhost.interpreter.PythonInterpreter;
import com.apython.python.pythonhost.views.PythonFragment;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;
import com.apython.python.pythonhost.views.sdl.SDLWindowFragment;
import com.apython.python.pythonhost.views.terminal.TerminalFragment;
import com.apython.python.pythonhost.views.terminalwm.WindowManagerFragment;

/**
 * This class implements the execution of python code from other Python apps.
 * It will be loaded and instantiated by the application library, which will
 * in turn be loaded by the Python app.
 * 
 * Since this class and all other classes from the Python host will be loaded by
 * a different classloader then the {@link AppInterpreter#hostingAppActivity}, we
 * need to take special care thet nothin accidantally uses the classloader of the
 * hostingAppActivity (e.g. the {@link android.view.LayoutInflater LayoutInflater})!
 * <p>
 * 
 * Created by Sebastian on 29.05.2016.
 */
@SuppressLint("Registered")
public class AppInterpreter extends Activity implements WindowManagerInterface {
    
    @SuppressWarnings("unused")
    public enum WindowType {
        NO_WINDOW      (null),
        TERMINAL       (TerminalFragment.class),
        SDL            (SDLWindowFragment.class),
        WINDOW_MANAGER (WindowManagerFragment.class),
        ANDROID        (null); // TODO: Implement

        public final Class<? extends PythonFragment> windowClass;

        static WindowType fromOrdinal(int ordinal) {
            for (WindowType windowType : WindowType.values()) {
                if (windowType.ordinal() == ordinal) return windowType;
            }
            throw new IllegalArgumentException("Invalid ordinal passed to WindowType#fromOrdinal: "
                                                       + ordinal);
        }

        WindowType(Class<? extends PythonFragment> windowClass) {
            this.windowClass = windowClass;
        }
    }

    private final Activity hostingAppActivity;
    private String                 logTag                 = "PythonApp";
    private PythonInterpreter      interpreter            = null;
    private PythonFragment         windowFragment         = null;
    private ActivityEventsListener activityEventsListener = null;
    
    public AppInterpreter(Context pyHostContext, Activity hostingAppActivity, String pythonVersion) {
        super();
        this.hostingAppActivity = new AppActivityProxy(hostingAppActivity, pyHostContext);
        interpreter = new PythonInterpreter(pyHostContext, pythonVersion);
        SDLWindowFragment.setWindowManager(this);
    }
    
    @SuppressWarnings("unused")
    public Object setWindow(int rawWindowType, ViewGroup parent) {
        WindowType windowType = WindowType.NO_WINDOW;
        try { windowType = WindowType.fromOrdinal(rawWindowType); }
        catch (IllegalArgumentException e) { Log.w(logTag, e); }
        if (windowType == WindowType.NO_WINDOW) return null;
        windowFragment = PythonFragment.create(windowType.windowClass, hostingAppActivity, "MainAppWindow");
        initWindowFragment();
        View view = windowFragment.createView(parent);
        parent.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                        ViewGroup.LayoutParams.MATCH_PARENT));
        return this;
    }

    @SuppressWarnings("unused")
    public void setLogTag(String logTag) {
        this.logTag = logTag;
        interpreter.setLogTag(logTag);
    }

    @SuppressWarnings("unused")
    public int startInterpreter(String[] args) {
        return interpreter.runPythonInterpreter(args);
    }
    
    private void initWindowFragment() {
        if (windowFragment instanceof TerminalInterface) {
            final TerminalInterface terminal = (TerminalInterface) windowFragment;
            terminal.setProgramHandler(interpreter);
            interpreter.setIoHandler(new PythonInterpreter.IOHandler() {
                @Override
                public void addOutput(final String text) {
                    hostingAppActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            terminal.addOutput(text);
                        }
                    });
                }

                @Override
                public void setupInput(final String prompt) {
                    final String enqueuedInput = interpreter.getEnqueueInput();
                    hostingAppActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            terminal.enableInput(prompt, enqueuedInput);
                        }
                    });
                }
            });
        }
    }

//    @Override
//    public void onBackPressed() {
//        interpreter.interrupt();
//        terminalView.disableInput();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (interpreter != null && interpreter.isRunning()) {
//            interpreter.terminate();
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activityEventsListener != null) activityEventsListener.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activityEventsListener != null) activityEventsListener.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (activityEventsListener != null) activityEventsListener.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activityEventsListener != null) activityEventsListener.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (windowFragment != null) {
            if (windowFragment instanceof TerminalInterface) {
                if (event.getKeyCode() != KeyEvent.KEYCODE_BACK && !((TerminalInterface) windowFragment).isInputEnabled()) {
                    return interpreter.dispatchKeyEvent(event);
                }
            } else if (windowFragment instanceof SDLWindowInterface) {
                return ((SDLWindowInterface) windowFragment).dispatchKeyEvent(event);
            }
        }
        return false;
    }

    @Override
    public <T extends PythonFragment & Window> T createWindow(Class<T> windowClass) {
        if (windowFragment != null && windowFragment instanceof Window) {
            //noinspection unchecked
            return (T) windowFragment;
        }
        return null;
    }

    @Override
    public void destroyWindow(Window window) {
        finish();
    }

    @Override
    public void setWindowName(Window window, String name) {
        setTitle(name);
    }

    @Override
    public void setWindowIcon(Window window, Drawable icon) {
//        getWindow().setIcon(icon);
    }

    @Override
    public void setActivityEventsListener(ActivityEventsListener eventsListener) {
        activityEventsListener = eventsListener;
    }

    @Override
    public PythonFragment getCurrentWindow() {
        return windowFragment;
    }
}