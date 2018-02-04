package com.apython.python.pythonhost.views.sdl;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Message box implementation for SDL.
 * 
 * Created by Sebastian on 16.11.2016.
 */
class SDLMessageBox {
    
    private Activity activity;
    private Dialog dialog;
    private final int flags;
    private final String title;
    private final String message;
    private final int[] buttonFlags;
    private final int[] buttonIds;
    private final String[] buttonTexts;
    private final int[] colors;
    
    /** Result of current messagebox. Also used for blocking the calling thread. */
    private final int[] messageboxSelection = new int[1];


    /**
     * A message box. Call {@link #show()} or {@link #showAndWait()}
     * to show it.
     * buttonFlags, buttonIds and buttonTexts must have same length.
     * @param buttonFlags array containing flags for every button.
     * @param buttonIds array containing id for every button.
     * @param buttonTexts array containing text for every button.
     * @param colors null for default or array of length 5 containing colors.
     */
    SDLMessageBox(Activity activity, int flags, String title, String message,
                         int[] buttonFlags, int[] buttonIds, String[] buttonTexts, int[] colors) {
        this.activity = activity;
        this.flags = flags;
        this.title = title;
        this.message = message;
        this.buttonFlags = buttonFlags;
        this.buttonIds = buttonIds;
        this.buttonTexts = buttonTexts;
        this.colors = colors;
        this.messageboxSelection[0] = -1;
    }
    
    private boolean createDialog() {
        // sanity checks
        if ((buttonFlags.length != buttonIds.length) || (buttonIds.length != buttonTexts.length)) {
            return false; // implementation broken
        }
        // TODO set values from "flags" to messagebox dialog

        // get colors
        int backgroundColor = Color.TRANSPARENT;
        int textColor = Color.TRANSPARENT;
        int buttonBorderColor = Color.TRANSPARENT;
        int buttonBackgroundColor = Color.TRANSPARENT;
        int buttonSelectedColor = Color.TRANSPARENT;
        if (colors != null) {
            int i = -1;
            backgroundColor = colors[++i];
            textColor = colors[++i];
            buttonBorderColor = colors[++i];
            buttonBackgroundColor = colors[++i];
            buttonSelectedColor = colors[++i];
        }

        // create dialog with title and a listener to wake up calling thread

        dialog = new Dialog(activity);
        dialog.setTitle(title);
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface unused) {
                synchronized (messageboxSelection) {
                    messageboxSelection.notify();
                }
            }
        });

        // create text

        TextView messageTextView = new TextView(activity);
        messageTextView.setGravity(Gravity.CENTER);
        messageTextView.setText(message);
        if (textColor != Color.TRANSPARENT) {
            messageTextView.setTextColor(textColor);
        }

        // create buttons

        final SparseArray<Button> mapping = new SparseArray<>();

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        for (int i = 0; i < buttonTexts.length; ++i) {
            Button button = new Button(activity);
            final int id = buttonIds[i];
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageboxSelection[0] = id;
                    dialog.dismiss();
                }
            });
            if (buttonFlags[i] != 0) {
                // see SDL_messagebox.h
                if ((buttonFlags[i] & 0x00000001) != 0) {
                    mapping.put(KeyEvent.KEYCODE_ENTER, button);
                }
                if ((buttonFlags[i] & 0x00000002) != 0) {
                    mapping.put(KeyEvent.KEYCODE_ESCAPE, button); /* API 11 */
                }
            }
            button.setText(buttonTexts[i]);
            if (textColor != Color.TRANSPARENT) {
                button.setTextColor(textColor);
            }
            if (buttonBorderColor != Color.TRANSPARENT) {
                // TODO set color for border of messagebox button
            }
            if (buttonBackgroundColor != Color.TRANSPARENT) {
                Drawable drawable = button.getBackground();
                if (drawable == null) {
                    // setting the color this way removes the style
                    button.setBackgroundColor(buttonBackgroundColor);
                } else {
                    // setting the color this way keeps the style (gradient, padding, etc.)
                    drawable.setColorFilter(buttonBackgroundColor, PorterDuff.Mode.MULTIPLY);
                }
            }
            if (buttonSelectedColor != Color.TRANSPARENT) {
                // TODO set color for selected messagebox button
            }
            buttons.addView(button);
        }

        // create content

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(messageTextView);
        content.addView(buttons);
        if (backgroundColor != Color.TRANSPARENT) {
            content.setBackgroundColor(backgroundColor);
        }

        // add content to dialog and return

        dialog.setContentView(content);
        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                Button button = mapping.get(keyCode);
                if (button != null) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        button.performClick();
                    }
                    return true; // also for ignored actions
                }
                return false;
            }
        });
        return true;
    }
    
    public void show() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog == null && !createDialog()) {
                    synchronized (messageboxSelection) {
                        messageboxSelection.notify();
                    }
                    return;
                }
                dialog.show();
            }
        });
    }
    
    public int getPressedButtonId() {
        return messageboxSelection[0];
    }
    
    public int showAndWait() {
        show();
        // block the calling thread
        synchronized (messageboxSelection) {
            try {
                messageboxSelection.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        // return selected value
        return getPressedButtonId();
    }
}
