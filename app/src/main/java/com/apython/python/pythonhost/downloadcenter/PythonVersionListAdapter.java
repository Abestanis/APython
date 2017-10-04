package com.apython.python.pythonhost.downloadcenter;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.downloadcenter.items.AdditionalLibraryItem;
import com.apython.python.pythonhost.downloadcenter.items.DataItem;
import com.apython.python.pythonhost.downloadcenter.items.Dependency;
import com.apython.python.pythonhost.downloadcenter.items.PythonVersionItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Adapter to show all available and installed Python versions
 * in the {@link PythonDownloadCenterActivity}.
 *
 * Created by Sebastian on 16.08.2015.
 */

class PythonVersionListAdapter extends BaseAdapter {
    
    private Activity activity;
    private Context  context;
    private Map<String, AdditionalLibraryItem>     additionalLibraries = new HashMap<>();
    private Map<String, DataItem>                  dataItems           = new HashMap<>();
    private Map<String, PythonVersionListItemView> versionMap          = new HashMap<>();
    private ArrayList<PythonVersionListItemView>   filteredItemList    = new ArrayList<>();
    private String                                 filterString        = "";
    
    interface ActionHandler {
        void onAction(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler);
    }
    
    private ActionHandler actionHandler = null;

    PythonVersionListAdapter(Activity activity) {
        super();
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return filteredItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return filteredItemList.get(position).getView(context, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return filteredItemList.isEmpty();
    }

    void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    void setFilter(String filter) {
        filter = filter.trim().toLowerCase();
        if (!filterString.equals(filter)) {
            filterString = filter;
            checkForChangedUI();
        }

    }

    private void applyFilter(Map<String, PythonVersionListItemView> map) {
        if ("".equals(filterString)) {
            return;
        }
        for (Iterator<Map.Entry<String, PythonVersionListItemView>> items = map.entrySet().iterator(); items.hasNext(); ) {
            PythonVersionListItemView item = items.next().getValue();
            if (!item.matchesFilter(filterString)) {
                items.remove();
            }
        }
    }

    private void checkForChangedUI() {
        Map<String, PythonVersionListItemView> newFilteredItemList = new HashMap<>(versionMap);
        applyFilter(newFilteredItemList);
        boolean uiChanged = false;
        for (Iterator<PythonVersionListItemView> items = filteredItemList.iterator(); items.hasNext(); ) {
            PythonVersionListItemView listItemView = items.next();
            if (!newFilteredItemList.containsKey(listItemView.getMainPythonVersion())
                    || !newFilteredItemList.get(listItemView.getMainPythonVersion()).equals(listItemView)) {
                uiChanged = true;
                items.remove();
            }
        }
        for (PythonVersionListItemView item : newFilteredItemList.values()) {
            if (!filteredItemList.contains(item)) {
                uiChanged = true;
                filteredItemList.add(item);
            }
        }
        Collections.sort(filteredItemList, new Comparator<PythonVersionListItemView>() {
            @Override
            public int compare(PythonVersionListItemView lhs, PythonVersionListItemView rhs) {
                String[] lVersionParts = lhs.getMainPythonVersion().split("\\.");
                String[] rVersionParts = rhs.getMainPythonVersion().split("\\.");
                if (lVersionParts[0].equals(rVersionParts[0])) {
                    return Integer.valueOf(lVersionParts[1]) - Integer.valueOf(rVersionParts[1]);
                }
                return Integer.valueOf(lVersionParts[0]) - Integer.valueOf(rVersionParts[0]);
            }
        });
        if (uiChanged) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    void updateInstalledLibraries() {
        ArrayList<String> installedLibraries = PackageManager.getInstalledDynLibraries(context);
        for (Iterator<Map.Entry<String, PythonVersionListItemView>> entries = versionMap.entrySet().iterator(); entries.hasNext();) {
            Map.Entry<String, PythonVersionListItemView> entry = entries.next();
            String mainVersion = entry.getKey();
            PythonVersionListItemView listItemView = entry.getValue();
            if (installedLibraries.contains("python" + mainVersion)) {
                if (!listItemView.isInstalled()) {
                    String version = PackageManager.getDetailedInstalledVersion(context, mainVersion);
                    listItemView.addPythonSubVersion(new PythonVersionItem(context, version));
                }
                installedLibraries.remove("python" + mainVersion);
            }
            if (!listItemView.containsInformation()) {
                entries.remove();
            }
        }
        for (AdditionalLibraryItem library : additionalLibraries.values()) {
            if (installedLibraries.contains(library.getLibraryName())) {
                installedLibraries.remove(library.getLibraryName());
            }
        }
        // TODO: Update additionalLibraries
        for (String newLibrary : installedLibraries) {
            if (newLibrary.startsWith("python") && Character.isDigit(newLibrary.charAt(6))) {
                String pythonMainVersion = newLibrary.replace("python", "");
                PythonVersionListItemView pythonVersionItem;
                if (!versionMap.containsKey(pythonMainVersion)) {
                    pythonVersionItem = new PythonVersionListItemView(activity, pythonMainVersion);
                    versionMap.put(pythonMainVersion, pythonVersionItem);
                } else {
                    pythonVersionItem = versionMap.get(pythonMainVersion);
                }
                PythonVersionItem pythonVersion = new PythonVersionItem(
                        context, PackageManager.getDetailedInstalledVersion(context, pythonMainVersion));
                pythonVersionItem.addPythonSubVersion(pythonVersion);
                pythonVersionItem.setActionHandler(actionHandler);
            }
        }
        checkForChangedUI();
    }

    boolean onUpdatedDownloads(DownloadServer.ServerDownloads downloads) {
        // Update data items //
        this.dataItems.clear();
        for (DataItem dataItem : downloads.dataItems) {
            dataItems.put(dataItem.getId(), dataItem);
        }
        // Update library items //
        this.additionalLibraries.clear();
        for (AdditionalLibraryItem library : downloads.libraries) {
            additionalLibraries.put(library.getId(), library);
        }
        /* Update the views */
        for (Iterator<Map.Entry<String, PythonVersionListItemView>> items = versionMap.entrySet().iterator(); items.hasNext();) {
            PythonVersionListItemView listItemView = items.next().getValue();
            for (PythonVersionItem subVersion : listItemView.getSubVersions()) {
                boolean found = false;
                for (PythonVersionItem pythonVersion : downloads.pythonVersions) {
                    if (subVersion.getPythonVersion().equals(pythonVersion.getPythonVersion())) {
                        found = true;
                        subVersion.updateFromDependency(pythonVersion);
                        break;
                    }
                }
                if (!found && !listItemView.isInstalled()) {
                    listItemView.removeSubVersion(subVersion);
                }
            }
            if (!listItemView.containsInformation()) {
                items.remove();
            }
        }
        for (PythonVersionItem pythonVersion : downloads.pythonVersions) {
            String mainVersion = Util.getMainVersionPart(pythonVersion.getPythonVersion());
            PythonVersionListItemView pythonVersionView;
            if (versionMap.containsKey(mainVersion)) {
                pythonVersionView = versionMap.get(mainVersion);
            } else {
                pythonVersionView = new PythonVersionListItemView(activity, mainVersion);
                pythonVersionView.setActionHandler(actionHandler);
                versionMap.put(mainVersion, pythonVersionView);
            }
            pythonVersionView.addPythonSubVersion(pythonVersion);
        }
        checkForChangedUI();
        return true;
    }

    /*
     * Check the installed versions for errors (e.g. missing Python library zips)
     */
    void checkInstalledVersions() {
        // TODO: Implement
    }
}
