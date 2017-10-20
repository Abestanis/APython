package com.apython.python.pythonhost.interpreter;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;

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
    }

    @org.junit.Test
    public void runPythonTestSuite() throws Exception {
        File pyLibDir = tempDir.newFolder("pyLib");
        File pythonLibZip = new File(PackageManager.getStandardLibPath(context), "python34.zip");
        Util.extractArchive(pythonLibZip, pyLibDir, null);
        String pyString = "import sys\n" +
                "sys.path.insert(0, '" + pyLibDir.getAbsolutePath() + "')\n" +
                "import test.__main__\n";
        PythonInterpreter interpreter = new PythonInterpreter(context, "3.4");
        assertEquals("Python test suite returned with non zero exit status",
                     0, interpreter.runPythonString(pyString, new String[]{"-v"}));
    }
}
