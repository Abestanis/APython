package com.apython.python.pythonhost.downloadcenter;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.downloadcenter.items.AdditionalLibraryItem;
import com.apython.python.pythonhost.downloadcenter.items.DataItem;
import com.apython.python.pythonhost.downloadcenter.items.Dependency;
import com.apython.python.pythonhost.downloadcenter.items.PythonModuleItem;
import com.apython.python.pythonhost.downloadcenter.items.PythonModulesZipItem;
import com.apython.python.pythonhost.downloadcenter.items.PythonVersionItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

public class PythonVersionListAdapter extends BaseAdapter {
    final short FILTER_OUT_INSTALLED    = 0x1000;
    final short FILTER_OUT_DOWNLOADABLE = 0x0100;
    
    private Activity activity;
    private Context  context;
    private Map<String, AdditionalLibraryItem>     additionalLibraries = new HashMap<>();
    private Map<String, DataItem>                  dataItems           = new HashMap<>();
    private Map<String, PythonVersionListItemView> versionMap          = new HashMap<>();
    private ArrayList<PythonVersionListItemView>   filteredItemList    = new ArrayList<>();
    private short                                  filterSettings      = 0x0000;
    private String                                 filterString        = "";

    private class Requirement {

        String id;

        boolean                satisfied    = true;
        ArrayList<Requirement> requirements = new ArrayList<>();
        ArrayList<Requirement> requiredFor  = new ArrayList<>();
        public Requirement(String id) {
            super();
            this.id = id;
        }
        public void setUnsatisfied() {
            if (satisfied) {
                satisfied = false;
                for (Requirement requirement : requiredFor) {
                    requirement.setUnsatisfied();
                }
            }
        }

    }
    public interface ActionHandler {

        void onAction(Dependency dependency, ProgressHandler.TwoLevelProgressHandler progressHandler);
    }
    private ActionHandler actionHandler = null;

