package com.apython.python.pythonhost.views.sdl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.apython.python.pythonhost.CalledByNative;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

// There is only one server per process.

/**
 * This is the main
 */
public class SDLServer {
    private final static String TAG = "SDLServer"; 
    private final UUID     serverId;
    private       Activity activity;
    private static boolean libraryIsLoaded = false;
    private Handler                commandHandler;
    private Error                  libraryLoadingError;
    
    // Manager
    private final SDLAudioManager audioHandler;
    private final SDLControllerManager controllerManager;
    private final SDLClipboardHandler clipboardHandler;
    private WindowManagerInterface windowManager;
    private final InputMethodManager inputMethodManager;
    
    /**
     * If we want to separate mouse and touch events.
     * This is only toggled in native code when a hint is set!
     **/
    private boolean separateMouseAndTouch = false;

    // APK expansion files support

    /** com.android.vending.expansion.zipfile.ZipResourceFile object or null. */
    private static Object expansionFile;

    /** com.android.vending.expansion.zipfile.ZipResourceFile's getInputStream() or null. */
    private static Method expansionFileMethod;

    public SDLServer(Activity activity, WindowManagerInterface windowManager) {
        this(UUID.randomUUID(), activity, windowManager);
    }

    public SDLServer(UUID serverId, Activity activity, WindowManagerInterface windowManager) {
        super();
        Log.v(TAG, "Device: " + Build.DEVICE);
        Log.v(TAG, "Model: " + Build.MODEL);
        Log.v(TAG, "Initializing SDL server...");
        this.serverId = serverId;
        this.activity = activity;
        this.windowManager = windowManager;
        inputMethodManager = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        audioHandler = new SDLAudioManager();
        controllerManager = new SDLControllerManager(this);
        clipboardHandler = new SDLClipboardHandler(this);
        if (!initLibraries()) {
            if (false) {
                throw getLibraryLoadingError();
            }
            return;
        }
        startServerThread();
        
        // Get filename from "Open with" of another application
        Intent intent = activity.getIntent();
        if (intent != null && intent.getData() != null) {
            String filepath = intent.getData().getPath();
            if (filepath != null) {
                Log.v(TAG, "Got file drop: " + filepath);
                SDLWindowFragment.onNativeDropFile(filepath);
            }
        }
    }

    static String[] getSDLLibraries() {
        return new String[] {
                "SDL2",
                // "SDL2_image",
                // "SDL2_mixer",
                // "SDL2_net",
                "SDL2_ttf",
                // "main"
        };
    }

