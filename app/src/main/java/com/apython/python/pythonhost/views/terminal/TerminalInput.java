package com.apython.python.pythonhost.views.terminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.apython.python.pythonhost.Util;

import java.util.LinkedList;
import java.util.ListIterator;

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
         */
        void onKeyEventWhileDisabled(KeyEvent event);
    }

    private boolean            inputEnabled        = false;
    private String             prompt              = "";
    private int                currentCommandIndex = 0;
    private TextWatcher        inputWatcher;
    private InputMethodManager inputManager;
    private OnCommitHandler    commitHandler;
    private final LinkedList<String> commandHistory = new LinkedList<>();
    private ListIterator<String> commandHistoryAccessor;

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
        this.commandHistory.add("");
        this.commandHistoryAccessor = this.commandHistory.listIterator();
        inputWatcher = new TextWatcher() {
            int start, count, numCharsAdded;
            private void restorePrompt() {
                String text = getText().toString();
                int promptMissingEnd = Math.min(prompt.length(), start + count);
                text = text.substring(0, start) + prompt.substring(start, promptMissingEnd) + text.substring(start);
                internalSetText(text);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
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
                if (isInputEnabled()) {
                    if (start < prompt.length()) {
                        restorePrompt();
                    } else if (input.length() > 0) {
                        if (input.indexOf('\n', start) >= prompt.length() && commitHandler != null) {
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
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (!isInputEnabled() && commitHandler != null) {
                    commitHandler.onKeyEventWhileDisabled(event);
                    return true;
                }
                return false;
            }
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
        this.inputEnabled = enabled;
        setCursorVisible(enabled);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * @return {@code true} if this input is enabled to receive input events or {@code false} otherwise.
     */
    public boolean isInputEnabled() {
        return inputEnabled;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                loadLastCommand();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                loadNextCommand();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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
        if (inputEnabled && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isInputMethodTarget()) {
                requestFocus();
                tryRegainSoftInputFocus();
            }
            setCursorVisible(true);
        }
        return super.onTouchEvent(event);
    }

    /**
     * Enables the user to type input.
     *
     * @param prompt The prompt to display
     * @param enqueuedInput The enqueued input from the time this input was disabled.
     */
    @SuppressLint("SetTextI18n")
    public void enableInput(String prompt, String enqueuedInput) {
        this.setEnabled(true);
        this.prompt = prompt;
        setCursorVisible(true);
        setText(prompt + enqueuedInput);
        setSelection(getText().length());
        requestFocus();
        if (!isInputMethodTarget()) {
            tryRegainSoftInputFocus();
        }
        if (enqueuedInput.indexOf('\n') != -1) {
            this.commitHandler.onCommit(this);
        }
    }

    /**
     * Disable this input and clear the input- and prompt-text.
     */
    public void disableInput() {
        this.prompt = "";
        setCursorVisible(false);
        internalSetText("");
        this.setEnabled(false);
    }

    /**
     * Pop the current input. After this function returns, the input will be empty and disabled.
     * Additionally, the input is written to the commandHistory, if appropriate.
     *
     * @return An array consisting of the current prompt-string and the last input.
     */
    public String[] popCurrentInput() {
        String text = this.getText().toString();
        String[] input = {this.prompt, text.substring(this.prompt.length(), text.length())};
        if (input[1].length() != 0) {
            int index = input[1].indexOf('\n');
            String newCommand = input[1].substring(0, index != -1 ? index : input[1].length());
            if (newCommand.length() != 0) {
                commandHistory.set(0, newCommand);
                commandHistory.addFirst("");
                commandHistoryAccessor = commandHistory.listIterator(0);
            }
        }
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
     * Overwrite the current input with the input that was entered before the current.
     * (Key "Up" on Pc terminals.)
     */
    public void loadLastCommand() {
        if (commandHistoryAccessor.hasNext()) {
            if (commandHistoryAccessor.nextIndex() == currentCommandIndex) {
                commandHistoryAccessor.next();
                if (!commandHistoryAccessor.hasNext()) {
                    return;
                }
            }
            currentCommandIndex = commandHistoryAccessor.nextIndex();
            internalSetText(this.prompt + commandHistoryAccessor.next());
            setSelection(getText().length());
        }
    }

    /**
     * Overwrite te current input with the input that was entered after the current input.
     * (Key "Down" on Pc terminals.)
     */
    public void loadNextCommand() {
        if (commandHistoryAccessor.hasPrevious()) {
            if (commandHistoryAccessor.previousIndex() == currentCommandIndex) {
                commandHistoryAccessor.previous();
                if (!commandHistoryAccessor.hasPrevious()) {
                    return;
                }
            }
            currentCommandIndex = commandHistoryAccessor.previousIndex();
            internalSetText(this.prompt + commandHistoryAccessor.previous());
            setSelection(getText().length());
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (prompt == null || prompt.equals("")) return;
        int newSelStart = -1, newSelEnd = -1;
        int min = prompt.length();
        if (selStart < min) newSelStart = min;
        if (selEnd < min) newSelEnd = min;
        if (newSelEnd != -1 || newSelStart != -1) {
            setSelection(newSelStart, newSelEnd);
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (prompt == null || text.toString().startsWith(prompt)) super.setText(text, type);
    }

    /**
     * Tries to regain the focus of the soft input method.
     */
    private void tryRegainSoftInputFocus() {
        this.inputManager.restartInput(this);
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
}
