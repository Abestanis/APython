package com.apython.python.pythonhost.views.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Represents and parses a multi byte terminal escape sequence.
 * 
 * Created by Sebastian on 11.07.2017.
 */

public class MultiCharTerminalEscSeq {
    private String text = "";
    private CSIData csiData = null;
    private boolean complete = false;
    
    class CSIData {
        char   command     = '\0';
        String privateMode = "";
        String commandModifiers = "";
        private int parseParamIndex = 0;
        ArrayList<String> parameters = new ArrayList<>();

        /**
         * Try to get a parameter of the CSI command as an int, use the fallback as a default.
         * 
         * @param index The index of the parameter.
         * @param fallback The default value.
         * @return The parameter as an integer.
         */
        Integer getIntParam(int index, Integer fallback) {
            try {
                return Integer.valueOf(parameters.get(index));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }

    /**
     * Parse the text as a escape sequence starting at startOffset.
     * @param text The text to parse.
     * @param startOffset The index to start parsing.
     * @return The number of chars parsed or -1 if the sequence is invalid.
     */
    public int parse(String text, int startOffset) {
        int numCharsParsed = 0;
        if (csiData != null) {
            numCharsParsed = parseCSI(text, startOffset);
        } else if (text.length() > startOffset) {
            if (text.charAt(startOffset) == '[') {
                numCharsParsed = parseCSI(text, startOffset + 1);
                if (numCharsParsed != -1) numCharsParsed++;
            } else {
                return -1;
            }
        }
        if (numCharsParsed > 0) {
            this.text += text.substring(startOffset, startOffset + numCharsParsed);
        }
        return numCharsParsed;
    }

    /**
     * Parse the text as a CSI escape sequence starting at startOffset.
     * @param text The text to parse.
     * @param startOffset The index to start parsing.
     * @return The number of chars parsed or -1 if the sequence is invalid.
     */
    private int parseCSI(String text, int startOffset) {
        if (csiData == null) csiData = new CSIData();
        int index = startOffset - 1;
        char character;
        while (++index < text.length()) {
            character = text.charAt(index);
            if (Character.isDigit(character)) {
                if (csiData.parameters.size() == csiData.parseParamIndex) {
                    csiData.parameters.add(String.valueOf(character));
                } else {
                    csiData.parameters.set(
                            csiData.parseParamIndex, csiData.parameters.get(
                                    csiData.parseParamIndex) + character);
                }
            } else if (character == ':' || character == ';') {
                if (csiData.parameters.size() == 0 || text.charAt(index - 1) == ':'
                        || text.charAt(index - 1) == ';') {
                    csiData.parameters.add("");
                }
                csiData.parseParamIndex++;
            } else if (character >= 48 && character <= 63
                    && csiData.parameters.size() == 0 && csiData.commandModifiers.length() == 0) {
                csiData.privateMode += character;
            } else if (character >= 32 && character <= 47) {
                csiData.commandModifiers += character;
            } else if (character >= 64 && character <= 126) {
                csiData.command = character;
                complete = true;
                break;
            } else { // Invalid character
                return -1;
            }
        }
        return index - startOffset;
    }

    /**
     * Get the parsed CSI data, if a CSI seq has been parsed.
     * @return The CSI data or null.
     */
    CSIData getCsiData() {
        return csiData;
    }

    /**
     * @return The text that represents the multi char escape sequence. 
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * @return Indicate if a complete escape sequence has been parsed.
     */
    public boolean isComplete() {
        return complete;
    }
}