    private boolean initLibraries() {
        libraryLoadingError = null;
        if (!libraryIsLoaded) {
            // This only needs to be done once.
            libraryIsLoaded = loadLibraries(activity);
        }
        if (libraryIsLoaded) {
            WindowManager windowManager = (WindowManager)
                    activity.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                SDLSurfaceView.updateDisplaySize(windowManager.getDefaultDisplay());
            }
            nativeSetupJNI(audioHandler, controllerManager, SDLWindowFragment.class);
        }
        return libraryIsLoaded;
    }

    private boolean loadLibraries(Context context) {
        try {
            for (String library : getSDLLibraries()) {
                PackageManager.loadDynamicLibrary(context, library);
            }
        } catch (Error e) {
            libraryLoadingError = e;
            return false;
        }
        return true;
    }

    public WindowManagerInterface getWindowManager() {
        return windowManager;
    }

    public void onActivityPause() {
        if (!libraryIsLoaded) return;
        nativePause();
    }

    public void onActivityDestroyed() {
        audioHandler.audioClose();
        audioHandler.captureClose();
        if (!libraryIsLoaded) return;
        nativeQuit();
    }

    public void onActivityResume() {
        if (!libraryIsLoaded) return;
        nativeResume();
    }

    public void onActivityLowMemory() {
        if (!libraryIsLoaded) return;
        nativeLowMemory();
    }

    public void setCommandHandler(Handler commandHandler) {
        this.commandHandler = commandHandler;
    }

    Error getLibraryLoadingError() {
        return libraryLoadingError;
    }
    
    public Thread startMainSDLThread(SDLMain sdlRunnable) {
        if (sdlRunnable == null) { sdlRunnable = new SDLMain(this); }
        final Thread sdlThread = new Thread(sdlRunnable, "SDLThread");
        sdlThread.start();
        // Set up a listener thread to catch when the native thread ends
        Thread sdlListenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sdlThread.join();
                } catch (Exception e) {
                    // Ignore any exception
                } finally {
                    // Native thread has finished
                    getActivity().finish();
                }
            }
        }, "SDLThreadListener");
        sdlListenerThread.start();
        return sdlThread;
    }

    public boolean separateMouseAndTouch() {
        return separateMouseAndTouch;
    }

    public SDLControllerManager getControllerManager() {
        return controllerManager;
    }
    
    UUID getServerId() {
        return serverId;
    }
    
    private void startServerThread() {
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    FileDescriptor clientFd = nativeWaitForSDLClient();
                    if (clientFd == null) { continue; }
                    Log.d(TAG, "Got client connection");
                    // TODO: use select in native
                    new SDLClientHandler(clientFd, SDLServer.this).start();
                }
            }
        }, "SDLMainServerThread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * These methods are called by SDL using JNI.
     */

    @CalledByNative
    public Activity getActivity() {
        return activity;
    }

    @CalledByNative
    public Object createWindow(long windowId) {
        if (windowManager == null) { return null; }
        SDLWindowFragment window = windowManager.createWindow(SDLWindowFragment.class);
        window.setSDLServer(this, windowId);
        return window;
    }

    @CalledByNative
    public boolean sendCommand(int command, Object param) {
        if (commandHandler == null) {
            Log.e(SDLWindowFragment.TAG, "No command handler given to handle command " + command
                    + " (param = " + param + ")");
            return false;
        }
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = param;
        return commandHandler.sendMessage(msg);
    }

    @CalledByNative
    public void setKeepScreenOn(final boolean value) {
        final Window window = activity.getWindow();
        if (window != null) {
            activity.runOnUiThread(new Runnable() {
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
    
    /**
     * Shows the message box from UI thread and block calling thread.
     * buttonFlags, buttonIds and buttonTexts must have same length.
     * 
     * @param buttonFlags Array containing flags for every button.
     * @param buttonIds Array containing id for every button.
     * @param buttonTexts Array containing text for every button.
     * @param colors null for default or array of length 5 containing colors.
     * @return Button id or -1.
     */
    @CalledByNative
    public int showMessageBox(final int flags, final String title, final String message,
                              final int[] buttonFlags, final int[] buttonIds,
                              final String[] buttonTexts, final int[] colors) {
        return new SDLMessageBox(activity, flags, title, message,
                                 buttonFlags, buttonIds, buttonTexts, colors).showAndWait();
    }

    /**
     * Allows to set the screen orientation.  
     */
    @CalledByNative
    public void setOrientation(String hint)
    {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (hint != null && !"".equals(hint)) {
            if (hint.contains("LandscapeRight") && hint.contains("LandscapeLeft")) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            } else if (hint.contains("LandscapeRight")) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            } else if (hint.contains("LandscapeLeft")) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            } else if (hint.contains("Portrait") && hint.contains("PortraitUpsideDown")) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            } else if (hint.contains("PortraitUpsideDown")) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            } else if (hint.contains("Portrait")) {
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
        }
        Log.v(TAG, "setOrientation() orientation=" + orientation + " hint=" + hint);
        if (orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.setRequestedOrientation(orientation);
        }
    }

    @CalledByNative
    public boolean isScreenKeyboardShown() {
        return inputMethodManager != null && inputMethodManager.isAcceptingText();
    }

    /**
     * This method is called by SDL using JNI.
     * @return an InputStream on success or null if no expansion file was used.
     * @throws IOException on errors. Message is set for the SDL error message.
     */
    @CalledByNative
    public InputStream openAPKExpansionInputStream(String fileName) throws IOException {
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
                        .invoke(null, activity, mainVersion, patchVersion);

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
            fileStream = (InputStream) expansionFileMethod.invoke(expansionFile, fileName);
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

    @CalledByNative
    public boolean clipboardHasText() {
        return clipboardHandler.clipboardHasText();
    }

    @CalledByNative
    public String clipboardGetText() {
        return clipboardHandler.clipboardGetText();
    }

    @CalledByNative
    public void clipboardSetText(String string) {
        clipboardHandler.clipboardSetText(string);
    }


    private native void nativeSetupJNI(SDLAudioManager audioManager,
                                       SDLControllerManager controllerManager,
                                       Class<SDLWindowFragment> windowClass);
    public native int nativeRunMain(String library, String function, String[] arguments);
    private native void nativeLowMemory();
    private native void nativeQuit();
    private native void nativePause();
    private native void nativeResume();
    public native String nativeGetHint(String name);
    public native void onNativeClipboardChanged();
    native static void nativeDisplayResize(int screenWidth, int screenHeight, float refreshRate);
    native FileDescriptor nativeWaitForSDLClient();
    native long nativeShareSurfaceWithClient(FileDescriptor fileDescriptor, Surface surface);
    native void nativeRenderThread(long hardwareBuffer, Surface surface);
}
