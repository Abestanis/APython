package com.apython.python.pythonhost.downloadcenter;

import android.content.Context;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.Pair;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.downloadcenter.items.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Represents a location from which we can download python and additional libraries.
 * 
 * Created by Sebastian on 02.10.2017.
 */
class DownloadServer {
    private static final String  INDEX_PATH           = "index.json";
    private static final String  TAG                  = "DownloadServer";
    private static final int     MAX_PROTOCOL_VERSION = 1;
    private static final Pattern VERSION_PATTERN      = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    
    class ServerDownloads {
        ArrayList<PythonVersionItem> pythonVersions;
        ArrayList<AdditionalLibraryItem> libraries;
        ArrayList<DataItem> dataItems;

        private ServerDownloads(ArrayList<PythonVersionItem> pythonVersions,
                               ArrayList<AdditionalLibraryItem> libraries,
                               ArrayList<DataItem> dataItems) {
            this.pythonVersions = pythonVersions;
            this.libraries = libraries;
            this.dataItems = dataItems;
        }
    }

    private class Requirement {
        String id;
        boolean                satisfied    = true;
        ArrayList<Requirement> requirements = new ArrayList<>();
        ArrayList<Requirement> requiredFor  = new ArrayList<>();

        Requirement(String id) {
            super();
            this.id = id;
        }

        void setUnsatisfied() {
            if (satisfied) {
                satisfied = false;
                for (Requirement requirement : requiredFor) {
                    requirement.setUnsatisfied();
                }
            }
        }
    }
    
    private URL address;
    private Context context;
    
    DownloadServer(Context context, String location) throws IllegalArgumentException{
        this.context = context;
        try {
            this.address = new URL(location);
        } catch (MalformedURLException error) {
            throw new IllegalArgumentException("Invalid server location", error);
        }
    }
    
    public ServerDownloads getDownloads() {
        JsonReader dataReader = streamServerData();
        if (dataReader == null) return null;
        try {
            return parseServerData(dataReader);
        } catch (IOException | IllegalStateException error) {
            Log.e(TAG, "Failed to parse the received server index", error);
        } finally {
            try {
                dataReader.close();
            } catch (IOException ignored) {}
        }
        return null;
    }
    
    private JsonReader streamServerData() {
        try {
            InputStream input = getIndexUrl(this.address).openStream();
            return new JsonReader(new InputStreamReader(input, "UTF-8"));
        } catch (IOException error) {
            Log.e(TAG, "Failed to download the version data form " + getIndexUrl(this.address),
                  error);
            return null;
        }
    }
    
    private ServerDownloads parseServerData(JsonReader dataReader) throws IOException {
        String name;
        Map<String, Requirement> requirements = null;
        Map<String, DataItem> dataItems = null;
        Map<String, AdditionalLibraryItem> libraries = null;
        Map<PythonVersionItem, ArrayList<PythonModuleItem>> pythonVersions = new HashMap<>();
        int protocolVersion = 1;
        dataReader.beginObject();
        while (dataReader.hasNext()) {
            name = dataReader.nextName();
            switch (name) {
            case "__version__":
                protocolVersion = getDataProtocolVersion(dataReader);
                if (protocolVersion > MAX_PROTOCOL_VERSION) {
                    Log.w(TAG, "Receiving data with unsupported protocol version "
                            + protocolVersion + " (max=" + MAX_PROTOCOL_VERSION + ")");
                    // Try to parse the data anyway
                }
                break;
            case "requirements":
                requirements = parseRequirements(dataReader, protocolVersion);
                break;
            case "data":
                dataItems = parseDataList(dataReader, protocolVersion);
                break;
            case "libraries":
                libraries = parseLibraries(dataReader, protocolVersion);
                break;
            default:
                if (VERSION_PATTERN.matcher(name).matches()) {
                    Pair<PythonVersionItem, ArrayList<PythonModuleItem>> pythonVersion =
                            parsePythonVersion(dataReader, protocolVersion, name);
                    if (pythonVersion != null) {
                        pythonVersions.put(pythonVersion.first, pythonVersion.second);
                    }
                } else {
                    Log.w(TAG, "(Version " + protocolVersion +
                            ") Got unknown json element: " + name);
                    dataReader.skipValue();
                }
            }
        }
        return filterDownloadItems(requirements, dataItems, libraries, pythonVersions);
    }
    
