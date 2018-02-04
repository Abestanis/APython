package com.apython.python.pythonhost.views.sdl;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

/**
 * Created by Sebastian on 02.02.2018.
 */
public class SDLClipboardHandler implements ClipboardManager.OnPrimaryClipChangedListener {
    private ClipboardManager clipboardManager;
    private SDLServer sdlServer;

    SDLClipboardHandler(SDLServer sdlServer) {
        this.sdlServer = sdlServer;
        clipboardManager = (ClipboardManager) sdlServer.getActivity()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.addPrimaryClipChangedListener(this);
        }
    }
    
    boolean clipboardHasText() {
        //noinspection deprecation
        return clipboardManager != null && clipboardManager.hasText();
    }

    String clipboardGetText() {
        if (clipboardManager == null) { return null; }
        ClipData clip = clipboardManager.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            return clip.getItemAt(0).coerceToText(sdlServer.getActivity()).toString();
        }
        return null;
    }

    void clipboardSetText(String string) {
        if (clipboardManager == null) { return; }
        clipboardManager.removePrimaryClipChangedListener(this);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, string));
        clipboardManager.addPrimaryClipChangedListener(this);
    }

    @Override
    public void onPrimaryClipChanged() {
        sdlServer.onNativeClipboardChanged();
    }
}
