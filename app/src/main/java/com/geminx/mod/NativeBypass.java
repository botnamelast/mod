package com.geminx.mod;

import android.util.Log;

public class NativeBypass {

    private static final String TAG = "GeminX";
    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("geminx_native");
            loaded = true;
            Log.i(TAG, "geminx_native loaded successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load geminx_native: " + t.getMessage());
        }
    }

    /**
     * Install native bypass untuk block libpairipcore.so
     * Dipanggil dari ZygoteInit sebelum game process dimulai
     */
    public static native void install();

    public static boolean isLoaded() {
        return loaded;
    }
}