    private int getDataProtocolVersion(JsonReader dataReader) throws IOException {
        return dataReader.nextInt();
    }
    
    private Map<String, Requirement> parseRequirements(JsonReader dataReader, int protocolVersion)
            throws IOException {
        Map<String, Requirement> requirements = new HashMap<>();
        dataReader.beginObject();
        while (dataReader.hasNext()) {
            String reqId = dataReader.nextName();
            if (dataReader.peek() != JsonToken.BEGIN_ARRAY) {
                Log.w(TAG, "(Version " + protocolVersion + ") Failed to parse requirements for "
                        + reqId + ": Expected array");
                dataReader.skipValue();
                continue;
            }
            ArrayList<String> requirementList = getStringArray(dataReader);
            Requirement requirement;
            if (requirements.containsKey(reqId)) {
                requirement = requirements.get(reqId);
            } else {
                requirement = new Requirement(reqId);
                requirements.put(reqId, requirement);
            }
            for (String requirementId : requirementList) {
                if (requirementId.startsWith("androidSdk/")) {
                    int minAndroidSdk = Build.VERSION_CODES.BASE;
                    try {
                        minAndroidSdk = Integer.valueOf(requirementId.replace("androidSdk/", ""));
                    } catch (NumberFormatException error) {
                        Log.w(TAG,"(Version " + protocolVersion +
                                ") Failed to parse minAndroidSdk version for requirement " +
                                reqId + ": " + requirementId + ", ignoring it!", error);
                    }
                    if (minAndroidSdk > Build.VERSION.SDK_INT) {
                        requirement.setUnsatisfied();
                        break;
                    }
                    continue;
                }
                Requirement subRequirement;
                if (requirements.containsKey(requirementId)) {
                    subRequirement = requirements.get(requirementId);
                    if (!subRequirement.satisfied) {
                        requirement.setUnsatisfied();
                        break;
                    }
                } else {
                    subRequirement = new Requirement(requirementId);
                    requirements.put(requirementId, subRequirement);
                }
                requirement.requirements.add(subRequirement);
                subRequirement.requiredFor.add(requirement);
            }
        }
        dataReader.endObject();
        return requirements;
    }
    
    private Map<String, DataItem> parseDataList(JsonReader dataReader, int protocolVersion)
            throws IOException {
        Map<String, DataItem> dataItems = new HashMap<>();
        dataReader.beginObject();
        while (dataReader.hasNext()) {
            String dataName = dataReader.nextName();
            String url = null, checksum = null;
            File destination = null;
            dataReader.beginObject();
            while (dataReader.hasNext()) {
                String attributeName = dataReader.nextName();
                switch (attributeName) {
                case "path":
                    ArrayList<String> pathList = saveGetStringArray(dataReader, 2, protocolVersion,
                                                                    "the path of the data item");
                    if (pathList == null) { continue; }
                    url = this.address + "/" + pathList.get(0);
                    checksum = pathList.get(1);
                    break;
                case "dest":
                    if (dataReader.peek() != JsonToken.STRING) {
                        Log.w(TAG, "(Version " + protocolVersion + ") Failed to parse the " +
                                "destination of a data item: Expected string");
                        dataReader.skipValue();
                        continue;
                    }
                    destination = new File(PackageManager.getDataPath(context),
                                           dataReader.nextString().replaceFirst("data/", ""));
                    break;
                default:
                    Log.w(TAG, "(Version " + protocolVersion + ") Got unknown attribute " +
                            "of a data item: " + attributeName);
                    dataReader.skipValue();
                }
            }
            dataReader.endObject();
            if (url == null || checksum == null || destination == null) {
                Log.w(TAG, "(Version " + protocolVersion + ") Failed to parse data item " + 
                        dataName + ": Missing url, checksum or destination");
                continue;
            }
            DataItem dataItem = new DataItem(context);
            dataItem.setUrl(url).setMd5Checksum(checksum).setInstallLocation(destination);
            dataItems.put(dataItem.getId(), dataItem);
        }
        dataReader.endObject();
        return dataItems;
    }
    
