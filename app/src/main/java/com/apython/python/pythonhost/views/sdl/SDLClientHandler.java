package com.apython.python.pythonhost.views.sdl;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * Created by Sebastian on 04.02.2018.
 */
public class SDLClientHandler extends Thread {
    private enum SDL_CMD_ID {
        CmdCreateWindow,
        CmdSendCommand,
        CmdSetKeepScreenOn,
        CmdShowMessageBox,
        CmdSetOrientation,
        CmdIsScreenKeyboardShown,
        CmdOpenAPKExpansionInputStream,
        CmdClipboardHasText,
        CmdClipboardGetText,
        CmdClipboardSetText,
        CmdGetActivity,
        CmdSetSeparateMouseAndTouch,
        CmdSetWindowTitle,
        CmdShowTextInput,
        CmdHideTextInput,
        CmdGetNativeSurface,
        CmdSetPixelFormat,
        CmdDestroy,
        CmdSetWindowIcon,
        CmdAudioOpen,
        CmdAudioWriteShortBuffer,
        CmdAudioWriteByteBuffer,
        CmdAudioClose,
        CmdCaptureOpen,
        CmdCaptureReadShortBuffer,
        CmdCaptureReadByteBuffer,
        CmdCaptureClose,
        CmdPollInputDevices,
        CmdPollHapticDevices,
        CmdHapticRun,
        CmdGetInputDeviceIds,
    }
    
    private final static String TAG = "SDLClientHandler";
    private final FileDescriptor clientFd;
    private final DataInputStream  clientInConnection;
    private final DataOutputStream clientOutConnection;
    private final SDLServer        sdlServer;
    private Map<Long, SDLWindowFragment> windowMap = new HashMap<>();
    
    public SDLClientHandler(FileDescriptor clientFd, SDLServer sdlServer) {
        super();
        setDaemon(true);
        this.sdlServer = sdlServer;
        this.clientFd = clientFd;
        this.clientInConnection = new DataInputStream(new FileInputStream(clientFd));
        this.clientOutConnection = new DataOutputStream(new FileOutputStream(clientFd));
    }

    @Override
    public void run() {
        UUID serverId = sdlServer.getServerId();
        SDL_CMD_ID[] sdlCommands = SDL_CMD_ID.values();
        try {
//            clientOutConnection.writeLong(serverId.getMostSignificantBits());
//            clientOutConnection.writeLong(serverId.getLeastSignificantBits());
            while (true) {
                byte commandNumber = clientInConnection.readByte();
                if (commandNumber >= sdlCommands.length) {
                    Log.w(TAG, "Ignoring unknown command " + (int) commandNumber);
                    continue;
                }
                SDL_CMD_ID command = sdlCommands[commandNumber];
                Log.d(TAG, "Got command " + command);
                switch (command) {
                case CmdCreateWindow: {
                    long windowId = clientInConnection.readLong();
                    Log.d(TAG, "Create window, windowId = " + windowId);
                    SDLWindowFragment window = (SDLWindowFragment) sdlServer.createWindow(windowId);
                    if (window != null) {
                        windowMap.put(windowId, window);
                    }
                    clientOutConnection.writeInt(1);
                    clientOutConnection.writeBoolean(window != null);
                    break;
                } case CmdSendCommand:
                    break;
                case CmdSetKeepScreenOn: {
                    boolean value = clientInConnection.readBoolean();
                    Log.d(TAG, "Command SetKeepScreenOn: " + value);
                    sdlServer.setKeepScreenOn(value);
                    break;
                } case CmdShowMessageBox:
                    break;
                case CmdSetOrientation:
                    break;
                case CmdIsScreenKeyboardShown:
                    break;
                case CmdOpenAPKExpansionInputStream:
                    break;
                case CmdClipboardHasText:
                    break;
                case CmdClipboardGetText:
                    break;
                case CmdClipboardSetText:
                    break;
                case CmdGetActivity:
                    break;
                case CmdSetSeparateMouseAndTouch:
                    break;
                case CmdSetWindowTitle:
                    break;
                case CmdShowTextInput:
                    break;
                case CmdHideTextInput:
                    break;
                case CmdGetNativeSurface: {
                    long windowId = clientInConnection.readLong();
                    SDLWindowFragment window = windowMap.get(windowId);
                    if (window != null) {
                        Surface surface = window.getNativeSurface();
                        try {
                            Field nativeObjectField = surface.getClass().getDeclaredField("mNativeObject");
                            nativeObjectField.setAccessible(true);
                            Number number = (Number) nativeObjectField.get(surface);
                            long nativeObject = number.longValue();
                            Log.d(TAG, "Got native surface object " + nativeObject);
                            clientOutConnection.writeInt(8);
                            clientOutConnection.writeLong(nativeObject);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        Parcel p = Parcel.obtain();
                        p.writeParcelable(surface, 0);
                        p.recycle();
                        
//                        android.app.ActivityThread.main(null);
                                
                        //                        Parcel p = Parcel.obtain();
//                        p.writeParcelable(surface, 0);
//                        byte data[] = p.marshall();
//                        clientOutConnection.writeInt(data.length);
//                        clientOutConnection.write(data);
//                        p.recycle();
                    } else {
                        Log.w(TAG, "Got GetNativeSurface command for unknown window " + windowId);
                    }
                    break;
                } case CmdSetPixelFormat:
                    break;
                case CmdDestroy: {
                    long windowId = clientInConnection.readLong();
                    SDLWindowFragment window = windowMap.remove(windowId);
                    if (window != null) {
                        window.destroy();
                    } else {
                        Log.w(TAG, "Got destroy for unknown window " + windowId);
                    }
                    break;
                } case CmdSetWindowIcon:
                    break;
                case CmdAudioOpen:
                    break;
                case CmdAudioWriteShortBuffer:
                    break;
                case CmdAudioWriteByteBuffer:
                    break;
                case CmdAudioClose:
                    break;
                case CmdCaptureOpen:
                    break;
                case CmdCaptureReadShortBuffer:
                    break;
                case CmdCaptureReadByteBuffer:
                    break;
                case CmdCaptureClose:
                    break;
                case CmdPollInputDevices:
                    break;
                case CmdPollHapticDevices:
                    break;
                case CmdHapticRun:
                    break;
                case CmdGetInputDeviceIds:
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Connection to client lost.");
        }
        removeAllWindows();
    }

    /**
     * Remove all windows created by this client from the window manager. 
     */
    private void removeAllWindows() {
        for (SDLWindowFragment window : this.windowMap.values()) {
            window.destroy();
        }
    }
}
