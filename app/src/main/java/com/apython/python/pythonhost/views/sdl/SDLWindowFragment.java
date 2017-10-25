package com.apython.python.pythonhost.views.sdl;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
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

import com.apython.python.pythonhost.CalledByNative;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.views.ActivityLifecycleEventListener;
import com.apython.python.pythonhost.views.PythonFragment;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * A fragment that contains an SDL window and serves as the main
 * interface for the native SDL library.
 * 
 * Created by Sebastian on 21.11.2015.
 */
public class SDLWindowFragment extends PythonFragment implements
        SDLWindowInterface, ActivityLifecycleEventListener {

    public static final String TAG = "SDL";

    private SDLSurfaceView surface;
    private RelativeLayout layout = null;
    private FrameLayout  wrapperLayout;
    private SDLInputView inputEditText;

    // Used from jni
    private long nativeWindowId;
    
    // Main components
    static final SDLJoystickHandler joystickHandler = new SDLJoystickHandler();

    private final InputMethodManager inputMethodManager;

    public SDLWindowFragment(Activity activity, String tag) {
        super(activity, tag);
        nativeWindowId = -1;
        Log.v(TAG, "Device: " + Build.DEVICE);
        Log.v(TAG, "Model: " + Build.MODEL);
        Log.v(TAG, "onCreate():" + this);
        this.inputMethodManager = (InputMethodManager) getActivity().getApplicationContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public View createView(ViewGroup container) {
        if (wrapperLayout != null) wrapperLayout.removeView(layout);
        Context context = getActivity().getBaseContext();
        wrapperLayout = new FrameLayout(context);
        if (surface == null) {
            layout = (RelativeLayout) LayoutInflater.from(context)
                    .inflate(context.getResources().getLayout(R.layout.view_sdl_layout), container, false);
            surface = (SDLSurfaceView) layout.findViewById(R.id.sdl_surface);
            surface.setSDLWindow(this);
            layout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Log.v(TAG, "onWindowFocusChanged(): " + hasFocus);
                    onNativeWindowFocusChanged(hasFocus);
                }
            });
        }
        wrapperLayout.addView(layout);
        return wrapperLayout;
    }
    
    @Override
    public void onPause() {
        Log.v(TAG, "onPause()");
        SDLLibraryHandler.onActivityPause();
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        SDLLibraryHandler.onActivityResume();
    }

    @Override
    public void onLowMemory() {
        Log.v(TAG, "onLowMemory()");
        SDLLibraryHandler.onActivityLowMemory();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        // Send a quit message to the application
        SDLLibraryHandler.onActivityDestroyed();
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

    @Override
    public void close() {
        Log.d(TAG, "Got window close request for window " + this);
        nativeOnWindowClose();
    }

    @Override
    public String toString() {
        return super.toString() + " (Window " + nativeWindowId + ")";
    }

    /* The native thread has finished */
    public void handleNativeExit() {
        destroy();
    }

    public SDLSurfaceView getSurface() {
        return surface;
    }

    @CalledByNative
    public boolean setWindowTitle(final String title) {
        // Called from native thread and can't directly affect the view
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (SDLLibraryHandler.getWindowManager() == null) {
                    getActivity().setTitle(title);
                } else {
                    SDLLibraryHandler.getWindowManager().setWindowName(SDLWindowFragment.this, title);
                }
            }
        });
        return true;
    }

    /**
     * This method is called by SDL using JNI.
     * @return result of getSystemService(name) but executed on UI thread.
     */
    @CalledByNative
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

    @CalledByNative
    public boolean showTextInput(final int x, final int y, final int w, final int h) {
        // Transfer the task to the main thread as a Runnable
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showInput(x, y, w, h);
            }
        });
        return true;
    }
    
    @CalledByNative
    public boolean hideTextInput() {
        // Transfer the task to the main thread as a Runnable
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideInput();
            }
        });
        return true;
    }

    @CalledByNative
    public Surface getNativeSurface() {
        return surface.waitForSurfaceCreation();
    }

    // Input

    /**
     * @return an array which may be empty but is never null.
     */
    @CalledByNative
    public static int[] inputGetInputDeviceIds(int sources) {
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
    }

    private void showInput(int x, int y, int w, int h) {
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

    private void hideInput() {
        if (inputEditText != null) {
            // Note: On some devices setting view to GONE creates a flicker in landscape.
            // Setting the View's sizes to 0 is similar to GONE but without the flicker.
            // The sizes will be set to useful values when the keyboard is shown again.
            inputEditText.setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
            inputMethodManager.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
        }
    }

    // Joystick glue code, just a series of stubs that redirect to the SDLJoystickHandler instance
    @CalledByNative
    public boolean handleJoystickMotionEvent(MotionEvent event) {
        return joystickHandler.handleMotionEvent(event);
    }

    @CalledByNative
    public static void pollInputDevices() {
        joystickHandler.pollInputDevices();
    }

    @CalledByNative
    public void setPixelFormat(int pixelFormat) {
        //        surface.setPixelFormat(pixelFormat);
    }

    // APK expansion files support

    /** com.android.vending.expansion.zipfile.ZipResourceFile object or null. */
    private static Object expansionFile;

    /** com.android.vending.expansion.zipfile.ZipResourceFile's getInputStream() or null. */
    private static Method expansionFileMethod;

    /**
     * This method is called by SDL using JNI.
     * @return an InputStream on success or null if no expansion file was used.
     * @throws IOException on errors. Message is set for the SDL error message.
     */
    @CalledByNative
    public static InputStream openAPKExpansionInputStream(String fileName) throws IOException {
        // Get a ZipResourceFile representing a merger of both the main and patch files
        if (expansionFile == null) {
            String mainHint = nativeGetHint("SDL_ANDROID_APK_EXPANSION_MAIN_FILE_VERSION");
            if (mainHint == null) {
                return null; // no expansion use if no main version was set
            }
            String patchHint = nativeGetHint("SDL_ANDROID_APK_EXPANSION_PATCH_FILE_VERSION");
            if (patchHint == null) {
                return null; // no expansion use if no patch version was set
            }

            Integer mainVersion;
            Integer patchVersion;
            try {
                mainVersion = Integer.valueOf(mainHint);
                patchVersion = Integer.valueOf(patchHint);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                throw new IOException("No valid file versions set for APK expansion files", ex);
            }

            try {
                // To avoid direct dependency on Google APK expansion library that is
                // not a part of Android SDK we access it using reflection
                expansionFile = Class.forName("com.android.vending.expansion.zipfile.APKExpansionSupport")
                        .getMethod("getAPKExpansionZipFile", Context.class, int.class, int.class)
                        .invoke(null, SDLLibraryHandler.getStaticContext(), mainVersion, patchVersion);

                expansionFileMethod = expansionFile.getClass()
                        .getMethod("getInputStream", String.class);
            } catch (Exception ex) {
                ex.printStackTrace();
                expansionFile = null;
                expansionFileMethod = null;
                throw new IOException("Could not access APK expansion support library", ex);
            }
        }

        // Get an input stream for a known file inside the expansion file ZIPs
        InputStream fileStream;
        try {
            fileStream = (InputStream)expansionFileMethod.invoke(expansionFile, fileName);
        } catch (Exception ex) {
            // calling "getInputStream" failed
            ex.printStackTrace();
            throw new IOException("Could not open stream from APK expansion file", ex);
        }

        if (fileStream == null) {
            // calling "getInputStream" was successful but null was returned
            throw new IOException("Could not find path in APK expansion file");
        }

        return fileStream;
    }

    // Messagebox

    /**
     * This method is called by SDL using JNI.
     * Shows the messagebox from UI thread and block calling thread.
     * buttonFlags, buttonIds and buttonTexts must have same length.
     * @param buttonFlags array containing flags for every button.
     * @param buttonIds array containing id for every button.
     * @param buttonTexts array containing text for every button.
     * @param colors null for default or array of length 5 containing colors.
     * @return button id or -1.
     */
    @CalledByNative
    public static int showMessageBox(
            final int flags,
            final String title,
            final String message,
            final int[] buttonFlags,
            final int[] buttonIds,
            final String[] buttonTexts,
            final int[] colors) {
        return new SDLMessageBox(SDLLibraryHandler.staticActivity, flags, title, message,
                                 buttonFlags, buttonIds, buttonTexts, colors).showAndWait();
    }

    @CalledByNative
    public static Object createWindow() {
        if (SDLLibraryHandler.getWindowManager() == null) {
            return null;
        }
        return SDLLibraryHandler.getWindowManager().createWindow(SDLWindowFragment.class);
    }

    private void destroy() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (SDLLibraryHandler.getWindowManager() == null) {
                    return;
                }
                SDLLibraryHandler.getWindowManager().destroyWindow(SDLWindowFragment.this);
            }
        });
    }

    @CalledByNative
    public void setWindowIcon(int[] iconData, int width, int height) {
        Drawable icon = null;
        if (iconData != null) {
            Bitmap iconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < iconData.length; ++i) { // TODO: Why do we have to do this?!!!
                // The alpha and green channels' positions are preserved while the red and blue are swapped
                iconData[i] = ((iconData[i] & 0xff00ff00)) | ((iconData[i] & 0x000000ff) << 16) | ((iconData[i] & 0x00ff0000) >> 16);
            }
            iconBitmap.copyPixelsFromBuffer(IntBuffer.wrap(iconData));
            icon = new BitmapDrawable(getActivity().getResources(), iconBitmap);
        }
        final Drawable finalIcon = icon;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (SDLLibraryHandler.getWindowManager() == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        ActionBar actionBar = getActivity().getActionBar();
                        if (actionBar != null) {
                            actionBar.setIcon(finalIcon);
                        }
                    }
                } else {
                    SDLLibraryHandler.getWindowManager().setWindowIcon(SDLWindowFragment.this, finalIcon);
                }
            }
        });
    }

    // C functions we call
    public native void onNativeResize(int w, int h, int format);
    public native void onNativeHideWindow();
    public native void onNativeRestoreWindow();
    public native void onNativeWindowFocusChanged(boolean hasFocus);
    public native int onNativePadDown(int device_id, int keycode);
    public native int onNativePadUp(int device_id, int keycode);
    public native static void onNativeJoy(int device_id, int axis, float value);
    public native static void onNativeHat(int device_id, int hat_id, int x, int y);
    public native void onNativeKeyDown(int keycode);
    public native void onNativeKeyUp(int keycode);
    public native void onNativeKeyboardFocusLost();
    public native void onNativeMouse(int button, int action, float x, float y);
    public native void onNativeTouch(int touchDevId, int pointerFingerId, int action, float x,
                                     float y, float p);
    public native void onNativeAccel(float x, float y, float z);
    public native void onNativeSurfaceDestroyed();
    public native static int nativeAddJoystick(int device_id, String name,
                                               int is_accelerometer, int nbuttons,
                                               int naxes, int nhats, int nballs);
    public native static int nativeRemoveJoystick(int device_id);
    public native void nativeCommitText(String text, int newCursorPosition);
    public native void nativeSetComposingText(String text, int newCursorPosition);
    public static native String nativeGetHint(String name);
    public static native void onNativeDropFile(String filename); // TODO: Support dropping on a per fragment basis
    public native void nativeOnWindowClose();
}