    private Map<String, AdditionalLibraryItem> parseLibraries(
            JsonReader dataReader, int protocolVersion) throws IOException {
        List<String> supportedCPUABIS = Arrays.asList(PackageManager.getSupportedCPUABIS());
        if (supportedCPUABIS.size() <= 0) {
            Log.e(TAG, "No supported CPU ABIs!");
            dataReader.skipValue();
            return null;
        }
        Map<String, AdditionalLibraryItem> libraries = new HashMap<>();
        dataReader.beginObject();
        while (dataReader.hasNext()) {
            int bestAbiIndex = Integer.MAX_VALUE;
            String url = null, checksum = null;
            String libraryName = dataReader.nextName();
            dataReader.beginObject();
            while (dataReader.hasNext()) {
                String abi = dataReader.nextName();
                if (bestAbiIndex == 0) {
                    dataReader.skipValue();
                    continue;
                }
                int abiIndex = supportedCPUABIS.indexOf(abi);
                if (abiIndex == -1 || abiIndex > bestAbiIndex) {
                    dataReader.skipValue();
                    continue;
                }
                ArrayList<String> libList = saveGetStringArray(
                        dataReader, 2, protocolVersion, "library CPU ABI item (" + abi + ")");
                if (libList == null) { continue; }
                bestAbiIndex = abiIndex;
                url = this.address + "/" + libList.get(0);
                checksum = libList.get(1);
            }
            dataReader.endObject();
            if (bestAbiIndex == Integer.MAX_VALUE) {
                Log.w(TAG, "(Version " + protocolVersion + ") The library " + libraryName +
                        " is not available for the current CPU Architecture.");
                continue;
            }
            AdditionalLibraryItem libraryItem = new AdditionalLibraryItem(context);
            libraryItem.setUrl(url).setMd5Checksum(checksum);
            libraries.put(libraryItem.getId(), libraryItem);
        }
        dataReader.endObject();
        return libraries;
    }
    
