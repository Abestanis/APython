package com.apython.python.pythonhost.views.sdl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

/**
 * SDLSurface view taken from the SDL sample project and modified to work with this project.
 *
 * Created by Sebastian on 20.11.2015.
 */
class SDLSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
        View.OnKeyListener, View.OnTouchListener, SensorEventListener {
    private static final String TAG = "SDLSurfaceView";

    // Sensors
    private SensorManager sensorManager;
    private Display       display;

    // Keep track of the surface size to normalize touch events
    private float width, height;
    private static int displayWidth = -1, displayHeight = -1;

    SDLWindowFragment sdlWindow;
    private SDLServer sdlServer;
    private final Object  surfaceCreationNotifier = new Object();
    private       boolean isSurfaceReady          = false;
    private       boolean surfaceWasDestroyed     = false;

    private static class SDLPixelFormat {
        static final int RGB565   = 0x15151002;
        static final int RGB332   = 0x14110801;
        static final int RGB888   = 0x16161804;
        static final int RGBA4444 = 0x15421002;
        static final int RGBA5551 = 0x15441002;
        static final int RGBA8888 = 0x16462004;
        static final int RGBX8888 = 0x16261804;
    }

    public SDLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public SDLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SDLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        setOnKeyListener(this);
        setOnTouchListener(this);

        WindowManager windowManager = (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        display = windowManager == null ? null : windowManager.getDefaultDisplay();
        if (!isInEditMode()) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        // Some arbitrary defaults to avoid a potential division by zero
        width = 1.0f;
        height = 1.0f;
    }

    public void handlePause() {
        enableSensor(Sensor.TYPE_ACCELEROMETER, false);
    }

    public void handleResume() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        setOnKeyListener(this);
        setOnTouchListener(this);
        enableSensor(Sensor.TYPE_ACCELEROMETER, true);
    }

    public Surface getNativeSurface() {
        return getHolder().getSurface();
    }

    // Called when we have a valid drawing surface
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated()");
        isSurfaceReady = true;
        synchronized (surfaceCreationNotifier) {
            surfaceCreationNotifier.notifyAll();
        }
        if (hasFocus()) {
            sdlWindow.onNativeWindowFocusChanged(true);
        }
        //noinspection deprecation
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    // Called when we lose the surface
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed()");
        if (isSurfaceReady) {
            sdlWindow.onNativeHideWindow();
            surfaceWasDestroyed = true;
        }
        isSurfaceReady = false;
        sdlWindow.onNativeSurfaceDestroyed();
    }

    // Called when the surface is resized
    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height) {
        Log.v(TAG, "surfaceChanged()");

        int sdlFormat = SDLPixelFormat.RGB565;
        switch (format) {
        //noinspection deprecation
        case PixelFormat.A_8:
            Log.v(TAG, "pixel format A_8");
            break;
        //noinspection deprecation
        case PixelFormat.LA_88:
            Log.v(TAG, "pixel format LA_88");
            break;
        //noinspection deprecation
        case PixelFormat.L_8:
            Log.v(TAG, "pixel format L_8");
            break;
        //noinspection deprecation
        case PixelFormat.RGBA_4444:
            Log.v(TAG, "pixel format RGBA_4444");
            sdlFormat = SDLPixelFormat.RGBA4444;
            break;
        //noinspection deprecation
        case PixelFormat.RGBA_5551:
            Log.v(TAG, "pixel format RGBA_5551");
            sdlFormat = SDLPixelFormat.RGBA5551;
            break;
        case PixelFormat.RGBA_8888:
            Log.v(TAG, "pixel format RGBA_8888");
            sdlFormat = SDLPixelFormat.RGBA8888;
            break;
        case PixelFormat.RGBX_8888:
            Log.v(TAG, "pixel format RGBX_8888");
            sdlFormat = SDLPixelFormat.RGBX8888;
            break;
        //noinspection deprecation
        case PixelFormat.RGB_332:
            Log.v(TAG, "pixel format RGB_332");
            sdlFormat = SDLPixelFormat.RGB332;
            break;
        case PixelFormat.RGB_565:
            Log.v(TAG, "pixel format RGB_565");
            sdlFormat = SDLPixelFormat.RGB565;
            break;
        case PixelFormat.RGB_888:
            Log.v(TAG, "pixel format RGB_888");
            // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
            sdlFormat = SDLPixelFormat.RGB888;
            break;
        default:
            Log.v(TAG, "pixel format unknown " + format);
            break;
        }
        updateDisplaySize(display);

        this.width = width;
        this.height = height;
        sdlWindow.onNativeResize(width, height, sdlFormat);
        Log.v(TAG, "Window size: " + width + "x" + height);
 
        boolean skip = false;
        int requestedOrientation = sdlServer.getActivity().getRequestedOrientation();
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
            if (width > height) {
               skip = true;
            }
        } else if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            if (width < height) {
               skip = true;
            }
        } /* else: Accept any*/

        // Special Patch for Square Resolution: Black Berry Passport
        if (skip) {
           double min = Math.min(width, height);
           double max = Math.max(width, height);
           
           if (max / min < 1.20) {
              Log.v(TAG, "Don't skip on such aspect-ratio. Could be a square resolution.");
              skip = false;
           }
        }

        if (skip) {
           Log.v(TAG, "Skip .. Surface is not ready.");
           return;
        }
        
        if (surfaceWasDestroyed) {
            surfaceWasDestroyed = false;
            sdlWindow.onNativeRestoreWindow();
        }
        isSurfaceReady = true;
    }

    // Key events
    @Override
    public boolean onKey(View  v, int keyCode, KeyEvent event) {
        // Dispatch the different events depending on where they come from
        // Some SOURCE_JOYSTICK, SOURCE_DPAD or SOURCE_GAMEPAD are also SOURCE_KEYBOARD
        // So, we try to process them as JOYSTICK/DPAD/GAMEPAD events first,
        // if that fails we try them as KEYBOARD.
        //
        // Furthermore, it's possible a game controller has SOURCE_KEYBOARD and
        // SOURCE_JOYSTICK, while its key events arrive from the keyboard source
        // So, retrieve the device itself and check all of its sources
        if (SDLJoystickHandler.isDeviceSDLJoystick(event.getDeviceId())) {
            // Note that we process events with specific key codes here
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (SDLControllerManager.onNativePadDown(event.getDeviceId(), keyCode) == 0) {
                    return true;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (SDLControllerManager.onNativePadUp(event.getDeviceId(), keyCode) == 0) {
                    return true;
                }
            }
        }
        int source = event.getSource();
        if ((source & InputDevice.SOURCE_KEYBOARD) != 0) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                sdlWindow.onNativeKeyDown(keyCode);
                return true;
            }
            else if (event.getAction() == KeyEvent.ACTION_UP) {
                sdlWindow.onNativeKeyUp(keyCode);
                return true;
            }
        }

        if ((source & InputDevice.SOURCE_MOUSE) != 0) {
            // on some devices key events are sent for mouse BUTTON_BACK/FORWARD presses
            // they are ignored here because sending them as mouse input to SDL is messy
            if ((keyCode == KeyEvent.KEYCODE_BACK) || (keyCode == KeyEvent.KEYCODE_FORWARD)) {
                switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                case KeyEvent.ACTION_UP:
                    // mark the event as handled or it will be handled by system
                    // handling KEYCODE_BACK by system will call onBackPressed()
                    return true;
                }
            }
        }

        return false;
    }

    // Touch events
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        /* Ref: http://developer.android.com/training/gestures/multi.html */
        final int touchDevId = event.getDeviceId();
        final int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();
        int pointerFingerId;
        int mouseButton;
        int i = -1;
        float x,y,p;

        if (event.getSource() == InputDevice.SOURCE_MOUSE && sdlServer.separateMouseAndTouch()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                mouseButton = 1; // all mouse buttons are the left button
            } else {
                mouseButton = event.getButtonState();
            }
            sdlWindow.onNativeMouse(mouseButton, action, event.getX(0), event.getY(0));
        } else {
            switch (action) {
            case MotionEvent.ACTION_MOVE:
                for (i = 0; i < pointerCount; i++) {
                    pointerFingerId = event.getPointerId(i);
                    x = event.getX(i) / width;
                    y = event.getY(i) / height;
                    p = event.getPressure(i);
                    if (p > 1.0f) {
                        // may be larger than 1.0f on some devices
                        // see the documentation of getPressure(i)
                        p = 1.0f;
                    }
                    sdlWindow.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_DOWN:
                // Primary pointer up/down, the index is always zero
                i = 0;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Non primary pointer up/down
                if (i == -1) {
                    i = event.getActionIndex();
                }

                pointerFingerId = event.getPointerId(i);
                x = event.getX(i) / width;
                y = event.getY(i) / height;
                p = event.getPressure(i);
                if (p > 1.0f) {
                    // may be larger than 1.0f on some devices
                    // see the documentation of getPressure(i)
                    p = 1.0f;
                }
                sdlWindow.onNativeTouch(touchDevId, pointerFingerId, action, x, y, p);
                break;

            case MotionEvent.ACTION_CANCEL:
                for (i = 0; i < pointerCount; i++) {
                    pointerFingerId = event.getPointerId(i);
                    x = event.getX(i) / width;
                    y = event.getY(i) / height;
                    p = event.getPressure(i);
                    if (p > 1.0f) {
                        // may be larger than 1.0f on some devices
                        // see the documentation of getPressure(i)
                        p = 1.0f;
                    }
                    sdlWindow.onNativeTouch(touchDevId, pointerFingerId, MotionEvent.ACTION_UP, x, y, p);
                }
                break;

            default:
                break;
            }
        }

        return true;
    }

    public Surface waitForSurfaceCreation() {
        synchronized (surfaceCreationNotifier) {
            if (!isSurfaceReady) {
                try {
                    surfaceCreationNotifier.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Was interrupted while waiting for surface creation.", e);
                    return null;
                }
            }
        }
        return getNativeSurface();
    }

    // Sensor events
    public void enableSensor(int sensorType, boolean enabled) {
        // TODO: This uses getDefaultSensor - what if we have >1 accels?
        if (enabled) {
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(sensorType),
                                           SensorManager.SENSOR_DELAY_GAME, null);
        } else {
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(sensorType));
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void updateDisplaySize(Display display) {
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        } else {
            // noinspection deprecation
            size.x = display.getWidth();
            // noinspection deprecation
            size.y = display.getHeight();
        }
        boolean changed = false;
        if (size.x != displayWidth) {
            displayWidth = size.x;
            changed = true;
        }
        if (size.y != displayHeight) {
            displayHeight = size.y;
            changed = true;
        }
        if (changed) {
            SDLServer.nativeDisplayResize(displayWidth, displayHeight, display.getRefreshRate());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x, y;
            switch (display.getRotation()) {
            case Surface.ROTATION_90:
                x = -event.values[1];
                y = event.values[0];
                break;
            case Surface.ROTATION_270:
                x = event.values[1];
                y = -event.values[0];
                break;
            case Surface.ROTATION_180:
                x = -event.values[1];
                y = -event.values[0];
                break;
            case Surface.ROTATION_0:
            default:
                x = event.values[0];
                y = event.values[1];
                break;
            }
            sdlWindow.onNativeAccel(-x / SensorManager.GRAVITY_EARTH,
                                    y / SensorManager.GRAVITY_EARTH,
                                    event.values[2] / SensorManager.GRAVITY_EARTH);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "OnKeyDown");
        sdlWindow.onNativeKeyDown(keyCode);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        sdlWindow.onNativeKeyUp(keyCode);
        return true;
    }

    public void setSDLWindow(SDLWindowFragment sdlWindow) {
        this.sdlWindow = sdlWindow;
    }

    @SuppressLint("ObsoleteSdkInt")
    public void setSDLServer(SDLServer sdlServer) {
        this.sdlServer = sdlServer;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            setOnGenericMotionListener(new SDLGenericMotionListener(sdlServer));
        }
    }

//    public void setPixelFormat(int pixelFormat) {
//        switch (pixelFormat) {
//            case SDLPixelFormat.RGB332:
//                getHolder().setFormat(PixelFormat.RGB_332);
//                break;
//            case SDLPixelFormat.RGB565:
//                getHolder().setFormat(PixelFormat.RGB_565);
//                break;
//            case SDLPixelFormat.RGB888:
//                getHolder().setFormat(PixelFormat.RGB_888);
//                break;
//            case SDLPixelFormat.RGBA4444:
//                getHolder().setFormat(PixelFormat.RGBA_4444);
//                break;
//            case SDLPixelFormat.RGBA5551:
//                getHolder().setFormat(PixelFormat.RGBA_5551);
//                break;
//            case SDLPixelFormat.RGBA8888:
//                getHolder().setFormat(PixelFormat.RGBA_8888);
//                break;
//            case SDLPixelFormat.RGBX8888:
//                getHolder().setFormat(PixelFormat.RGBX_8888);
//                break;
//            default:
//                Log.e(TAG, "Got unknown pixel format: " + pixelFormat);
//                return;
//        }
//        displayPixelFormat = pixelFormat;
//    }
}
