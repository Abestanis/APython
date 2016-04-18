package com.apython.python.pythonhost.views.terminalwm;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;

/**
 * A Fragment capable of displaying multiple tab-based windows.
 * 
 * Created by Sebastian on 26.03.2016.
 */
public class WindowManagerFragmentTabHost extends WindowManagerTabHost {

    private FragmentManager fragmentManager = null;

    private class FragmentTab extends Tab {
        Fragment fragment;
    }

    public class FragmentTabSpec extends TabSpec {
        private Class<?> fragmentClass;

        public FragmentTabSpec() {
            super();
        }

        public FragmentTabSpec(String tag) {
            super(tag);
        }

        public FragmentTabSpec setFragmentClass(Class<?> fragmentClass) {
            this.fragmentClass = fragmentClass;
            return this;
        }

        @Override
        protected Tab createTab() {
            FragmentTab tab = new FragmentTab();
            initTab(tab);
            tab.fragment = Fragment.instantiate(getContext(), fragmentClass.getName());
            return tab;
        }
    }

    public WindowManagerFragmentTabHost(Context context) {
        this(context, null);
    }

    public WindowManagerFragmentTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    protected void displayTabContent(Tab tab) {
        if (tab instanceof FragmentTab && tab.view == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(android.R.id.tabcontent, ((FragmentTab) tab).fragment, ((FragmentTab) tab).tag);
            transaction.commit();
        } else {
            super.displayTabContent(tab);
        }
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
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.detach(((FragmentTab) tab).fragment);
            transaction.commit();
        }
    }

    public FragmentTabSpec getFragmentTabSpec() {
        return new FragmentTabSpec();
    }

    public FragmentTabSpec getFragmentTabSpec(String tag) {
        return new FragmentTabSpec(tag);
    }
}
