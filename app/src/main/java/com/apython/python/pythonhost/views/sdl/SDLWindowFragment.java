package com.apython.python.pythonhost.views.sdl;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;

import java.nio.IntBuffer;

/**
 * A fragment that contains an SDL window.
 */
public class SDLWindowFragment extends PythonFragment implements
        SDLWindowInterface, ActivityLifecycleEventListener {
    public static final String TAG = "SDL";

    private SDLSurfaceView surface;
    private RelativeLayout layout = null;
    private FrameLayout  wrapperLayout;
    private SDLInputView inputEditText;
    private SDLServer sdlServer;
    
    private long windowId = -1;

    private final InputMethodManager inputMethodManager;

    public SDLWindowFragment(Activity activity, String tag) {
        super(activity, tag);
        inputMethodManager = (InputMethodManager) getActivity().getApplicationContext()
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
    
    public void setSDLServer(SDLServer sdlServer, long windowId) {
        this.sdlServer = sdlServer;
        this.windowId = windowId;
        surface.setSDLServer(sdlServer);
    }
    
    @Override
    public void onPause() {
        Log.v(TAG, "onPause()");
        if (sdlServer.getWindowManager() != null) {
            sdlServer.onActivityPause();
        }
        surface.handlePause();
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        if (sdlServer.getWindowManager() != null) {
            sdlServer.onActivityResume();
        }
        surface.handleResume();
    }

    @Override
    public void onLowMemory() {
        Log.v(TAG, "onLowMemory()");
        sdlServer.onActivityLowMemory();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        if (sdlServer.getWindowManager() != null) {
            sdlServer.onActivityDestroyed();
        }
    }

    @Override
    public Boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        // Ignore certain special keys so they're handled by Android
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_CAMERA ||
            keyCode == KeyEvent.KEYCODE_ZOOM_IN || /* API 11 */
            keyCode == KeyEvent.KEYCODE_ZOOM_OUT /* API 11 */
            ) {
            return false;
        }
        return null;
    }

    @Override
    public void close() {
        Log.d(TAG, "Got window close request for window " + this);
        nativeOnWindowClose();
    }

    @Override
    public String toString() {
        return super.toString() + " (Window " + windowId + ")";
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

    @CalledByNative
    public void setWindowTitle(final String title) {
        // Called from native thread and can't directly affect the view
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sdlServer.getWindowManager() == null) {
                    getActivity().setTitle(title);
                } else {
                    sdlServer.getWindowManager().setWindowName(SDLWindowFragment.this, title);
                }
            }
        });
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

    @CalledByNative
    public void setPixelFormat(int pixelFormat) {
        //        surface.setPixelFormat(pixelFormat);
    }

    @CalledByNative
    private void destroy() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManagerInterface windowManager = sdlServer.getWindowManager();
                if (windowManager != null) {
                    windowManager.destroyWindow(SDLWindowFragment.this);
                }
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
                WindowManagerInterface windowManager = sdlServer.getWindowManager();
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
    public static native void onNativeDropFile(String filename);
    public native void onNativeResize(int w, int h, int format);
    public native void onNativeSurfaceDestroyed();
    public native void onNativeKeyDown(int keycode);
    public native void onNativeKeyUp(int keycode);
    public native void onNativeKeyboardFocusLost();
    public native void onNativeTouch(int touchDevId, int pointerFingerId, int action, float x,
                                     float y, float p);
    public native void onNativeMouse(int button, int action, float x, float y);
    public native void onNativeAccel(float x, float y, float z);
    public native void onNativeWindowFocusChanged(boolean hasFocus);
    public native void onNativeHideWindow();
    public native void onNativeRestoreWindow();
    public native void nativeOnWindowClose();
}
