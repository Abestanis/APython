package launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.preference.PreferenceManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.apython.python.pythonhost.PackageManager
import com.apython.python.pythonhost.PythonSettingsActivity
import com.apython.python.pythonhost.TestUtil
import com.apython.python.pythonhost.interpreter.PythonInterpreterActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val PYTHON_VERSION = "3.4"

/**
 * Test that allows to launch the python interpreter within the development environment.
 *
 * Created by Sebastian on 02.10.2017.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class APythonDev {

    @Before
    fun setupInterpreter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        Assert.assertTrue(TestUtil.copyNativePythonLibraries(context))
        Assert.assertTrue(TestUtil.installPythonLibraries(
            instrumentationContext, context, PYTHON_VERSION))
        Assert.assertTrue(TestUtil.installLibraryData(instrumentationContext, context))
        Assert.assertTrue(PackageManager.ensurePythonInstallation(context, PYTHON_VERSION, null))
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
            PythonSettingsActivity.KEY_PYTHON_DOWNLOAD_URL, "http://10.0.2.2:8000"
        ).apply()
    }

    /**
     * Run the Python interpreter.
     */
    @Test
    @Throws(Exception::class)
    fun run() {
        val intent = Intent.makeMainActivity(
            ComponentName(
                InstrumentationRegistry.getInstrumentation().targetContext,
                PythonInterpreterActivity::class.java
            )
        )
        intent.putExtra("pythonVersion", PYTHON_VERSION)
        val lock = ReentrantLock()
        val finishCondition = lock.newCondition()
        val pythonScenario = ActivityScenario.launch<PythonInterpreterActivity>(intent)
        ActivityLifecycleMonitorRegistry.getInstance()
            .addLifecycleCallback { activity: Activity, stage: Stage ->
                if (activity.intent == intent && stage == Stage.DESTROYED) {
                    lock.withLock {
                        finishCondition.signal()
                    }
                }
            }
        do {
            lock.withLock {
                finishCondition.await(1, TimeUnit.SECONDS)
            }
        } while (pythonScenario.state.isAtLeast(Lifecycle.State.INITIALIZED))
        Assert.assertEquals(pythonScenario.result.resultCode, 0)
    }
}