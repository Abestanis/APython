package com.apython.python.pythonhost.interpreter;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.TestUtil;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.interpreter.handles.PythonInterpreterProcessHandle;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sebastian on 25.04.2017.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PythonTestModule {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    private Context context;

    @org.junit.Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        assertTrue(TestUtil.copyNativePythonLibraries(context));
        assertTrue(TestUtil.installPythonLibraries(context, "3.4"));
        assertTrue(TestUtil.installLibraryData(context));
        assertTrue(PackageManager.ensurePythonInstallation(context, "3.4", null));
    }

    @org.junit.Test
    public void runPythonTestSuite() throws Exception {
        File pyLibDir = tempDir.newFolder("pyLib");
        File pythonLibZip = new File(PackageManager.getStandardLibPath(context), "python34.zip");
        Util.extractArchive(pythonLibZip, pyLibDir, null);
        String pyString = "import sys\n" +
                "sys.path.insert(0, '" + pyLibDir.getAbsolutePath() + "')\n" +
                "import test.__main__\n";
        Looper.prepare();
        PythonInterpreterProcessHandle handle = new PythonInterpreterProcessHandle(context);
        handle.startInterpreter("3.4", new String[] {"-c", pyString, "-v"});
        handle.attach();
        assertEquals("Python test suite returned with non zero exit status",
                     0, (long) handle.getInterpreterResult(true));
        handle.detach();
    }
}
