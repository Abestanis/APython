package com.apython.python.pythonhost.views.sdl;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

/**
 * A runnable that calls the native SDL_Main function.
 * 
 * Created by Sebastian on 16.11.2016.
 */
public class SDLMain implements Runnable {

    private String[] sdlArguments = new String[0];
    private final SDLServer sdlServer;

    public SDLMain(SDLServer sdlServer) {
        this.sdlServer = sdlServer;
    }
    
    public SDLMain(SDLServer sdlServer, String[] args) {
        this(sdlServer);
        if (args == null) throw new IllegalArgumentException("null argument list not allowed");
        sdlArguments = args;
    }

    @Override
    public void run() {
        Error brokenLibrariesError = sdlServer.getLibraryLoadingError();
        if (brokenLibrariesError != null) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(sdlServer.getActivity());
            dlgAlert.setMessage("An error occurred while trying to start the application. " +
                                        "Please try again and/or reinstall.\n\nError: " +
                                        brokenLibrariesError.getMessage());
            dlgAlert.setTitle("SDL Error");
            dlgAlert.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // if this button is clicked, close current activity
                    sdlServer.getActivity().finish();
                    dialog.dismiss();
                }
            });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
            return;
        }
        String library = getMainSharedObject();
        String function = getMainFunction();
        String[] arguments = getArguments();

        Log.v("SDL", "Running main function " + function + " from library " + library);
        sdlServer.nativeRunMain(library, function, arguments);

        Log.v("SDL", "Finished main function");
    }
    
    /**
     * This method returns the name of the shared object with the application entry point
     * It can be overridden by derived classes.
     */
    protected String getMainSharedObject() {
        String library;
        String[] libraries = SDLServer.getSDLLibraries();
        if (libraries.length > 0) {
            library = "lib" + libraries[libraries.length - 1] + ".so";
        } else {
            library = "libmain.so";
        }
        return library;
    }

    /**
     * This method returns the name of the application entry point
     * It can be overridden by derived classes.
     */
    protected String getMainFunction() {
        return "SDL_main";
    }

    /**
     * This method is called by SDL before starting the native application thread.
     * It can be overridden to provide the arguments after the application name.
     * The default implementation returns an empty array. It never returns null.
     * @return arguments for the native application.
     */
    protected String[] getArguments() {
        return sdlArguments;
    }
}
