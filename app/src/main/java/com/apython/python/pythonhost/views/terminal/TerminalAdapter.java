package com.apython.python.pythonhost.views.terminal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.R;

/**
 * An adapter to use with a {@link android.widget.ListView} to display a terminal.
 *
 * Created by Sebastian on 13.11.2015.
 */
class TerminalAdapter extends BaseAdapter {
    private final Context context;
    private final OutputData screenData = new OutputData();
    private TerminalTextSpan currentTextAttr = null;
    private MultiCharTerminalEscSeq currentEscSeq = null;
    private final ToneGenerator beepGenerator;
    private EditText inputView        = null;
    private boolean  lineInputEnabled = false;
    private static final int[] COLORS = { // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
            Color.rgb(0, 0, 0),
            Color.rgb(205, 0, 0),
            Color.rgb(0, 205, 0),
            Color.rgb(205, 205, 0),
            Color.rgb(0, 0, 238),
            Color.rgb(205, 0, 205),
            Color.rgb(0, 205, 205),
            Color.rgb(255, 255, 255),
    };

    TerminalAdapter(Context context) {
        this.context = context;
        beepGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }

    @Override
    public int getCount() {
        return screenData.getLineCount();
    }

    @Override
    public String getItem(int position) {
        return screenData.getLine(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CharSequence text = screenData.getLineWithUiData(position);
        int cursorPos;
        if (inputView != null && lineInputEnabled &&
                (cursorPos = screenData.getCursorPosInLine(position)) != -1) {
            inputView.setEnabled(true);
            inputView.setText(text);
            inputView.setSelection(cursorPos);
            return inputView;
        }
        if (convertView instanceof TextView && !(convertView instanceof EditText)) {
            ((TextView) convertView).setText(text);
            return convertView;
        }
        TextView view = new TextView(context);
        view.setTextColor(Color.WHITE);
        view.setTypeface(Typeface.MONOSPACE);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                         context.getResources().getDimension(R.dimen.interpreter_text_size));
        view.setFocusable(false);
        view.setEnabled(false);
        view.setFocusableInTouchMode(false);
        return view;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    /**
     * Add text to the screen data.
     *
     * @param output The text to add.
     */
    void addOutput(String output) {
        applyTerminalCodes(output);
        notifyDataSetChanged();
    }

    /**
     * Determine the index of the first "special" character in the given text.
     *
     * @param text The text to scan trough.
     * @return An index or {@code -1}, if no special character was found.
     */
    private int getNextSpecialCharacterIndex(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) < 32) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Interpret and apply special terminal codes and modify then screen data accordingly.
     *
     * @param text The text to add to the screen data.
     */
    private void applyTerminalCodes(String text) {
        if (currentEscSeq != null) {
            text = '\033' + text;
        }
        int specialCharacterIndex = getNextSpecialCharacterIndex(text);
        while (specialCharacterIndex != -1) {
            screenData.add(text.substring(0, specialCharacterIndex));
            switch (text.charAt(specialCharacterIndex)) {
            case '\r':
                if (text.length() > specialCharacterIndex + 1 &&
                        text.charAt(specialCharacterIndex + 1) == '\n') {
                    specialCharacterIndex++; // ignore the \r and fall trough to the \n
                } else {
                    int endIndex = screenData.getCursorPosition();
                    screenData.setCursorPosition(screenData.lastIndexOf("\n", endIndex, -1) + 1);
                    break;
                }
            case '\n': {
                int cursorPosition = screenData.getCursorPosition();
                // We may not be on the last line. Check if there is line after this one.
                cursorPosition = screenData.indexOf("\n", cursorPosition, -1);
                if (cursorPosition == -1) {
                    screenData.setCursorPosition(-1);
                    screenData.add("\n");
                } else screenData.setCursorPosition(cursorPosition + 1); // Use the existing newline
                break; }
            case '\b': {
                int cursorPosition = screenData.getCursorPosition();
                // Don't go back if we are on the first character of a new line
                if (cursorPosition != 0 && screenData.charAt(cursorPosition - 1) != '\n') {
                    screenData.setCursorPosition(cursorPosition - 1);
                }
                break; }
            case '\07': { // '\a'
                playBell();
                break; }
            case '\033': // ANSI escape sequence
                if (currentEscSeq == null) currentEscSeq = new MultiCharTerminalEscSeq();
                int charsParsed = currentEscSeq.parse(text, specialCharacterIndex + 1);
                boolean invalidSeq = charsParsed == -1;
                if (currentEscSeq.isComplete()) {
                    if (applyEscapeSequence(currentEscSeq)) {
                        specialCharacterIndex += 1 + charsParsed;
                        currentEscSeq = null;
                    } else {
                        invalidSeq = true;
                    }
                }
                if (invalidSeq) {
                    // print a /033 and parse the rest
                    screenData.add("\033");
                    specialCharacterIndex++;
                    if (!currentEscSeq.getText().isEmpty()) {
                        // TODO: Test this
                        text = text.substring(0, specialCharacterIndex) + currentEscSeq.getText()
                                + text.substring(specialCharacterIndex);
                    }
                    currentEscSeq = null;
                }
                break;
            default:
                Log.w(MainActivity.TAG, "Character '" + text.charAt(specialCharacterIndex) + "' (" +
                        ((int) text.charAt(specialCharacterIndex)) + ") not implemented.");
            }
            text = text.substring(specialCharacterIndex + 1);
            specialCharacterIndex = getNextSpecialCharacterIndex(text);
        }
        screenData.add(text);
    }

