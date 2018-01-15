package ru.bscmsk.fingerly.utils;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by izashkalov on 12.01.2018.
 */

/**
 * Application logger. This logger adds a prefix("FINGERLY_TAG_LIB : ") to all entries in the log
 */
public class AppLogger {
    private static final String LIB_PREFIX = "FINGERLY_TAG_LIB : ";

    private AppLogger() {
    }

    public static void e(@NonNull Object object, String msg) {
        Log.e(LIB_PREFIX + object.getClass().getCanonicalName(), msg);
    }

    public static void e(@NonNull Object object, String msg, Throwable e) {
        Log.e(LIB_PREFIX + object.getClass().getCanonicalName(), msg, e);
    }

    public static void i(Object object, String msg) {
        Log.i(LIB_PREFIX + object.getClass().getCanonicalName(), msg);
    }

    public static void i(Object object, String msg, Throwable e) {
        Log.i(LIB_PREFIX + object.getClass().getCanonicalName(), msg, e);
    }

    public static void d(@NonNull Object object, String msg) {
        Log.d(LIB_PREFIX + object.getClass().getCanonicalName(), msg);
    }

    public static void d(@NonNull Object object, String msg, Throwable e) {
        Log.d(LIB_PREFIX + object.getClass().getCanonicalName(), msg, e);
    }
}
