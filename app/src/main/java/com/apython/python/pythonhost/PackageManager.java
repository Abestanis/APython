package com.apython.python.pythonhost;

/*
 * Handles the Python installation and Python modules.
 *
 * Created by Sebastian on 16.06.2015.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.apython.python.pythonhost.interpreter.PythonInterpreter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PackageManager {

    public static File getSharedLibrariesPath(Context context) {
        return new File(context.getApplicationInfo().nativeLibraryDir);
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

    public static File getDataPath(Context context) {
        return new File(context.getFilesDir(), "data");
    }

    public static boolean isAdditionalLibraryInstalled(Context context, String libName) {
        return new File(getDynamicLibraryPath(context), System.mapLibraryName(libName)).exists();
    }

    public static boolean isPythonVersionInstalled(Context context, String pythonVersion) {
        return new File(getDynamicLibraryPath(context), System.mapLibraryName("python" + pythonVersion)).exists()
                && new File(getStandardLibPath(context), "python" + pythonVersion.replace(".", "") + ".zip").exists();
    }

    public static ArrayList<String> getInstalledPythonVersions(Context context) {
        File libPath = getDynamicLibraryPath(context);
        ArrayList<String> versions = new ArrayList<>();
        if (!libPath.exists() || !libPath.isDirectory()) {
            return versions;
        }
        for (File libFile : libPath.listFiles()) {
            String name = libFile.getName();
            if (!name.startsWith("libpython") || !name.endsWith(".so")) {
                continue;
            }
            String version = name.replace("libpython", "").replace(".so", "");
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

    /**
     * Load a library that was downloaded and was not bundled with the Python Host apk.
     *
     * @param context The current context.
     * @param libraryName The name of the library to load.
     *
     * @throws UnsatisfiedLinkError if the library was not downloaded
     *                              or it is not in the usual directory.
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
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
                return filename.endsWith(".so") && !filename.matches(".*python\\d+\\.\\d+\\.so");
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
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void loadAdditionalLibraries(Context context) {
        File[] additionalLibraries = getAdditionalLibraries(context);
        if (additionalLibraries.length == 0) return;
        int numPrevFailed = additionalLibraries.length;
        ArrayList<File> failedLibs = new ArrayList<>(additionalLibraries.length);
        while (additionalLibraries[0] != null) {
            for (File additionalLibrary : additionalLibraries) {
                if (additionalLibrary == null) break;
                try {
                    System.load(additionalLibrary.getAbsolutePath());
                } catch (UnsatisfiedLinkError unused) {
                    failedLibs.add(additionalLibrary);
                }
            }
            if (numPrevFailed == failedLibs.size()) {
                Log.w(MainActivity.TAG, "The following additional libraries could not be loaded:");
                for (File failedLib : failedLibs) {
                    Log.w(MainActivity.TAG, failedLib.getAbsolutePath());
                }
                return;
            }
            additionalLibraries = failedLibs.toArray(additionalLibraries);
            numPrevFailed = failedLibs.size();
            failedLibs.clear();
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
                                  System.mapLibraryName("python" + pythonVersion));
        File pythonModules = new File(PackageManager.getStandardLibPath(context), pythonVersion.replace(".", "") + ".zip");
        return pythonLib.length() + pythonModules.length() + Util.calculateDirectorySize(pythonDir);
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
        String pyExecutableName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                "python_pie" : "python";
        try {
            for (String cpuAbi : getSupportedCPUABIS()) {
                InputStream inputStream;
                try {
                    inputStream = context.getAssets().open(cpuAbi + "/" + pyExecutableName);
                } catch (FileNotFoundException ignored) { continue; }
                return Util.installFromInputStream(executable, inputStream, progressHandler);
            }
            Log.e(MainActivity.TAG, "Failed to install the python executable!");
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to install the python executable!", e);
        }
        return false;
    }

    public static boolean installRequirements(final Context context, String requirements, String pythonVersion, final ProgressHandler progressHandler) {
        return Pip.install(context, pythonVersion, progressHandler)
                && Pip.installRequirements(context, requirements, pythonVersion, progressHandler);
    }

    public static boolean checkSitePackagesAvailability(Context context, String pythonVersion, ProgressHandler progressHandler) {
        File sitePackages = getSitePackages(context, pythonVersion);
        if (sitePackages.exists()) {
            if (!Util.makePathAccessible(sitePackages, context.getFilesDir())) { return false; }
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

    public static ArrayList<String> getInstalledDynLibraries(Context context) {
        ArrayList<String> installedLibs = new ArrayList<>();
        File[] files = getDynamicLibraryPath(context).listFiles();
        if (files != null) {
            for (File file : files) {
                installedLibs.add(Util.getLibraryName(file));
            }
        }
        return installedLibs;
    }
}
