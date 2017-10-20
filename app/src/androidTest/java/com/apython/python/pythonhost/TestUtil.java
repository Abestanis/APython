package com.apython.python.pythonhost;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sebastian on 20.10.2017.
 */

public class TestUtil {
    public static boolean installLibraryData(Context context) {
        Object[][] data = {
                {new File(context.getFilesDir(), "data/tcl8.6.4/library"), "tcl_library"},
                {new File(context.getFilesDir(), "data/tcl8.6.4/library/tk8.6"), "tk_library"},
        };
        for (Object[] dataItem : data) {
            File dest = (File) dataItem[0];
            String resourcePath = (String) dataItem[1];
            if (!dest.exists()) {
                if (!dest.mkdirs()) {
                    Log.e(MainActivity.TAG, "Could not create directory " + dest.getAbsolutePath());
                    return false;
                }
                File tempArchive = new File(context.getCacheDir(), dest.getName() + ".zip");
                try {
                    if (!Util.installFromInputStream(tempArchive, context.getAssets().open("DevAssets/" + resourcePath + ".zip"), null)) {
                        throw new IOException();
                    }
                } catch (IOException error) {
                    Log.e(MainActivity.TAG, "Could not create temporary archive at " + tempArchive.getAbsolutePath(), error);
                    return false;
                }
                boolean success = Util.extractArchive(tempArchive, dest, null); // TODO: See if there is a zip stream
                tempArchive.delete();
                if (!success) {
                    Log.e(MainActivity.TAG, "failed to extract archive to " + dest.getAbsolutePath());
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean copyNativePythonLibraries(Context context) {
        final List<String> excludeLibList = Arrays.asList(
                System.mapLibraryName("application"),
                System.mapLibraryName("pyInterpreter"),
                System.mapLibraryName("pyLog")
        );
        File dynLibPath = PackageManager.getDynamicLibraryPath(context);
        File nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
        if (!dynLibPath.isDirectory() && !dynLibPath.mkdirs()) {
            Log.e(MainActivity.TAG, "Failed to create the dynLib path");
            return false;
        }
        for (File libFile : nativeLibDir.listFiles()) {
            if (!excludeLibList.contains(libFile.getName())) {
                try {
                    FileInputStream inStream = new FileInputStream(libFile);
                    FileOutputStream outStream = new FileOutputStream(new File(dynLibPath, libFile.getName()));
                    FileChannel inChannel = inStream.getChannel();
                    inChannel.transferTo(0, inChannel.size(), outStream.getChannel());
                    inStream.close();
                    outStream.close();
                } catch (IOException error) {
                    Log.e(MainActivity.TAG, "Failed to copy native library " + libFile.getName(), error);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean installPythonLibraries(Context context, String pythonVersion) {
        File libDest = PackageManager.getStandardLibPath(context);
        if (!(libDest.mkdirs() || libDest.isDirectory())) {
            Log.e(MainActivity.TAG, "Failed to create the 'lib' directory!");
            return false;
        }
        File libZip = new File(libDest, "python" + pythonVersion.replace(".", "") + ".zip");
        if (libZip.exists()) {
            return true;
        }
        Util.makeFileAccessible(libDest, false);
        InputStream libLocation;
        try {
            libLocation = context.getAssets().open("DevAssets/lib" + pythonVersion.replace('.', '_') + ".zip");
        } catch (IOException error) {
            Log.e(MainActivity.TAG, "Did not find the library zip for the python version " +
                    pythonVersion, error);
            return false;
        }
        return Util.installFromInputStream(libZip, libLocation, null);
    }
}
