package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.apython.python.pythonhost.downloadcenter.PythonDownloadCenterActivity;

import java.io.File;

/*
 * This manager handles the entire communication with the Python app.
 * It returns all information necessary to run python code from this interpreter.
 * It also installs required python modules if necessary.
 *
 * Created by Sebastian on 31.05.2015.
 */

public class PythonAppCommunicationManager {

    // The tag used by the Python host.
    public static final  String TAG                  = MainActivity.TAG;
    // The newest protocol version this Python host can understand.
    private static final int    MAX_PROTOCOL_VERSION = 0;
    // The actual protocol version used in the current communication.
    private              int    PROTOCOL_VERSION     = -1;

    // The current activity started by the Python app.
    private final Activity activity;
    
    // The path to a requirements.txt file which contains all the requirements for this App
    private String requirements = null;
    // The python version this App needs to run
    private String pythonVersion = null;
    private String minPythonVersion = null;
    private String maxPythonVersion = null;
    private String[] disallowedPythonVersions = null;

    PythonAppCommunicationManager(final Activity activity) {
        this.activity = activity;
    }

    boolean parseArgs(Intent args) {
        PROTOCOL_VERSION = args.getIntExtra("protocolVersion", -1);
        switch (PROTOCOL_VERSION) {
        case 0:
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

    void startPythonApp(ProgressHandler progressHandler) {
        Context context = this.activity.getApplicationContext();
        Intent args = new Intent();
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
            // TODO: Check return
            PackageManager.installRequirements(context, this.requirements, determinedPythonVersion, progressHandler);
        }
        // TODO: Check return
        PackageManager.checkSitePackagesAvailability(context, determinedPythonVersion, progressHandler);
        
        args.putExtra("pythonVersion", determinedPythonVersion);
        args.putExtra("libPath", new File(PackageManager.getSharedLibrariesPath(context),
                                          System.mapLibraryName("application")).getAbsolutePath());
        Intent securityIntent = this.activity.getIntent().getParcelableExtra("securityIntent");
        if (securityIntent != null) {
            securityIntent.putExtras(args);
            this.activity.startActivity(securityIntent);
        }
        this.activity.setResult(Activity.RESULT_OK, args);
    }
}
