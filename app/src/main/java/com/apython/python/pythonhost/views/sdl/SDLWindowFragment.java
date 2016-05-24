package com.apython.python.pythonhost.views.sdl;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;

import java.io.File;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * A fragment that contains an SDL window and serves as the main
 * interface for the native SDL library.
 * 
 * Created by Sebastian on 21.11.2015.
 */
public class SDLWindowFragment extends Fragment implements SDLWindowInterface {

    public static final String TAG = "SDL";

    private static Context staticContext = null;

    private SDLSurfaceView surface;
    private RelativeLayout layout = null;
    private FrameLayout  wrapperLayout;
    private SDLInputView inputEditText;

    // Used from jni
    private long nativeWindowId;

    // Main components
    private static SDLJoystickHandler joystickHandler = new SDLJoystickHandler();

    // Audio
    private SDLAudioHandler audioHandler = null;

    // Handler for the messages
    Handler commandHandler = null;

    private static WindowManager windowManager = null;

    private InputMethodManager inputMethodManager;

    public static String[] getSDLLibraries() {
        return new String[] {
                "SDL2",
                "SDL2_ttf",
        };
    }

    public static boolean initLibraries(Context context) {
        boolean sdl2Avaliable = true;
        if (staticContext == null) {
            // This only needs to be done once.
            if (loadLibraries(context)) {
                SDLSurfaceView.updateDisplaySize(((android.view.WindowManager)
                        context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
                nativeInit();
            } else {
                sdl2Avaliable = false;
            }
        }
        staticContext = context;
        return sdl2Avaliable;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Device: " + Build.DEVICE);
        Log.v(TAG, "Model: " + Build.MODEL);
        Log.v(TAG, "onCreate():" + this);
        this.commandHandler = new SDLCommandHandler(this);
        this.audioHandler = new SDLAudioHandler();
        this.inputMethodManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        initLibraries(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (wrapperLayout != null) wrapperLayout.removeView(layout);
        wrapperLayout = new FrameLayout(getActivity().getApplicationContext());
        if (surface == null) {
            layout = (RelativeLayout) inflater.inflate(R.layout.view_sdl_layout, container, false);
            surface = (SDLSurfaceView) layout.findViewById(R.id.sdl_surface);
            surface.setSDLWindow(this);
            layout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Log.v(TAG, "onWindowFocusChanged(): " + hasFocus);
                    onNativeWindowFocusChanged(hasFocus);
                }
            });
            if (Build.VERSION.SDK_INT >= 12) {
                surface.setOnGenericMotionListener(new View.OnGenericMotionListener() {
                    @Override
                    public boolean onGenericMotion(View v, MotionEvent event) {
                        return handleJoystickMotionEvent(event);
                    }
                });
            }
        }
        wrapperLayout.addView(layout);
        return wrapperLayout;
    }

