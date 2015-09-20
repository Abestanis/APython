package com.apython.python.pythonhost;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * This class provides a bunch of general useful utility functions.
 *
 * Created by Sebastian on 05.07.2015.
 */

public class Util {

    /**
     * Ensures that a file or directory can be accessed from other apps.
     * More specific, ensures that other apps have rad and execute,
     * but not write permission.
     *
     * @param file The file or directory, that should be made accessible.
     * @param recursive If {@code true}, makes all files and subdirectories of the given
     *                  directory accessible, if the given FileObject is a directory.
     * @return {@code true} on success, {@code false} otherwise.
     */
    public static boolean makeFileAccessible(File file, boolean recursive) {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    Log.w(MainActivity.TAG, "Could not make file '" + file.getAbsolutePath() + "' accessible: Could not create file.");
                    return false;
                }
            } catch (IOException e) {
                Log.w(MainActivity.TAG, "Could not make file '" + file.getAbsolutePath() + "' accessible: Could not create file.", e);
                return false;
            }
        }
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) { // TODO: Check this
        //            if (file.setReadable(true, false) && file.setExecutable(true, false)) {
        //                Log.d(TAG, "Successfully made '" + file.getAbsolutePath() + "' accessible.");
        //                return true;
        //            }
        //        }
        String rec = recursive ? "-R " : "";
        try {
            int result = Runtime.getRuntime().exec("chmod " + rec + "755 " + file.getAbsolutePath()).waitFor();
            if (result == 0) {
                Log.d(MainActivity.TAG, "Successfully made '" + file.getAbsolutePath() + "' accessible via chmod.");
                return true;
            } else {
                Log.w(MainActivity.TAG, "Failed to make '" + file.getAbsolutePath() + "' accessible via chmod: Process failed with exit status " + result);
                return true;
            }
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to make '" + file.getAbsolutePath() + "' accessible via chmod!", e);
        } catch (InterruptedException ie) {
            Log.e(MainActivity.TAG, "Failed to make '" + file.getAbsolutePath() + "' accessible via chmod: Chmod was interrupted!", ie);
        }
        return false;
    }

    /**
     * Checks if the given directory and all it's content
     * are accessible from other apps. More specific, checks
     * that all apps have the right to read and execute.
     *
     * @param dir The directory that should be checked.
     * @return {@code true}, if the directory and all it's contents are accessible.
     */
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

    /**
     * Connects to an url and returns the {@code HttpResponse} on success.
     *
     * @param url The url to connect to.
     * @param timeout The maximum amount of time to wait for the connection to be established.
     * @return A {@code HttpResponse} on success and {@code null} on failure.
     */
    public static HttpResponse connectToUrl(String url, int timeout) {
        HttpResponse response = null;
        try {
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, timeout * 1000);
            HttpClient client = new DefaultHttpClient(httpParams);
            HttpGet request = new HttpGet();
            request.setURI(new URI(url));
            response = client.execute(request);
        } catch (URISyntaxException uriE) {
            Log.e(MainActivity.TAG, "Failed to connect to '" + url + "': Invalid url!", uriE);
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to connect to '" + url + "'!", e);
        }
        return response;
    }

    /**
     * See {@link #installFromInputStream(File, InputStream, long, ProgressHandler)}.
     *
     * @param destination The destination where the {@code InputStream} should be saved.
     * @param resourceLocation The {@code InputStream} to read from.
     * @param progressHandler An optional progress handler (can be {@code null}).
     * @return {@code true}, if the installation succeeded, {@code false} otherwise.
     */
    public static boolean installFromInputStream(File destination, InputStream resourceLocation, ProgressHandler progressHandler) {
        long size;
        try {
            size = resourceLocation.available();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to install resource to " + destination.getAbsolutePath() + ": Failed to get the estimated length of the input.", e);
            return false;
        }
        return installFromInputStream(destination, resourceLocation, size, progressHandler);
    }

    /**
     * Installs the contents of the given {@code InputStream} and saves it to the specified
     * destination. Uses the {@code ProgressHandler} to report progress.
     *
     * @param destination The destination where the {@code InputStream} should be saved.
     * @param resourceLocation The {@code InputStream} to read from.
     * @param inputLength The total amount of bytes that can be read from the {@code InputStream}.
     * @param progressHandler An optional progress handler (can be {@code null}).
     * @return {@code true}, if the installation succeeded, {@code false} otherwise.
     */
    public static boolean installFromInputStream(File destination, InputStream resourceLocation, long inputLength, ProgressHandler progressHandler) {
        byte[] buffer = new byte[1024];
        int count;
        try {
            int totalCount = 0;
            float nextUpdate = 0;
            float onePercent = inputLength / 100.0f;
            if (!destination.getParentFile().exists() && !destination.getParentFile().mkdirs()) {
                Log.w(MainActivity.TAG, "Failed to install resource to " + destination.getAbsolutePath() + ": Could not make the directories!");
                return false;
            }
            if (progressHandler != null) {
                progressHandler.setProgress(0);
            }
            FileOutputStream outputFile = new FileOutputStream(destination);
            while ((count = resourceLocation.read(buffer)) != -1) {
                outputFile.write(buffer, 0, count);
                if (progressHandler != null) {
                    totalCount += count;
                    if (totalCount >= nextUpdate) {
                        progressHandler.setProgress((float) totalCount / inputLength);
                        nextUpdate += onePercent;
                    }
                }
            }
            if (progressHandler != null) {
                progressHandler.setProgress(-1);
            }
            outputFile.close();
            Util.makeFileAccessible(destination, false);
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to install resource to " + destination.getAbsolutePath() + "!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Downloads a file from a specified url and saves the contents in the
     * given destination. The content will be checked against the given
     * md5CheckSum. Progress is reported via the optional {@code ProgressHandler}.
     *
     * @param url The url from which to download from.
     * @param destination The file to save the contents of the data retrieved from the url to.
     * @param md5CheckSum The md5 check sum the downloaded data must have in order to be valid.
     * @param progressHandler An optional progress handler (can be {@code null}).
     * @return {@code true} if the data was successfully downloaded, verified and made accessible
     * to other apps.
     */
    public static boolean downloadFile(String url, File destination, String md5CheckSum, ProgressHandler progressHandler) {
        HttpResponse response = Util.connectToUrl(url, 30);
        if (response == null) {
            Log.e(MainActivity.TAG, "Failed to connect to '" + url + "'!");
            return false;
        }
        HttpEntity entity = response.getEntity();
        MessageDigest hashAlgorithm;
        try {
            hashAlgorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(MainActivity.TAG, "Failed to download from '" + url + "': Md5 Hash is not supported on this device!", e);
            return false;
        }
        try {
            DigestInputStream inputStream = new DigestInputStream(entity.getContent(), hashAlgorithm);
            entity.getContentLength();
            if (!Util.installFromInputStream(destination, inputStream, entity.getContentLength(), progressHandler)) {
                return false;
            }
            if (!hashToHex(hashAlgorithm.digest()).equals(md5CheckSum)) {
                Log.w(MainActivity.TAG, "Md5 checksum of downloaded File invalid!");
                if (!destination.delete()) {
                    Log.e(MainActivity.TAG, "Failed to delete downloaded file '" + destination.getAbsolutePath() + "'.");
                }
                return false;
            }
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to download '" + destination.getName() + "'!", e);
            if (destination.exists()) {
                if (!destination.delete()) {
                    Log.w(MainActivity.TAG, "Failed to clean up downloaded file '" + destination.getAbsolutePath() + "'.");
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Converts a hash into a hex string.
     *
     * @param hash The hash to convert
     * @return A hexadecimal representation of the hash.
     */
    public static String hashToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hash) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Add two arrays together.
     *
     * @param first The first array.
     * @param second The second array.
     * @param <T> The type of array.
     * @return A new array that contains all the elements of {@code first} and {@code second}.
     */
    public static <T> T[] mergeArrays(T[] first, T[] second) {
        if (second == null) {
            if (first != null) {
                return first;
            } else {
                throw new IllegalArgumentException("No array given to mergeArrays.");
            }
        }
        if (first == null) { return second; }

        int aLen = first.length;
        int bLen = second.length;

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(first.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(first, 0, result, 0, aLen);
        System.arraycopy(second, 0, result, aLen, bLen);

        return result;
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

    /**
     * Delete a directory and all it's content.
     *
     * @param directory The directory to delete.
     * @return {@code true} on success, {@code false} otherwise.
     */
    public static boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                } else {
                    if (!file.delete()) {
                        Log.w(MainActivity.TAG, "Failed to delete file '" + file.getAbsolutePath() + "'.");
                        return false;
                    }
                }
            }
        }
        if (directory.exists() && !directory.delete()) {
            Log.w(MainActivity.TAG, "Failed to delete directory '" + directory.getAbsolutePath() + "'.");
            return false;
        }
        return true;
    }

    /**
     * Converts the content of a given {@code InputStream} to a string.
     *
     * @param input An {@code InputStream} to read from.
     * @return The content of the given {@code InputStream} as a {@code String}
     */
    public static String convertStreamToString(InputStream input) {
        if (input == null) { return ""; }
        Scanner s = new Scanner(input).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Returns the first and the second part of a python version.
     *
     * @param version A python Version
     * @return The first and the second part of a python version.
     */
    public static String getMajorMinorVersionPart(@NonNull String version) {
        String[] versionParts = version.split("\\.");
        return versionParts[0] + "." + versionParts[1];
    }

    /**
     * Converts a library File to the library name.
     *
     * @param libFile the library file
     * @return The name of the library
     */
    public static String getLibraryName(File libFile) {
        String libFileName = libFile.getName();
        return libFileName.substring(3, libFileName.lastIndexOf('.'));
    }
}
