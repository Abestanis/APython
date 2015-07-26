package com.apython.python.pythonhost;

/*
 * Handles the Python installation and Python modules.
 *
 * Created by Sebastian on 16.06.2015.
 */

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import org.apache.http.HttpResponse;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

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

    public static File getSitePackages(Context context) {
        return new File(getStandardLibPath(context), "python2.7/site-packages");
    }

    public static File getXDCBase(Context context) {
        return new File(context.getFilesDir(), "pythonApps");
    }

    // Checks that all components of this Python installation are present.
    public static boolean ensurePythonInstallation(Context context) {
        return installPythonExecutable(context)
                && installPythonLibraries(context)
                && configurePip(context)
                && installPip(context)
                && checkSitePackagesAvailability(context);
    }

    public static boolean installPythonExecutable(Context context) {
        File executable = getPythonExecutable(context);
        if (executable.exists()) { // TODO: Check if we really need to update it.
            if (!executable.delete()) {
                Log.w(MainActivity.TAG, "Could not delete previously installed python executable.");
                return true; // That's hopefully ok.
            }
        }
        return executable.exists() || Util.installRawResource(executable, context.getResources().openRawResource(R.raw.python));
    }

    public static boolean installPythonLibraries(Context context) {
        File libDest = getStandardLibPath(context);
        if (libDest.isDirectory()) {
            return true;
        }
        if (!(libDest.mkdirs() || libDest.isDirectory())) {
            Log.e(MainActivity.TAG, "Failed to create the 'lib' directory!");
            return false;
        }
        Util.makeFileAccessible(libDest);
        InputStream libLocation = context.getResources().openRawResource(R.raw.lib);
        return Util.installRawResource(new File(libDest, "python27.zip"), libLocation);
    }

    public static boolean installRequirements(Context context, String requirements) {
        File reqFile;
        try {
            reqFile = File.createTempFile("python-", "-requirements.txt", getTempDir(context));
            FileWriter fw = new FileWriter(reqFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(requirements);
            bw.close();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to write requirements to a temporary file!");
            e.printStackTrace();
            return false;
        }
        PythonInterpreter interpreter = new PythonInterpreter(context);
        int result = interpreter.runPythonModule("pip", new String[] {"install", "-r", reqFile.getAbsolutePath()});
        if (!reqFile.delete()) {
            Log.w(MainActivity.TAG, "Cannot delete temporary file '" + reqFile.getAbsolutePath() + "'!");
        }
        return result != 0;
    }

    public static boolean checkSitePackagesAvailability(Context context) {
        File sitePackages = getSitePackages(context);
        if (sitePackages.exists()) {
            if (!(Util.makeFileAccessible(sitePackages.getParentFile()) && Util.makeFileAccessible(sitePackages))) { return false; }
            for (File file : Util.checkDirContentsAccessibility(sitePackages)) {
                if (file.isDirectory()) {
                    if (!Util.recursiveMakeDirAccessible(file)) { return false; }
                } else {
                    if (!Util.makeFileAccessible(file)) { return  false; }
                }
            }
        }
        return true;
    }

    public static boolean installPip(Context context) {
        File downloadDir = getTempDir(context);
        File sitePackages = getSitePackages(context);
        if (sitePackages.exists()) {// TODO: This is temporary
            return true;
        }
        String url = "https://bootstrap.pypa.io/get-pip.py";
        // TODO: This is temporary!!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        HttpResponse response = Util.connectToUrl(url);
        InputStream stream;
        try {
            stream = response.getEntity().getContent();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int count;
        final int BUFFER_SIZE = 2048;
        byte data[] = new byte[BUFFER_SIZE];
        FileOutputStream fos;
        if (!(downloadDir.mkdirs() || downloadDir.isDirectory())) {
            Log.e(MainActivity.TAG, "Failed to create the 'downloads' directory!");
            return false;
        }
        for (String child : downloadDir.list()) {
            if (!(new File(downloadDir, child).delete())) {
                Log.w(MainActivity.TAG, "Could not delete file '" + child + "' in '" + downloadDir.getAbsolutePath() + "'.");
            }
        }

        File file = new File(downloadDir, "get-pip.py");
        try {
            fos = new FileOutputStream(file);
            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
            while ((count = stream.read(data, 0, BUFFER_SIZE)) != -1) {
                dest.write(data, 0, count);
            }
            dest.close();
        } catch (IOException ioE) {
            Log.e(MainActivity.TAG, "Failed to save the pip installation file at '" + file.getAbsolutePath() + "'!");
            ioE.printStackTrace();
            return false;
        }
        PythonInterpreter interpreter = new PythonInterpreter(context);
        int res = interpreter.runPythonFile(file, null);
        if (!file.delete()) {
            Log.w(MainActivity.TAG, "Failed to delete '" + file.getAbsolutePath() + "'!");
        }
        if (res != 0) {
            Log.e(MainActivity.TAG, "Installing pip failed!");
            return false;
        }
        return true;
    }

    public static boolean configurePip(Context context) {
        File config = new File(context.getFilesDir(), "pythonApps/.config/pip");
        if (!(config.exists() || config.mkdirs())) {
            Log.e(MainActivity.TAG, "Could not make directories for path '" + config.getAbsolutePath() + "'!");
            return false;
        }
        config = new File(config, "pip.conf");
        return (config.exists() || Util.installRawResource(config, context.getResources().openRawResource(R.raw.pip_conf))) && Util.recursiveMakeDirAccessible(new File(context.getFilesDir(), "pythonApps"));
    }
}
