package com.apython.python.pythonhost.downloadcenter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
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
class PythonVersionListItemView {
    private Activity activity;
    private String   mainPythonVersion;
    private PythonVersionItem            installedSubVersion  = null;
    private ArrayList<PythonVersionItem> availableSubVersions = new ArrayList<>();
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
    private View.OnClickListener actionButtonClickListener = new View.OnClickListener() {
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
                                            String.format("%s Python version %s failed!", 
                                                          state.selectedSubVersion.isInstalled() ?
                                                                  "Downloading" : "Updating",
                                                          state.selectedSubVersion.getPythonVersion()),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
            pyInfoValue.setText("");
            pyInfoText.setText("");
            if (!state.selectedSubVersion.isInstalled()) {
                state.selectedSubVersion.setAction(Dependency.Action.DOWNLOAD);
            }
            if (downloadHandler != null) {
                downloadHandler.onAction(state.selectedSubVersion, progressHandler);
            }
        }
    };

    private View.OnLongClickListener actionButtonDeleteListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    activity, R.style.AppDialogTheme);
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
                    installedSubVersion.setAction(Dependency.Action.REMOVE); // TODO: this falsly removes libPythonPatch if another py version is installed
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
            return true;
        }
    };

    private class State {
        PythonVersionItem selectedSubVersion = null;
        boolean           detailViewShown    = false;
    }
    
    PythonVersionListItemView(Activity activity, String mainPythonVersion) {
        super();
        this.mainPythonVersion = mainPythonVersion;
        this.activity = activity;
    }
    
    View getView(Context context, View preserveView, ViewGroup parent) {
        if (preserveView == null) {
            preserveView = LayoutInflater.from(context)
                    .inflate(R.layout.download_center_list_item, parent, false);
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
            for (PythonVersionItem subVersion : availableSubVersions) {
                subversionAdapter.add(subVersion.getPythonVersion());
            }
        }
        pythonSubversionContainer.setAdapter(subversionAdapter);
        actionButton.setOnClickListener(this.actionButtonClickListener);
        actionButton.setOnLongClickListener(this.actionButtonDeleteListener);
        updateView();
        return preserveView;
    }
    
    private void updateView() {
        if (titleView == null) {
            return;
        }
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
                    pythonSubversionContainer.setSelection(availableSubVersions.indexOf(state.selectedSubVersion));
                }
                pythonSubversionContainer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                        PythonVersionItem selectedVersion = availableSubVersions.get(position);
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
                        onModuleConfigButtonClick();
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
    
    private void onModuleConfigButtonClick() {
        Context context = new ContextThemeWrapper(
                activity.getApplicationContext(), R.style.AppDialogTheme);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                activity, R.style.AppDialogTheme);
        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(context).inflate(
                R.layout.download_center_python_module_dialog, null);
        dialogBuilder.setView(dialogView);
        ListView moduleListView = (ListView) dialogView.findViewById(
                R.id.dialog_center_python_modules_list);
        ArrayList<PythonModuleItem> additionalModules = state.selectedSubVersion.getAdditionalModules();
        ArrayAdapter<PythonModuleItem> moduleItemAdapter = new ArrayAdapter<PythonModuleItem>(
                context, android.R.layout.simple_list_item_1, additionalModules) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = new CheckBox(getContext());
                }
                CheckBox checkBox = (CheckBox) convertView;
                final PythonModuleItem module = getItem(position);
                assert module != null;
                checkBox.setText(module.getModuleName());
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
        };
        moduleListView.setAdapter(moduleItemAdapter);
        moduleListView.setEmptyView(dialogView.findViewById(R.id.dialog_center_python_modules_list_no_modules));
        dialogBuilder.setTitle("Additional Python Modules");
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                updateModuleInfo();
            }
        });
        dialogBuilder.show();
    }
    
    private void configureViewForDownload() {
        titleView.setText(activity.getString(R.string.python_version_format, mainPythonVersion));
        mainProgressBar.setVisibility(View.GONE);
        detailProgressBar.setVisibility(View.GONE);
        pythonSubversionContainer.setVisibility(View.VISIBLE);
        subversionText.setVisibility(View.VISIBLE);
        moduleConfigButton.setVisibility(View.VISIBLE);
        mainContainer.setOnTouchListener(null);
        actionButton.setImageResource(R.drawable.add);
        actionButton.setClickable(true);
        actionButton.setLongClickable(false);
        pyInfoText.setText(R.string.downloadManager_download_size);
        pyInfoValue.setVisibility(View.VISIBLE);
        updateModuleInfo();
    }

    private void configureViewDuringAction() {
        titleView.setText(activity.getString(R.string.python_version_format, mainPythonVersion));
        mainProgressBar.setVisibility(View.VISIBLE);
        detailProgressBar.setVisibility(View.VISIBLE);
        pythonSubversionContainer.setVisibility(View.GONE);
        subversionText.setVisibility(View.GONE);
        moduleConfigButton.setVisibility(View.GONE);
        actionButton.setClickable(false);
        actionButton.setLongClickable(false);
        actionButton.setImageResource(R.drawable.downloading_icon);
        mainContainer.setOnTouchListener(null);
    }

    private void configureViewInstalled() {
        titleView.setText(activity.getString(R.string.python_version_format, mainPythonVersion));
        mainProgressBar.setVisibility(View.GONE);
        detailProgressBar.setVisibility(View.GONE);
        pythonSubversionContainer.setVisibility(View.VISIBLE);
        subversionText.setVisibility(View.VISIBLE);
        moduleConfigButton.setVisibility(View.VISIBLE);
        actionButton.setImageResource(R.drawable.installed);
        actionButton.setClickable(true);
        actionButton.setLongClickable(true);
        updateModuleInfo();
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
        // TODO: Update Icon if modules need changing
    }

    String getMainPythonVersion() {
        return mainPythonVersion;
    }

    void setActionHandler(PythonVersionListAdapter.ActionHandler downloadHandler) {
        this.downloadHandler = downloadHandler;
    }

    boolean matchesFilter(String filter) {
        String pyVersion = mainPythonVersion;
        if (isInstalled()) pyVersion = installedSubVersion.getPythonVersion();
        if (("python " + pyVersion).contains(filter)) {
            return true;
        }
        String filterPyVersion = filter.replace("python", "").trim();
        if (filterPyVersion.split("\\.").length == 3) {
            for (PythonVersionItem subVersion : availableSubVersions) {
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

    void addPythonSubVersion(PythonVersionItem subVersion) {
        PythonVersionItem prev = null;
        for (PythonVersionItem availableSubVersion : availableSubVersions) {
            if (availableSubVersion.getPythonVersion().equals(subVersion.getPythonVersion())) {
                prev = availableSubVersion;
            }
        }
        if (prev != null) {
            prev.updateFromDependency(subVersion);
        } else {
            availableSubVersions.add(subVersion);
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

    ArrayList<PythonVersionItem> getSubVersions() {
        return new ArrayList<>(availableSubVersions);
    }

    void removeSubVersion(PythonVersionItem subVersion) {
        availableSubVersions.remove(subVersion);
        if (state.selectedSubVersion == subVersion) {
            state.selectedSubVersion = availableSubVersions.get(0);
            updateView();
        }
    }

    boolean containsInformation() {
        for (PythonVersionItem subVersion : availableSubVersions) {
            if (subVersion.isInstalled() || (subVersion.getUrl() != null && subVersion.getMd5Checksum() != null)) {
                return true;
            }
        }
        return false;
    }
}
