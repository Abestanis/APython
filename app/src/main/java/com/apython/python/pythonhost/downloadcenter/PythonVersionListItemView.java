package com.apython.python.pythonhost.downloadcenter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.downloadcenter.items.Dependency;
import com.apython.python.pythonhost.downloadcenter.items.PythonModuleItem;
import com.apython.python.pythonhost.downloadcenter.items.PythonVersionItem;

import java.util.ArrayList;

/**
 * Defines the UI of a Python version item in the download center list.
 * 
 * Created by Sebastian on 02.04.2016.
 */
public class PythonVersionListItemView {
    private Activity activity;
    private String   mainPythonVersion;
    private PythonVersionItem            installedSubVersion  = null;
    private ArrayList<PythonVersionItem> avaliableSubVersions = new ArrayList<>();
    private TextView                     titleView            = null;
    private ImageView   actionButton;
    private ImageView   dropdownButton;
    private View        mainContainer;
    private FrameLayout detailViewContainer;
    private Spinner     pythonSubversionContainer;
    private ImageView   moduleConfigButton;
    private TextView    moduleDescription;
    private TextView    subversionText;
    private ProgressBar mainProgressBar;
    private ProgressBar detailProgressBar;
    private ArrayAdapter<String>                   subversionAdapter = null;
    private State                                  state             = new State();
    private PythonVersionListAdapter.ActionHandler downloadHandler   = null;
    private TextView pyInfoValue;
    private TextView pyInfoText;
    private View.OnTouchListener deleteSwipeDetector = null;

    private class State {

        PythonVersionItem selectedSubVersion = null;

