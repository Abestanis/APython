package com.apython.python.pythonhost;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/*
 * This class provides a bunch of general useful utility unctions.
 *
 * Created by Sebastian on 05.07.2015.
 */

public class Util {

    public static boolean recursiveMakeDirAccessible(File dir) {
        if (!makeFileAccessible(dir)) { return false; }
        for (File child: dir.listFiles()) {
            if (child.isDirectory()) {
                if (!recursiveMakeDirAccessible(child)) { return false; }
            } else {
                if (!makeFileAccessible(child)) { return false; }
            }
        }
        return true;
    }

    public static boolean makeFileAccessible(File file) {
        if (!file.exists()) {
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

    public static ArrayList<File> checkDirContentsAccessibility(File dir) {
        ArrayList<File> inaccessibleFiles = new ArrayList<>();
        ProcessBuilder processBuilder = new ProcessBuilder("ls", "-l").directory(dir);
        Process process;
        try {
            process = processBuilder.start();
            PrintWriter out = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            out.flush();
            String line;
            while((line = in.readLine()) != null) {
                String[] parts = line.trim().split(" ");
                String permission = parts[0];
                if (permission.charAt(1) == '-' || permission.charAt(2) == '-' || permission.charAt(3) == '-'
                        || permission.charAt(4) == '-' || permission.charAt(6) == '-'
                        || permission.charAt(7) == '-' || permission.charAt(9) == '-') {
                    inaccessibleFiles.add(new File(dir, parts[parts.length - 1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inaccessibleFiles;
    }

    public static HttpResponse connectToUrl(String url) {
        HttpResponse response = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));
            response = client.execute(request);
        } catch (URISyntaxException uriE) {
            uriE.printStackTrace();
        } catch (ClientProtocolException cpE) {
            cpE.printStackTrace();
        } catch (IOException ioE) {
            ioE.printStackTrace();
        }
        return response;
    }

    public static boolean installRawResource(File destination, InputStream resourceLocation) {
        byte[] buffer = new byte[1024];
        int count;
        try {
            FileOutputStream outputFile = new FileOutputStream(destination);
            while ((count = resourceLocation.read(buffer)) != -1) {
                outputFile.write(buffer, 0, count);
            }
            outputFile.close();
            Util.makeFileAccessible(destination);
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to extract the raw resource to " + destination.getAbsolutePath() + "!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static <T> T[] mergeArrays(T[] first, T[] second) {
        if (second == null) { return first; }
        if (first == null) { return second; }

        int aLen = first.length;
        int bLen = second.length;

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(first.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(first, 0, result, 0, aLen);
        System.arraycopy(second, 0, result, aLen, bLen);

        return result;
    }

}
