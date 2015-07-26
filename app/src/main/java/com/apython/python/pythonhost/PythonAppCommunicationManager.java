package com.apython.python.pythonhost;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

    public PythonAppCommunicationManager(final Activity activity) {
        this.activity = activity;
    }

    public boolean parseArgs(Intent args) {
        PROTOCOL_VERSION = args.getIntExtra("protocolVersion", -1);
        switch (PROTOCOL_VERSION) {
        case 0:
            this.appPackage   = args.getStringExtra("package");
            this.launchClass  = args.getStringExtra("launchClass");
            this.requirements = args.getStringExtra("requirements");
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

    public void startPythonApp() {
        Context context = this.activity.getApplicationContext();
        Intent args = new Intent();
        args.setComponent(new ComponentName(this.appPackage, this.launchClass));
        if (this.requirements != null) { // TODO: Add more checks to speed up startup time
            PackageManager.installRequirements(context, this.requirements);
        }
        PackageManager.checkSitePackagesAvailability(context);
        String libPath = PackageManager.getSharedLibrariesPath(context).getAbsolutePath() + "/";
        ArrayList<String> pythonLibs = new ArrayList<>();
        pythonLibs.add(libPath + System.mapLibraryName("pythonPatch"));
        pythonLibs.add(libPath + System.mapLibraryName("bzip"));
        pythonLibs.add(libPath + System.mapLibraryName("ffi"));
        pythonLibs.add(libPath + System.mapLibraryName("openSSL"));
        pythonLibs.add(libPath + System.mapLibraryName("python2.7.2"));
        pythonLibs.add(libPath + System.mapLibraryName("pyLog"));
        pythonLibs.add(libPath + System.mapLibraryName("pyInterpreter"));
        pythonLibs.add(libPath + System.mapLibraryName("application"));
        args.putExtra("pythonLibs", pythonLibs);
        args.putExtra("pythonHome", this.activity.getApplicationContext().getFilesDir().getAbsolutePath());
        args.putExtra("pythonExecutablePath", PackageManager.getPythonExecutable(context).getAbsolutePath());
        args.putExtra("xdgBasePath", PackageManager.getXDCBase(context).getAbsolutePath());
        this.activity.startActivity(args);
        this.activity.finish();
    }
}
