package com.apython.python.pythonhost.views.terminal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.apython.python.pythonhost.PythonSettingsActivity;
import com.apython.python.pythonhost.Util;

/**
 * An input view to use within a terminal.
 *
 * Created by Sebastian on 12.11.2015.
 */
public class TerminalInput extends EditText {

    public interface OnCommitHandler {
        /**
         * This method gets called when the "Enter" key is pressed (or a newline is
         * inserted into the input).
         * Keep in mind that:
         * <p>
         * A - there is no guarantee that there is a newline character at the end of the input.
         * <p>
         * B - there might be multiple newline characters in the input.
         *
         * @param terminalInput The instance of the Terminal input which received the enter.
         */
        void onCommit(TerminalInput terminalInput);

        /**
         * This method gets called when this input receives a key event while it is disabled.
         * @param event The event this input received.
         * @return Whether the event was consumed.
         */
        boolean onKeyEventWhileDisabled(KeyEvent event);
    }

    private boolean lineInputEnabled = false;
    private String  prompt           = null;
    private TextWatcher        inputWatcher;
    private InputMethodManager inputManager;
    private OnCommitHandler    commitHandler;
    private SharedPreferences  preferences;

    public TerminalInput(Context context) {
        super(context);
        init();
    }

    public TerminalInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TerminalInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        setFocusable(true);
        setFocusableInTouchMode(true);
        inputWatcher = new TextWatcher() {
            int start, count, numCharsAdded, cursorPosBefore;
            
            private String restorePrompt(String newText) {
                String text = prompt == null ? "" : prompt;
                if (start == 0 && newText.startsWith(text)) {
                    return newText;
                }
                text += newText.substring(start);
                internalSetText(text);
                // TODO: Ring bell
                return text;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                cursorPosBefore = getSelectionEnd();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                this.start = start;
                this.numCharsAdded = count;
                this.count = before;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (isLineInputEnabled()) {
                    String promptStr = prompt == null ? "" : prompt;
                    if (start < promptStr.length()) {
                        input = restorePrompt(input);
                    }
                    if (input.length() > 0) {
                        if (input.indexOf('\n', start) >= promptStr.length() && commitHandler != null) {
                            if (numCharsAdded == 1 && input.charAt(start) == '\n') {
                                internalSetText(input.substring(0, start) + input.substring(start + 1) + '\n');
                            }
                            commitHandler.onCommit(TerminalInput.this);
                        }
                    }
                } else {
                    /* Soft-input methods may not use {@link PythonInterpreterActivity#dispatchKeyEvent(KeyEvent)}. **/
                    dispatchInputToMainWindow(input);
                    internalSetText("");
                }
            }
        };
        this.addTextChangedListener(inputWatcher);
        this.setOnKeyListener((v, keyCode, event) -> {
            if (!isLineInputEnabled() || keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                return commitHandler != null && commitHandler.onKeyEventWhileDisabled(event);
            }
            if (keyCode == KeyEvent.KEYCODE_TAB) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    int start = Math.max(getSelectionStart(), 0);
                    int end = Math.max(getSelectionEnd(), 0);
                    String tab = preferences.getBoolean(
                            PythonSettingsActivity.KEY_REPLACE_TABS, false) ? "    " : "\t"; 
                    if (start != end) {
                        getText().insert(prompt == null ? 0 : prompt.length(), tab);
                    } else {
                        getText().replace(start, end, tab, 0, tab.length());
                    }
                }
                return true;
            }
            return false;
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * Disabling of this view is not wanted, since it would hide the soft keyboard every time.
     * Instead, the view will not be disabled, but the input must be captured outside before it
     * gets passed to this view.
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.lineInputEnabled = enabled;
        setCursorVisible(enabled);
        setFocusable(true);
        setFocusableInTouchMode(true);
        if (enabled) {
            requestKeyboardFocus(false);
        } else {
            this.prompt = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * @return {@code true} if this input is in read line mode or {@code false} otherwise.
     */
    public boolean isLineInputEnabled() {
        return lineInputEnabled;
    }

    @Override
    public void onEditorAction(int actionCode) {
        if (actionCode == EditorInfo.IME_ACTION_DONE) {
            dispatchInputToMainWindow("\n");
        } else {
            super.onEditorAction(actionCode);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (lineInputEnabled && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isInputMethodTarget()) {
                requestKeyboardFocus(true);
            }
            setCursorVisible(true);
        }
        return super.onTouchEvent(event);
    }

    /**
     * Disable this input and clear the input text.
     */
    public void disableInput() {
        this.setEnabled(false);
        internalSetText("");
    }

    /**
     * Pop the current input. After this function returns, the input will be empty and disabled.
     *
     * @return The last input.
     */
    public String popCurrentInput() {
        String text = this.getText().toString();
        String promptStr = prompt == null ? "" : prompt;
        String input = text.substring(promptStr.length());
        disableInput();
        return input;
    }

    /**
     * Register an handler to handle enter keys as well as key events when we are inactive.
     * @param commitHandler The commit handler to register.
     */
    public void setCommitHandler(OnCommitHandler commitHandler) {
        this.commitHandler = commitHandler;
    }

    /**
     * @return The current commit handler.
     */
    public OnCommitHandler getCommitHandler() {
        return commitHandler;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (prompt == null || prompt.equals("")) return;
        int newSelStart = selStart, newSelEnd = selEnd;
        int min = Math.min(prompt.length(), getText().length());
        if (selStart < min) newSelStart = min;
        if (selEnd < min) newSelEnd = min;
        if (newSelEnd != selStart || newSelStart != selEnd) {
            setSelection(newSelStart, newSelEnd);
        }
    }
    
    void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Set the input to text without notifying our {@link #inputWatcher}.
     * @param text The text to set the input to.
     */
    private void internalSetText(CharSequence text) {
        this.removeTextChangedListener(inputWatcher);
        setText(text);
        this.addTextChangedListener(inputWatcher);
    }

    /**
     * Dispatch an input event to the main window, while we are inactive.
     * @param input The input event to dispatch.
     */
    private void dispatchInputToMainWindow(String input) {
        if (commitHandler != null) {
            for (KeyEvent event : Util.stringToKeyEvents(input)) {
                commitHandler.onKeyEventWhileDisabled(event);
            }
        }
    }

    /**
     * Try to get the keyboard focus.
     * 
     * @param fromUserInteraction Whether or not the request originated form a user interaction.
     */
    private void requestKeyboardFocus(boolean fromUserInteraction) {
        if (requestFocus() && inputManager != null) {
            inputManager.showSoftInput(this, fromUserInteraction ?
                    InputMethodManager.SHOW_FORCED : InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean checkInputConnectionProxy(View view) {
        return true;
    }
}