    private Pair<PythonVersionItem, ArrayList<PythonModuleItem>> parsePythonVersion(
            JsonReader dataReader, int protocolVersion, String pythonVersion) throws IOException {
        PythonVersionItem versionItem = new PythonVersionItem(context, pythonVersion);
        ArrayList<PythonModuleItem> modules = new ArrayList<>(0);
        boolean hasModuleZip = false;
        int pythonLibAbiIndex = Integer.MAX_VALUE;
        List<String> supportedCPUABIS = Arrays.asList(PackageManager.getSupportedCPUABIS());
        if (supportedCPUABIS.size() <= 0) {
            Log.e(TAG, "No supported CPU ABIs!");
            dataReader.skipValue();
            return null;
        }
        dataReader.beginObject();
        while (dataReader.hasNext()) {
            String abi = dataReader.nextName();
            int abiIndex;
            if ("lib".equals(abi)) {
                // Get the moduleZip data
                ArrayList<String> moduleList = saveGetStringArray(dataReader, 2, protocolVersion,
                                                                  "the python std. modules item");
                if (moduleList == null) { continue; }
                hasModuleZip = true;
                versionItem.getPythonModulesZip().setUrl(this.address + "/" + moduleList.get(0))
                        .setMd5Checksum(moduleList.get(1));
                continue;
            }
            abiIndex = supportedCPUABIS.indexOf(abi);
            if (abiIndex == -1 || abiIndex > pythonLibAbiIndex) {
                dataReader.skipValue();
                continue;
            }
            ArrayList<PythonModuleItem> newModules = new ArrayList<>();
            dataReader.beginObject();
            while (dataReader.hasNext()) {
                String moduleName = dataReader.nextName();
                ArrayList<String> pathList = saveGetStringArray(dataReader, 2, protocolVersion,
                                                                "item for module " + moduleName);
                if (pathList == null) { continue; }
                if ("pythonLib".equals(moduleName)) {
                    // Get the data of the Python library
                    pythonLibAbiIndex = abiIndex;
                    versionItem.setUrl(this.address + "/" + pathList.get(0))
                            .setMd5Checksum(pathList.get(1));
                } else {
                    PythonModuleItem moduleItem = new PythonModuleItem(context, pythonVersion);
                    moduleItem.setUrl(this.address + "/" + pathList.get(0))
                            .setMd5Checksum(pathList.get(1));
                    moduleItem.addDependency(versionItem);
                    newModules.add(moduleItem);
                }
            }
            dataReader.endObject();
            if (pythonLibAbiIndex == abiIndex) {
                modules = newModules;
            }
        }
        dataReader.endObject();
        if (!hasModuleZip) {
            Log.w(TAG, "(Version " + protocolVersion + ") Invalid Python version data for" +
                    " version '" + pythonVersion + "': No information about the lib file given!");
            return null;
        } else if (pythonLibAbiIndex == Integer.MAX_VALUE) {
            Log.w(TAG, "(Version " + protocolVersion + ") Invalid Python version data for " +
                    "version '" + pythonVersion + "': Data does not contain the Python library.");
            return null;
        }
        return new Pair<>(versionItem, modules);
    }
    
    private ServerDownloads filterDownloadItems(
            Map<String, Requirement> requirements, Map<String, DataItem> dataItems,
            Map<String, AdditionalLibraryItem> libraries,
            Map<PythonVersionItem, ArrayList<PythonModuleItem>> pythonVersionMap) {
        ArrayList<PythonVersionItem> pythonVersions = new ArrayList<>(pythonVersionMap.keySet());
        if (dataItems == null) {
            Log.w(TAG, "Got no data items from the server index.");
            dataItems = new HashMap<>();
        }
        if (libraries == null) {
            Log.w(TAG, "Got no library items from the server index.");
            libraries = new HashMap<>();
        }
        if (requirements == null) {
            Log.w(TAG, "Got no requirements from the server index.");
            libraries = new HashMap<>();
        } else {
            // Determine unsatisfied requirements
            // Data //
            for (Map.Entry<String, Requirement> requirementEntry : requirements.entrySet()) {
                if (requirementEntry.getKey().startsWith("data/")
                        && !dataItems.containsKey(requirementEntry.getKey())) {
                    requirementEntry.getValue().setUnsatisfied();
                }
            }
            ArrayList<DataItem> unsatisfiedDataItems = new ArrayList<>();
            for (DataItem dataItem : dataItems.values()) {
                if (requirements.containsKey(dataItem.getId())) {
                    Requirement requirement = requirements.get(dataItem.getId());
                    if (!requirement.satisfied) {
                        unsatisfiedDataItems.add(dataItem);
                    } else if (!updateDependency(dataItem, requirement.requirements,
                                                 dataItems, libraries)) {
                        requirement.setUnsatisfied();
                        unsatisfiedDataItems.add(dataItem);
                    }
                }
            }
            for (DataItem dataItem : unsatisfiedDataItems) { dataItems.remove(dataItem.getId()); }
            // Libraries //
            for (Map.Entry<String, Requirement> requirementEntry : requirements.entrySet()) {
                if (requirementEntry.getKey().startsWith("libraries/")
                        && !libraries.containsKey(requirementEntry.getKey())) {
                    requirementEntry.getValue().setUnsatisfied();
                }
            }
            ArrayList<AdditionalLibraryItem> unsatisfiedLibraries = new ArrayList<>();
            for (AdditionalLibraryItem library : libraries.values()) {
                if (requirements.containsKey(library.getId())) {
                    Requirement requirement = requirements.get(library.getId());
                    if (!requirement.satisfied) {
                        unsatisfiedLibraries.add(library);
                    } else if (!updateDependency(library, requirement.requirements,
                                                 dataItems, libraries)) {
                        requirement.setUnsatisfied();
                        unsatisfiedLibraries.add(library);
                    }
                } else {
                    requirements.put(library.getId(), new Requirement(library.getId()));
                }
            }
            for (AdditionalLibraryItem library : unsatisfiedLibraries) {
                libraries.remove(library.getId());
            }
            // Python Modules //
            for (Map.Entry<PythonVersionItem, ArrayList<PythonModuleItem>> pythonVersionEntry
                    : pythonVersionMap.entrySet()) {
                for (PythonModuleItem module : pythonVersionEntry.getValue()) {
                    if (requirements.containsKey("pyModule/" + module.getModuleName())) {
                        Requirement requirement = requirements.get(
                                "pyModule/" + module.getModuleName());
                        if (requirement.satisfied && updateDependency(
                                module, requirement.requirements, dataItems, libraries)) {
                            pythonVersionEntry.getKey().addAdditionalModule(module);
                        } else {
                            requirement.setUnsatisfied();
                        }
                    } else {
                        requirements.put(module.getId(), new Requirement(module.getId()));
                    }
                }
            }
        }
        return new ServerDownloads(pythonVersions, new ArrayList<>(libraries.values()),
                                   new ArrayList<>(dataItems.values()));
    }
    
