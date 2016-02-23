package com.apython.python.pythonhost.views.terminal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.apython.python.pythonhost.Util;

/**
 * An adapter to use with a {@link android.widget.ListView} to display a terminal.
 *
 * Created by Sebastian on 13.11.2015.
 */
public class TerminalAdapter extends BaseAdapter {

    private static final int INITIAL_SCREEN_DATA_BUFFER_SIZE = 2048;
    private static final int LAST_CHAR_OF_SCREEN_DATA = -1;

    private StringBuffer screenData = new StringBuffer(INITIAL_SCREEN_DATA_BUFFER_SIZE);
    private int lines = 1;
    private int lastIndex = 0;
    /**
     * lastStringStartIndex might be -1 to indicate the start of the screen data,
     * but lastStringEndIndex can never be -1!
     **/
    private int lastStringStartIndex = -1, lastStringEndIndex = 0;
    private int cursorPosition = LAST_CHAR_OF_SCREEN_DATA;
    private Context context;

    public TerminalAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return lines;
    }

    @Override
    public String getItem(int position) {
        return accessLine(position);
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
        if (convertView != null) {
            ((TextView) convertView).setText(accessLine(position));
            return convertView;
        }
        TextView view = new TextView(context);
        view.setTextColor(Color.WHITE);
        view.setTypeface(Typeface.MONOSPACE);
        view.setText(accessLine(position));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
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
     * Access a specific line from the screen data.
     *
     * @param lineNumber The line to get.
     * @return The content of the line given by linenumber.
     */
    private String accessLine(int lineNumber) {
        if (lineNumber >= lines) {
            throw new IndexOutOfBoundsException("Tried to access line " + lineNumber
                      + " of the terminal output, but only had " + lines + " lines.");
        }
        int positionOffset = lineNumber - lastIndex;
        if (positionOffset < 0) {
            int lastEnd = lastStringStartIndex;
            int newStart;
            for (int i = 0;;) {
                newStart = screenData.lastIndexOf("\n", lastEnd - 1);
                i--;
                if (i == positionOffset) {
                    lastStringEndIndex = lastEnd;
                    lastStringStartIndex = newStart;
                    break;
                }
                lastEnd = newStart;
            }
        } else if (positionOffset > 0) {
            int lastStart = lastStringEndIndex;
            int newEnd;
            for (int i = 0;;) {
                newEnd = screenData.indexOf("\n", lastStart + 1);
                i++;
                if (i == positionOffset) {
                    lastStringEndIndex = newEnd;
                    lastStringStartIndex = lastStart;
                    break;
                }
                lastStart = newEnd;
            }
        }
        if (lastStringEndIndex == -1) {
            lastStringEndIndex = screenData.length();
        }
        int endIndex = lastStringEndIndex;
        if (endIndex == -1) { endIndex = 0; }
        lastIndex = lineNumber;
        return screenData.substring(lastStringStartIndex + 1, endIndex);
    }

    /**
     * Add text to the screen data.
     *
     * @param output The text to add.
     */
    public void addOutput(String output) {
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
        final char[] specialCharacters = {'\r'};
        for (char specialCharacter : specialCharacters) {
            int index = text.indexOf(specialCharacter);
            if (index != -1) {
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
            switch (text.charAt(specialCharacterIndex)) {
            case '\r':
                internalAddToScreenData(text.substring(0, specialCharacterIndex));
                int endIndex = cursorPosition == LAST_CHAR_OF_SCREEN_DATA ? screenData.length() : cursorPosition;
                cursorPosition = screenData.substring(0, endIndex).lastIndexOf("\n") + 1;
                text = text.substring(specialCharacterIndex + 1);
                break;
            default:
                throw new UnsupportedOperationException("Character '"
                          + text.charAt(specialCharacterIndex) + "' not implemented.");
            }
            specialCharacterIndex = getNextSpecialCharacterIndex(text);
        }
        internalAddToScreenData(text);
    }

    /**
     * Internal function to add text to the screen data at the current cursor position.
     * Recalculates {@link #lines}, {@link #lastStringEndIndex}, {@link #lastStringStartIndex}
     * and {@link #cursorPosition}.
     *
     * @param text The text to add to the screen data.
     */
    private void internalAddToScreenData(String text) {
        if (text.length() == 0) { return; }
        if (cursorPosition == LAST_CHAR_OF_SCREEN_DATA) {
            // Just append the text
            if (lines == 1 || (screenData.charAt(screenData.length() - 1) != '\n' && lastIndex == lines)) {
                // Recalculate lastStringEndIndex if we add to the last accessed line
                lastStringEndIndex = screenData.length() + text.indexOf('\n');
            }
            lines += Util.countCharacterOccurrence(text, '\n');
            screenData.append(text);
        } else {
            // We must overwrite the current screen data with the given text
            int nextPosition = cursorPosition + text.length();
            String overWrittenScreenData = screenData.substring(cursorPosition, Math.min(nextPosition, screenData.length()));
            int deletedLines = Util.countCharacterOccurrence(overWrittenScreenData, '\n');
            screenData.replace(cursorPosition, nextPosition, text);
            // Recalculate lastStringStartIndex, if it was in the changed text
            if (lastStringStartIndex >= cursorPosition && lastStringStartIndex < nextPosition) {
                // Calculate how many lines we overwrote
                int lineDelta = Util.countCharacterOccurrence(overWrittenScreenData.substring(0, lastStringStartIndex - cursorPosition), '\n');
                // recalculate lastStringStartIndex, if possible without changing lastIndex
                lastStringStartIndex = screenData.lastIndexOf("\n", cursorPosition);
                for (int i = 0; i < lineDelta; i++) {
                    int stringStartIndex = screenData.indexOf("\n", Math.max(0, lastStringStartIndex));
                    if (stringStartIndex == -1) {
                        // We have fewer lines in the new string than in the old string, we must
                        // decrease lastIndex
                        lastIndex -= lineDelta - i;
                        break;
                    } else {
                        lastStringStartIndex = stringStartIndex;
                    }
                }
            }
            // Recalculate lastStringEndIndex, if it was above the old cursorPosition
            if (lastStringEndIndex > cursorPosition) {
                lastStringEndIndex = screenData.indexOf("\n", lastStringStartIndex + 1);
                lastStringEndIndex = lastStringEndIndex == -1 ? screenData.length() : lastStringEndIndex;
            }
            // Recalculate cursorPosition and lines
            cursorPosition = nextPosition == screenData.length() ? LAST_CHAR_OF_SCREEN_DATA : nextPosition;
            lines += Util.countCharacterOccurrence(text, '\n') - deletedLines;
        }
    }
}