        boolean           detailViewShown    = false;
    }
    public PythonVersionListItemView(final Activity activity, String mainPythonVersion) {
        super();
        this.mainPythonVersion = mainPythonVersion;
        this.activity = activity;
    }
    public View getView(Context context, View preserveView, ViewGroup parent) {
        if (preserveView == null) {
            preserveView = LayoutInflater.from(context).inflate(R.layout.download_center_list_item, parent, false);
        }
        mainContainer = preserveView;
        titleView = (TextView) preserveView.findViewById(R.id.download_center_item_title);
        actionButton = (ImageView) preserveView.findViewById(R.id.download_center_item_action_button);
        dropdownButton = (ImageView) preserveView.findViewById(R.id.download_center_item_dropdown_button);
        detailViewContainer = (FrameLayout) preserveView.findViewById(R.id.download_center_detail_container);
        mainProgressBar = (ProgressBar) preserveView.findViewById(R.id.download_center_item_total_progress_view);
        pythonSubversionContainer = (Spinner) detailViewContainer.findViewById(R.id.download_center_python_subversion_container);
        if (pythonSubversionContainer == null) {
            if (detailViewContainer.getChildCount() != 0) {
                detailViewContainer.removeAllViews();
            }
            LayoutInflater.from(context).inflate(R.layout.download_center_python_details_view, detailViewContainer);
            pythonSubversionContainer = (Spinner) detailViewContainer.findViewById(R.id.download_center_python_subversion_container);
        }
        moduleConfigButton = (ImageView) detailViewContainer.findViewById(R.id.download_center_python_module_config_button);
        moduleDescription = (TextView) detailViewContainer.findViewById(R.id.download_center_python_modules_info_text);
        detailProgressBar = (ProgressBar) detailViewContainer.findViewById(R.id.download_center_python_progress_view);
        pyInfoText = (TextView) detailViewContainer.findViewById(R.id.download_center_python_info_text);
        pyInfoValue = (TextView) detailViewContainer.findViewById(R.id.download_center_python_info_value_text);
        subversionText = (TextView) detailViewContainer.findViewById(R.id.download_center_python_subversion_title);
        if (subversionAdapter == null) {
            subversionAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
            for (PythonVersionItem subVersion : avaliableSubVersions) {
                subversionAdapter.add(subVersion.getPythonVersion());
            }
        }
        pythonSubversionContainer.setAdapter(subversionAdapter);
        updateView();
        return preserveView;
    }
    private void updateView() {
        if (titleView != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dropdownButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            state.detailViewShown = !state.detailViewShown;
                            detailViewContainer.setVisibility(state.detailViewShown
                                                                      ? View.VISIBLE : View.GONE);
                            Animation animation = AnimationUtils.loadAnimation(
                                    activity.getApplicationContext(),
                                    state.detailViewShown ? R.anim.python_version_list_expand_info :
                                            R.anim.python_version_list_collaps_info
                            );
                            v.startAnimation(animation);
                        }
                    });
                    if (state.selectedSubVersion != null) {
                        pythonSubversionContainer.setSelection(avaliableSubVersions.indexOf(state.selectedSubVersion));
                    }
                    pythonSubversionContainer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                            PythonVersionItem selectedVersion = avaliableSubVersions.get(position);
                            if (selectedVersion != state.selectedSubVersion) {
                                state.selectedSubVersion = selectedVersion;
                                updateView();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                    detailViewContainer.setVisibility(state.detailViewShown ? View.VISIBLE : View.GONE);
                    moduleConfigButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                            @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(
                                    activity.getApplicationContext()).inflate(R.layout.download_center_python_module_dialog, null);
                            dialogBuilder.setView(dialogView);
                            ListView moduleListView = (ListView) dialogView.findViewById(R.id.dialog_center_python_modules_list);
                            ArrayList<PythonModuleItem> additionalModules = state.selectedSubVersion.getAdditionalModules();
                            moduleListView.setAdapter(new ArrayAdapter<PythonModuleItem>(
                                    activity.getApplicationContext(),
                                    android.R.layout.simple_list_item_1, additionalModules) {
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    if (convertView == null) {
                                        convertView = new CheckBox(activity.getApplicationContext());
                                    }
                                    CheckBox checkBox = (CheckBox) convertView;
                                    final PythonModuleItem module = getItem(position);
                                    checkBox.setText(module.getModuleName());
                                    checkBox.setTextColor(Color.BLACK);
                                    checkBox.setOnCheckedChangeListener(null);
                                    checkBox.setChecked(module.isInstalled() || module.getAction() == Dependency.Action.DOWNLOAD);
                                    checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            module.setAction(isChecked ? Dependency.Action.DOWNLOAD : Dependency.Action.REMOVE);
                                        }
                                    });
                                    return convertView;
                                }
                            });
                            dialogBuilder.setTitle("Additional Python Modules");
                            dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    updateModuleInfo();
                                }
                            });
                            dialogBuilder.show();
                        }
                    });
                    if (isInstalled()) {
                        configureViewInstalled();
                    } else {
                        configureViewForDownload();
                    }
                }
            });
        }
    }
    
    private void configureViewForDownload() {
        titleView.setText("Python " + mainPythonVersion);
        mainProgressBar.setVisibility(View.GONE);
        detailProgressBar.setVisibility(View.GONE);
        pythonSubversionContainer.setVisibility(View.VISIBLE);
        subversionText.setVisibility(View.VISIBLE);
        moduleConfigButton.setVisibility(View.VISIBLE);
        mainContainer.setOnTouchListener(null);
        actionButton.setImageResource(R.drawable.add);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressHandler.TwoLevelProgressHandler progressHandler = ProgressHandler.Factory
                        .createTwoLevel(
                                activity, pyInfoText, mainProgressBar,
                                detailProgressBar, new Runnable() {
                                    @Override
                                    public void run() {
                                        configureViewDuringAction();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                        if (state.selectedSubVersion.isInstalled()) {
                                            installedSubVersion = state.selectedSubVersion;
                                        }
                                        updateView();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                        if (state.selectedSubVersion.isInstalled()) {
                                            installedSubVersion = state.selectedSubVersion;
                                        }
                                        updateView();
                                        Toast.makeText(
                                                activity.getApplicationContext(),
                                                "Downloading Python version " +
                                                        state.selectedSubVersion.
                                                                getPythonVersion()
                                                        + " failed!",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                pyInfoValue.setText("");
                pyInfoText.setText("");
                state.selectedSubVersion.setAction(Dependency.Action.DOWNLOAD);
                if (downloadHandler != null) {
                    downloadHandler.onAction(state.selectedSubVersion, progressHandler);
                }
            }
        });
        actionButton.setClickable(true);
        pyInfoText.setText(R.string.downloadManager_download_size);
        pyInfoValue.setVisibility(View.VISIBLE);
        updateModuleInfo();
    }

    private void configureViewDuringAction() {
        titleView.setText("Python " + state.selectedSubVersion.getPythonVersion());
        mainProgressBar.setVisibility(View.VISIBLE);
        detailProgressBar.setVisibility(View.VISIBLE);
        pythonSubversionContainer.setVisibility(View.GONE);
        subversionText.setVisibility(View.GONE);
        moduleConfigButton.setVisibility(View.GONE);
        actionButton.setClickable(false);
        actionButton.setImageResource(R.drawable.downloading_icon);
        mainContainer.setOnTouchListener(null);
    }

    private void configureViewInstalled() {
        titleView.setText("Python " + installedSubVersion.getPythonVersion());
        mainProgressBar.setVisibility(View.GONE);
        detailProgressBar.setVisibility(View.GONE);
        pythonSubversionContainer.setVisibility(View.VISIBLE);
        subversionText.setVisibility(View.VISIBLE);
        moduleConfigButton.setVisibility(View.VISIBLE);
        actionButton.setImageResource(R.drawable.installed);
        actionButton.setOnClickListener(null);
        actionButton.setClickable(true);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressHandler.TwoLevelProgressHandler progressHandler = ProgressHandler.Factory
                        .createTwoLevel(
                                activity, pyInfoText, mainProgressBar,
                                detailProgressBar, new Runnable() {
                                    @Override
                                    public void run() {
                                        configureViewDuringAction();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                        if (state.selectedSubVersion.isInstalled()) {
                                            installedSubVersion = state.selectedSubVersion;
                                        }
                                        updateView();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {
                                        if (state.selectedSubVersion.isInstalled()) {
                                            installedSubVersion = state.selectedSubVersion;
                                        }
                                        updateView();
                                        Toast.makeText(
                                                activity.getApplicationContext(),
                                                "Updating Python version " +
                                                        state.selectedSubVersion.
                                                                getPythonVersion()
                                                        + " failed!",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        );
                pyInfoValue.setText("");
                pyInfoText.setText("");
                if (downloadHandler != null) {
                    downloadHandler.onAction(state.selectedSubVersion, progressHandler);
                }
            }
        });
        updateModuleInfo();
        if (deleteSwipeDetector == null) {
            deleteSwipeDetector = new View.OnTouchListener() {
                float startX;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float dist;
                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        dist = startX - event.getX();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            mainContainer.setAlpha(1.0f - (dist / 100.0f));
                        }
                        if (dist > 100.0f) {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                            dialogBuilder.setTitle("Delete Python version " + installedSubVersion.getPythonVersion() + "?");
                            dialogBuilder.setCancelable(true);
                            dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            dialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    installedSubVersion.setAction(Dependency.Action.REMOVE);
                                    ProgressHandler.TwoLevelProgressHandler progressHandler = ProgressHandler.Factory
                                            .createTwoLevel(
                                                    activity, pyInfoText, mainProgressBar,
                                                    detailProgressBar, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            configureViewDuringAction();
                                                        }
                                                    }, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (installedSubVersion != null && !installedSubVersion.isInstalled()) {
                                                                installedSubVersion = null;
                                                            }
                                                            updateView();
                                                        }
                                                    }, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            PythonVersionItem prevInstalledSubVersion = installedSubVersion;
                                                            if (!installedSubVersion.isInstalled()) {
                                                                installedSubVersion = null;
                                                            }
                                                            updateView();
                                                            Toast.makeText(
                                                                    activity.getApplicationContext(),
                                                                    "Removing Python version " +
                                                                            prevInstalledSubVersion.getPythonVersion()
                                                                            + " failed!",
                                                                    Toast.LENGTH_LONG
                                                            ).show();
                                                        }
                                                    }
                                            );
                                    downloadHandler.onAction(installedSubVersion, progressHandler);
                                    dialog.cancel();
                                }
                            });
                            dialogBuilder.show();
                            startX = Float.MIN_VALUE;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            mainContainer.setAlpha(1.0f);
                        }
                        break;
                    }
                    return true;
                }
            };
        }
        mainContainer.setOnTouchListener(deleteSwipeDetector);
        pyInfoText.setText(R.string.downloadManager_used_storage_space);
        String[] formattedSpace = Util.getFormattedBytes(PackageManager.getUsedStorageSpace(
                activity.getApplicationContext(), Util.getMainVersionPart(installedSubVersion.getPythonVersion())));
        pyInfoValue.setText(formattedSpace[0] + " " + formattedSpace[1]);
        pyInfoValue.setVisibility(View.VISIBLE);
    }

    private void updateModuleInfo() {
        ArrayList<String> toDownload = new ArrayList<>();
        ArrayList<String> toRemove   = new ArrayList<>();
        ArrayList<String> installed  = new ArrayList<>();
        for (PythonModuleItem module : state.selectedSubVersion.getAdditionalModules()) {
            if (module.isInstalled()) {
                if (module.getAction() == Dependency.Action.REMOVE) {
                    toRemove.add(module.getModuleName());
                } else {
                    installed.add(module.getModuleName());
                }
            } else if (module.getAction() == Dependency.Action.DOWNLOAD) {
                toDownload.add(module.getModuleName());
            }
        }
        int installedSize  = installed.size();
        int downloadedSize = toDownload.size();
        int removedSize    = toRemove.size();
        if (installedSize == 0 && removedSize == 0 && downloadedSize == 0) {
            if (state.selectedSubVersion.isInstalled()) {
                moduleDescription.setText(R.string.downloadManager_no_modules_installed);
            } else {
                moduleDescription.setText(R.string.downloadManager_no_modules_selected);
            }
        } else {
            String moduleInfoText = "";
            if (downloadedSize > 0) {
                moduleInfoText = activity.getApplicationContext().getString(
                        R.string.downloadManager_module_info_download,
                        Util.join(", ", toDownload)
                );
                if (installedSize > 0 || removedSize > 0) {
                    if (installedSize > 0 && removedSize > 0) {
                        moduleInfoText += ", ";
                    } else {
                        moduleInfoText += " and";
                    }
                }
            }
            if (removedSize > 0) {
                moduleInfoText += activity.getApplicationContext().getString(
                        R.string.downloadManager_module_info_remove,
                        Util.join(", ", toRemove)
                );
                if (installedSize > 0) {
                    moduleInfoText += " and";
                }
            }
            if (installedSize > 0) {
                moduleInfoText += activity.getApplicationContext().getString(
                        R.string.downloadManager_module_info_installed,
                        Util.join(", ", installed)
                );
            }
            moduleDescription.setText(moduleInfoText);
        }
    }

    public String getMainPythonVersion() {
        return mainPythonVersion;
    }

    public void setActionHandler(PythonVersionListAdapter.ActionHandler downloadHandler) {
        this.downloadHandler = downloadHandler;
    }

    public boolean matchesFilter(String filter) {
        String pyVersion = mainPythonVersion;
        if (isInstalled()) pyVersion = installedSubVersion.getPythonVersion();
        if (("python " + pyVersion).contains(filter)) {
            return true;
        }
        String filterPyVersion = filter.replace("python", "").trim();
        if (filterPyVersion.split("\\.").length == 3) {
            for (PythonVersionItem subVersion : avaliableSubVersions) {
                if (subVersion.getPythonVersion().equals(filterPyVersion)) {
                    state.selectedSubVersion = subVersion;
                    state.detailViewShown = true;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInstalled() {
        return installedSubVersion != null && installedSubVersion.isInstalled();
    }

    public void addPythonSubVersion(PythonVersionItem subVersion) {
        PythonVersionItem prev = null;
        for (PythonVersionItem avaliableSubVersion : avaliableSubVersions) {
            if (avaliableSubVersion.getPythonVersion().equals(subVersion.getPythonVersion())) {
                prev = avaliableSubVersion;
            }
        }
        if (prev != null) {
            prev.updateFromDependency(subVersion);
        } else {
            avaliableSubVersions.add(subVersion);
            if (subversionAdapter != null) subversionAdapter.add(subVersion.getPythonVersion());
        }
        if (subVersion.isInstalled()) {
            if (installedSubVersion != null && installedSubVersion != prev) {
                // TODO: Something isn't right
            } else {
                installedSubVersion = subVersion;
            }
        }
        if (state.selectedSubVersion == null) {
            state.selectedSubVersion = subVersion;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateView();
                }
            });
        } else if (state.selectedSubVersion == prev) {
            state.selectedSubVersion = subVersion;
        }
    }

    public ArrayList<PythonVersionItem> getSubVersions() {
        return new ArrayList<>(avaliableSubVersions);
    }

    public void removeSubVersion(PythonVersionItem subVersion) {
        avaliableSubVersions.remove(subVersion);
        if (state.selectedSubVersion == subVersion) {
            state.selectedSubVersion = avaliableSubVersions.get(0);
            updateView();
        }
    }

    public boolean containsInformation() {
        for (PythonVersionItem subVersion : avaliableSubVersions) {
            if (subVersion.isInstalled() || (subVersion.getUrl() != null && subVersion.getMd5Checksum() != null)) {
                return true;
            }
        }
        return false;
    }
}