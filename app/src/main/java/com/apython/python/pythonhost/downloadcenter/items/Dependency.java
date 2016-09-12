package com.apython.python.pythonhost.downloadcenter.items;

import android.content.Context;

import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;

import java.io.File;
import java.util.ArrayList;

/**
 * This represents a dependency that can be installed or downloaded
 * from the data server.
 * 
 * Created by Sebastian on 11.04.2016.
 */
public abstract class Dependency {
    public String getUrl() {
        return url;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }

    public File getInstallLocation() {
        return installLocation;
    }

    public enum Action {DOWNLOAD, REMOVE, NONE}
    private Context context;
    protected ArrayList<Dependency> dependencies    = new ArrayList<>();
    protected String                url             = null;
    protected String                md5Checksum     = null;
    protected File                  installLocation = null;
    protected Action                action          = Action.NONE;
    private   boolean               inAction        = false; 

    public Dependency(Context context) {
        super();
        this.context = context;
    }
    
    protected Context getContext() {
        return context;
    }

    public Dependency setUrl(String url) {
        if (url != null) {
            this.url = url;
            if (installLocation == null) {
                setInstallLocation(new File(getInstallDir(), new File(url).getName()));
            }
        }
        return this;
    }
    
    public Dependency setMd5Checksum(String md5Checksum) {
        if (md5Checksum != null) this.md5Checksum = md5Checksum;
        return this;
    }
    
    public Dependency setInstallLocation(File file) {
        if (file != null) installLocation = file;
        return this;
    }
    
    public void setAction(Action action) {
        for (Dependency dependency : dependencies) {
            dependency.setAction(action);
        }
        if ((action == Action.DOWNLOAD && isInstalled()) || (action == Action.REMOVE && !isInstalled()))
            action = Action.NONE;
        this.action = action;
    }
    
    public void addDependency(Dependency dependency) {
        if (dependency == null) throw new IllegalArgumentException("Tried to add null dependency");
        if (dependencies.contains(dependency)) {
            dependencies.get(dependencies.indexOf(dependency)).updateFromDependency(dependency);
        } else {
            dependencies.add(dependency);
        }
    }
    
    ArrayList<Dependency> getDependencies() {
        return dependencies;
    }

    public int getActionSteps(ArrayList<String> list) {
        int steps = 0;
        if (!list.contains(getId())) {
            list.add(getId());
            for (Dependency dependency : dependencies) {
                steps += dependency.getActionSteps(list);
            }
            if (action != Action.NONE) steps += getActionSteps();
        }
        return steps;
    }
    
    public void updateFromDependency(Dependency dependency) {
        setUrl(dependency.getUrl())
                .setMd5Checksum(dependency.getMd5Checksum())
                .setInstallLocation(dependency.getInstallLocation());
        this.dependencies = dependency.dependencies;
    }

    public boolean isInstalled() {
        return installLocation != null && installLocation.exists();
    }

    public boolean applyAction(ProgressHandler.TwoLevelProgressHandler progressHandler) {
        if (inAction) return true;
        inAction = true;
        boolean success = true;
        if (action != Action.REMOVE) {
            for (Dependency dependency : dependencies) {
                if (!dependency.applyAction(progressHandler)) {
                    return false;
                }
            }
        }
        Action action = this.action;
        setAction(Action.NONE); // Prevent recursion
        switch (action) {
        case DOWNLOAD:
            success = download(progressHandler);
            break;
        case REMOVE:
            success = remove(progressHandler);
            break;
        }
        if (success) {
            if (action == Action.REMOVE) {
                for (Dependency dependency : dependencies) {
                    if (!dependency.applyAction(progressHandler)) {
                        return false;
                    }
                }
            }
        } else {
            setAction(action);
        }
        inAction = false;
        return success;
    }
    
    protected boolean download(ProgressHandler.TwoLevelProgressHandler progressHandler) {
        if (isInstalled()) return true;
        if (progressHandler != null)
            progressHandler.enable(context.getString(R.string.downloading_object, getUIDescription()));
        return Util.downloadFile(url, installLocation, md5Checksum, progressHandler)
                && Util.makePathAccessible(installLocation.getParentFile(), context.getFilesDir());
    }
    
    protected boolean remove (ProgressHandler.TwoLevelProgressHandler progressHandler) {
        if (!isInstalled()) return true;
        if (progressHandler != null)
            progressHandler.enable(context.getString(R.string.removing_object, getUIDescription()));
        if (installLocation.isDirectory()) {
            return Util.deleteDirectory(installLocation);
        }
        return installLocation.delete();
    }
    
    abstract public String getId();
    abstract protected File getInstallDir();
    abstract protected int getActionSteps();
    abstract protected String getUIDescription();
    
    

    @Override
    public boolean equals(Object o) {
        return o instanceof Dependency && getId().equals(((Dependency) o).getId());
    }
}
