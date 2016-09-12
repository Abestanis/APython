package com.apython.python.pythonhost.views.terminalwm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * A custom TabHost that allows the removal of single tabs. 
 * 
 * Created by Sebastian on 29.01.2016.
 */
public class WindowManagerTabHost extends LinearLayout {
    private static final int NO_INDEX = -1;

    protected class Tab {
        View                   view;
        WindowManagerTabWidget tabIndicator;
        String                 tag;
        Tab previousSelected = null;
    }
    
    private final HorizontalScrollView tabIndicatorScrollContainer;
    protected     LinearLayout         tabIndicatorContainer;
    protected     FrameLayout          tabContentContainer;
    private ArrayList<Tab> tabs    = new ArrayList<>();
    private int            currTab = NO_INDEX;

    public class TabSpec {

        private String mTag;
        private String   tabTitle   = null;
        private Drawable tabIcon    = null;
        private View     tabContent = null;

        public TabSpec() {
            this(null);
        }

        public TabSpec(String tag) {
            mTag = tag;
        }

        /**
         * Specify a label as the tab title.
         */
        public TabSpec setTitle(String title) {
            tabTitle = title;
            return this;
        }

        /**
         * Specify an icon as the tab icon.
         */
        public TabSpec setIcon(Drawable icon) {
            tabIcon = icon;
            return this;
        }

        /**
         * Specify a view as the tab content.
         */
        public TabSpec setContent(View view) {
            tabContent = view;
            return this;
        }

        public String getTag() {
            return mTag;
        }

        protected Tab createTab() {
            Tab tab = new Tab();
            initTab(tab);
            return tab;
        }

        protected void initTab(Tab tab) {
            tab.tabIndicator = new WindowManagerTabWidget(getContext());
            if (tabTitle != null) {
                tab.tabIndicator.setTitle(tabTitle);
            }
            if (tabIcon != null) {
                tab.tabIndicator.setIcon(tabIcon);
            }
            tab.tabIndicator.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
            tab.tag = getTag();
            tab.view = tabContent;
        }

    }
    public WindowManagerTabHost(Context context) {
        this(context, null);
    }

    public WindowManagerTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);
        tabIndicatorContainer = new LinearLayout(context);
        tabIndicatorContainer.setOrientation(HORIZONTAL);
        tabContentContainer = new FrameLayout(context);
        tabIndicatorScrollContainer = new HorizontalScrollView(context);
        tabIndicatorScrollContainer.setBackgroundColor(Color.WHITE);

        tabContentContainer.setFocusable(false);
        tabContentContainer.setFocusableInTouchMode(false);
        tabIndicatorContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tabIndicatorContainer.setVisibility(GONE);
        tabContentContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        tabContentContainer.setId(android.R.id.tabcontent);
        tabIndicatorScrollContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        tabIndicatorScrollContainer.addView(tabIndicatorContainer);
        addView(tabIndicatorScrollContainer);
        addView(tabContentContainer);
        if (isInEditMode()) {
            setupEditMode();
        }
    }

    @SuppressLint("SetTextI18n")
    private void setupEditMode() {
        TextView testContent1 = new TextView(getContext());
        TextView testContent2 = new TextView(getContext());
        testContent1.setTextColor(Color.WHITE);
        testContent2.setTextColor(Color.WHITE);
        testContent1.setText("Hello World");
        testContent2.setText("Hello World 2");
        Drawable systemAppIcon;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            systemAppIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon, getContext().getTheme());
        } else {
            //noinspection deprecation
            systemAppIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }

        addTab(new TabSpec().setTitle("Selected Tab").setContent(testContent1).setIcon(systemAppIcon));
        addTab(new TabSpec().setTitle("Unselected Tab").setContent(testContent2));
    }

    public void addTab(TabSpec tabSpec) {
        final Tab newTab = tabSpec.createTab();
        newTab.tabIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentTab(newTab, tabs.indexOf(newTab));
            }
        });
        newTab.tabIndicator.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
        tabIndicatorContainer.addView(newTab.tabIndicator);
        tabs.add(newTab);
        if (tabs.size() == 2) tabIndicatorContainer.setVisibility(VISIBLE);
        if (currTab == NO_INDEX) {
            setCurrentTab(newTab, 0);
        }
    }

    protected Tab findTabByTag(String tag) {
        for (Tab tab : tabs) {
            if (tag.equals(tab.tag)) {
                return tab;
            }
        }
        throw new InvalidParameterException("No tab was found with the tag '" + tag + "'");
    }

    public void setCurrentTab(String tag) {
        Tab tab = findTabByTag(tag);
        setCurrentTab(tab, tabs.indexOf(tab));
    }

    public void setCurrentTab(int index) {
        setCurrentTab(tabs.get(index), index);
    }

    private void setCurrentTab(Tab tab, int index) {
        if (currTab == index) return;
        if (currTab != NO_INDEX) {
            Tab prevTab = tabs.get(currTab);
            unSetTab(prevTab);
            tab.previousSelected = prevTab;

        }
        tab.tabIndicator.setIsSelectedTab(true);
        int xOffset = 0;
        for (Tab tab_left : tabs) {
            if (tab_left == tab) break;
            xOffset += tab_left.tabIndicator.getWidth();
        }
        final int finalXOffset = xOffset;
        tabIndicatorScrollContainer.post(new Runnable() {
            @Override
            public void run() {
                tabIndicatorScrollContainer.scrollTo(finalXOffset, 0);
            }
        });
        displayTabContent(tab);
        currTab = index;
    }

    protected void displayTabContent(Tab tab) {
        tabContentContainer.addView(tab.view);
    }

    protected void removeTabContent(Tab tab) {
        tabContentContainer.removeView(tab.view);
    }

    private void unSetTab(Tab tab) {
        tab.tabIndicator.setIsSelectedTab(false);
        removeTabContent(tab);
    }

    public void removeTab(String tag) {
        Tab tab = findTabByTag(tag);
        removeTab(tab, tabs.indexOf(tab));
    }

    public void removeTabAt(int index) {
        removeTab(tabs.get(index), index);
    }

    public void removeTab(Tab tab, int index) {
        tabs.remove(index);
        if (tabs.size() == 1) tabIndicatorContainer.setVisibility(GONE);
        if (currTab == index) {
            currTab = NO_INDEX;
            removeTabContent(tab);
            if (tab.previousSelected != null) {
                setCurrentTab(tabs.indexOf(tab.previousSelected));
            } else if (tabs.size() != 0) {
                setCurrentTab(0);
            }
        }
        tabIndicatorContainer.removeViewAt(index);
        for (Tab tb : tabs) {
            if (tb.previousSelected == tab) {
                tb.previousSelected = tab.previousSelected;
            }
        }
    }

    public String getTabTitle(String tag) {
        return findTabByTag(tag).tabIndicator.getTitle();
    }

    public void setTabTitle(String tag, String title) {
        findTabByTag(tag).tabIndicator.setTitle(title);
    }

    public String getCurrentTabTag() {
        return tabs.get(currTab).tag;
    }

    public TabSpec getTabSpec() {
        return new TabSpec();
    }

    public TabSpec getTabSpec(String tag) {
        return new TabSpec(tag);
    }
    
    public void setTabIcon(String tag, Drawable icon) {
        findTabByTag(tag).tabIndicator.setIcon(icon);
    }
}