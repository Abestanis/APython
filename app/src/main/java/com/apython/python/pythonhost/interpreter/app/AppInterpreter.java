package com.apython.python.pythonhost.interpreter.app;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.apython.python.pythonhost.CalledByNative;
import com.apython.python.pythonhost.interpreter.handles.InterpreterPseudoTerminalIOHandle;
import com.apython.python.pythonhost.interpreter.handles.PythonInterpreterHandle;
import com.apython.python.pythonhost.interpreter.handles.PythonInterpreterThreadHandle;
import com.apython.python.pythonhost.views.ActivityLifecycleEventListener;
import com.apython.python.pythonhost.views.PythonFragment;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;
import com.apython.python.pythonhost.views.sdl.SDLServer;
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
 * need to take special care that nothing accidentally uses the classloader of the
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
            throw new IllegalArgumentException(
                    "Invalid ordinal passed to WindowType#fromOrdinal: " + ordinal);
        }

        WindowType(Class<? extends PythonFragment> windowClass) {
            this.windowClass = windowClass;
        }
    }

    private final Activity hostingAppActivity;
    private       String   pythonVersion;
    private String                  logTag         = "PythonApp";
    private PythonInterpreterHandle interpreter;
    private PythonFragment          windowFragment = null;
    private SDLServer               sdlServer      = null;
    
    public AppInterpreter(Context pyHostContext, Activity hostingAppActivity, String pythonVersion) {
        super();
        this.hostingAppActivity = new AppActivityProxy(hostingAppActivity, pyHostContext);
        interpreter = new PythonInterpreterThreadHandle(pyHostContext);
        this.pythonVersion = pythonVersion;
    }
    
    @CalledByNative
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

    @CalledByNative
    public void setLogTag(String logTag) {
        this.logTag = logTag;
        interpreter.setLogTag(logTag);
    }

    @CalledByNative
    public int startInterpreter(String[] args) {
        Integer exitCode;
        interpreter.startInterpreter(pythonVersion, args);
        interpreter.attach();
        while ((exitCode = interpreter.getInterpreterResult(true)) == null) {
            interpreter.interrupt();
        }
        return exitCode;
    }
    
    private void initWindowFragment() {
        if (windowFragment instanceof TerminalInterface) {
            final TerminalInterface terminal = (TerminalInterface) windowFragment;
            terminal.setProgramHandler(new TerminalInterface.ProgramHandler() {
                @Override
                public void sendInput(String input) {
                    interpreter.sendInput(input);
                }

                @Override
                public void terminate() {
                    // TODO: Handle
                }

                @Override
                public void onTerminalSizeChanged(int newWidth, int newHeight,
                                                  int pixelWidth, int pixelHeight) {
                    if (interpreter instanceof InterpreterPseudoTerminalIOHandle) {
                        ((InterpreterPseudoTerminalIOHandle) interpreter).setTerminalSize(
                                newWidth, newHeight, pixelWidth, pixelHeight);
                    }
                }

                @Override
                public void interrupt() {
                    interpreter.interrupt();
                }
            });
            interpreter.setIOHandler(new PythonInterpreterHandle.LineIOHandler() {
                @Override
                public void onOutput(final String output) {
                    hostingAppActivity.runOnUiThread(() -> terminal.addOutput(output));
                }

                @Override
                public void enableLineMode(String prompt) {
                    terminal.enableLineInput(prompt);
                }

                @Override
                public void stopLineMode() {
                    terminal.disableLineInput();
                }
            });
        } else if (windowFragment instanceof SDLWindowFragment) {
            if (sdlServer == null) {
                sdlServer = new SDLServer(hostingAppActivity, this);
            }
        }
    }

    @Override
    public void onLowMemory() {
        if (windowFragment instanceof ActivityLifecycleEventListener)
            ((ActivityLifecycleEventListener) windowFragment).onLowMemory();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (windowFragment instanceof SDLWindowInterface) {
            Boolean result = ((SDLWindowInterface) windowFragment).dispatchKeyEvent(event);
            if (result != null) { return result; }
        }
        return super.dispatchKeyEvent(event);
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
        hostingAppActivity.finish();
    }

    @Override
    public void setWindowName(Window window, String name) {
        hostingAppActivity.setTitle(name);
    }

    @Override
    public void setWindowIcon(Window window, Drawable icon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = hostingAppActivity.getActionBar();
            if (actionBar != null) {
                actionBar.setIcon(icon);
            }
        }
    }

    @Override
    public PythonFragment getCurrentWindow() {
        return windowFragment;
    }

    public static final int ON_DESTROY = 0;
    public static final int ON_PAUSE = 1;
    public static final int ON_RESUME = 2;
    
    public void onActivityLifecycleEvent(int eventType) {
        if (windowFragment instanceof ActivityLifecycleEventListener) {
            switch (eventType) {
                case ON_DESTROY:
                    interpreter.detach();
                    interpreter.stopInterpreter();
                    ((ActivityLifecycleEventListener) windowFragment).onDestroy(); break;
                case ON_PAUSE:
                    ((ActivityLifecycleEventListener) windowFragment).onPause(); break;
                case ON_RESUME:
                    ((ActivityLifecycleEventListener) windowFragment).onResume(); break;
            }
        }
        
    }
}
