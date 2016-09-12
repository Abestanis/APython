package com.apython.python.pythonhost.downloadcenter.items;

import android.content.Context;
import android.util.Log;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.ProgressHandler;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;

import java.io.File;

/**
 * {@inheritDoc}
 * 
 * This dependency describes a data dependency, stored under
 * the directory returned by {@link PackageManager#getDataPath(Context)}.
 * 
 * Created by Sebastian on 13.04.2016.
 */
public class DataItem extends Dependency {
    private String dataName = null;
    private boolean shouldExtract = false;

    public DataItem(Context context) {
        super(context);
    }

    @Override
    public Dependency setUrl(String url) {
        super.setUrl(url);
        checkIfShouldExtract();
        return this;
    }

    private void checkIfShouldExtract() {
        String destExt = Util.getFileExt(installLocation);
        shouldExtract = destExt == null || !destExt.equals(Util.getFileExt(new File(url)));
    }

    @Override
    public Dependency setInstallLocation(File file) {
        super.setInstallLocation(file);
        if (dataName == null) {
            dataName = file.getName();
            String ext = Util.getFileExt(file);
            if (ext != null) dataName = dataName.replace("." + ext, "");
        }
        checkIfShouldExtract();
        return this;
    }

    @Override
    public String getId() {
        return "data/" + dataName;
    }

    @Override
    protected File getInstallDir() {
        return PackageManager.getDataPath(getContext());
    }

    @Override
    protected int getActionSteps() {
        return 1;
    }

    @Override
    protected String getUIDescription() {
        return dataName;
    }

    @Override
    protected boolean download(ProgressHandler.TwoLevelProgressHandler progressHandler) {
        if (!shouldExtract) {
            return super.download(progressHandler);
        } else {
            File tempArchive = new File(getContext().getCacheDir(), new File(url).getName());
            if (progressHandler != null) progressHandler.enable(getContext().getString(R.string.downloading_object, getUIDescription()));
            if (!Util.downloadFile(url, tempArchive, md5Checksum, progressHandler)) return false;
            if (progressHandler != null) progressHandler.enable(getContext().getString(R.string.extracting_object, getUIDescription()));
            boolean success = Util.extractArchive(tempArchive, installLocation, progressHandler);
            if (!tempArchive.delete()) {
                Log.w(MainActivity.TAG, "Could not remove downloaded temporary archive " + tempArchive.getAbsolutePath());
            }
            return success && Util.makePathAccessible(installLocation.getParentFile(), getContext().getFilesDir());
        }
    }
}