    private static boolean updateDependency(
            Dependency dependency, ArrayList<Requirement> requirements,
            Map<String, DataItem> dataItems, Map<String, AdditionalLibraryItem> libraries) {
        for (Requirement subRequirement : requirements) {
            Dependency requirement = null;
            if (subRequirement.id.startsWith("data/")) {
                if (dataItems.containsKey(subRequirement.id)) {
                    requirement = dataItems.get(subRequirement.id);
                }
            } else if (subRequirement.id.startsWith("libraries/")) {
                if (libraries.containsKey(subRequirement.id)) {
                    requirement = libraries.get(subRequirement.id);
                }
            }
            if (requirement == null) { return false; }
            dependency.addDependency(requirement);
        }
        return true;
    }
    
    private static ArrayList<String> saveGetStringArray(
            JsonReader dataReader, int length, int protocolVersion, String itemName)
            throws IOException {
        if (dataReader.peek() != JsonToken.BEGIN_ARRAY) {
            Log.w(TAG, "(Version " + protocolVersion + ") Failed to parse " +
                    itemName + ": Expected array");
            dataReader.skipValue();
            return null;
        }
        ArrayList<String> array = getStringArray(dataReader);
        if (array.size() < length) {
            Log.w(TAG, "(Version " + protocolVersion + ") Failed to parse " + itemName +
                    ": Expected array of length " + length + ", got length " + array.size());
            return null;
        }
        return array;
    }
    
    
    private static ArrayList<String> getStringArray(JsonReader dataReader) throws IOException {
        ArrayList<String> list = new ArrayList<>();
        dataReader.beginArray();
        while (dataReader.hasNext()) {
            list.add(dataReader.nextString());
        }
        dataReader.endArray();
        return list;
    }
    
    private static URL getIndexUrl(URL serverUrl) {
        try {
            return new URL(serverUrl, INDEX_PATH);
        } catch (MalformedURLException error) {
            throw new RuntimeException(error); // This can only happen if INDEX_PATH is invalid.
        }
    }
    
}
