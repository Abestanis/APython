package com.apython.python.pythonhost.views.sdl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;

import java.io.File;

/**
 * Handler for the native shared SDL library.
 * There is only one handler per process.
 * 
 * Created by Sebastian on 11.11.2016.
 */
public class SDLLibraryHandler {
    
    static Activity staticActivity; // TODO: Store this reference in native code and  release it in onDestroy
    private static boolean libraryIsLoaded = false;
    private static WindowManagerInterface windowManager;
    private static Handler                commandHandler;

    /**
     * If we want to separate mouse and touch events.
     * This is only toggled in native code when a hint is set!
     **/
    static         boolean separateMouseAndTouch;
    private static Error   libraryLoadingError;


    private static String[] getSDLLibraries() {
        return new String[] {
                "SDL2",
                "SDL2_ttf",
        };
    }

    public static boolean initLibraries(Activity activity, WindowManagerInterface windowManager) {
        SDLLibraryHandler.windowManager = windowManager;
        staticActivity = activity;
        if (!libraryIsLoaded) {
            // This only needs to be done once.
            if (loadLibraries(activity)) {
                SDLSurfaceView.updateDisplaySize(((android.view.WindowManager)
                        activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
                nativeInitLibrary(SDLWindowFragment.class);
                libraryIsLoaded = true;
            }
        }
        return libraryIsLoaded;
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
        } catch (Error e) {
            libraryLoadingError = e;
            return false;
        }
        return true;
    }

    public static WindowManagerInterface getWindowManager() {
        return windowManager;
    }

    static Context getStaticContext() {
        return staticActivity;
    }

    public static void onActivityPause() {
        if (!libraryIsLoaded) return;
        nativePause();
    }
    
    public static void onActivityDestroyed() {
        if (!libraryIsLoaded) return;
        nativeQuit();
    }
    
    public static void onActivityResume() {
        if (!libraryIsLoaded) return;
        nativeResume();
    }

    public static void onActivityLowMemory() {
        if (!libraryIsLoaded) return;
        nativeLowMemory();
    }
    
    public static void setCommandHandler(Handler commandHandler) {
        SDLLibraryHandler.commandHandler = commandHandler;
    }

    static Error getLibraryLoadingError() {
        return libraryLoadingError;
    }

    public static boolean sendMessage(int command, int param) {
        if (commandHandler == null) {
            Log.e(SDLWindowFragment.TAG, "No command handler given to handle message " + command
                    + " (param = " + param + ")");
            return false;
        }
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = param;
        return commandHandler.sendMessage(msg);
    }

    public static void setKeepScreenOn(final boolean value) {
        final Window window = staticActivity.getWindow();
        if (window != null) {
            staticActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (value) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            });
        }
    }

    // 
    private native static void nativeInitLibrary(Class windowFragmentClass);

    private native static void nativeLowMemory();

    private native static void nativeQuit();

    private native static void nativePause();

    private native static void nativeResume();

    native static void nativeDisplayResize(int screenWidth, int screenHeight, float refreshRate);
}