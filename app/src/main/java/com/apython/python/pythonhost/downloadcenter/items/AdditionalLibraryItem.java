package com.apython.python.pythonhost.downloadcenter.items;

import android.content.Context;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;

import java.io.File;

/**
 * {@inheritDoc}
 * 
 * This dependency describes a native library.
 * 
 * Created by Sebastian on 13.04.2016.
 */
public class AdditionalLibraryItem extends Dependency {
    private String libraryName = null;

    public AdditionalLibraryItem(Context context) {
        super(context);
    }

    @Override
    public Dependency setUrl(String url) {
        super.setUrl(url);
        if (libraryName == null) this.libraryName = Util.getLibraryName(new File(url));
        return this;
    }

    @Override
    public Dependency setInstallLocation(File file) {
        super.setInstallLocation(file);
        if (libraryName == null) libraryName = Util.getLibraryName(file);
        return this;
    }

    @Override
    public String getId() {
        return "libraries/" + libraryName;
    }

    @Override
    protected File getInstallDir() {
        return PackageManager.getDynamicLibraryPath(getContext());
    }

    @Override
    protected int getActionSteps() {
        return 1;
    }

    @Override
    protected String getUIDescription() {
        return "library " + libraryName;
    }

    public String getLibraryName() {
        return libraryName;
    }
}
