package com.apython.python.pythonhost.interpreter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * An input view to use within a terminal.
 *
 * Created by Sebastian on 12.11.2015.
 */
public class TerminalInput extends EditText {

    private class PromptEditableFactory extends Editable.Factory {

        private int promptLength = 0;

        public void setPromptLength(int promptLength) {
            this.promptLength = promptLength;
        }

        @Override
        public Editable newEditable(CharSequence source) {
            return new SpannableStringBuilder(source) {
                @Override
                public void setSpan(Object what, int start, int end, int flags) {
                    super.setSpan(what, Math.max(start, promptLength), Math.max(end, promptLength), flags);
                }
            };
        }
    }

    public interface onCommitHandler {
        void onCommit(TerminalInput terminalInput);
    }

    private boolean inputEnabled = false;
    private String prompt = "";
    private final PromptEditableFactory promptEditableFactory = new PromptEditableFactory();
    private onCommitHandler commitHandler;

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
        this.setEditableFactory(promptEditableFactory);
        this.addTextChangedListener(new TextWatcher() {
            int start, count;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                this.start = start;
                this.count = count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.startsWith(prompt)) {
                    setText(prompt);
                } else if (input.length() > 0) {
                    if (input.indexOf('\n', start) != -1 && commitHandler != null) {
                        commitHandler.onCommit(TerminalInput.this);
                    }
                }
            }
        });
    }

    /**
     * Enables the user to type input.
     *
     * @param prompt The prompt to display
     * @param enqueuedInput The enqueued input from the time this input was disabled.
     */
    @SuppressLint("SetTextI18n")
    public void enableInput(String prompt, String enqueuedInput) {
        this.prompt = prompt;
        this.promptEditableFactory.setPromptLength(prompt.length());
        setText(prompt + enqueuedInput);
        setSelection(getText().length());
        setCursorVisible(true);
        this.setEnabled(true);
    }

    /**
     * Disable this input and clear the input- and prompt-text.
     */
    public void disableInput() {
        this.prompt = "";
        setCursorVisible(false);
        this.promptEditableFactory.setPromptLength(0);
        setText("");
        this.setEnabled(false);
    }

    /**
     * Pop the current input. After this function returns, the input will be empty.
     *
     * @return An array consisting of the current prompt-string and the last input.
     */
    public String[] popCurrentInput() {
        String text = this.getText().toString();
        String[] input = {this.prompt, text.substring(this.prompt.length(), text.length())};
        disableInput();
        return input;
    }

    /**
     * Register an handler to handle enter keys.
     * @param commitHandler The commit handler to register.
     */
    public void setCommitHandler(onCommitHandler commitHandler) {
        this.commitHandler = commitHandler;
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
        if (enabled) { super.setEnabled(true); }
    }

    @Override
    public boolean isEnabled() {
        return inputEnabled;
    }
}
