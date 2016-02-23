package com.apython.python.pythonhost.views.terminalwm;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabWidget;

import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.views.interfaces.TerminalWindowManagerInterface;
import com.apython.python.pythonhost.views.terminal.TerminalFragment;

import java.util.ArrayList;

/**
 * A window manager fragment designed to hold multiple "windows" as tabs.
 * The first "window" will always be a {@link TerminalFragment}.
 *
 * Created by Sebastian on 08.01.2016.
 */

public class TerminalWindowManagerFragment extends Fragment implements TerminalWindowManagerInterface {
    private FragmentTabHost tabHost;
    private TabWidget       tabWidget;
    private static final String                        DEFAULT_UNTITLED_NAME = "Untitled";
    private              ArrayList<String>             windowNames           = new ArrayList<>(5);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.terminal_window_manager_layout, container, false);
        tabHost = (FragmentTabHost) root.findViewById(R.id.tabHost);
        tabHost.setup(getActivity().getApplicationContext(), getActivity().getSupportFragmentManager(),
                      android.R.id.tabcontent);
        tabWidget = tabHost.getTabWidget();
        tabWidget.setBackgroundColor(Color.WHITE);
        // tabWidget.setVisibility(View.GONE);
        FragmentTabHost.TabSpec interpreterTabSpec = tabHost.newTabSpec("Python");
        interpreterTabSpec.setIndicator("Python");
        windowNames.add("Python");
        tabHost.addTab(interpreterTabSpec, TerminalFragment.class, null);
        return root;
    }

    @Override
    public Fragment getCurrentWindow() {
        return getActivity().getSupportFragmentManager().findFragmentByTag(tabHost.getCurrentTabTag());
    }

    private String getUnusedWindowName() {
        return getUnusedWindowName(DEFAULT_UNTITLED_NAME);
    }

    private String getUnusedWindowName(String proposedName) {
        String name = proposedName;
        int i = 1;
        while (windowNames.contains(name)) {
            name = proposedName + " " + i;
            i++;
        }
        return name;
    }
}
