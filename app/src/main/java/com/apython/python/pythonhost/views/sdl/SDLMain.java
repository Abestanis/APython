package com.apython.python.pythonhost.views.sdl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;

/**
 * A runnable that calls the native SDL_Main function.
 * 
 * Created by Sebastian on 16.11.2016.
 */
public class SDLMain implements Runnable {

    private String[] sdlArguments = new String[0];
    private boolean brokenLibraries = false;
    private Activity activity;
    /**
     * The return value of SDLMain. Is {@code null} if SDLMain did not return jet.
     */
    private Integer returnVal = null;

    public SDLMain(Activity activity, WindowManagerInterface windowManager) {
        this.activity = activity;
        brokenLibraries = !SDLLibraryHandler.initLibraries(activity, windowManager);
    }

    public SDLMain(Activity activity) {
        this(activity, null);
    }
    
    public SDLMain(Activity activity, String[] args, WindowManagerInterface windowManager) {
        this(activity, windowManager);
        if (args == null) throw new IllegalArgumentException("null argument list not allowed");
        sdlArguments = args;
    }

    @Override
    public void run() {
        if (brokenLibraries) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(activity);
            dlgAlert.setMessage("An error occurred while trying to start the application. Please try again and/or reinstall."
                                        + System.getProperty("line.separator")
                                        + System.getProperty("line.separator")
                                        + "Error: " + SDLLibraryHandler.getLibraryLoadingError().getMessage());
            dlgAlert.setTitle("SDL Error");
            dlgAlert.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // if this button is clicked, close current activity
                    activity.finish();
                    dialog.dismiss();
                }
            });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();

            return;
        }
        // Get filename from "Open with" of another application
        Intent intent = activity.getIntent();

        if (intent != null && intent.getData() != null) {
            String filename = intent.getData().getPath();
            if (filename != null) {
                Log.v(SDLWindowFragment.TAG, "Got filename: " + filename);
                SDLWindowFragment.onNativeDropFile(filename);
            }
        }
        returnVal = nativeInit(sdlArguments);
    }

    /**
     * Get the int returned from sdlMain. If sdlMain did not return yet, this function
     * returns {@code null}.
     * @return {@code null} or the return value of sdlMain.
     */
    public Integer getReturnValue() {
        return returnVal;
    }
    
    private native static int nativeInit(String[] arguments);
}
