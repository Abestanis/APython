package launcher;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.apython.python.pythonhost.MainActivity;
import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.TestUtil;
import com.apython.python.pythonhost.Util;
import com.apython.python.pythonhost.interpreter.PythonInterpreterActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test that allows to launch the python interpreter within the development environment.
 * 
 * Created by Sebastian on 02.10.2017.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class APythonDev {
    
    class PythonInterpreterActivityTestRule extends ActivityTestRule<PythonInterpreterActivity>{
        private final Object activityRunningLock = new Object();
        
        PythonInterpreterActivityTestRule(boolean initialTouchMode, boolean launchActivity) {
            super(PythonInterpreterActivity.class, initialTouchMode, launchActivity);
        }
        
        @Override
        protected void afterActivityFinished() {
            synchronized (this.activityRunningLock) {
                this.activityRunningLock.notifyAll();
            }
            super.afterActivityFinished();
        }
        
        void waitForActivity() throws InterruptedException {
            synchronized (this.activityRunningLock) {
                while (!getActivity().isFinishing()) {
                    activityRunningLock.wait(1000);
                }
            }
        }
    }
    
    private final String PYTHON_VERSION = "3.4";

    @Rule
    public PythonInterpreterActivityTestRule activityRule =
            new PythonInterpreterActivityTestRule(true, false);
    
    @Before
    public void setupInterpreter() {
        Context context = InstrumentationRegistry.getTargetContext();
        assertTrue(TestUtil.copyNativePythonLibraries(context));
        assertTrue(TestUtil.installPythonLibraries(context, PYTHON_VERSION));
        assertTrue(TestUtil.installLibraryData(context));
        assertTrue(PackageManager.ensurePythonInstallation(context, PYTHON_VERSION, null));
    }

    /**
     * Run the python interpreter.
     */
    @org.junit.Test
    public void run() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("pythonVersion", PYTHON_VERSION);
        activityRule.launchActivity(intent);
        activityRule.waitForActivity();
    }
}