    // Events
    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
        if (windowManager == null) {
            nativePause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        if (windowManager == null) {
            nativeResume();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.v(TAG, "onLowMemory()");
        if (windowManager == null) {
            nativeLowMemory();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        if (windowManager == null) {
            // Send a quit message to the application
            nativeQuit();
        }
        resetState();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int KEYCODE_ZOOM_IN, KEYCODE_ZOOM_OUT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            KEYCODE_ZOOM_IN  = KeyEvent.KEYCODE_ZOOM_IN;
            KEYCODE_ZOOM_OUT = KeyEvent.KEYCODE_ZOOM_OUT;
        } else {
            KEYCODE_ZOOM_IN  = 168;
            KEYCODE_ZOOM_OUT = 169;
        }
        int keyCode = event.getKeyCode();
        // Ignore certain special keys so they're handled by Android
        return keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KEYCODE_ZOOM_IN ||
                keyCode == KEYCODE_ZOOM_OUT;
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private static boolean loadLibraries(Context context) {
        try {
            System.load(new File(PackageManager.getDynamicLibraryPath(context),
                                 System.mapLibraryName("pythonPatch")).getAbsolutePath());
            for (String library : getSDLLibraries()) {
                System.load(new File(PackageManager.getDynamicLibraryPath(context),
                                     System.mapLibraryName(library)).getAbsolutePath());
            }
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
        return true;
    }

    private void resetState() {
        audioHandler.audioQuit();
    }

    public static void setWindowManager(WindowManager wMgr) {
        windowManager = wMgr;
        windowManager.setActivityEventsListener(new WindowManager.ActivityEventsListener() {
            @Override
            public void onPause() {
                nativePause();
            }

            @Override
            public void onResume() {
                nativeResume();
            }

            @Override
            public void onLowMemory() {
                nativeLowMemory();
            }

            @Override
            public void onDestroy() {
                nativeQuit();
            }
        });
    }

    /* The native thread has finished */
    public void handleNativeExit() {
        destroy();
    }


    // Messages from the SDLMain thread
    static final int COMMAND_CHANGE_TITLE = 1;
    static final int COMMAND_UNUSED = 2;
    static final int COMMAND_TEXTEDIT_HIDE = 3;

    protected static final int COMMAND_USER = 0x8000;

    /**
     * This method is called by SDL if SDL did not handle a message itself.
     * This happens if a received message contains an unsupported command.
     * Method can be overwritten to handle Messages in a different class.
     * @param command the command of the message.
     * @param param the parameter of the message. May be null.
     * @return if the message was handled in overridden method.
     */
    protected boolean onUnhandledMessage(int command, Object param) {
        return false;
    }

    public SDLSurfaceView getSurface() {
        return surface;
    }

    /**
     * A Handler class for Messages from native SDL applications.
     * It uses current Activities as target (e.g. for the title).
     * static to prevent implicit references to enclosing object.
     */
    protected static class SDLCommandHandler extends Handler {
        SDLWindowFragment sdlWindow;

        public SDLCommandHandler(SDLWindowFragment sdlWindow) {
            this.sdlWindow = sdlWindow;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
            case COMMAND_CHANGE_TITLE:
                sdlWindow.changeWindowTitle((String) msg.obj);
                break;
            case COMMAND_TEXTEDIT_HIDE:
                sdlWindow.hideInput();
                break;
            default:
                if (!sdlWindow.onUnhandledMessage(msg.arg1, msg.obj)) {
                    Log.w(TAG, "Ignoring unknown message, command is " + msg.arg1);
                }
            }
        }
    }

    private void changeWindowTitle(String title) {
        if (windowManager == null) {
            getActivity().setTitle(title);
        } else {
            windowManager.setWindowName(this, title);
        }
    }

    // Send a message from the SDLMain thread
    boolean sendCommand(int command, Object data) {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        return commandHandler.sendMessage(msg);
    }

    public void flipBuffers() {
        nativeFlipBuffers();
    }

    public boolean setWindowTitle(String title) {
        // Called from native thread and can't directly affect the view
        return sendCommand(COMMAND_CHANGE_TITLE, title);
    }

    public boolean sendMessage(int command, int param) {
        return sendCommand(command, param);
    }

    public static Context getStaticContextContext() {
        return staticContext;
    }

    /**
     * @return result of getSystemService(name) but executed on UI thread.
     */
    public Object getSystemServiceFromUiThread(final String name) {
        final Object lock = new Object();
        final Object[] results = new Object[2]; // array for writable variables
        synchronized (lock) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        results[0] = getActivity().getSystemService(name);
                        results[1] = Boolean.TRUE;
                        lock.notify();
                    }
                }
            });
            if (results[1] == null) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return results[0];
    }

    public boolean showTextInput(final int x, final int y, final int w, final int h) {
        // Transfer the task to the main thread as a Runnable
        return commandHandler.post(new Runnable() {
            @Override
            public void run() {
                showInput(x, y, w, h);
            }
        });
    }

    public Surface getNativeSurface() {
        return surface.waitForSurfaceCreation();
    }

    // Audio
    public int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        return audioHandler.audioInit(sampleRate, is16Bit, isStereo, desiredFrames);
    }

    public void audioWriteShortBuffer(short[] buffer) {
        audioHandler.audioWriteShortBuffer(buffer);
    }

    public void audioWriteByteBuffer(byte[] buffer) {
        audioHandler.audioWriteByteBuffer(buffer);
    }

    public void audioQuit() {
        audioHandler.audioQuit();
    }

    // Input

    /**
     * @return an array which may be empty but is never null.
     */
    public static int[] inputGetInputDeviceIds(int sources) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            int[] ids = InputDevice.getDeviceIds();
            int[] filtered = new int[ids.length];
            int used = 0;
            for (int id : ids) {
                InputDevice device = InputDevice.getDevice(id);
                if ((device != null) && ((device.getSources() & sources) != 0)) {
                    filtered[used++] = device.getId();
                }
            }
            return Arrays.copyOf(filtered, used);
        } else {
            return new int[] {};
        }
    }

    public void showInput(int x, int y, int w, int h) {
        final int HEIGHT_PADDING = 15;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(w, h + HEIGHT_PADDING);
        params.leftMargin = x;
        params.topMargin = y;

        if (inputEditText == null) {
            if (getActivity() == null) return;
            inputEditText = new SDLInputView(getActivity().getApplicationContext());
            inputEditText.setSDLWindow(this);
            layout.addView(inputEditText, params);
        } else {
            inputEditText.setLayoutParams(params);
        }

        inputEditText.setVisibility(View.VISIBLE);
        inputEditText.requestFocus();

        inputMethodManager.showSoftInput(inputEditText, 0);
        if (!inputMethodManager.isActive(inputEditText)) {
            this.inputMethodManager.restartInput(inputEditText);
        }
    }

    public void hideInput() {
        if (inputEditText != null) {
            inputEditText.setVisibility(View.GONE);
            inputMethodManager.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
        }
    }

    // Joystick glue code, just a series of stubs that redirect to the SDLJoystickHandler instance
    public boolean handleJoystickMotionEvent(MotionEvent event) {
        return joystickHandler.handleMotionEvent(event);
    }

    public static void pollInputDevices() {
        joystickHandler.pollInputDevices();
    }

    public void setPixelFormat(int pixelFormat) {
        //        surface.setPixelFormat(pixelFormat);
    }

    public static Object createWindow() {
        if (windowManager == null) {
            return null;
        }
        return windowManager.createWindow();
    }

    private void destroy() {
        commandHandler.post(new Runnable() {
            @Override
            public void run() {
                if (windowManager == null) {
                    return;
                }
                windowManager.destroyWindow(SDLWindowFragment.this);
            }
        });
    }
    
    public void setWindowIcon(int[] iconData, int width, int height) {
        Drawable icon = null;
        if (iconData != null) {
            Bitmap iconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < iconData.length; ++i) { // TODO: Why do we have to do this?!!!
                // The alpha and green channels' positions are preserved while the red and blue are swapped
                iconData[i] = ((iconData[i] & 0xff00ff00)) | ((iconData[i] & 0x000000ff) << 16) | ((iconData[i] & 0x00ff0000) >> 16);
            }
            iconBitmap.copyPixelsFromBuffer(IntBuffer.wrap(iconData));
            icon = new BitmapDrawable(getContext().getResources(), iconBitmap);
        }
        final Drawable finalIcon = icon;
        commandHandler.post(new Runnable() {
            @Override
            public void run() {
                if (windowManager == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        ActionBar actionBar = getActivity().getActionBar();
                        if (actionBar != null) {
                            actionBar.setIcon(finalIcon);
                        }
                    }
                } else {
                    windowManager.setWindowIcon(SDLWindowFragment.this, finalIcon);
                }
            }
        });
    }

    // C functions we call
    public native static void nativeInit();

    public native static void nativeLowMemory();

    public native static void nativeQuit();

    public native static void nativePause();

    public native static void nativeResume();
    
    public native static void nativeDisplayResize(int screenWidth, int screenHeight);

    public native void onNativeResize(int w, int h, int format);

    public native void onNativeHideWindow();

    public native void onNativeRestoreWindow();

    public native void onNativeWindowFocusChanged(boolean hasFocus);

    public native int onNativePadDown(int device_id, int keycode);

    public native int onNativePadUp(int device_id, int keycode);

    public native static void onNativeJoy(int device_id, int axis,
                                          float value);

    public native static void onNativeHat(int device_id, int hat_id,
                                          int x, int y);

    public native void onNativeKeyDown(int keycode);

    public native void onNativeKeyUp(int keycode);

    public native void onNativeKeyboardFocusLost();

    public native void onNativeTouch(int touchDevId, int pointerFingerId,
                                     int action, float x,
                                     float y, float p);

    public native void onNativeAccel(float x, float y, float z);

    public native void onNativeSurfaceChanged();
    public native void onNativeSurfaceDestroyed();
    public native void nativeFlipBuffers();
    public native static int nativeAddJoystick(int device_id, String name,
                                               int is_accelerometer, int nbuttons,
                                               int naxes, int nhats, int nballs);
    public native static int nativeRemoveJoystick(int device_id);
    public native void nativeCommitText(String text, int newCursorPosition);
    public native void nativeSetComposingText(String text, int newCursorPosition);
}
