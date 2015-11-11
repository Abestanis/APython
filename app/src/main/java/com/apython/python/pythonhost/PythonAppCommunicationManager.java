package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.apython.python.pythonhost.downloadcenter.PythonDownloadCenterActivity;

import java.io.File;
import java.util.ArrayList;

/*
 * This manager handles the entire communication with the Python app.
 * It returns all information necessary to run python code from this interpreter.
 * It also installs required python modules if necessary.
 *
 * Created by Sebastian on 31.05.2015.
 */

public class PythonAppCommunicationManager {

    // The tag used by the Python host.
    public static final String TAG                  = MainActivity.TAG;
    // The newest protocol version this Python host can understand.
    public static final int    MAX_PROTOCOL_VERSION = 0;
    // The actual protocol version used in the current communication.
    public              int    PROTOCOL_VERSION     = -1;

    // The current activity started by the Python app.
    private Activity activity;

    // The package of the Python app with which we communicate.
    private String appPackage;
    // The classpath that serves as an entry point when we should launch the Python app.
    private String launchClass;
    // The path to a requirements.txt file which contains all the requirements for this App
    private String requirements = null;
    // The python version this App needs to run
    private String pythonVersion = null;
    private String minPythonVersion = null;
    private String maxPythonVersion = null;
    private String[] disallowedPythonVersions = null;

    public PythonAppCommunicationManager(final Activity activity) {
        this.activity = activity;
    }

    public boolean parseArgs(Intent args) {
        PROTOCOL_VERSION = args.getIntExtra("protocolVersion", -1);
        switch (PROTOCOL_VERSION) {
        case 0:
            this.appPackage       = args.getStringExtra("package");
            this.launchClass      = args.getStringExtra("launchClass");
            this.requirements     = args.getStringExtra("requirements");
            this.pythonVersion    = args.getStringExtra("pythonVersion");
            this.minPythonVersion = args.getStringExtra("minPythonVersion");
            this.maxPythonVersion = args.getStringExtra("maxPythonVersion");
            this.disallowedPythonVersions = args.getStringArrayExtra("disallowedPythonVersions");
            break;
        case -1:
            Log.e(TAG, "Client did not send protocol version!");
            PROTOCOL_VERSION = 0;
            exitWithError("Missing intent field: protocolVersion");
            return false;
        default:
            Log.w(TAG, "Client uses an unknown protocol version (" + PROTOCOL_VERSION
                                 + "), maximum supported protocol version is " + MAX_PROTOCOL_VERSION);
            PROTOCOL_VERSION = 0;
            exitWithError("Unknown protocol version");
            return false;
        }
        return true;
    }

    private void exitWithError(String errorMessage) {
        Log.e(TAG, "PythonHost error: " + errorMessage);
        Intent result = new Intent();
        result.putExtra("protocolVersion", PROTOCOL_VERSION);
        result.putExtra("errorMessage", errorMessage);
        this.activity.setResult(Activity.RESULT_CANCELED, result);
        this.activity.finish();
    }

    public void startPythonApp(ProgressHandler progressHandler) {
        Context context = this.activity.getApplicationContext();
        Intent args = new Intent();
        args.setComponent(new ComponentName(this.appPackage, this.launchClass));

        // Determine the python version to use
        // TODO: This should consider the installed requirements
        String determinedPythonVersion = PackageManager.getOptimalInstalledPythonVersion(
                context, this.pythonVersion, this.minPythonVersion, this.maxPythonVersion,
                this.disallowedPythonVersions);
        if (determinedPythonVersion == null) {
            // TODO: This must work automatically without the interaction from the user
            this.activity.startActivity(new Intent(context, PythonDownloadCenterActivity.class));
            // FIXME: This is temporary
            exitWithError("No suitable Python version installed, try again!");
            return;
        }
        PackageManager.installPythonExecutable(context, progressHandler);
        if (this.requirements != null) {
            PackageManager.installRequirements(context, this.requirements, determinedPythonVersion, progressHandler);
        }
        PackageManager.checkSitePackagesAvailability(context, determinedPythonVersion, progressHandler);

        String stdLibPath = PackageManager.getSharedLibrariesPath(context).getAbsolutePath() + "/";
        String dynamicLibPath = PackageManager.getDynamicLibraryPath(context).getAbsolutePath() + "/";
        ArrayList<String> pythonLibs = new ArrayList<>();
        pythonLibs.add(dynamicLibPath + System.mapLibraryName("pythonPatch"));
        for (File libFile : PackageManager.getAdditionalLibraries(context)) {
            pythonLibs.add(libFile.getAbsolutePath());
        }
        pythonLibs.add(dynamicLibPath + System.mapLibraryName("python" + Util.getMainVersionPart(determinedPythonVersion)));
        pythonLibs.add(stdLibPath + System.mapLibraryName("pyLog"));
        pythonLibs.add(stdLibPath + System.mapLibraryName("pyInterpreter"));
        pythonLibs.add(stdLibPath + System.mapLibraryName("application"));
        args.putExtra("pythonLibs", pythonLibs);
        args.putExtra("pythonHome", this.activity.getApplicationContext().getFilesDir().getAbsolutePath());
        args.putExtra("pythonExecutablePath", PackageManager.getPythonExecutable(context).getAbsolutePath());
        args.putExtra("xdgBasePath", PackageManager.getXDGBase(context).getAbsolutePath());
        args.putExtra("pythonVersion", determinedPythonVersion);
        this.activity.startActivity(args);
    }
}
