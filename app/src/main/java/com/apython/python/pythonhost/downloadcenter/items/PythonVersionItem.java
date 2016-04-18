package com.apython.python.pythonhost.downloadcenter.items;

import android.content.Context;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;

import java.io.File;
import java.util.ArrayList;

/**
 * {@inheritDoc}
 * 
 * This dependency describes a Python native library.
 * 
 * Created by Sebastian on 13.04.2016.
 */
public class PythonVersionItem extends Dependency {
    private ArrayList<PythonModuleItem> additionalModules = new ArrayList<>();
    private String               pythonVersion;
    private PythonModulesZipItem pythonModulesZip;
    

    public PythonVersionItem(Context context, String pythonVersion) {
        super(context);
        this.pythonVersion = pythonVersion;
        pythonModulesZip = new PythonModulesZipItem(context);
        pythonModulesZip.setPythonVersion(pythonVersion);
    }

    @Override
    public String getId() {
        return "python/" + pythonVersion;
    }

    @Override
    protected File getInstallDir() {
        return PackageManager.getDynamicLibraryPath(getContext());
    }

    @Override
    protected int getActionSteps() {
        return 2;
    }

    @Override
    public int getActionSteps(ArrayList<String> list) {
        int steps = super.getActionSteps(list);
        for (PythonModuleItem additionalModule : additionalModules) {
            if (action == Action.REMOVE) additionalModule.setAction(Action.REMOVE);
            steps += additionalModule.getActionSteps(list);
        }
        return steps;
    }

    @Override
    protected String getUIDescription() {
        return "Python library " + pythonVersion;
    }

    public PythonModulesZipItem getPythonModulesZip() {
        return pythonModulesZip;
    }
    
    public void addAdditionalModule(PythonModuleItem module) {
        if (additionalModules.contains(module)) {
            additionalModules.get(additionalModules.indexOf(module)).updateFromDependency(module);
        } else {
            additionalModules.add(module);
        }
    }
    
    public ArrayList<PythonModuleItem> getAdditionalModules() {
        return new ArrayList<>(additionalModules);
    }

    @Override
    public boolean isInstalled() {
        return super.isInstalled() && pythonModulesZip.isInstalled();
    }

    @Override
    protected boolean download(ProgressHandler.TwoLevelProgressHandler progressHandler) {
        return pythonModulesZip.download(progressHandler) && super.download(progressHandler);
    }

    @Override
    protected boolean remove(ProgressHandler.TwoLevelProgressHandler progressHandler) {
        return pythonModulesZip.remove(progressHandler) && super.remove(progressHandler);
    }

    @Override
    public boolean applyAction(ProgressHandler.TwoLevelProgressHandler progressHandler) {
        Action action = this.action;
        if (super.applyAction(progressHandler)) {
            for (PythonModuleItem additionalModule : additionalModules) {
                if (action == Action.REMOVE) additionalModule.setAction(Action.REMOVE);
                if (!additionalModule.applyAction(progressHandler)) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void updateFromDependency(Dependency dependency) {
        super.updateFromDependency(dependency);
        pythonModulesZip = ((PythonVersionItem) dependency).pythonModulesZip;
        pythonModulesZip.setAction(action);
        additionalModules = ((PythonVersionItem) dependency).additionalModules;
    }

    @Override
    public void setAction(Action action) {
        super.setAction(action);
        pythonModulesZip.setAction(action);
    }

    public String getPythonVersion() {
        return pythonVersion;
    }
}
