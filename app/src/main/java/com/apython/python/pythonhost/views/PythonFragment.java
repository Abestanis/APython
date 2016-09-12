package com.apython.python.pythonhost.views;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

/**
 * A Fragment implementation
 * 
 * Created by Sebastian on 28.08.2016.
 */
public abstract class PythonFragment {
    private final Activity activity;
    private final String   tag;
    
    public static <T extends PythonFragment> T create(Class<T> fragment, Activity activity, String tag) {
        try {
            return fragment.getConstructor(Activity.class, String.class).newInstance(activity, tag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected PythonFragment(Activity activity, String tag) {
        super();
        this.activity = activity;
        this.tag = tag;
    }
    
    public abstract View createView(ViewGroup parent);

    protected Activity getActivity() {
        return activity;
    }

    public String getTag() {
        return tag;
    }
}
