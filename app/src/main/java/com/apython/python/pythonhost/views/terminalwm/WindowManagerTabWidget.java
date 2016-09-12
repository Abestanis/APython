package com.apython.python.pythonhost.views.terminalwm;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.apython.python.pythonhost.R;

/**
 * A tabWidget for a window-tab in the {@link WindowManagerTabHost}. 
 * 
 * Created by Sebastian on 26.03.2016.
 */
public class WindowManagerTabWidget extends FrameLayout {
    private View        bottomSeparator;
    private ImageView   icon;
    private ImageView closeButton;
    private TextView    textView;
    private boolean     selected = false;

    public interface OnCloseListener {
        void onClose(WindowManagerTabWidget tabWidget);
    }

    public WindowManagerTabWidget(Context context) {
        this(context, null);
    }

    public WindowManagerTabWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.wm_tab_widget, this);
        bottomSeparator = findViewById(R.id.wm_tabWidget_separator);
        icon = (ImageView) findViewById(R.id.wm_tabWidget_icon);
        closeButton = (ImageView) findViewById(R.id.wm_tabWidget_closeButton);
        textView = (TextView) findViewById(R.id.wm_tabWidget_title);
        setIsSelectedTab(false);
    }

    public void setOnCloseListener(final OnCloseListener listener) {
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClose(WindowManagerTabWidget.this);
            }
        });
    }

    public void setIcon(Drawable icon) {
        this.icon.setVisibility(icon == null ? GONE : VISIBLE);
        this.icon.setImageDrawable(icon);
    }

    public void setTitle(String title) {
        textView.setText(title);
    }

    public String getTitle() {
        return textView.getText().toString();
    }

    public void setIsSelectedTab(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            bottomSeparator.setPadding(bottomSeparator.getPaddingLeft(), selected ? 0 : 6,
                                       bottomSeparator.getPaddingRight(), 0);
        }
    }
}
