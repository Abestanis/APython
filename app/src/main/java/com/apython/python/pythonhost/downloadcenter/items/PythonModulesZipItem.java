package com.apython.python.pythonhost.downloadcenter.items;

import android.content.Context;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;

import java.io.File;

/**
 * {@inheritDoc}
 * 
 * This dependency describes the standard Python modules zip.
 * 
 * Created by Sebastian on 13.04.2016.
 */
public class PythonModulesZipItem extends Dependency {
    private String pythonVersion = null;

    PythonModulesZipItem(Context context) {
        super(context);
    }

    public void setPythonVersion(String pythonVersion) {
        this.pythonVersion = pythonVersion;
        setInstallLocation(null);
    }

    @Override
    public String getId() {
        return "pyZip/" + pythonVersion;
    }

    @Override
    public Dependency setInstallLocation(File file) {
        installLocation = new File(getInstallDir(), "python"
                + Util.getMainVersionPart(pythonVersion).replace(".", "") + ".zip");
        return this;
    }

    @Override
    protected File getInstallDir() {
        return PackageManager.getStandardLibPath(getContext());
    }

    @Override
    protected int getActionSteps() {
        return 1;
    }

    @Override
    protected String getUIDescription() {
        return "the Python standard modules archive";
    }
}
