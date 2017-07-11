package com.apython.python.pythonhost.views.terminal;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

/**
 * Created by Sebastian on 09.07.2017.
 */

class TerminalTextSpan {
    private ForegroundColorSpan foreground = null;
    private BackgroundColorSpan background = null;
    private StyleSpan           style      = null;

    TerminalTextSpan(TerminalTextSpan other) {
        if (other == null) return;
        this.foreground = other.foreground;
        this.background = other.background;
    }

    TerminalTextSpan setForeground(ForegroundColorSpan foreground) {
        this.foreground = foreground;
        return this;
    }

    TerminalTextSpan setBackground(BackgroundColorSpan background) {
        this.background = background;
        return this;
    }

    TerminalTextSpan setStyle(int style) {
        return setStyle(style, true);
    }
    
    TerminalTextSpan setStyle(int style, boolean enable) {
        if (this.style == null) {
            this.style = null;
        } else {
            style = enable ? style | this.style.getStyle() : this.style.getStyle() & ~style;
            this.style = new StyleSpan(style);
        }
        return this;
    }

    CharSequence apply(CharSequence text, int startIndex, int endIndex) {
        Spannable spannable = text instanceof Spannable ?
                (Spannable) text : new SpannableString(text);
        
        if (foreground != null) {
            spannable.setSpan(foreground, startIndex, endIndex, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (background != null) {
            spannable.setSpan(background, startIndex, endIndex, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (style != null) {
            spannable.setSpan(style, startIndex, endIndex, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        
        return spannable;
    }
}
