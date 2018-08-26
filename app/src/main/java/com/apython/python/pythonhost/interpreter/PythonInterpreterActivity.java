package com.apython.python.pythonhost.interpreter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.PythonSettingsActivity;
import com.apython.python.pythonhost.R;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.interpreter.handles.InterpreterPseudoTerminalIOHandle;
import com.apython.python.pythonhost.interpreter.handles.PythonInterpreterHandle;
import com.apython.python.pythonhost.interpreter.handles.PythonInterpreterProcessHandle;
import com.apython.python.pythonhost.views.ActivityLifecycleEventListener;
import com.apython.python.pythonhost.views.PythonFragment;
import com.apython.python.pythonhost.views.interfaces.SDLWindowInterface;
import com.apython.python.pythonhost.views.interfaces.WindowManagerInterface;
import com.apython.python.pythonhost.views.interfaces.TerminalInterface;
import com.apython.python.pythonhost.views.terminal.TerminalFragment;
import com.apython.python.pythonhost.views.terminalwm.WindowManagerFragment;

import java.io.File;
import java.util.ArrayList;

/*
 * This Activity starts and displays a Python interpreter.
 *
 * Created by Sebastian on 08.06.2015.
 */

public class PythonInterpreterActivity extends Activity {
    private TerminalInterface       terminalView;
    private WindowManagerInterface  terminalWindowManager;
    private PythonInterpreterHandle interpreter = null;
    private String[] interpreterArgs = null;
    boolean startedInterpreter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.terminalWindowManager = PythonFragment.create(WindowManagerFragment.class, this, "wm");
        this.setContentView(R.layout.activity_python_interpreter);
        ViewGroup container = ((ViewGroup) this.findViewById(R.id.pyHostWindowContainer));
        container.addView(((PythonFragment) terminalWindowManager).createView(container));
        addTerminalWindow();
        interpreter = new PythonInterpreterProcessHandle(this);
        interpreter.setIOHandler(new PythonInterpreterHandle.LineIOHandler() {
            @Override
            public void onOutput(final String output) {
                PythonInterpreterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        terminalView.addOutput(output);
                    }
                });
            }

            @Override
            public void enableLineMode(final String prompt) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        terminalView.enableLineInput(prompt);
                    }
                });
            }

            @Override
            public void stopLineMode() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        terminalView.disableLineInput();
                    }
                });
            }
        });
        interpreter.setExitHandler(new PythonInterpreter.ExitHandler() {
            @Override
            public void onExit(int exitCode) {
                Log.d(MainActivity.TAG, "Python interpreter exited with exit code " + exitCode);
                finish();
            }
        });
        Intent intent = getIntent();
        Uri filePathData = intent.getData();
        if (filePathData != null) { // TODO: Show warnings for permissions, read python version from file
            Util.makeFileAccessible(new File(filePathData.toString()), false);
            interpreterArgs = new String[] {filePathData.toString()};
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ClipData clipData = getIntent().getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                Uri fileUri = clipData.getItemAt(0).getUri();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                        fileUri != null) {
                    String filePath = Util.getRealPathFromURI(this, fileUri);
                    if (filePath != null) {
                        Util.makeFileAccessible(new File(filePath), false);
                        interpreterArgs = new String[] {filePath};
                    }
                }
            }
        }
        String pyVersion = intent.getStringExtra("pythonVersion");
        if (pyVersion == null) {
            pyVersion = PreferenceManager.getDefaultSharedPreferences(this).getString(
                    PythonSettingsActivity.KEY_PYTHON_VERSION,
                    PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED
            );
        }
        if (!PythonSettingsActivity.PYTHON_VERSION_NOT_SELECTED.equals(pyVersion)
                && PackageManager.isPythonVersionInstalled(this, pyVersion)) {
            this.interpreter.startInterpreter(Util.getMainVersionPart(pyVersion), interpreterArgs);
            startedInterpreter = true;
        } else {
            showPythonVersionDialog();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (startedInterpreter) {
            bindInterpreter();
        }
    }
    
    @Override
    public void onBackPressed() {
        interpreter.interrupt();
        terminalView.disableLineInput();
    }

    @Override
    protected void onDestroy() {
        if (terminalWindowManager instanceof ActivityLifecycleEventListener) {
            ((ActivityLifecycleEventListener) terminalWindowManager).onDestroy();
        }
        interpreter.stopInterpreter();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        if (terminalWindowManager instanceof ActivityLifecycleEventListener) {
            ((ActivityLifecycleEventListener) terminalWindowManager).onLowMemory();
        }
        super.onLowMemory();
    }

    @Override
    protected void onPause() {
        if (terminalWindowManager instanceof ActivityLifecycleEventListener)
            ((ActivityLifecycleEventListener) terminalWindowManager).onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        this.interpreter.detach();
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (terminalWindowManager instanceof ActivityLifecycleEventListener)
            ((ActivityLifecycleEventListener) terminalWindowManager).onResume();
        super.onResume();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        PythonFragment currentWindow = terminalWindowManager.getCurrentWindow();
        if (currentWindow instanceof SDLWindowInterface) {
            Boolean result = ((SDLWindowInterface) currentWindow).dispatchKeyEvent(event);
            if (result != null) { return result; }
        }
        return super.dispatchKeyEvent(event);
    }
    
    private void bindInterpreter() {
        interpreter.attach();
        terminalView.setProgramHandler(new TerminalInterface.ProgramHandler() {
            @Override
            public void sendInput(String input) {
                interpreter.sendInput(input);
            }

            @Override
            public void terminate() {
                // TODO: Handle
            }

            @Override
            public void onTerminalSizeChanged(int newWidth, int newHeight,
                                              int pixelWidth, int pixelHeight) {
                if (interpreter instanceof InterpreterPseudoTerminalIOHandle) {
                    ((InterpreterPseudoTerminalIOHandle) interpreter).setTerminalSize(
                            newWidth, newHeight, pixelWidth, pixelHeight);
                }
            }

            @Override
            public void interrupt() {
                interpreter.interrupt();
            }
        });
    }

    private void showPythonVersionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppDialogTheme);
        final ArrayList<String> versions = PackageManager.getInstalledPythonVersions(getApplicationContext());
        if (versions.size() <= 1) {
            if (versions.size() == 1) {
                interpreter.startInterpreter(versions.get(0), interpreterArgs);
                startedInterpreter = true;
                return;
            }
            Log.i(MainActivity.TAG, "No Python version installed. Please download a version to use the interpreter.");
            Toast.makeText(
                    PythonInterpreterActivity.this,
                    "No Python version installed. Please download a version to use the interpreter.",
                    Toast.LENGTH_SHORT
            ).show(); // TODO: Open the download activity?
            finish();
            return;
        }
        String[] items = new String[versions.size()];
        for (int i = 0; i < versions.size(); i++) {
            items[i] = "Python " + versions.get(i);
        }
        builder.setSingleChoiceItems(items, 0, null);
        builder.setNegativeButton("Just once", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ListView listView = ((AlertDialog) dialog).getListView();
                interpreter.startInterpreter(
                        versions.get(listView.getCheckedItemPosition()), interpreterArgs);
                bindInterpreter();
            }
        });
        builder.setPositiveButton("Set as default", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ListView listView = ((AlertDialog) dialog).getListView();
                String version = versions.get(listView.getCheckedItemPosition());
                PreferenceManager.getDefaultSharedPreferences(PythonInterpreterActivity.this)
                        .edit().putString(PythonSettingsActivity.KEY_PYTHON_VERSION, version).apply();
                interpreter.startInterpreter(version, interpreterArgs);
                bindInterpreter();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setTitle("Choose a Python version");
        builder.show();
    }

    private void addTerminalWindow() {
        WindowManagerInterface.Window window = terminalWindowManager.createWindow(TerminalFragment.class);
        terminalWindowManager.setWindowName(window, "Python");
        terminalWindowManager.setWindowIcon(window, Util.getResourceDrawable(this, R.drawable.python_launcher_icon));
        terminalView = (TerminalInterface) window;
    }
}
