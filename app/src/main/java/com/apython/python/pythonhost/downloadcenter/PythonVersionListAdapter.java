package com.apython.python.pythonhost.downloadcenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The Adapter to show all avaliable and installed Python versions
 * in the {@link PythonDownloadCenterActivity}.
 *
 * Created by Sebastian on 16.08.2015.
 */

public class PythonVersionListAdapter extends BaseAdapter {

    private Activity activity;
    private Context  context;
    private ArrayList<View>                  views               = new ArrayList<>();
    private Map<String, PythonVersionObject> versions            = new HashMap<>();
    private Map<String, ArrayList<String>>   additionalLibraries = new HashMap<>();
    private Map<String, ArrayList<Boolean>>  moduleConfiguration = new HashMap<>();
    private Map<String, String[]>            additionalLibData   = new HashMap<>();
    private ArrayList<String>                simpleVersionList   = new ArrayList<>();
    private ArrayList<String>                filteredVersionList = new ArrayList<>();
    private Map<String, ProgressHandler>     progressHandlerList = new HashMap<>();

    interface ActionHandler {
        void onDownload(String version, String[] downloadUrls, String[] md5Hashes,
                        int numRequirements, int numDependencies, int numModules,
                        ProgressHandler progressHandler);

        void onUpdateProgressHandler(String pythonVersion, ProgressHandler progressHandler);
    }

    private ActionHandler actionHandler = null;

    public PythonVersionListAdapter(Activity activity) {
        super();
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return filteredVersionList.size();
    }

    @Override
    public Object getItem(int position) {
        return versions.get(filteredVersionList.get(position));
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
        String version = filteredVersionList.get(position);

        if (convertView != null && views.contains(convertView)) {
            TextView versionText = (TextView) convertView.findViewById(R.id.version_list_main_version_text);
            if (versionText != null && version.equals(Util.getMainVersionPart(versionText.getText().toString()))) {
                return convertView;
            }
        }
        int index = simpleVersionList.indexOf(version);
        if (index < views.size()) {
            return views.get(index);
        }
        View view = getVersionItemView(version, parent);
        views.add(position, view);
        return view;
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
        return filteredVersionList.isEmpty();
    }

