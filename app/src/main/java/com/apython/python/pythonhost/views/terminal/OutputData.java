package com.apython.python.pythonhost.views.terminal;

import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.data.BinaryTreeMap;

/**
 * Handles terminal output.
 * 
 * Created by Sebastian on 21.02.2017.
 */

public class OutputData {
    
    private static final int INITIAL_OUTPUT_DATA_BUFFER_SIZE = 2048;
    private static final int LAST_CHAR_OF_OUTPUT_DATA        = -1;

    private final StringBuffer                    outputData      = new StringBuffer(INITIAL_OUTPUT_DATA_BUFFER_SIZE);
    private final BinaryTreeMap<TerminalTextSpan> uiControlPoints = new BinaryTreeMap<>();
    private       int                             lines           = 1;
    private       int                             lastIndex       = 0;
    /**
     * lastStringStartIndex might be -1 to indicate the start of the screen data,
     * but lastStringEndIndex can never be -1!
     **/
    private       int                                lastStringStartIndex = -1, lastStringEndIndex = 0;
    private int cursorPosition = LAST_CHAR_OF_OUTPUT_DATA;

    int getLineCount() {
        return lines;
    }
    
    String getLine(int position) {
        return accessLine(position);
    }
    
    int getCursorPosition() {
        return cursorPosition == LAST_CHAR_OF_OUTPUT_DATA ? outputData.length() : cursorPosition;
    }
    
    void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }
    
    void addUiControl(TerminalTextSpan control) {
        uiControlPoints.put(getCursorPosition(), control);
    }

    public int getLength() {
        return outputData.length();
    }
    
    CharSequence getLineWithUiData(int line) {
        BinaryTreeMap<TerminalTextSpan>.Surrounding surrounding;
        CharSequence text = getLine(line);
        int lineStartIndex = lastStringStartIndex + 1;
        int lineEndIndex = lineStartIndex + text.length();
        surrounding = uiControlPoints.getSurrounding(lineStartIndex);
        if (surrounding.lesserObject != null) {
            int endIndex = lineEndIndex - lineStartIndex;
            if (surrounding.higherKey != -1) {
                endIndex = Math.min(endIndex, (int) (surrounding.higherKey - lineStartIndex));
            }
            text = surrounding.lesserObject.apply(text, 0, endIndex);
        }
        while (surrounding.higherKey != -1 && surrounding.higherKey <= lineEndIndex) {
            surrounding = surrounding.getNext();
            if (surrounding.lesserObject == null) continue;
            int startIndex = (int) (surrounding.lesserKey - lineStartIndex);
            int endIndex = lineEndIndex - lineStartIndex;
            if (surrounding.higherKey != -1) {
                endIndex = Math.min(endIndex, (int) (surrounding.higherKey - lineStartIndex));
            }
            text = surrounding.lesserObject.apply(text, startIndex, endIndex);
        }
        return text;
    }
    
    int indexOf(@SuppressWarnings("SameParameterValue") String subString, int start, int fallback) {
        int index = this.outputData.indexOf(subString, start);
        return index == -1 ? fallback : index;
    }

    int lastIndexOf(
            @SuppressWarnings("SameParameterValue") String subString, int start, int fallback) {
        int index = this.outputData.lastIndexOf(subString, start);
        return index == -1 ? fallback : index;
    }
    
    char charAt(int index) {
        return this.outputData.charAt(index);
    }
    
    private void findLine(int lineNumber) {
        if (lineNumber >= lines) {
            throw new IndexOutOfBoundsException(
                    "Tried to access line " + lineNumber + " of the terminal output, " +
                            "but only had " + lines + " lines.");
        }
        int positionOffset = lineNumber - lastIndex;
        if (positionOffset < 0) {
            int lastEnd, newStart;
            lastEnd = newStart = lastStringStartIndex;
            for (int i = 0; i != positionOffset; i--) {
                lastEnd = newStart;
                newStart = outputData.lastIndexOf("\n", lastEnd - 1);
            }
            lastStringEndIndex = lastEnd;
            lastStringStartIndex = newStart;
        } else if (positionOffset > 0) {
            int newEnd, lastStart;
            newEnd = lastStart = lastStringEndIndex;
            for (int i = 0; i != positionOffset; i++) {
                lastStart = newEnd;
                newEnd = outputData.indexOf("\n", lastStart + 1);
            }
            lastStringEndIndex = newEnd;
            lastStringStartIndex = lastStart;
        }
        if (lastStringEndIndex == -1) {
            lastStringEndIndex = outputData.length();
        }
        lastIndex = lineNumber;
    }
    
    /**
     * Access a specific line from the screen data.
     *
     * @param lineNumber The line to get.
     * @return The content of the line given by linenumber.
     */
    private String accessLine(int lineNumber) {
        findLine(lineNumber);
        int endIndex = lastStringEndIndex;
        if (endIndex == -1) { endIndex = 0; }
        return outputData.substring(lastStringStartIndex + 1, endIndex);
    }
    
    public void remove(int start, int end) {
        int lineCount = Util.countCharacterOccurrence(outputData.substring(start, end), '\n');
        lines -= lineCount;
        outputData.replace(start, end, "");
        BinaryTreeMap<TerminalTextSpan>.Surrounding surrounding = uiControlPoints.getSurrounding(start);
        TerminalTextSpan lastUiControl = null;
        while (surrounding.higherKey != -1 && surrounding.higherKey < end) {
            long uiControlIndex = surrounding.higherKey;
            lastUiControl = surrounding.higherObject;
            surrounding = surrounding.getNext();
            uiControlPoints.remove(uiControlIndex);
        }
        if (lastUiControl != null) {
            uiControlPoints.put(start, lastUiControl);
        }
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

    /**
     * Get the cursor position relative in the specific line or -1,
     * if the cursor is not in the line.  
     * 
     * @param lineNumber The line of the output data.
     * @return The relative cursor position to the start of the line.
     */
    int getCursorPosInLine(int lineNumber) {
        findLine(lineNumber);
        int cursorPos = getCursorPosition();
        if (lastStringStartIndex < cursorPos && lastStringEndIndex >= cursorPos) {
            return cursorPos - Math.max(lastStringStartIndex, 0) - 1;
        }
        return -1;
    }
}
