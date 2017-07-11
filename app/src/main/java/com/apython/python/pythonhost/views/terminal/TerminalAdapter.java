package com.apython.python.pythonhost.views.terminal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.R;

import java.util.ArrayList;

/**
 * An adapter to use with a {@link android.widget.ListView} to display a terminal.
 *
 * Created by Sebastian on 13.11.2015.
 */
class TerminalAdapter extends BaseAdapter {
    private final Context context;
    private final OutputData screenData = new OutputData();
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
        if (convertView != null) {
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
        int specialCharacterIndex = getNextSpecialCharacterIndex(text);
        while (specialCharacterIndex != -1) {
            screenData.add(text.substring(0, specialCharacterIndex));
            switch (text.charAt(specialCharacterIndex)) {
            case '\r':
                int endIndex = screenData.getCursorPosition();
                screenData.setCursorPosition(screenData.lastIndexOf("\n", endIndex, -1) + 1);
                break;
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
            case '\033': // ANSI escape sequence
                if (text.charAt(specialCharacterIndex + 1) == '[') { // CSI code
                    specialCharacterIndex++;
                    char character;
                    ArrayList<Integer> args = new ArrayList<>();
                    int argIndex = 0;
                    String privateModeCharacters = "";
                    String trailingIntermediateCharacters = "";
                    while ((character = text.charAt(++specialCharacterIndex)) != '\0' && (character < 64 || character > 126)) {
                        if (Character.isDigit(character)) {
                            if (args.size() <= argIndex) {
                                args.add(Character.getNumericValue(character));
                            } else {
                                args.set(argIndex, args.get(argIndex) * 10 + Character.getNumericValue(character));
                            }
                        } else if (character == ':') {
                            argIndex++;
                        } else if (character >= 32 && character <= 47) {
                            trailingIntermediateCharacters += character;
                        } else if (character >= 48 && character <= 63) {
                            privateModeCharacters += character;
                        }
                    }
                    switch (character) {
                    case 'K': // EL – Erase in Line
                        int start = screenData.getCursorPosition();
                        int end = start;
                        if (args.size() == 0 || args.get(0) == 0) {
                            end = screenData.indexOf("\n", start + 1, screenData.getLength());
                        } else if (args.get(0) == 1) {
                            start = screenData.lastIndexOf("\n", end, 0);
                        } else if (args.get(0) == 2) {
                            end = screenData.indexOf("\n", start + 1, screenData.getLength());
                            start = screenData.lastIndexOf("\n", start, 0);
                        }
                        screenData.remove(start, end);
                        break;
                    case 'm': // Select Graphic Rendition
                        if (args.size() == 1) {
                            switch (args.get(0)) {
                            case 0: // Reset / Normal
                                screenData.addUiControl(new ForegroundColorSpan(Color.WHITE));
                                break;
                            default:
                                if (args.get(0) >= 30 && args.get(0) <= 37) { // foregroundColor
                                    screenData.addUiControl(new ForegroundColorSpan(COLORS[args.get(0) - 30]));
                                }
                            }
                        }
                        break;
                    }
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
}
