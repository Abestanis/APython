package com.apython.python.pythonhost;

/*
 * Test class to test the python library.
 *
 * Created by Sebastian on 08.06.2015.
 */

public class PythonExecute {

    static  {
        System.loadLibrary("python2.7.2");
        System.loadLibrary("pythonLog");
        System.loadLibrary("application");
    }

    public void start() {
        nativeTest();
    }

    public native void nativeTest();

}
