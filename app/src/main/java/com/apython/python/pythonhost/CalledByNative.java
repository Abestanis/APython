package com.apython.python.pythonhost;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that a function is called from native code and therefore not unused.
 * 
 * Created by Sebastian on 21.10.2017.
 */
@Target(ElementType.METHOD)
public @interface CalledByNative {}
