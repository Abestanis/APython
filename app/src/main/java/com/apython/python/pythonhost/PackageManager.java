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
        return new File(context.getFilesDir(), "lib");///python2.7");
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
        InputStream moduleLocation = context.getResources().openRawResource(R.raw.lib);
        return unpackModules(modulesDest, moduleLocation);
    }

    private static boolean unpackModules(File destination, InputStream moduleLocation)
    {
//        ZipInputStream archive;
//        try {
//            String filename;
//            archive = new ZipInputStream(new BufferedInputStream(moduleLocation));
//            ZipEntry zipEntry;
//            byte[] buffer = new byte[1024];
//            int count;
//
//            while ((zipEntry = archive.getNextEntry()) != null) {
//                filename = zipEntry.getName();
//                if (zipEntry.isDirectory()) {
//                    // Create the directory if not exists
//                    File directory = new File(destination, filename);
//                    if (!(directory.mkdirs() || directory.isDirectory())) {
//                        Log.e(MainActivity.TAG, "Could not save directory '" + filename + "' to path '"
//                                + directory.getAbsolutePath() + "' while trying to install the Python modules!");
//                        archive.close();
//                        return false;
//                    }
//                } else {
//                    // Save the file
//                    FileOutputStream outputFile = new FileOutputStream(new File(destination, filename));
//                    while ((count = archive.read(buffer)) != -1) {
//                        outputFile.write(buffer, 0, count);
//                    }
//                    outputFile.close();
//                }
//                archive.closeEntry();
//            }
//            archive.close();
//            return true;
//        }
//        catch(IOException e) {
//            Log.e(MainActivity.TAG, "Failed to extract the Python modules!");
//            e.printStackTrace();
//            return false;
//        }
        byte[] buffer = new byte[1024];
        int count;
        try {
            FileOutputStream outputFile = new FileOutputStream(new File(destination, "python27.zip"));
            while ((count = moduleLocation.read(buffer)) != -1) {
                outputFile.write(buffer, 0, count);
            }
            outputFile.close();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to extract the Python modules!");
            e.printStackTrace();
            return false;
        }
        Log.d(MainActivity.TAG, "" + new File(destination, "python2.7.zip").isFile());
        return true;
    }
}
