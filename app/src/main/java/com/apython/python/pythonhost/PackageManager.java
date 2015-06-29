package com.apython.python.pythonhost;

/*
 * Handles the installation of python modules.
 *
 * Created by Sebastian on 16.06.2015.
 */

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PackageManager {

    public static File getModulePath(Context context) {
        return new File(context.getFilesDir(), "lib");
    }

    public static boolean installStandardModules(Context context) {
        File modulesDest = getModulePath(context);
        if (modulesDest.isDirectory()) {
            return true;
        }
        if (!(modulesDest.mkdirs() || modulesDest.isDirectory())) {
            Log.e(MainActivity.TAG, "Failed to create the 'lib' directory!");
            return false;
        }
        makeFileAccessible(modulesDest);
        InputStream moduleLocation = context.getResources().openRawResource(R.raw.lib);
        return unpackModules(modulesDest, moduleLocation);
    }

    private static boolean unpackModules(File destination, InputStream moduleLocation)
    {
        byte[] buffer = new byte[1024];
        int count;
        try {
            File destZip = new File(destination, "python27.zip");
            FileOutputStream outputFile = new FileOutputStream(destZip);
            while ((count = moduleLocation.read(buffer)) != -1) {
                outputFile.write(buffer, 0, count);
            }
            outputFile.close();
            makeFileAccessible(destZip);
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to extract the Python modules!");
            e.printStackTrace();
            return false;
        }
        Log.d(MainActivity.TAG, "" + new File(destination, "python2.7.zip").isFile());
        return true;
    }

    private static boolean makeFileAccessible(File file) {
        if(!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    Log.w(MainActivity.TAG, "Could not make file '" + file.getAbsolutePath() + "' accessible: Could not create file.");
                    return false;
                }
            } catch (IOException e) {
                Log.w(MainActivity.TAG, "Could not make file '" + file.getAbsolutePath() + "' accessible: Could not create file.");
                e.printStackTrace();
                return false;
            }
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) { // TODO: Check this
//            if (file.setReadable(true, false) && file.setExecutable(true, false)) {
//                Log.d(TAG, "Successfully made '" + file.getAbsolutePath() + "' accessible.");
//                return true;
//            }
//        }
        try {
            Runtime.getRuntime().exec("chmod 755 " + file.getAbsolutePath());
            Log.d(MainActivity.TAG, "Successfully made '" + file.getAbsolutePath() + "' accessible via chmod.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
