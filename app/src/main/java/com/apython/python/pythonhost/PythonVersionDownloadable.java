package com.apython.python.pythonhost;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * A data object that holds all information necessary to download
 * a specific Python version.
 *
 * Created by Sebastian on 29.08.2015.
 */

public class PythonVersionDownloadable extends PythonVersionObject {

    String downloadUrl = null;
    String libZipUrl = null;
    ArrayList<String> moduleUrls = new ArrayList<>();

    PythonVersionDownloadable(Context context, String version, Map<String, ArrayList<String>> moduleDependencies, JSONObject data, int dataVersion) throws JSONException{
        super(context, moduleDependencies);
        this.version = version;
        parseData(data, dataVersion);
    }

    @Override
    public boolean isInstallable() {
        return downloadUrl != null && libZipUrl != null;
    }

    private void parseData(JSONObject data, int dataVersion) throws JSONException {
        // Get the libZip data
        JSONArray pythonLibData = data.optJSONArray("lib");
        if (pythonLibData == null) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + this.version + "': No information about the lib file given!");
            throw new JSONException("No entry named 'lib'.");
        }
        if (pythonLibData.length() != 2) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + this.version + "': Lib data has an invalid length (" + pythonLibData.length() + ")!");
            throw new JSONException("'lib' entry has invalid length (" + pythonLibData.length() + ").");
        }
        this.libZipUrl = pythonLibData.getString(0);
        this.libZipMd5Checksum = pythonLibData.getString(1);
        // Find the correct system dependent data
        JSONObject abiSpecificLibs = null;
        for (String abi : PackageManager.getSupportedCPUABIS()) {
            if (data.has(abi)) {
                //Log.d(MainActivity.TAG, "Found supported abi: " + abi);
                abiSpecificLibs = data.getJSONObject(abi);
                break;
            }
        }
        if (abiSpecificLibs == null) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Found no supported platform libraries for Python version " + this.version);
            Log.d(MainActivity.TAG, "Supported platform ABIs: " + Arrays.toString(PackageManager.getSupportedCPUABIS()));
            throw new JSONException("No entry for this platform.");
        }
        // Get the data of the Python library
        if (!abiSpecificLibs.has("pythonLib")) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + this.version + "': Data does not contain the Python library.");
            throw new JSONException("Missing entry 'pythonLib'.");
        }
        JSONArray pythonLibraryData = abiSpecificLibs.getJSONArray("pythonLib");
        if (pythonLibraryData == null) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version for version '" + this.version + "': Data for the PythonLib is not a JSONArray!");
            throw new JSONException("'pythonLib' entry is not a JSONArray.");
        }
        if (pythonLibraryData.length() != 2) {
            Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid Python version data for version '" + this.version + "': PythonLib data has an invalid length (" + pythonLibraryData.length() + ")!");
            throw new JSONException("'pythonLib' entry has invalid length (" + pythonLibraryData.length() + ").");
        }
        this.downloadUrl = pythonLibraryData.getString(0);
        this.md5CheckSum = pythonLibraryData.getString(1);
        // Get the data of the other modules
        JSONArray platformLibraryNames = abiSpecificLibs.names();
        for (int j = 0; j < platformLibraryNames.length(); j++) {
            String libName = platformLibraryNames.getString(j);
            if ("pythonLib".equals(libName)) {
                continue;
            }
            JSONArray libData = abiSpecificLibs.optJSONArray(libName);
            if (libData == null) {
                Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid module data for version '" + this.version + "': Data for module '" + libName + "' is not a JSONArray!");
                continue;
            }
            if (libData.length() != 2) {
                Log.w(MainActivity.TAG, "(Version " + dataVersion + ") Invalid module data for version '" + this.version + "': Data for module '" + libName + "' has an invalid length (" + libData.length() + ")!");
                continue;
            }
            modules.add(libName);
            moduleUrls.add(libData.getString(0));
            modulesMd5Checksum.add(libData.getString(1));
        }
    }
}
