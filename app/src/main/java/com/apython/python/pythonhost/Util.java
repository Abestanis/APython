package com.apython.python.pythonhost;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*
 * This class provides a bunch of general useful utility functions.
 *
 * Created by Sebastian on 05.07.2015.
 */

public class Util {

    /**
     * Ensures that a file or directory can be accessed from other apps.
     * More specific, ensures that other apps have read and execute,
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
        //            if (file.setReadable(true, false) && file.setExecutable(true, false)) { // TODO: Check this
        //                Log.d(TAG, "Successfully made '" + file.getAbsolutePath() + "' accessible.");
        //                return true;
        //            }
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
     * Makes the path between stop and startPath read and executable by everyone.
     * 
     * @param startPath The directory from which to start and work up towards stopPath.
     * @param stopPath The directory to stop.
     * @return {@code true} if every directory between stop and startPath has been made accessible.
     */
    public static boolean makePathAccessible(File startPath, File stopPath) {
        while (!stopPath.equals(startPath)) {
            if (!Util.makeFileAccessible(startPath, false)) return false;
            startPath = startPath.getParentFile();
        }
        return true;
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
     * Connects to an url and returns the {@link URLConnection} on success.
     *
     * @param url The url to connect to.
     * @param timeout The maximum time in milliseconds to wait for the connection to be established.
     * @return The {@code URLConnection} on success and {@code null} on failure.
     */
    public static URLConnection connectToUrl(String url, int timeout) {
        URL validUrl;
        try {
            validUrl = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(MainActivity.TAG, "Failed to connect to '" + url + "': Invalid url!", e);
            return null;
        }
        try {
            URLConnection connection = validUrl.openConnection();
            connection.setConnectTimeout(timeout);
            return connection;
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Failed to connect to '" + url + "'!", e);
            return null;
        }
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
        long startTime;
        long timeDiff;
        int secCount = 0;
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
            startTime = System.currentTimeMillis();
            while ((count = resourceLocation.read(buffer)) != -1) {
                outputFile.write(buffer, 0, count);
                if (progressHandler != null) {
                    totalCount += count;
                    secCount += count;
                    if (totalCount >= nextUpdate) {
                        timeDiff = System.currentTimeMillis() - startTime;
                        float progress = (float) totalCount / inputLength;
                        if (timeDiff >= 1000L) {
                            int bytesPerSecond = Math.round(secCount * (timeDiff / 1000.0f));
                            progressHandler.setProgress(
                                    progress,
                                    bytesPerSecond,
                                    Math.round(Math.max(inputLength - totalCount, 0) / Math.max(0.0001f, bytesPerSecond))
                            );
                            startTime = System.currentTimeMillis();
                            secCount = 0;
                        } else {
                            progressHandler.setProgress(progress);
                        }
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
            Log.e(MainActivity.TAG, "Failed to install resource to " +
                    destination.getAbsolutePath() + "!", e);
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
        URLConnection connection = Util.connectToUrl(url, 20000);
        if (connection == null) {
            Log.e(MainActivity.TAG, "Failed to connect to '" + url + "'!");
            return false;
        }
        MessageDigest hashAlgorithm;
        try {
            hashAlgorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(MainActivity.TAG, "Failed to download from '" + url + "': Md5 Hash is not supported on this device!", e);
            return false;
        }
        try {
            DigestInputStream inputStream = new DigestInputStream(connection.getInputStream(), hashAlgorithm);
            if (!Util.installFromInputStream(destination, inputStream, connection.getContentLength(), progressHandler)) {
                inputStream.close();
                return false;
            }
            if (!hashToHex(hashAlgorithm.digest()).equals(md5CheckSum)) {
                Log.w(MainActivity.TAG, "Md5 checksum of downloaded File invalid!");
                if (!destination.delete()) {
                    Log.e(MainActivity.TAG, "Failed to delete downloaded file '" + destination.getAbsolutePath() + "'.");
                }
                inputStream.close();
                return false;
            }
            inputStream.close();
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
     * Extracts a given archive into the given directory.
     * Progress is reported via the optional {@code ProgressHandler}.
     * 
     * @param archive The archive to extract.
     * @param destDir The directory to extract the archive into.
     * @param progressHandler An optional progress handler (can be {@code null}).
     * @return {@code true} if the data was successfully extracted.
     */
    public static boolean extractArchive(File archive, File destDir, ProgressHandler progressHandler) {
        if (archive.getName().endsWith(".zip")) {
            try {
                ZipFile zipFile = new ZipFile(archive);
                int numEntries = 0;
                if (progressHandler != null) {
                    numEntries = zipFile.size();
                    progressHandler.setProgress(0);
                }
                Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
                ZipEntry zipEntry;
                float entryCount = 1.0f;
                byte[] buffer = new byte[8192];
                while (zipEntries.hasMoreElements()) {
                    zipEntry = zipEntries.nextElement();
                    File file = new File(destDir, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                                                dir.getAbsolutePath());
                    }
                    if (!zipEntry.isDirectory()) {
                        FileOutputStream outputStream = new FileOutputStream(file);
                        InputStream inputStream = zipFile.getInputStream(zipEntry);
                        int count;
                        try {
                            while ((count = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, count);
                            }
                        } finally {
                            try {
                                outputStream.close();
                                inputStream.close();
                            } catch (IOException unused) { /* ignore it */ }
                        }
                    }
                    if (progressHandler != null) progressHandler.setProgress(entryCount / numEntries);
                    entryCount++;
                }
                zipFile.close();
                Util.makeFileAccessible(destDir, true);
            } catch (IOException e) {
                Log.e(MainActivity.TAG, "Extracting archive " + archive.getAbsolutePath() + " to "
                        + destDir.getAbsolutePath() + " failed!", e);
                return false;
            }
        } else {
            Log.w(MainActivity.TAG, "Could not extract archive from " + archive.getAbsolutePath()
                    + ": Unknown archive format!");
            return false;
        }
        return true;
    }

    /**
     * Converts a hash into a hex string.
     *
     * @param hash The hash to convert.
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
     * @return The content of the given {@code InputStream} as a {@code String}.
     */
    public static String convertStreamToString(InputStream input) {
        if (input == null) { return ""; }
        Scanner s = new Scanner(input).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Returns the first and the second part of a Python version.
     *
     * @param version A Python version.
     * @return The first and the second part of a Python version.
     */
    public static String getMainVersionPart(@NonNull String version) {
        String[] versionParts = version.split("\\.");
        return versionParts[0] + "." + versionParts[1];
    }

    /**
     * Converts a library File to the library name.
     *
     * @param libFile the library file.
     * @return The name of the library (without {@code lib} and {@code .so}).
     */
    public static String getLibraryName(File libFile) {
        String libFileName = libFile.getName();
        return libFileName.substring(3, libFileName.lastIndexOf('.'));
    }

    /**
     * Like {@link Math#round(float)}, but takes an additional parameter to specify at which
     * decimal should be rounded.
     *
     * @param number The float to be rounded.
     * @param decimalPlace The number of digits after the decimal point that the rounded float should have.
     * @return The rounded float with the specified number of digits after the decimal point.
     */
    public static float round(float number, int decimalPlace) {
        BigDecimal decimal = new BigDecimal(Float.toString(number));
        decimal = decimal.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return decimal.floatValue();
    }

    /**
     * Generates a string which contains information about a download progress.
     * The returned String can be used in the UI.
     *
     * @param context The current context.
     * @param bytesPerSecond The amount of bytes read over the last second.
     * @param remainingSeconds The amount of seconds remaining until the action has finished.
     * @return A human readable string which contains the provided information.
     */
    public static String generateDownloadInfoText(Context context, int bytesPerSecond, int remainingSeconds) {
        float bytesAmount = bytesPerSecond;
        String bytesUnit = "B";
        DecimalFormat decimalFormatter = new DecimalFormat();
        if (bytesAmount >= 1000000000) {
            bytesAmount = round(bytesAmount / 1000000000, 2);
            bytesUnit = "GB";
        } else if (bytesAmount >= 1000000) {
            bytesAmount = round(bytesAmount / 1000000, 2);
            bytesUnit = "MB";
        } else if (bytesAmount >= 1000) {
            bytesAmount = round(bytesAmount / 1000, 2);
            bytesUnit = "kB";
        }
        decimalFormatter.setMaximumFractionDigits(2);
        decimalFormatter.setMinimumFractionDigits(0);
        decimalFormatter.setGroupingUsed(false);
        return context.getResources().getQuantityString(
                R.plurals.downloadManager_progress_download, remainingSeconds,
                decimalFormatter.format(bytesAmount), bytesUnit, remainingSeconds);
    }

    /**
     * Returns a formatted version of the given amount bytes with a unit.
     *
     * @param bytes The amount of bytes.
     * @return The formatted amount of bytes and a unit name.
     */
    public static String[] getFormattedBytes(long bytes) {
        if (bytes >= 1000000000) {
            return new String[] {String.valueOf(round(bytes / 1000000000.0f, 2)), "GB"};
        } else if (bytes >= 1000000) {
            return new String[] {String.valueOf(round(bytes / 1000000.0f, 2)), "MB"};
        } else if (bytes >= 1000) {
            return new String[] {String.valueOf(round(bytes / 1000.0f, 2)), "kB"};
        } else {
            return new String[] {String.valueOf(bytes), "B"};
        }
    }

    /**
     * Checks if an url is valid.
     *
     * @param url The url to check.
     * @return {@code true} if the url is valid, {@code false} otherwise.
     */
    public static boolean isValidUrl(String url) {
        try {
            URL validUrl = new URL(url);
            return !"".equals(validUrl.getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Calculates the size of the given directory.
     *
     * @param directory The directory to calculate the size for.
     * @return The size in bytes
     */
    public static long calculateDirectorySize(File directory) {
        long size = 0;
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * Convert the String pythonVersion to an Array of ints where the first element is the
     * major, the second is the minor and the third is the micro version-part.
     *
     * @param pythonVersion The Python version to convert.
     * @return The int[] representation of the given Python version.
     */
    public static int[] getNumericPythonVersion(String pythonVersion) {
        String[] versionParts = pythonVersion.split("\\.");
        int[] version = {Integer.valueOf(versionParts[0]), 0, 0};
        if (versionParts.length >= 3) {
            version[1] = Integer.valueOf(versionParts[1]);
            version[2] = Integer.valueOf(versionParts[2]);
        } else if (versionParts.length == 2) {
            version[1] = Integer.valueOf(versionParts[1]);
        }
        return version;
    }

    /**
     * Count how often the given character occurs in the given string.
     *
     * @param string The string to search in.
     * @param character The character to search for.
     * @return The number of times the character was found in the string.
     */
    public static int countCharacterOccurrence(String string, char character) {
        return string.length() - string.replace(String.valueOf(character), "").length();
    }

    /**
     * Translates a string into a list of key events, which, executed on an
     * {@link android.widget.EditText EditText} would produce the given string.
     * 
     * @param input The string which should be converted into key events.
     * @return A list of key events.
     */
    public static KeyEvent[] stringToKeyEvents(String input) {
        KeyCharacterMap charMap;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        } else {
            charMap = KeyCharacterMap.load(KeyCharacterMap.ALPHA);
        }
        KeyEvent[] keyEvents = charMap.getEvents(input.toCharArray());
        return keyEvents != null ? keyEvents : new KeyEvent[0];
    }

    /**
     * Returns the file extension of a given file.
     * <p>
     * Examples:
     * <br>
     * test.txt -> txt
     * <br>
     * archive.tar.gz -> tar.gz
     * 
     * @param file The file who's extension should be returned. 
     * @return The file extension of the given file.
     */
    public static String getFileExt(File file) {
        final String[] SECONDARY_EXTENSIONS = {"tar"}; // For tar.gz
        String extension = null;
        String[] parts = file.getName().split("\\.");
        if (parts.length > 2) {
            for (String secondaryExtension : SECONDARY_EXTENSIONS) {
                if (secondaryExtension.equals(parts[1])) {
                    extension = parts[1] + parts[2];
                    break;
                }
            }
            if (extension == null) extension = parts[2];
        } else if (parts.length > 1) {
            extension = parts[1];
        }
        return extension;
    }

    /**
     * Joins all given strings with the connector. 
     * 
     * @param connector A string which should connect all given strings.
     * @param parts A list of strings which should be concatenated.
     * @return The joined string.
     */
    public static String join(String connector, ArrayList<String> parts) {
        Iterator<String> partsIterator = parts.iterator();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(partsIterator.next());
        while (partsIterator.hasNext()) stringBuilder.append(connector).append(partsIterator.next());
        return stringBuilder.toString();
    }

    /**
     * Convenience method to get a drawable.
     * 
     * @param context The context from which to get the drawable.
     * @param id The identifier of the resource
     * @return The requested drawable.
     * @see android.content.res.Resources#getDrawable(int, Resources.Theme) 
     */
    public static Drawable getResourceDrawable(Context context, @DrawableRes int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(id, context.getTheme());
        } else {
            //noinspection deprecation
            return context.getResources().getDrawable(id);
        }
    }
}
