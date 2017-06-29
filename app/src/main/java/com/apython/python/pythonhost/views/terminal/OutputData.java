package com.apython.python.pythonhost.views.terminal;

import android.support.v4.util.LongSparseArray;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.apython.python.pythonhost.Util;

/**
 * Handles terminal output.
 * 
 * Created by Sebastian on 21.02.2017.
 */

public class OutputData {
    
    private static final int INITIAL_OUTPUT_DATA_BUFFER_SIZE = 2048;
    private static final int LAST_CHAR_OF_OUTPUT_DATA        = -1;

    private final StringBuffer outputData           = new StringBuffer(INITIAL_OUTPUT_DATA_BUFFER_SIZE);
    private final LongSparseArray<ForegroundColorSpan> uiControlPoints = new LongSparseArray<>();
    private int                lines                = 1;
    private int                lastIndex            = 0;
    /**
     * lastStringStartIndex might be -1 to indicate the start of the screen data,
     * but lastStringEndIndex can never be -1!
     **/
    private int                lastStringStartIndex = -1, lastStringEndIndex = 0;
    private int cursorPosition = LAST_CHAR_OF_OUTPUT_DATA;

    public int getLineCount() {
        return lines;
    }
    
    public String getLine(int position) {
        return accessLine(position);
    }
    
    public int getCursorPosition() {
        return cursorPosition == LAST_CHAR_OF_OUTPUT_DATA ? outputData.length() : cursorPosition;
    }
    
    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }
    
    public void addUiControl(ForegroundColorSpan control) {
        uiControlPoints.append(getCursorPosition(), control);
    }

    public int getLength() {
        return outputData.length();
    }
    
    public CharSequence getLineWithUiData(int line) {
        CharSequence text = getLine(line);
        int lineStartIndex = lastStringStartIndex + 1;
        int lineEndIndex = lineStartIndex + text.length();
        int numControlIndices = uiControlPoints.size();
        for (int i = 0; i < numControlIndices; i++) {
            if (uiControlPoints.keyAt(i) > lineEndIndex) break;
            if (i + 1 >= numControlIndices || uiControlPoints.keyAt(i + 1) > lineStartIndex) {
                int endIndex = Math.min(text.length(), i + 1 < numControlIndices ? (int) (uiControlPoints.keyAt(i + 1) - lineStartIndex) : text.length());
                int startIndex = Math.max(0, (int) (uiControlPoints.keyAt(i) - lineStartIndex));
                Spannable spannable;
                if (text instanceof Spannable) {
                    spannable = (Spannable) text;
                } else {
                    spannable = new SpannableString(text);
                }
                spannable.setSpan(uiControlPoints.valueAt(i), startIndex, endIndex, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                text = spannable;
            }
        }
        return text;
    }
    
    int indexOf(String subString, int start, int fallback) {
        int index = this.outputData.indexOf(subString, start);
        return index == -1 ? fallback : index;
    }

    int lastIndexOf(String subString, int start, int fallback) {
        int index = this.outputData.lastIndexOf(subString, start);
        return index == -1 ? fallback : index;
    }
    
    char charAt(int index) {
        return this.outputData.charAt(index);
    }
    
    /**
     * Access a specific line from the screen data.
     *
     * @param lineNumber The line to get.
     * @return The content of the line given by linenumber.
     */
    public String accessLine(int lineNumber) {
        if (lineNumber >= lines) {
            throw new IndexOutOfBoundsException("Tried to access line " + lineNumber +
                                                        " of the terminal output, but only had " + lines + " lines.");
        }
        int positionOffset = lineNumber - lastIndex;
        if (positionOffset < 0) {
            int lastEnd = lastStringStartIndex;
            int newStart;
            for (int i = 0;;) {
                newStart = outputData.lastIndexOf("\n", lastEnd - 1);
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
                newEnd = outputData.indexOf("\n", lastStart + 1);
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
            lastStringEndIndex = outputData.length();
        }
        int endIndex = lastStringEndIndex;
        if (endIndex == -1) { endIndex = 0; }
        lastIndex = lineNumber;
        return outputData.substring(lastStringStartIndex + 1, endIndex);
    }
    
    public void remove(int start, int end) {

    }

    /**
     * Internal function to add text to the screen data at the current cursor position.
     * Recalculates {@link #lines}, {@link #lastStringEndIndex}, {@link #lastStringStartIndex}
     * and {@link #cursorPosition}.
     *
     * @param text The text to add to the screen data.
     */
    public void add(String text) {
        if (text.length() == 0) { return; }
        if (cursorPosition == LAST_CHAR_OF_OUTPUT_DATA) {
            // Just append the text
            if (lines == 1 || (outputData.charAt(outputData.length() - 1) != '\n' && lastIndex == lines)) {
                // Recalculate lastStringEndIndex if we add to the last accessed line
                int newLineIndex = text.indexOf('\n');
                if (newLineIndex != -1) lastStringEndIndex = outputData.length() + newLineIndex;
            }
            lines += Util.countCharacterOccurrence(text, '\n');
            outputData.append(text);
        } else {
            // We must overwrite the current screen data with the given text // TODO: How to handle overwriting newlines?
            int nextPosition = cursorPosition + text.length();
            String overWrittenScreenData = outputData.substring(cursorPosition, Math.min(nextPosition, outputData.length()));
            int deletedLines = Util.countCharacterOccurrence(overWrittenScreenData, '\n');
            outputData.replace(cursorPosition, nextPosition, text);
            // Recalculate lastStringStartIndex, if it was in the changed text
            if (lastStringStartIndex >= cursorPosition && lastStringStartIndex < nextPosition) {
                // Calculate how many lines we overwrote
                int lineDelta = Util.countCharacterOccurrence(overWrittenScreenData.substring(0, lastStringStartIndex - cursorPosition), '\n');
                // recalculate lastStringStartIndex, if possible without changing lastIndex
                lastStringStartIndex = outputData.lastIndexOf("\n", cursorPosition);
                for (int i = 0; i < lineDelta; i++) {
                    int stringStartIndex = outputData.indexOf("\n", Math.max(0, lastStringStartIndex));
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
                lastStringEndIndex = outputData.indexOf("\n", lastStringStartIndex + 1);
                lastStringEndIndex = lastStringEndIndex == -1 ? outputData.length() : lastStringEndIndex;
            }
            // Recalculate cursorPosition and lines
            cursorPosition = nextPosition == outputData.length() ? LAST_CHAR_OF_OUTPUT_DATA : nextPosition;
            lines += Util.countCharacterOccurrence(text, '\n') - deletedLines;
        }
    }
}
