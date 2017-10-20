package com.apython.python.pythonhost;

import android.content.Context;
import android.util.Log;

import com.apython.python.pythonhost.interpreter.PythonInterpreter;
import com.apython.python.pythonhost.interpreter.PythonInterpreterHandle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * A manager for the Python PIP module.
 *
 * Created by Sebastian on 18.10.2015.
 */
public class Pip {

    public static boolean installRequirements(final Context context, String requirements, String pythonVersion, final ProgressHandler progressHandler) {
        // TODO: Need fast check if a requirement is already installed.
        File reqFile;
        try {
            reqFile = File.createTempFile("python-", "-requirements.txt", PackageManager.getTempDir(context));
            FileWriter fw = new FileWriter(reqFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(requirements);
            bw.close();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to write requirements to a temporary file!");
            e.printStackTrace();
            return false;
        }
        PythonInterpreterHandle.IOHandler ioHandler = null;
        if (progressHandler != null) {
            progressHandler.setProgress(-1);
            progressHandler.enable(context.getString(R.string.install_requirements));
            ioHandler = new PythonInterpreterHandle.IOHandler() {
                @Override
                public void addOutput(String text) { parsePipOutput(context, progressHandler, text); }

                @Override
                public void setupInput(String prompt) {}
            };
        }
//        PythonInterpreter interpreter = new PythonInterpreter(context, pythonVersion, ioHandler);
        int result = 1;//interpreter.runPythonModule("pip", new String[] {"install", "-r", reqFile.getAbsolutePath()});
        if (!reqFile.delete()) {
            Log.w(MainActivity.TAG, "Cannot delete temporary file '" + reqFile.getAbsolutePath() + "'!");
        }
        return result == 0;
    }

    public static boolean isInstalled(Context context) {
        for (String version : PackageManager.getInstalledPythonVersions(context)) {
            if (isInstalled(context, version)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInstalled(Context context, String pythonVersion) {
        return new File(PackageManager.getSitePackages(context, pythonVersion), "pip").exists();
    }

    public static boolean install(final Context context, String pythonVersion, final ProgressHandler progressHandler) {
        if (isInstalled(context, pythonVersion)) {
            return true;
        }
        File downloadDir = PackageManager.getTempDir(context);
        //String pythonVersion = PackageManager.getNewestInstalledPythonVersion(context);
        // TODO: Maybe make this go into the global site packages.
        if (progressHandler != null) {
            progressHandler.enable(context.getString(R.string.install_pip));
        }
        String url = "https://bootstrap.pypa.io/get-pip.py";
        URLConnection connection = Util.connectToUrl(url, 10000);
        if (connection == null) {
            Log.w(MainActivity.TAG, "Failed to download pip installer.");
            return false;
        }
        InputStream stream;
        long totalDownloadSize;
        try {
            stream = connection.getInputStream();
            totalDownloadSize = connection.getContentLength();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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
        File installFile = new File(downloadDir, "get-pip.py");

        if (!Util.installFromInputStream(installFile, stream, totalDownloadSize, progressHandler)) {
            Log.e(MainActivity.TAG, "Failed to save the pip installation file at '" + installFile.getAbsolutePath() + "'!");
            return false;
        }
        PythonInterpreterHandle.IOHandler ioHandler = null;
        if (progressHandler != null) {
            progressHandler.setProgress(-1);
            progressHandler.setText(context.getString(R.string.run_pip_installer));
            ioHandler = new PythonInterpreterHandle.IOHandler() {
                @Override
                public void addOutput(String text) { parsePipOutput(context, progressHandler, text); }

                @Override
                public void setupInput(String prompt) {}
            };
        }
//        PythonInterpreter interpreter = new PythonInterpreter(context, pythonVersion, ioHandler);
        int res = 1;//interpreter.runPythonFile(installFile, null);
        if (!installFile.delete()) {
            Log.w(MainActivity.TAG, "Failed to delete '" + installFile.getAbsolutePath() + "'!");
        }
        if (res != 0) {
            Log.e(MainActivity.TAG, "Installing pip failed with exit status " + res + "!");
            return false;
        }
        return configure(context, progressHandler);
    }

    public static boolean configure(Context context, ProgressHandler progressHandler) {
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
        return Util.installFromInputStream(config, context.getResources().openRawResource(R.raw.pip_conf), progressHandler)
                && Util.makeFileAccessible(new File(context.getFilesDir(), "pythonApps"), true);
    }

    /**
     * Parse output given by pip.
     *
     * @param context The current context.
     * @param progressHandler A progressHandler to report the output to.
     * @param text A line of output from pip.
     */
    public static void parsePipOutput(Context context, ProgressHandler progressHandler, String text) {
        text = text.trim().replace("[?25h", "");
        if (text.startsWith("[K")) {
            String[] parts = text.split(" ");
            try {
                if (parts.length >= 4) {
                    progressHandler.setProgress((float) Integer.valueOf(parts[4].substring(0, parts[4].length() - 1)) / 100.0f);
                }
            } catch (NumberFormatException e) {
                Log.w(MainActivity.TAG, "Failed to extract percentage from '" + text + "'.");
            }
        } else if (text.startsWith("Collecting")) {
            String name = text.substring(11);
            name = name.replaceFirst("(<|>|!|=| \\()+.*", "");
            progressHandler.setProgress(-1);
            progressHandler.setText(context.getString(R.string.module_installation_searching, name));
        } else if (text.startsWith("Downloading")) {
            progressHandler.setProgress(-1);
            progressHandler.setText(context.getString(R.string.module_installation_downloading, text.substring(12)));
        } else if (text.startsWith("Installing collected packages:")) {
            progressHandler.setProgress(-1);
            progressHandler.setText(context.getString(R.string.module_installation_installing, text.substring(31)));
        } else if (text.startsWith("Building")) {
            progressHandler.setProgress(-1);
            progressHandler.setText(context.getString(R.string.module_installation_building));
        }
        Log.d(MainActivity.TAG, text);
    }
}
