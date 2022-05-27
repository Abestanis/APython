package com.apython.python.pythonhost

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.*

/**
 * Created by Sebastian on 20.10.2017.
 */
object TestUtil {
    private data class VersionResolver(val entryPath: String, val versionRegex: Regex) {
        fun resolve(zipStream: ZipArchiveInputStream): String {
            var zipEntry: ArchiveEntry
            while (zipStream.nextEntry.also { zipEntry = it } != null) {
                if (zipEntry.name == entryPath) {
                    val versionMatch = zipStream.bufferedReader().lines()
                        .map { line -> versionRegex.matchEntire(line) }
                        .filter { match -> match != null }
                        .findFirst().orElse(null)
                    if (versionMatch != null && versionMatch.groups.size > 1) {
                        return versionMatch.groupValues[1]
                    }
                }
            }
            throw RuntimeException("'$entryPath' was not found in test asset")
        }
    }

    private data class LibraryData(
        val assetName: String,
        private val destinationPath: String,
        val versionResolver: VersionResolver? = null
    ) {
        private object VersionCache {
            val versions: MutableMap<String, String> = mutableMapOf()
            fun format(libraryData: LibraryData, instrumentationContext: Context): String {
                if (libraryData.versionResolver != null) {
                    versions[libraryData.assetName.replace("_library.zip", "Version")] =
                        libraryData.versionResolver.resolve(
                            ZipArchiveInputStream(
                                libraryData.open(instrumentationContext)
                            )
                        )
                }
                var formattedPath = libraryData.destinationPath
                versions.forEach { (name, value) ->
                    formattedPath = formattedPath.replace("{$name}", value)
                }
                Log.i(MainActivity.TAG, "'$formattedPath'")
                return formattedPath
            }
        }

        fun open(instrumentationContext: Context): InputStream {
            return instrumentationContext.assets.open(assetName)
        }

        fun destinationDir(instrumentationContext: Context, targetContext: Context): File {
            return File(targetContext.filesDir, VersionCache.format(this, instrumentationContext))
        }
    }

    fun installLibraryData(instrumentationContext: Context, targetContext: Context): Boolean {
        val data = arrayOf(
            LibraryData(
                "tcl_library.zip", "data/tcl{tclVersion}/library", VersionResolver(
                    "init.tcl", Regex("""package require -exact Tcl (\d+\.\d+\.\d+)""")
                )
            ),
            LibraryData(
                "tk_library.zip", "data/tcl{tclVersion}/library/tk{tkVersion}", VersionResolver(
                    "tk.tcl", Regex("""package require -exact Tk\s+(\d+\.\d+)\.\d+""")
                )
            ),
            LibraryData("terminfo.tar", "data/terminfo")
        )
        for (libraryData in data) {
            val destinationDir = try {
                libraryData.destinationDir(instrumentationContext, targetContext)
            } catch (error: IOException) {
                Log.e(MainActivity.TAG, "Unable to read test asset ${libraryData.assetName}", error)
                return false
            }

            if (destinationDir.exists()) {
                continue
            }
            if (!destinationDir.mkdirs()) {
                Log.e(
                    MainActivity.TAG,
                    "Could not create directory " + destinationDir.absolutePath
                )
                return false
            }
            val tempArchive = File(targetContext.cacheDir, libraryData.assetName)
            try {
                if (!Util.installFromInputStream(
                        tempArchive, libraryData.open(instrumentationContext), null
                    )
                ) {
                    throw IOException("Unable to install from the asset resource")
                }
            } catch (error: IOException) {
                Util.deleteDirectory(destinationDir)
                Log.e(
                    MainActivity.TAG,
                    "Could not create temporary archive at " + tempArchive.absolutePath,
                    error
                )
                return false
            }
            val success = Util.extractArchive(
                tempArchive,
                destinationDir,
                null
            ) // TODO: See if there is a zip stream
            tempArchive.delete()
            if (!success) {
                Util.deleteDirectory(destinationDir)
                Log.e(
                    MainActivity.TAG,
                    "Failed to extract archive to " + destinationDir.absolutePath
                )
                return false
            }
        }
        return true
    }

    fun copyNativePythonLibraries(context: Context): Boolean {
        val excludeLibList = listOf(
            System.mapLibraryName("application"),
            System.mapLibraryName("pyInterpreter"),
            System.mapLibraryName("pyLog")
        )
        val dynLibPath = PackageManager.getDynamicLibraryPath(context)
        val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        if (!dynLibPath.isDirectory && !dynLibPath.mkdirs()) {
            Log.e(MainActivity.TAG, "Failed to create the dynLib path")
            return false
        }
        for (libFile in nativeLibDir.listFiles().orEmpty()) {
            if (!excludeLibList.contains(libFile.name)) {
                try {
                    val inStream = FileInputStream(libFile)
                    val outStream = FileOutputStream(File(dynLibPath, libFile.name))
                    val inChannel = inStream.channel
                    inChannel.transferTo(0, inChannel.size(), outStream.channel)
                    inStream.close()
                    outStream.close()
                } catch (error: IOException) {
                    Log.e(MainActivity.TAG, "Failed to copy native library " + libFile.name, error)
                    return false
                }
            }
        }
        return true
    }

    fun installPythonLibraries(
        instrumentationContext: Context,
        targetContext: Context?,
        pythonVersion: String
    ): Boolean {
        val libDest = PackageManager.getStandardLibPath(targetContext)
        if (!(libDest.mkdirs() || libDest.isDirectory)) {
            Log.e(MainActivity.TAG, "Failed to create the 'lib' directory!")
            return false
        }
        val libZip = File(libDest, "python" + pythonVersion.replace(".", "") + ".zip")
        if (libZip.exists()) {
            return true
        }
        Util.makeFileAccessible(libDest, false)
        val testAssets = instrumentationContext.assets
        val libLocation: InputStream = try {
            testAssets.open("lib" + pythonVersion.replace('.', '_') + ".zip")
        } catch (error: IOException) {
            Log.e(
                MainActivity.TAG, "Did not find the library Zip for the Python version " +
                        pythonVersion, error
            )
            return false
        }
        return Util.installFromInputStream(libZip, libLocation, null)
    }
}