    public PythonVersionListAdapter(Activity activity) {
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

    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public void setFilter(String filter) {
        filter = filter.trim().toLowerCase();
        if (!filterString.equals(filter)) {
            filterString = filter;
            checkForChangedUI();
        }

    }

    public void applyFilter(Map<String, PythonVersionListItemView> map) {
        if ("".equals(filterString)) {
            return;
        }
        for (Iterator<Map.Entry<String, PythonVersionListItemView>> items = map.entrySet().iterator(); items.hasNext(); ) {
            PythonVersionListItemView item = items.next().getValue();
            if ((filterSettings & FILTER_OUT_INSTALLED) != 0) {
                if (item.isInstalled()) {
                    items.remove();
                    continue;
                }
            } else if ((filterSettings & FILTER_OUT_DOWNLOADABLE) != 0) {
                if (!item.isInstalled()) {
                    items.remove();
                    continue;
                }
            }
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

    public void updateInstalledLibraries() {
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
        // TODO: Update dataItems
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
                pythonVersion.setInstallLocation(new File(PackageManager.getDynamicLibraryPath(context), newLibrary));
                pythonVersionItem.addPythonSubVersion(pythonVersion);
                pythonVersionItem.setActionHandler(actionHandler);
            }
        }
        checkForChangedUI();
    }

    public boolean parseJSONData(JSONObject root, String serverUrl) {
        ArrayList<String> versions = new ArrayList<>(root.length());
        for (Iterator<String> keys = root.keys(); keys.hasNext();) versions.add(keys.next());
        // Get the data version
        int dataVersion = 1;
        try {
            dataVersion = root.getInt("__version__");
            versions.remove("__version__");
        } catch (JSONException e) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion +
                    ") No version identifier found in response, assuming version 1.");
        }
        if (dataVersion != 1) {
            Log.w(MainActivity.TAG, "Could not parse json data: Unsupported data version " + dataVersion);
            return false;
        }
        Map<String, Requirement> requirements = new HashMap<>();
        /* requirements */
        try {
            if (root.has("requirements")) {
                JSONObject jsonRequirements = root.getJSONObject("requirements");
                versions.remove("requirements");
                for (Iterator<String> requirementObjects = jsonRequirements.keys()
                     ; requirementObjects.hasNext();) {
                    String requirementDescription = requirementObjects.next();
                    JSONArray jsonRequirementList = jsonRequirements.getJSONArray(requirementDescription);
                    Requirement requirementObject;
                    if (requirements.containsKey(requirementDescription)) {
                        requirementObject = requirements.get(requirementDescription);
                    } else {
                        requirementObject = new Requirement(requirementDescription);
                        requirements.put(requirementDescription, requirementObject);
                    }
                    for (int i = 0; i < jsonRequirementList.length(); i++) {
                        String requirementId = jsonRequirementList.getString(i);
                        if (requirementId.startsWith("androidSdk/")) {
                            int minAndroidSdk;
                            try {
                                minAndroidSdk = Integer.valueOf(requirementId.replace("androidSdk/", ""));
                            } catch (NumberFormatException e) {
                                Log.w(MainActivity.TAG, "(Version " + dataVersion +
                                        ") Failed to parse minAndroidSdk version for requirement " +
                                        requirementDescription + ": " + requirementId +
                                        ", ignoring it!", e);
                                minAndroidSdk = Build.VERSION_CODES.BASE;
                            }
                            if (minAndroidSdk > Build.VERSION.SDK_INT) {
                                requirementObject.setUnsatisfied();
                                break;
                            }
                            continue;
                        }
                        Requirement requirement;
                        if (requirements.containsKey(requirementId)) {
                            requirement = requirements.get(requirementId);
                            if (!requirement.satisfied) {
                                requirementObject.setUnsatisfied();
                                break;
                            }
                        } else {
                            requirement = new Requirement(requirementId);
                            requirements.put(requirementId, requirement);
                        }
                        requirementObject.requirements.add(requirement);
                        requirement.requiredFor.add(requirementObject);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(MainActivity.TAG, "(Version " + dataVersion + ") Failed to parse the requirement tree!", e);
            return false;
        }
        /* data */
        try {
            if (root.has("data")) {
                JSONObject jsonDataObject = root.getJSONObject("data");
                versions.remove("data");
                for (Iterator<String> dataObjects = jsonDataObject.keys()
                     ; dataObjects.hasNext();) {
                    String dataName = dataObjects.next();
                    if (requirements.containsKey("data/" + dataName)
                            && !requirements.get("data/" + dataName).satisfied) {
                        break;
                    }
                    JSONObject jsonDataInfo = jsonDataObject.getJSONObject(dataName);
                    String url, md5Checksum;
                    File destination;
                    JSONArray downloadInfo = jsonDataInfo.getJSONArray("path");
                    if (downloadInfo.length() < 2) {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") The entries for the data " + dataName + " has an invalid length (" + downloadInfo.length() + " instead of 2).");
                        if (!requirements.containsKey("data/" + dataName)) {
                            requirements.put("data/" + dataName, new Requirement("data/" + dataName));
                        }
                        requirements.get("data/" + dataName).setUnsatisfied();
                        continue;
                    }
                    url = serverUrl + "/" + downloadInfo.getString(0);
                    md5Checksum = downloadInfo.getString(1);
                    destination = new File(PackageManager.getDataPath(context), jsonDataInfo.optString("dest", "").replaceFirst("data/", ""));
                    DataItem dataItem = dataItems.get("data/" + dataName);
                    if (dataItem == null) {
                        dataItem = new DataItem(context);
                        dataItem.setUrl(url).setMd5Checksum(md5Checksum).setInstallLocation(destination);
                        dataItems.put(dataItem.getId(), dataItem);
                    } else {
                        dataItem.setUrl(url).setMd5Checksum(md5Checksum).setInstallLocation(destination);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(MainActivity.TAG, "(Version " + dataVersion + ") Failed to parse the 'data' entry!", e);
            return false;
        }
        /* libraries */
        Map<String, AdditionalLibraryItem> downloadableLibraries = new HashMap<>();
        try {
            if (root.has("libraries")) {
                JSONObject jsonLibrariesObject = root.getJSONObject("libraries");
                versions.remove("libraries");
                for (Iterator<String> additionalLibraries = jsonLibrariesObject.keys()
                     ; additionalLibraries.hasNext();) {
                    String libraryName = additionalLibraries.next();
                    if (requirements.containsKey("libraries/" + libraryName)
                            && !requirements.get("libraries/" + libraryName).satisfied) {
                        continue;
                    }
                    JSONObject libData = jsonLibrariesObject.getJSONObject(libraryName);
                    JSONArray library = null;
                    for (String abi : PackageManager.getSupportedCPUABIS()) {
                        if (libData.has(abi)) {
                            library = libData.getJSONArray(abi);
                            break;
                        }
                    }
                    if (library == null) {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") The library " + libraryName + " is not available for the current CPU Architecture.");
                        if (!requirements.containsKey("libraries/" + libraryName)) {
                            requirements.put("libraries/" + libraryName, new Requirement("libraries/" + libraryName));
                        }
                        requirements.get("libraries/" + libraryName).setUnsatisfied();
                        continue;
                    }
                    if (library.length() < 2) {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") The entries for the library " + libraryName + " has an invalid length (" + library.length() + " instead of 2).");
                        if (!requirements.containsKey("libraries/" + libraryName)) {
                            requirements.put("libraries/" + libraryName, new Requirement("libraries/" + libraryName));
                        }
                        requirements.get("libraries/" + libraryName).setUnsatisfied();
                        continue;
                    }
                    AdditionalLibraryItem libraryItem = new AdditionalLibraryItem(context);
                    libraryItem.setUrl(serverUrl + "/" + library.getString(0)).setMd5Checksum(library.getString(1));
                    downloadableLibraries.put(libraryItem.getId(), libraryItem);
                }
            }
        } catch (JSONException e) {
            Log.e(MainActivity.TAG, "(Version " + dataVersion + ") Failed to parse the 'library' entry!", e);
            return false;
        }
        // Filter unsatisfied requirements
        for (Map.Entry<String, Requirement> requirementEntry : requirements.entrySet()) {
            if (!requirementEntry.getValue().satisfied && downloadableLibraries.containsKey(requirementEntry.getKey())) {
                downloadableLibraries.remove(requirementEntry.getKey());
            }
        }
        // Fill requirements
        for (Requirement requirementItem : requirements.values()) {
            if (requirementItem.satisfied && !requirementItem.id.startsWith("pyModule/")) {
                AdditionalLibraryItem additionalLibrary = downloadableLibraries.get(requirementItem.id);
                for (Requirement requirement : requirementItem.requirements) {
                    if (requirement.id.startsWith("data/")) {
                        additionalLibrary.addDependency(dataItems.get(requirement.id));
                    } else {
                        additionalLibrary.addDependency(downloadableLibraries.get(requirement.id));
                    }
                }
            }
        }
        // Update the list
        for (Iterator<AdditionalLibraryItem> libraries = additionalLibraries.values().iterator(); libraries.hasNext();) {
            AdditionalLibraryItem additionalLibrary = libraries.next();
            AdditionalLibraryItem downloadableLibrary = downloadableLibraries.get(additionalLibrary.getId());
            if (downloadableLibrary != null) {
                additionalLibrary.updateFromDependency(downloadableLibrary);
            } else if (!additionalLibrary.isInstalled()) {
                libraries.remove();
            }
        }
        for (AdditionalLibraryItem downloadableLibrary : downloadableLibraries.values()) {
            if (!additionalLibraries.containsKey(downloadableLibrary.getId())) {
                additionalLibraries.put(downloadableLibrary.getId(), downloadableLibrary);
            }
        }
        /* Python versions */
        ArrayList<PythonVersionItem> pythonVersions = new ArrayList<>();
        for (String pythonVersion : versions) {
            try {
                JSONObject jsonVersionData = root.getJSONObject(pythonVersion);
                // Find the correct system dependent data
                JSONObject abiSpecificData = null;
                for (String abi : PackageManager.getSupportedCPUABIS()) {
                    if (jsonVersionData.has(abi)) {
                        //Log.d(MainActivity.TAG, "Found supported abi: " + abi);
                        abiSpecificData = jsonVersionData.getJSONObject(abi);
                        break;
                    }
                }
                if (abiSpecificData == null) {
                    Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Found no supported platform libraries for Python version " + pythonVersion);
                    Log.d(MainActivity.TAG, "Supported platform ABIs: " + Arrays.toString(PackageManager.getSupportedCPUABIS()));
                    continue;
                }
                // Get the data of the Python library
                if (!abiSpecificData.has("pythonLib")) {
                    Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + pythonVersion + "': Data does not contain the Python library.");
                    continue;
                }
                JSONArray pythonLibData = abiSpecificData.getJSONArray("pythonLib");
                if (pythonLibData.length() < 2) {
                    Log.w(MainActivity.TAG, "(Version " + dataVersion + ") The entries for the Python library for version " + pythonVersion + " has an invalid length (" + pythonLibData.length() + " instead of 2).");
                    continue;
                }
                PythonVersionItem pythonVersionItem = new PythonVersionItem(context, pythonVersion);
                pythonVersionItem.setUrl(serverUrl + "/" + pythonLibData.getString(0)).setMd5Checksum(pythonLibData.getString(1));
                pythonVersionItem.addDependency(additionalLibraries.get("libraries/pythonPatch"));
                // Get the moduleZip data
                JSONArray pythonModuleZipData = jsonVersionData.optJSONArray("lib");
                if (pythonModuleZipData == null) {
                    Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + pythonVersion + "': No information about the lib file given!");
                    continue;
                }
                if (pythonModuleZipData.length() != 2) {
                    Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + pythonVersion + "': Lib data has an invalid length (" + pythonModuleZipData.length() + ")!");
                    continue;
                }
                PythonModulesZipItem pythonModulesZipItem = pythonVersionItem.getPythonModulesZip();
                pythonModulesZipItem.setUrl(serverUrl + "/" + pythonModuleZipData.getString(0)).setMd5Checksum(pythonModuleZipData.getString(1));
                pythonVersions.add(pythonVersionItem);
                // Parse the modules
                for (Iterator<String> moduleNames = abiSpecificData.keys(); moduleNames.hasNext();) {
                    String moduleName = moduleNames.next();
                    if ("pythonLib".equals(moduleName)) {
                        continue;
                    }
                    Requirement moduleRequirement = requirements.get("pyModule/" + moduleName);
                    if (moduleRequirement != null && !moduleRequirement.satisfied) {
                        continue;
                    }
                    JSONArray moduleData = abiSpecificData.optJSONArray(moduleName);
                    if (moduleData == null) {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid module data for version '" + pythonVersion + "': Data for module '" + moduleName + "' is not a JSONArray!");
                        continue;
                    }
                    if (moduleData.length() != 2) {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid module data for version '" + pythonVersion + "': Data for module '" + moduleName + "' has an invalid length (" + moduleData.length() + ")!");
                        continue;
                    }
                    PythonModuleItem moduleItem = new PythonModuleItem(context, pythonVersion);
                    moduleItem.setUrl(serverUrl + "/" + moduleData.getString(0)).setMd5Checksum(moduleData.getString(1));
                    moduleItem.addDependency(pythonVersionItem);
                    if (moduleRequirement != null) {
                        boolean missingRequirement = false;
                        for (Requirement requirement : moduleRequirement.requirements) {
                            if (!additionalLibraries.containsKey(requirement.id)) {
                                missingRequirement = true;
                                break;
                            }
                            moduleItem.addDependency(downloadableLibraries.get(requirement.id));
                        }
                        if (missingRequirement) continue;
                        pythonVersionItem.addAdditionalModule(moduleItem);
                    }
                }
            } catch (JSONException e) {
                Log.e(MainActivity.TAG, "(Version " + dataVersion + ") Failed to parse Python version " + pythonVersion + ". Skipping entry.", e);
            }
        }
        /* Update the views */
        for (Iterator<Map.Entry<String, PythonVersionListItemView>> items = versionMap.entrySet().iterator(); items.hasNext();) {
            PythonVersionListItemView listItemView = items.next().getValue();
            for (PythonVersionItem subVersion : listItemView.getSubVersions()) {
                boolean found = false;
                for (PythonVersionItem pythonVersion : pythonVersions) {
                    if (subVersion.getPythonVersion().equals(pythonVersion.getPythonVersion())) {
                        found = true;
                        subVersion.updateFromDependency(pythonVersion);
                        break;
                    }
                }
                if (!found) {
                    listItemView.removeSubVersion(subVersion);
                }
            }
            if (!listItemView.containsInformation()) {
                items.remove();
            }
        }
        for (PythonVersionItem pythonVersion : pythonVersions) {
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
    public void checkInstalledVersions() {
        // TODO: Implement
    }
}