    protected View getVersionItemView(final String version, ViewGroup parent) {
        final View view = LayoutInflater.from(context).inflate(R.layout.python_version_list_item, parent, false);
        final ProgressBar totalProgressView = (ProgressBar) view.findViewById(R.id.version_list_total_progress_view);
        final ProgressBar progressView = (ProgressBar) view.findViewById(R.id.version_list_progress_view);
        final ImageView deleteButton = (ImageView) view.findViewById(R.id.version_list_delete_button);
        ImageView dropdownButton = (ImageView) view.findViewById(R.id.version_list_dropdown_button);
        final RelativeLayout infoContainer = (RelativeLayout) view.findViewById(R.id.version_list_info_container);
        final Spinner subversionContainer = (Spinner) view.findViewById(R.id.version_list_subversion_container);
        final ImageView downloadConfButton = (ImageView) view.findViewById(R.id.version_list_download_config_button);
        final TextView infoText = (TextView) view.findViewById(R.id.version_list_info_text);
        final Dialog optionalModulesDialog = new Dialog(activity);

        progressHandlerList.put(version, ProgressHandler.Factory.createTwoLevel(
                activity,
                infoText,
                totalProgressView,
                progressView,
                2,
                new Runnable() {
                    @Override
                    public void run() {
                        totalProgressView.setVisibility(View.VISIBLE);
                        progressView.setVisibility(View.VISIBLE);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        totalProgressView.setVisibility(View.GONE);
                        progressView.setVisibility(View.GONE);
                        configureViewWhileInstalled(view, version);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        totalProgressView.setVisibility(View.GONE);
                        progressView.setVisibility(View.GONE);
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setTitle("Failed to download Python version " + version); // TODO: Use full version
                        builder.setMessage("An error occurred while downloading the python version " + version + ".");
                        builder.show();
                    }
                }));
        actionHandler.onUpdateProgressHandler(version, progressHandlerList.get(version));

        if (PackageManager.isPythonVersionInstalled(context, version)) { // Already installed
            configureViewWhileInstalled(view, version);
        } else {
            configureViewForDownload(view, version);
        }
        dropdownButton.setOnClickListener(new View.OnClickListener() {
            boolean expanded = false;

            @Override
            public void onClick(View v) {
                ImageView button = (ImageView) v;
                expanded = !expanded;
                if (!expanded) {
                    infoContainer.setVisibility(View.GONE);
                    button.startAnimation(AnimationUtils.loadAnimation(context, R.anim.python_version_list_collaps_info));
                } else {
                    infoContainer.setVisibility(View.VISIBLE);
                    if (subversionContainer.getAdapter() == null) {
                        final ArrayList<String> subVersions = getAllAvaliableSubVersions(version);
                        subversionContainer.setAdapter(new ArrayAdapter<String>(context, 0, subVersions) {
                            @Override
                            public View getDropDownView(final int position, View convertView, ViewGroup parent) {
                                if (convertView == null || convertView instanceof TextView) {
                                    return getTextView(context, position);
                                } else {
                                    return convertView;
                                }
                            }

                            @Override
                            public View getView(final int position, View convertView, ViewGroup parent) {
                                if (convertView == null || convertView instanceof TextView) {
                                    return getTextView(context, position);
                                } else {
                                    return convertView;
                                }
                            }

                            private TextView getTextView(Context context, int position) {
                                TextView view = new TextView(context);
                                view.setText(subVersions.get(position));
                                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                return view;
                            }
                        });
                    }
                    optionalModulesDialog.setContentView(R.layout.python_version_list_sub_module_dialog);
                    optionalModulesDialog.setTitle("Select additional modules");
                    ListView modulesConfigContainer = (ListView) optionalModulesDialog.findViewById(R.id.python_version_list_modules_config_container);
                    modulesConfigContainer.setAdapter(new ArrayAdapter<String>(context, 0, versions.get(subversionContainer.getSelectedItem().toString()).modules) {
                        @Override
                        public View getView(final int position, View convertView, ViewGroup parent) {
                            return getDropDownView(position, convertView, parent);
                        }

                        @Override
                        public View getDropDownView(final int position, View recycleView, ViewGroup root) {
                            if (recycleView != null) {
                                return recycleView;
                            } else {
                                LinearLayout layout = new LinearLayout(getContext());
                                CheckBox checkBox = new CheckBox(getContext());
                                checkBox.setText(getItem(position));
                                checkBox.setTextColor(Color.BLACK);
                                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        moduleConfiguration.get(subversionContainer.getSelectedItem().toString()).set(position, isChecked);
                                    }
                                });
                                layout.addView(checkBox);
                                return layout;
                            }
                        }
                    });
                    modulesConfigContainer.setEmptyView(optionalModulesDialog.findViewById(R.id.python_version_list_no_modules));
                    button.startAnimation(AnimationUtils.loadAnimation(context, R.anim.python_version_list_expand_info));
                    downloadConfButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            optionalModulesDialog.show();
                        }
                    });
                }
            }
        });
        final GestureDetector detector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(e2.getX() - e1.getX()) <= Math.abs(e2.getY() - e1.getY())) Log.d(MainActivity.TAG, "Cancelled");
                return Math.abs(e2.getY() - e1.getY()) < 20 ||Math.abs(e2.getX() - e1.getX()) <= Math.abs(e2.getY() - e1.getY());
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d(MainActivity.TAG, "last: " + e2.getX() + ", first: " + e1.getX() + ", distance: " + (e2.getX() - e1.getX()));
                if (PackageManager.isPythonVersionInstalled(context, version) &&
                        e2.getX() < e1.getX() && e2.getX() - e1.getX() < -80) {
                    Log.d(MainActivity.TAG, "Delete Python " + version);
                    deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String fullVersion = PackageManager.getDetailedInstalledVersion(context, version);
                            if (!PackageManager.deletePythonVersion(context, version)) {
                                Log.w(MainActivity.TAG, "Deleting Python " + version + " failed.");
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setIcon(android.R.drawable.ic_dialog_alert);
                                builder.setTitle("Failed to delete Python version " + fullVersion);
                                builder.setMessage("An error occurred while deleting the python version " + fullVersion + ".");
                                builder.show();
                            } else {
                                deleteButton.setVisibility(View.GONE);
                                Log.d(MainActivity.TAG, fullVersion + " is installable: " + versions.get(fullVersion).isInstallable());
                                if (versions.get(fullVersion).isInstallable()) {
                                    configureViewForDownload(view, version);
                                } else {
                                    removeVersion(fullVersion);
                                }
                            }
                        }
                    });
                    deleteButton.setVisibility(View.VISIBLE);
                } else if (e2.getX() > e1.getX() && e2.getX() - e1.getX() > 80) {
                    deleteButton.setVisibility(View.GONE);
                }
                return false;
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });

        return view;
    }

    protected void configureViewForDownload(final View view, final String version) {
        final TextView versionText = (TextView) view.findViewById(R.id.version_list_main_version_text);
        final ProgressBar totalProgressView = (ProgressBar) view.findViewById(R.id.version_list_total_progress_view);
        final ProgressBar progressView = (ProgressBar) view.findViewById(R.id.version_list_progress_view);
        final ImageView actionButton = (ImageView) view.findViewById(R.id.version_list_action_button);
        final Spinner subversionContainer = (Spinner) view.findViewById(R.id.version_list_subversion_container);

        versionText.setText("Python " + version);
        actionButton.setImageResource(R.drawable.add);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionHandler != null) {
                    configureViewDuringDownload(view, version);
                    PythonVersionDownloadable versionDownloadable;
                    if (subversionContainer.getSelectedItem() != null) {
                        versionDownloadable = (PythonVersionDownloadable) versions.get(subversionContainer.getSelectedItem().toString());
                    } else {
                        Log.d(MainActivity.TAG, "Installing " + getAllAvaliableSubVersions(version).get(0) + ", installable = " + versions.get(getAllAvaliableSubVersions(version).get(0)).isInstallable());
                        versionDownloadable = (PythonVersionDownloadable) versions.get(getAllAvaliableSubVersions(version).get(0));
                    }
                    ArrayList<String> downloadUrls = new ArrayList<>();
                    ArrayList<String> md5Hashes = new ArrayList<>();
                    int numRequirements = 0;
                    int numModules = 0;

                    downloadUrls.add(versionDownloadable.downloadUrl);
                    md5Hashes.add(versionDownloadable.md5CheckSum);
                    downloadUrls.add(versionDownloadable.libZipUrl);
                    md5Hashes.add(versionDownloadable.libZipMd5Checksum);

                    if (versionDownloadable.moduleDependencies.containsKey("<General>")) {
                        for (String requiredDependency : versionDownloadable.moduleDependencies.get("<General>")) {
                            if (additionalLibData.keySet().contains(requiredDependency)
                                    && !PackageManager.isAdditionalLibraryInstalled(context, requiredDependency)) {
                                numRequirements++;
                                downloadUrls.add(additionalLibData.get(requiredDependency)[0]);
                                md5Hashes.add(additionalLibData.get(requiredDependency)[1]);
                            }
                        }
                    }

                    ArrayList<Boolean> modulesChosenForDownload = moduleConfiguration.get(versionDownloadable.version);
                    ArrayList<String> dependencies = new ArrayList<>();
                    for (int i = 0; i < versionDownloadable.modules.size(); i++) {
                        if (modulesChosenForDownload.get(i)) {
                            for (String dependency : versionDownloadable.getModuleDependencies(versionDownloadable.modules.get(i))) {
                                if (!dependencies.contains(dependency)
                                        && additionalLibData.keySet().contains(dependency)
                                        && !PackageManager.isAdditionalLibraryInstalled(context, dependency)) {
                                    dependencies.add(dependency);
                                    downloadUrls.add(additionalLibData.get(dependency)[0]);
                                    md5Hashes.add(additionalLibData.get(dependency)[1]);
                                }
                            }
                        }
                    }

                    for (int i = 0; i < versionDownloadable.modules.size(); i++) {
                        if (modulesChosenForDownload.get(i)) {
                            numModules++;
                            downloadUrls.add(versionDownloadable.moduleUrls.get(i));
                            md5Hashes.add(versionDownloadable.modulesMd5Checksum.get(i));
                        }
                    }
                    ProgressHandler.Factory.TwoLevelProgressHandler progressHandler = (ProgressHandler.Factory.TwoLevelProgressHandler) progressHandlerList.get(version);
                    progressHandler.setTotalSteps(2 + numRequirements + dependencies.size() + numModules);
                    totalProgressView.setIndeterminate(true);
                    totalProgressView.setVisibility(View.VISIBLE);
                    progressView.setIndeterminate(true);
                    progressView.setVisibility(View.VISIBLE);
                    actionHandler.onDownload(versionDownloadable.version,
                                             downloadUrls.toArray(new String[downloadUrls.size()]),
                                             md5Hashes.toArray(new String[md5Hashes.size()]),
                                             numRequirements, dependencies.size(), numModules,
                                             progressHandler);
                }
            }
        });
    }

    protected void configureViewDuringDownload(View view, String version) {
        final ImageView actionButton = (ImageView) view.findViewById(R.id.version_list_action_button);

        actionButton.setImageResource(R.drawable.downloading_icon);
        AnimationDrawable animation = (AnimationDrawable) actionButton.getDrawable();
        animation.start();
    }

    protected void configureViewWhileInstalled(View view, String version) {
        final TextView versionText = (TextView) view.findViewById(R.id.version_list_main_version_text);
        final TextView infoText = (TextView) view.findViewById(R.id.version_list_info_text);
        final TextView infoValueText = (TextView) view.findViewById(R.id.version_list_info_value_text);
        final ImageView actionButton = (ImageView) view.findViewById(R.id.version_list_action_button);

        versionText.setText("Python " + PackageManager.getDetailedInstalledVersion(context, version));
        infoText.setText(R.string.used_starage_space);
        String[] storageUsage = Util.getFormattedBytes(PackageManager.getUsedStorageSpace(context, version));
        infoValueText.setText(storageUsage[0] + " " + storageUsage[1]);
        actionButton.setImageResource(R.drawable.installed);
        actionButton.setClickable(false);
    }

    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public void filter(String filterString) {
        if ("".equals(filterString)) {
            if (filteredVersionList.size() == simpleVersionList.size()) { return; }
            filteredVersionList.clear();
            filteredVersionList.addAll(simpleVersionList);
            notifyDataSetChanged();
            return;
        }

        filteredVersionList.clear();
        for (String version : simpleVersionList) {
            if (("Python " + version).contains(filterString)) {
                filteredVersionList.add(version);
            }
        }
        notifyDataSetChanged();
    }

    private ArrayList<String> getAllAvaliableSubVersions(String mainVersion) {
        ArrayList<String> result = new ArrayList<>();
        for (String version : versions.keySet()) {
            if (version.startsWith(mainVersion)) {
                result.add(version);
            }
        }
        return result;
    }

    public void removeVersion(String version) {
        Log.d(MainActivity.TAG, "Removing " + version);
        versions.remove(version);
        int index = simpleVersionList.indexOf(Util.getMainVersionPart(version));
        simpleVersionList.remove(index);
        views.remove(index);
        filteredVersionList.remove(Util.getMainVersionPart(version));
        notifyDataSetChanged();
    }

    public void invalidateVersionList() {
        versions.clear();
        simpleVersionList.clear();
        filteredVersionList.clear();
        additionalLibraries.clear();
    }

    private void invalidateDownloadedVersionItems() {
        ArrayList<String> invalidVersions = new ArrayList<>();
        for (String version : versions.keySet()) {
            if (versions.get(version).isInstallable()) {
                invalidVersions.add(version);
            }
        }
        for (String invalidVersion : invalidVersions) {
            versions.remove(invalidVersion);
        }
        for (String invalidVersion : invalidVersions) {
            String mainVersion = Util.getMainVersionPart(invalidVersion);
            if (getAllAvaliableSubVersions(mainVersion).isEmpty()) {
                simpleVersionList.remove(mainVersion);
                filteredVersionList.remove(mainVersion);
            }

        }
        additionalLibraries.clear();
    }

    public void updateInstalledVersionsList() {
        ArrayList<String> installedVersions = PackageManager.getInstalledPythonVersions(context);
        boolean changedUI = false;
        for (String installedVersion : installedVersions) {
            String version = PackageManager.getDetailedInstalledVersion(context, installedVersion);
            if (!versions.containsKey(version)) {
                versions.put(version, new PythonVersionObject(context, version));
                changedUI = true;
            }
            if (!simpleVersionList.contains(installedVersion)) {
                simpleVersionList.add(installedVersion);
            }
        }
        if (changedUI) {
            filteredVersionList.addAll(simpleVersionList);
            notifyDataSetChanged();
        }
    }

    public void parseJSONData(JSONObject root) {
        JSONArray versionsData = root.names();
        invalidateDownloadedVersionItems();
        // Get the data version
        int dataVersion = 1;
        try {
            dataVersion = root.getInt("__version__");
        } catch (JSONException e) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") No version identifier found in response, assuming version 1.");
        }
        JSONObject additionalLibs = null;
        JSONArray libraries = null;
        try {
            additionalLibs = root.getJSONObject("libraries");
            libraries = additionalLibs.names();
        } catch (JSONException e) {
            Log.w(MainActivity.TAG, "No data about the additional libraries found!");
        }
        if (libraries != null) {
            for (int i = 0; i < libraries.length(); i++) {
                try {
                    JSONObject libData = additionalLibs.getJSONObject(libraries.getString(i));
                    JSONArray library = null;
                    for (String abi : PackageManager.getSupportedCPUABIS()) {
                        if (libData.has(abi)) {
                            //Log.d(MainActivity.TAG, "Found supported abi: " + abi);
                            library = libData.getJSONArray(abi);
                            break;
                        }
                    }
                    if (library == null) {
                        Log.w(MainActivity.TAG, "The library " + libraries.getString(i) + " is not avaliable for the current CPU Architecture.");
                        continue;
                    }
                    if (library.length() < 2) {
                        Log.w(MainActivity.TAG, "The entries for the library " + libraries.getString(i) + " has an invalid length (" + library.length() + " instead of 2).");
                        continue;
                    }
                    ArrayList<String> keys = new ArrayList<>();
                    if (libData.has("required_for")) {
                        JSONArray keysObject = libData.getJSONArray("required_for");
                        for (int j = 0; j < keysObject.length(); j++) {
                            keys.add(keysObject.getString(j));
                        }
                    } else {
                        keys.add("<General>");
                    }
                    for (String key : keys) {
                        if (additionalLibraries.containsKey(key)) {
                            additionalLibraries.get(key).add(libraries.getString(i));
                        } else {
                            ArrayList<String> dep = new ArrayList<>(1);
                            dep.add(libraries.getString(i));
                            additionalLibraries.put(key, dep);
                        }
                    }
                    additionalLibData.put(libraries.getString(i), new String[]{library.getString(0), library.getString(1)});
                } catch (JSONException e) {
                    try {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Failed to parse the additional library " + libraries.getString(i) + "!");
                    } catch (JSONException e1) {
                        Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Failed to parse an additional library!");
                    }
                }
            }
        }
        for (int i = 0; i < versionsData.length(); i++) {
            try {
                if ("__version__".equals(versionsData.getString(i)) || "libraries".equals(versionsData.getString(i))) {
                    continue;
                }
                JSONObject JSONVersionData = root.getJSONObject(versionsData.getString(i));
                String versionString = versionsData.getString(i).trim();
                PythonVersionDownloadable downloadable = new PythonVersionDownloadable(context, versionString, additionalLibraries, JSONVersionData, dataVersion);
                versions.put(versionString, downloadable);
                ArrayList<Boolean> moduleConfig = new ArrayList<>(Collections.nCopies(downloadable.modules.size(), Boolean.FALSE));
                moduleConfiguration.put(versionString, moduleConfig);
                if (!simpleVersionList.contains(Util.getMainVersionPart(versionString))) {
                    simpleVersionList.add(Util.getMainVersionPart(versionString));
                }
            } catch (JSONException e) {
                Log.e(MainActivity.TAG, "(Version " + dataVersion + ") An entry in downloaded JSON file has an unexpected format. Skipping entry.", e);
            }
        }
        filteredVersionList.clear();
        filteredVersionList.addAll(simpleVersionList);
    }

    public ProgressHandler getProgressHandler(String version) {
        return progressHandlerList.get(version);
    }
}
