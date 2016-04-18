package com.apython.python.pythonhost.downloadcenter.items;

import android.content.Context;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;

import java.io.File;

/**
 * {@inheritDoc}
 * 
 * This dependency describes an additional Python module.
 * 
 * Created by Sebastian on 13.04.2016.
 */
public class PythonModuleItem extends Dependency {
    private String moduleName = null;
    private String pythonVersion;

    public PythonModuleItem(Context context, String pythonVersion) {
        super(context);
        this.pythonVersion = pythonVersion;
    }

    @Override
    public String getId() {
        return "pyModule/" + pythonVersion + "/" + moduleName;
    }

    @Override
    public Dependency setInstallLocation(File file) {
        super.setInstallLocation(file);
        moduleName = installLocation.getName().replace(".so", "");
        return this;
    }

    @Override
    protected File getInstallDir() {
        return PackageManager.getLibDynLoad(getContext(), Util.getMainVersionPart(pythonVersion));
    }

    @Override
    protected int getActionSteps() {
        return 1;
    }

    @Override
    protected String getUIDescription() {
        return "additional Python module " + moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Action getAction() {
        return action;
    }
}
