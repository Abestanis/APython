package com.apython.python.pythonhost;

/*
 * Handles the Python installation and Python modules.
 *
 * Created by Sebastian on 16.06.2015.
 */

import android.content.Context;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PackageManager {

    public static String pythonVersion = "2.7";

    public interface ProgressHandler {
        void enable(String text);

        void setText(String text);

        void setProgress(float progress);
    }

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
        return new File(getStandardLibPath(context), "python" + pythonVersion + "/site-packages");
    }

    public static File getXDCBase(Context context) {
        return new File(context.getFilesDir(), "pythonApps");
    }

    public static File getLibDynLoad(Context context) {
        return new File(getStandardLibPath(context), "python" + pythonVersion + "/lib-dynload");
    }

    // Checks that all components of this Python installation are present.
    public static boolean ensurePythonInstallation(Context context, ProgressHandler progressHandler) {
        return installPythonExecutable(context, progressHandler)
                && installPythonLibraries(context, progressHandler)
                && installPip(context, progressHandler)
                && checkSitePackagesAvailability(context, progressHandler);
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
        return executable.exists() || Util.installRawResource(executable, context.getResources().openRawResource(R.raw.python), progressHandler);
    }

    public static boolean installPythonLibraries(Context context, ProgressHandler progressHandler) {
        File libDest = getStandardLibPath(context);
        if (libDest.isDirectory()) {
            return true;
        }
        if (!(libDest.mkdirs() || libDest.isDirectory())) {
            Log.e(MainActivity.TAG, "Failed to create the 'lib' directory!");
            return false;
        }
        if (progressHandler != null) {
            progressHandler.enable(context.getString(R.string.install_pythonLibs));
        }
        Util.makeFileAccessible(libDest, false);
        InputStream libLocation = context.getResources().openRawResource(R.raw.lib);
        return Util.installRawResource(new File(libDest, "python" + pythonVersion.replace(".", "") + ".zip"), libLocation, progressHandler);
    }

    public static boolean installRequirements(final Context context, String requirements, final ProgressHandler progressHandler) {
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
        PythonInterpreter.IOHandler ioHandler = null;
        if (progressHandler != null) {
            progressHandler.setProgress(-1);
            progressHandler.enable(context.getString(R.string.install_requirements));
            ioHandler = new PythonInterpreter.IOHandler() {
                @Override
                public void addOutput(String text) { Util.parsePipOutput(context, progressHandler, text); }

                @Override
                public void setupInput(String prompt) {}
            };
        }
        PythonInterpreter interpreter = new PythonInterpreter(context, ioHandler);
        int result = interpreter.runPythonModule("pip", new String[] {"install", "-r", reqFile.getAbsolutePath()});
        if (!reqFile.delete()) {
            Log.w(MainActivity.TAG, "Cannot delete temporary file '" + reqFile.getAbsolutePath() + "'!");
        }
        return result == 0;
    }

    public static boolean checkSitePackagesAvailability(Context context, ProgressHandler progressHandler) {
        File sitePackages = getSitePackages(context);
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

    public static boolean installPip(final Context context, final ProgressHandler progressHandler) {
        File downloadDir = getTempDir(context);
        File sitePackages = getSitePackages(context);
        if (!sitePackages.exists()) {// TODO: This is temporary
            if (progressHandler != null) {
                progressHandler.enable(context.getString(R.string.install_pip));
            }
            String url = "https://bootstrap.pypa.io/get-pip.py";
            HttpResponse response = Util.connectToUrl(url);
            InputStream stream;
            long totalDownloadSize;
            try {
                HttpEntity entity = response.getEntity();
                stream = entity.getContent();
                totalDownloadSize = entity.getContentLength();
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
            if (progressHandler != null) {
                progressHandler.setText(context.getString(R.string.download_pip));
            }
            File file = new File(downloadDir, "get-pip.py");
            int totalCount = 0;
            float nextUpdate = 0;
            float onePercent = (float) totalDownloadSize / 100;
            try {
                fos = new FileOutputStream(file);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                while ((count = stream.read(data, 0, BUFFER_SIZE)) != -1) {
                    dest.write(data, 0, count);
                    if (progressHandler != null) {
                        totalCount += count;
                        if (totalCount >= nextUpdate) {
                            progressHandler.setProgress((float) totalCount / totalDownloadSize);
                            nextUpdate += onePercent;
                        }
                    }
                }
                dest.close();
            } catch (IOException ioE) {
                Log.e(MainActivity.TAG, "Failed to save the pip installation file at '" + file.getAbsolutePath() + "'!");
                ioE.printStackTrace();
                return false;
            }
            PythonInterpreter.IOHandler ioHandler = null;
            if (progressHandler != null) {
                progressHandler.setProgress(-1);
                progressHandler.setText(context.getString(R.string.run_pip_installer));
                ioHandler = new PythonInterpreter.IOHandler() {
                    @Override
                    public void addOutput(String text) { Util.parsePipOutput(context, progressHandler, text); }

                    @Override
                    public void setupInput(String prompt) {}
                };
            }
            PythonInterpreter interpreter = new PythonInterpreter(context, ioHandler);
            int res = interpreter.runPythonFile(file, null);
            if (!file.delete()) {
                Log.w(MainActivity.TAG, "Failed to delete '" + file.getAbsolutePath() + "'!");
            }
            if (res != 0) {
                Log.e(MainActivity.TAG, "Installing pip failed ith exit status " + res + "!");
                return false;
            }
        }
        return configurePip(context, progressHandler);
    }

    public static boolean configurePip(Context context, ProgressHandler progressHandler) {
        File config = new File(context.getFilesDir(), "pythonApps/.config/pip");
        if (!(config.exists() || config.mkdirs())) {
            Log.e(MainActivity.TAG, "Could not make directories for path '" + config.getAbsolutePath() + "'!");
            return false;
        }
        config = new File(config, "pip.conf");
        if (config.exists()) { return true; }
        if (progressHandler != null) {
            progressHandler.enable(context.getString(R.string.configure_pip));
        }
        return Util.installRawResource(config, context.getResources().openRawResource(R.raw.pip_conf), progressHandler) && Util.makeFileAccessible(new File(context.getFilesDir(), "pythonApps"), true);
    }
}