    /**
     * Play a bell sound.
     */
    private void playBell() {
        beepGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
    }

    /**
     * Apply the given multi char sequence to the terminal data.
     * Unknown sequences are ignored.
     * @param seq The sequence to apply.
     * @return False, if the sequence was invalid.
     */
    private boolean applyEscapeSequence(MultiCharTerminalEscSeq seq) {
        MultiCharTerminalEscSeq.CSIData csiData = seq.getCsiData();
        if (csiData != null) {
            switch (csiData.command) {
            case 'K': // EL – Erase in Line
                int start = screenData.getCursorPosition();
                int end = start;
                if (csiData.parameters.size() == 0 || csiData.getIntParam(0, -1) == 0) {
                    end = screenData.indexOf("\n", start + 1, screenData.getLength());
                } else if (csiData.getIntParam(0, -1) == 1) {
                    start = screenData.lastIndexOf("\n", end, 0);
                } else if (csiData.getIntParam(0, -1) == 2) {
                    end = screenData.indexOf("\n", start + 1, screenData.getLength());
                    start = screenData.lastIndexOf("\n", start, 0);
                }
                screenData.remove(start, end);
                break;
            case 'm': // Select Graphic Rendition
                if (csiData.parameters.size() == 1) {
                    switch (csiData.getIntParam(0, -1)) {
                    case -1: // Invalid
                        break;
                    case 0: // Reset / Normal
                        currentTextAttr = null;
                        screenData.addUiControl(null);
                        break;
                    case 1: // bold
                        currentTextAttr = new TerminalTextSpan(currentTextAttr).setStyle(Typeface.BOLD);
                        break;
                    case 22:
                        currentTextAttr = new TerminalTextSpan(currentTextAttr)
                                .setStyle(Typeface.BOLD, false);
                        break;
                    case 3: // italic
                        currentTextAttr = new TerminalTextSpan(currentTextAttr).setStyle(Typeface.ITALIC);
                        break;
                    case 23:
                        currentTextAttr = new TerminalTextSpan(currentTextAttr)
                                .setStyle(Typeface.ITALIC, false);
                        break;
                    default:
                        int parameter = csiData.getIntParam(0, -1);
                        if (parameter >= 30 && parameter <= 37) { // foregroundColor
                            currentTextAttr = new TerminalTextSpan(currentTextAttr).setForeground(
                                    new ForegroundColorSpan(COLORS[parameter - 30]));
                            screenData.addUiControl(currentTextAttr);
                        } else if (parameter == 39) {
                            currentTextAttr = new TerminalTextSpan(currentTextAttr).setForeground(null);
                            screenData.addUiControl(currentTextAttr);
                        } else if (parameter >= 40 && parameter <= 47) { // backgroundColor
                            currentTextAttr = new TerminalTextSpan(currentTextAttr).setBackground(
                                    new BackgroundColorSpan(COLORS[parameter - 40]));
                            screenData.addUiControl(currentTextAttr);
                        } else if (parameter == 49) {
                            currentTextAttr = new TerminalTextSpan(currentTextAttr).setBackground(null);
                            screenData.addUiControl(currentTextAttr);
                        }
                    }
                } else if (csiData.parameters.size() >= 5) {
                    if (csiData.getIntParam(0, -1) == 38) {
                        if (csiData.getIntParam(1, -1) == 2) {
                            int color = Color.rgb(csiData.getIntParam(2, 0),
                                                  csiData.getIntParam(3, 0), 
                                                  csiData.getIntParam(4, 0));
                            currentTextAttr = new TerminalTextSpan(currentTextAttr).setForeground(
                                    new ForegroundColorSpan(color));
                            screenData.addUiControl(currentTextAttr);
                        }
                    } else if (csiData.getIntParam(0, -1) == 48) {
                        if (csiData.getIntParam(1, -1) == 2) {
                            int color = Color.rgb(csiData.getIntParam(2, 0),
                                                  csiData.getIntParam(3, 0),
                                                  csiData.getIntParam(4, 0));
                            currentTextAttr = new TerminalTextSpan(currentTextAttr).setBackground(
                                    new BackgroundColorSpan(color));
                            screenData.addUiControl(currentTextAttr);
                        }
                    }
                }
                break;
            }
        }
        return true;
    }

    void setInputView(EditText view) {
        inputView = view;
    }
    
    void enableLineInput(String prompt) {
        lineInputEnabled = true;
        if (inputView != null) {
            if (inputView instanceof TerminalInput) {
                ((TerminalInput) inputView).setPrompt(prompt);
            }
            notifyDataSetChanged();
        }
    }
    
    void disableLineInput() {
        lineInputEnabled = false;
        if (inputView != null) {
            inputView.setEnabled(false);
        }
    }
}
