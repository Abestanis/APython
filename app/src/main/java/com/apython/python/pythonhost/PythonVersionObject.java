package com.apython.python.pythonhost;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Python version.
 *
 * Created by Sebastian on 28.08.2015.
 */

public class PythonVersionObject {
    protected Context                        context            = null;
    protected String                         version            = null;
    protected String                         libZipMd5Checksum  = null;
    protected ArrayList<String>              modules            = new ArrayList<>();
    protected Map<String, ArrayList<String>> moduleDependencies = new HashMap<>();
    protected String                         md5CheckSum        = null;
    protected ArrayList<String>              modulesMd5Checksum = new ArrayList<>();


    PythonVersionObject(Context context, String version) {
        // TODO: Load installed Python version data.
    }

    protected PythonVersionObject(Context context, Map<String, ArrayList<String>> moduleDependencies) {
        this.context = context;
        this.moduleDependencies = moduleDependencies;
    }


    public String getMainVersion() {
        return Util.getMainVersionPart(version);
    }

    public ArrayList<String> getModuleDependencies(String moduleName) {
        if (moduleDependencies.containsKey(moduleName)) {
            return moduleDependencies.get(moduleName);
        }
        return new ArrayList<>();
    }

    public boolean isInstalled() { // TODO: Separate between full version and mayor-min version
        return PackageManager.isPythonVersionInstalled(context, version);
    }

    public boolean isModuleInstalled(String moduleName) {
        return new File(PackageManager.getLibDynLoad(context, version), moduleName + ".so").exists();
    }

    public boolean isInstallable() {
        return false;
    }
}
