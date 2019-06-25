package com.apython.python.pythonhost.views.terminalwm;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.apython.python.pythonhost.views.PythonFragment;

/**
 * A Fragment capable of displaying multiple tab-based windows.
 * 
 * Created by Sebastian on 26.03.2016.
 */
public class WindowManagerFragmentTabHost extends WindowManagerTabHost {
    
    private class FragmentTab extends Tab {
        PythonFragment fragment;
    }
    
    public class FragmentTabSpec extends TabSpec {
        private       Class<? extends PythonFragment> fragmentClass;
        private final Activity                        activity;
        
        FragmentTabSpec(Activity activity, String tag) {
            super(tag);
            this.activity = activity;
        }

        FragmentTabSpec setFragmentClass(Class<? extends PythonFragment> fragmentClass) {
            this.fragmentClass = fragmentClass;
            return this;
        }

        @Override
        protected Tab createTab() {
            FragmentTab tab = new FragmentTab();
            initTab(tab);
            tab.fragment = PythonFragment.create(fragmentClass, activity, tab.tag);
            return tab;
        }
    }

    public WindowManagerFragmentTabHost(Context context) {
        this(context, null);
    }

    public WindowManagerFragmentTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void displayTabContent(Tab tab) {
        if (tab instanceof FragmentTab && tab.view == null) {
            tab.view = ((FragmentTab) tab).fragment.createView(tabContentContainer);
        }
        super.displayTabContent(tab);
    }

    @Override
    protected void removeTabContent(Tab tab) {
        if (tab instanceof FragmentTab && tab.view == null) {
            ((FragmentTab) tab).view = tabContentContainer.getChildAt(0);
        }
        super.removeTabContent(tab);
    }

    @Override
    public void removeTab(Tab tab, int index) {
        super.removeTab(tab, index);
        if (tab instanceof FragmentTab) {
            ((ViewGroup) findViewById(android.R.id.tabcontent)).removeView(tab.view);
        }
    }

    public FragmentTabSpec getFragmentTabSpec(Activity activity, String tag) {
        return new FragmentTabSpec(activity, tag);
    }

    public WindowManagerTabWidget getTabWidget(String windowTag) {
        return findTabByTag(windowTag).tabIndicator;
    }
    
    public PythonFragment getCurrentFragment() {
        return ((FragmentTab) getCurrentTab()).fragment;
    }
}
