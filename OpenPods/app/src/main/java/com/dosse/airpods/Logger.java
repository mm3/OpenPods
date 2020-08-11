package com.dosse.airpods;

import android.util.Log;

public class Logger {
    //Log is only displayed if this is a debug build, not release
    private static final boolean ENABLE_LOGGING = BuildConfig.DEBUG;
    public static final String TAG = "AirPods";

    public static void debug(String msg) {
        if(ENABLE_LOGGING) {
            Log.d(TAG, msg);
        }
    }

    public static void error(Throwable t) {
        if(ENABLE_LOGGING) {
            Log.e(TAG, "ERROR", t);
        }
    }
}
