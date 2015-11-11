package com.apython.python.pythonhost;

/*
 * Handles the Python installation and Python modules.
 *
 * Created by Sebastian on 16.06.2015.
 */

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.apython.python.pythonhost.interpreter.PythonInterpreter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PackageManager {

    public static File getSharedLibrariesPath(Context context) {
        return new File(context.getApplicationInfo().dataDir, "lib");
    }

    public static File getStandardLibPath(Context context) {
        return new File(context.getFilesDir(), "lib");
    }

    public static File getPythonExecutable(Context context) {
        return new File(context.getApplicationInfo().dataDir, "python");
    }

    public static File getTempDir(Context context) {
        return context.getCacheDir();
    }

    public static File getSitePackages(Context context, String pythonVersion) {
        return new File(getStandardLibPath(context), "python" + pythonVersion + "/site-packages");
    }

    public static File getGlobalSitePackages(Context context) {
        return new File(getStandardLibPath(context), "site-packages");
    }

    public static File getXDGBase(Context context) {
        return new File(context.getFilesDir(), "pythonApps");
    }

    public static File getLibDynLoad(Context context, String pythonVersion) {
        return new File(getStandardLibPath(context), "python" + pythonVersion + "/lib-dynload");
    }

    public static File getDynamicLibraryPath(Context context) {
        return new File(context.getFilesDir(), "dynLibs");
    }

    public static boolean isAdditionalLibraryInstalled(Context context, String libName) {
        return new File(getDynamicLibraryPath(context), System.mapLibraryName(libName)).exists();
    }

    public static boolean isPythonVersionInstalled(Context context, String pythonVersion) {
        return new File(getDynamicLibraryPath(context), System.mapLibraryName("python" + pythonVersion)).exists()
                && new File(getStandardLibPath(context), "python" + pythonVersion.replace(".", "") + ".zip").exists();
    }

    public static ArrayList<String> getInstalledPythonVersions(Context context) { // TODO: Make this better
        File libPath = getDynamicLibraryPath(context);
        ArrayList<String> versions = new ArrayList<>();
        if (!libPath.exists() || !libPath.isDirectory()) {
            return versions;
        }
        for (File libFile : libPath.listFiles()) {
            String name = libFile.getName();
            if (!name.startsWith("lib" + "python") || !name.endsWith(".so") || name.contains("pythonPatch")) {
                continue;
            }
            String version = name.replace("lib" + "python", "").replace(".so", "");
            if (new File(getStandardLibPath(context), "python" + version.replace(".", "") + ".zip").exists()) {
                versions.add(version);
            }
        }
        return versions;
    }

    /**
     * Returns the installed Python version with the highest version number.
     *
     * @param context The current context.
     * @return The newest Python version.
     */
    public static String getNewestInstalledPythonVersion(Context context) {
        String newestVersion = null;
        int[] newestVersionNumbers = {0, 0};
        for (String version : getInstalledPythonVersions(context)) {
            String[] versionParts = version.split("\\.");
            int[] versionNumbers = {Integer.valueOf(versionParts[0]), Integer.valueOf(versionParts[1])};
            if (versionNumbers[0] > newestVersionNumbers[0]
                    || (versionNumbers[0] == newestVersionNumbers[0] && versionNumbers[1] > newestVersionNumbers[1])) {
                newestVersion = version;
                newestVersionNumbers = versionNumbers;
            }
        }
        return newestVersion;
    }

    /**
     * Return the optimal installed Python version.
     *
     * @param context The current context.
     * @param requestedPythonVersion A requested specific Python version.
     * @param minPythonVersion The minimum Python version to use.
     * @param maxPythonVersion The maximum Python version to use.
     * @param disallowedPythonVersions A list of disallowed Python versions.
     * @return The optimal Python version or {@code null} if no optimal version was found.
     */
    public static String getOptimalInstalledPythonVersion(Context context, String requestedPythonVersion,
                                                          String minPythonVersion, String maxPythonVersion,
                                                          String[] disallowedPythonVersions) {
        if (requestedPythonVersion != null) {
            return isPythonVersionInstalled(context, requestedPythonVersion) ? requestedPythonVersion : null;
        }

        ArrayList<String> installedPythonVersions = getInstalledPythonVersions(context);
        if (disallowedPythonVersions != null) {
            installedPythonVersions.removeAll(Arrays.asList(disallowedPythonVersions));
        }
        Collections.sort(installedPythonVersions, Collections.reverseOrder());
        int[] minPyVersion = minPythonVersion != null ? Util.getNumericPythonVersion(minPythonVersion) : new int[] {-1, -1, -1};
        int[] maxPyVersion = maxPythonVersion != null ? Util.getNumericPythonVersion(maxPythonVersion) : new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        for (String installedPythonVersion : installedPythonVersions) {
            int[] version = Util.getNumericPythonVersion(installedPythonVersion);
            if (version[0] <= maxPyVersion[0] && version[0] >= minPyVersion[0]) {
                if (version[1] <= maxPyVersion[1] && version[1] >= minPyVersion[1]) {
                    if (version[2] <= maxPyVersion[2] && version[2] >= minPyVersion[2]) {
                        return installedPythonVersion;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the detailed version string of the main Python version which is currently installed.
     *
     * @param context The current Application context
     * @param mainVersion The version which detailed version should be checked.
     * @return The detailed version string.
     */
    public static String getDetailedInstalledVersion(Context context, String mainVersion) {
        // TODO: This must be faster
        return new PythonInterpreter(context, mainVersion).getPythonVersion();
    }

    /**
     * Get the ABIs supported by this device.
     *
     * @return a list of supported ABIs.
     */
    public static String[] getSupportedCPUABIS() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            return new String[] {Build.CPU_ABI, Build.CPU_ABI2};
        } else {
            return Build.SUPPORTED_ABIS;
        }
    }

    public static boolean deletePythonVersion(Context context, String pythonVersion) {
        File pythonLib = new File(getDynamicLibraryPath(context), "libpython" + pythonVersion + ".so");
        File moduleZip = new File(getStandardLibPath(context), "python" + pythonVersion.replace(".", "") + ".zip");
        File moduleDir = getLibDynLoad(context, pythonVersion);

        if (!Util.deleteDirectory(moduleDir)) {
            Log.w(MainActivity.TAG, "Failed to delete the Python version + " + pythonVersion + ": Could not delete the lib-dynload directory!");
            return false;
        }
        if (!moduleZip.delete()) {
            Log.w(MainActivity.TAG, "Failed to delete the Python version + " + pythonVersion + ": Could not delete the module zip!");
            return false;
        }
        if (!pythonLib.delete()) {
            Log.e(MainActivity.TAG, "Failed to delete the Python version + " + pythonVersion + ": Could not delete the Python library!");
            return false;
        }
        if (pythonVersion.equals(PreferenceManager.getDefaultSharedPreferences(context).getString( // TODO: main version?
                PythonSettingsActivity.KEY_PYTHON_VERSION,
                PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED))) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
                    PythonSettingsActivity.KEY_PYTHON_VERSION,
                    PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED
            ).commit();
        }
        return true;
    }

    /**
     * Load a library that was downloaded and was not bundled with the Python Host apk.
     *
     * @param context The current context.
     * @param libraryName The name of the library to load.
     *
     * @throws LinkageError if the library was not downloaded or it is not in the usual directory.
     */
    public static void loadDynamicLibrary(Context context, String libraryName) {
        System.load(new File(PackageManager.getDynamicLibraryPath(context),
                             System.mapLibraryName(libraryName)).getAbsolutePath());
    }

    /**
     * Returns a list of all additional libraries installed.
     *
     * @param context The current context.
     * @return A list of {@link File} objects pointing to the library files.
     */
    public static File[] getAdditionalLibraries(Context context) {
        File[] additionalLibraries = getDynamicLibraryPath(context).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".so")
                        && !filename.matches(".*python\\d+\\.\\d+\\.so")
                        && !filename.endsWith("pythonPatch.so");
            }
        });
        return additionalLibraries != null ? additionalLibraries : new File[0];
    }

    /**
     * Returns a list of all additional Python modules installed for the given Python version.
     *
     * @param context The current context.
     * @param pythonVersion The Python version for which to search for libraries.
     * @return A list of {@link File} objects pointing to the library files.
     */
    public static File[] getAdditionalModules(Context context, String pythonVersion) {
        File[] additionalModules = getLibDynLoad(context, pythonVersion).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".so");
            }
        });
        return additionalModules != null ? additionalModules : new File[0];
    }

    /**
     * Loads all additional libraries that are installed.
     *
     * @param context The current context.
     */
    public static void loadAdditionalLibraries(Context context) {
        for (File additionalLibrary : getAdditionalLibraries(context)) {
            System.load(additionalLibrary.getAbsolutePath());
        }
    }

    /**
     * Calculates the storage space currently used by the given Python version.
     *
     * @param context The current context.
     * @param pythonVersion The Python version to calculate the used storage space for.
     * @return The used storage space in bytes.
     */
    public static long getUsedStorageSpace(Context context, String pythonVersion) {
        File pythonDir = new File(getStandardLibPath(context), "python" + pythonVersion);
        File pythonLib = new File(PackageManager.getDynamicLibraryPath(context),
                                  System.mapLibraryName("python" + Util.getMainVersionPart(pythonVersion)));
        return pythonLib.length() + Util.calculateDirectorySize(pythonDir);
    }

    // Checks that all components of this Python installation are present.
    public static boolean ensurePythonInstallation(Context context, String pythonVersion, ProgressHandler progressHandler) {
        return installPythonExecutable(context, progressHandler)
                //&& installPythonLibraries(context, progressHandler)
                //&& installPip(context, progressHandler)
                && checkSitePackagesAvailability(context, pythonVersion, progressHandler);
    }

    public static boolean installPythonExecutable(Context context, ProgressHandler progressHandler) {
        File executable = getPythonExecutable(context);
        if (executable.exists()) { // TODO: Check if we really need to update it.
            if (!executable.delete()) {
                Log.w(MainActivity.TAG, "Could not delete previously installed python executable.");
                return true; // That's hopefully ok.
            }
        }
        if (progressHandler != null) {
            progressHandler.enable(context.getString(R.string.install_executable));
        }
        return executable.exists() || Util.installFromInputStream(executable, context.getResources().openRawResource(R.raw.python), progressHandler);
    }

    public static boolean installRequirements(final Context context, String requirements, String pythonVersion, final ProgressHandler progressHandler) {
        return Pip.install(context, pythonVersion, progressHandler)
                && Pip.installRequirements(context, requirements, pythonVersion, progressHandler);
    }

    public static boolean checkSitePackagesAvailability(Context context, String pythonVersion, ProgressHandler progressHandler) {
        File sitePackages = getSitePackages(context, pythonVersion);
        if (sitePackages.exists()) {
            if (!(Util.makeFileAccessible(sitePackages.getParentFile(), false)
                    && Util.makeFileAccessible(sitePackages, false))) { return false; }
            ArrayList<File> files = Util.checkDirContentsAccessibility(sitePackages);
            if (!files.isEmpty()) {
                if (progressHandler != null) {
                    progressHandler.enable(context.getString(R.string.changing_sitePackages_permissions));
                }
                int i;
                int size = files.size();
                for (i = 0; i < size; i++) {
                    Util.makeFileAccessible(files.get(i), files.get(i).isDirectory());
                    if (progressHandler != null) {
                        progressHandler.setProgress((float) (i + 1) / size);
                    }
                }
                if (progressHandler != null) {
                    progressHandler.setProgress(-1);
                }
            }
        }
        return true;
    }
}
