package com.apython.python.pythonhost.interpreter;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.apython.python.pythonhost.PackageManager;
import com.apython.python.pythonhost.Util;

//import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by Sebastian on 24.04.2017.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class PythonInterpreterTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    Context context;
    PythonInterpreter interpreter;
    
    @org.junit.Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        interpreter = new PythonInterpreter(context, "3.4");
    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void getPythonVersion() throws Exception {
        assertEquals("Did not get the expected Python version",
                     "3.4.3", interpreter.getPythonVersion());
    }

//    @org.junit.Test
//    public void setIoHandler() throws Exception {
//        final StringBuilder output = new StringBuilder();
//        final StringBuilder prompt = new StringBuilder();
//        interpreter.setIoHandler(new PythonInterpreter.IOHandler() {
//            @Override
//            public void addOutput(String text) {
//                output.append(text);
//                if (output.toString().contains("for more information.")) {
//                    synchronized (output) {
//                        output.notifyAll();
//                    }
//                }
//            }
//
//            @Override
//            public void setupInput(String text) {
//                prompt.append(text);
//                synchronized (prompt) {
//                    prompt.notifyAll();
//                }
//            }
//        });
//        Thread interpreterThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                interpreter.runPythonInterpreter(null);
//            }
//        });
//        interpreterThread.start();
//        synchronized (output) {
//            output.wait(10000);
//        }
//        assertThat(output.toString(), Matchers.containsString("Type \"help\", \"copyright\", \"credits\" or \"license\" for more information."));
//        synchronized (prompt) {
//            prompt.wait(2000);
//        }
//        assertEquals("The IO-Handler did not get the expected prompt", ">>> ", prompt.toString());
//        interpreter.stopInterpreter();
//        synchronized (prompt) {
//            prompt.wait(1000);
//        }
//        assertFalse(interpreterThread.isAlive());
//        interpreter.setIoHandler(null);
//    }

    @org.junit.Test
    public void setLogTag() throws Exception {

    }

    @org.junit.Test
    public void runPythonInterpreter() throws Exception {

    }

    @org.junit.Test
    public void runPythonFile() throws Exception {

    }

    @org.junit.Test
    public void runPythonFile1() throws Exception {

    }

    @org.junit.Test
    public void runPythonModule() throws Exception {

    }

    @org.junit.Test
    public void runPythonString() throws Exception {

    }

    @org.junit.Test
    public void isRunning() throws Exception {

    }

    @org.junit.Test
    public void terminate() throws Exception {
        final int[] pyResult = new int[1];
        Thread pythonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pyResult[0] = interpreter.runPythonInterpreter(null);
            }
        });
//        interpreter.stopInterpreter();
        pythonThread.start();
        synchronized (pyResult) {
            pyResult.wait(1000);
        }
//        interpreter.stopInterpreter();
        pythonThread.join(2000);
        assertFalse(pythonThread.isAlive());
        assertEquals("Python interpreter did not return the expected return code when terminated.",
                     pyResult[0], 127);
//        interpreter.stopInterpreter();
    }

//    @org.junit.Test
//    public void interrupt() throws Exception {
//        final int[] pyResult = new int[1];
//        final StringBuilder output = new StringBuilder();
//        final Object interactive = new Object();
//        interpreter.setIoHandler(new PythonInterpreter.IOHandler() {
//            @Override
//            public void addOutput(String text) {
//                output.append(text);
//                if (output.toString().contains("start")
//                        || output.toString().contains("KeyboardInterrupt")) {
//                    synchronized (output) {
//                        output.notifyAll();
//                    }
//                }
//            }
//
//            @Override
//            public void setupInput(String prompt) {
//                synchronized (interactive) {
//                    interactive.notifyAll();
//                }
//            }
//        });
//        Thread pythonThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                pyResult[0] = interpreter.runPythonString(
//                        "print('start')from time import sleep;sleep(10)", new String[]{"-i"});
//            }
//        });
//        pythonThread.start();
//        synchronized (output) {
//            output.wait(10000);
//        }
////        interpreter.interrupt();
//        pythonThread.start();
//        synchronized (output) {
//            output.wait(1000);
//        }
//        assertThat("Python did not throw a KeyboardInterrupt when interrupted",
//                   output.toString(), Matchers.containsString(
//                "Traceback (most recent call last):\n" +
//                "  File \"<string>\", line 1, in <module>\n" +
//                "KeyboardInterrupt"));
//        synchronized (interactive) {
//            interactive.wait(1000);
//        }
////        interpreter.interrupt();
//        pythonThread.join(2000);
//        assertFalse("Python did not exit when interrupting on interactive input",
//                    pythonThread.isAlive());
//        assertEquals("Python did not return the expected return code when interrupted",
//                     pyResult[0], 0);
//    }

    @org.junit.Test
    public void dispatchKey() throws Exception {

    }

    @org.junit.Test
    public void sendStringToStdin() throws Exception {

    }

    @org.junit.Test
    public void getEnqueueInput() throws Exception {

    }
}