package com.geminx.mod;

import android.util.Log;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Method;

public class ZygoteInit implements IXposedHookZygoteInit {

    private static final String TAG        = "GeminX";
    private static final String TARGET_PKG = "com.xd.SettlementSurvival.gp.global";

    @Override
    public void initZygote(StartupParam startupParam) {
        Log.i(TAG, "initZygote called — hooking native load early");

        // Hook Runtime.nativeLoad — ini dipanggil untuk SEMUA library loading
        // termasuk yang via native namespace (clns-9), jauh lebih dalam dari loadLibrary0
        try {
            // nativeLoad adalah native method di java.lang.Runtime
            // signature: private static native String nativeLoad(
            //     String filename, ClassLoader loader, Class<?> caller)
            Method nativeLoad = null;
            for (Method m : Runtime.class.getDeclaredMethods()) {
                if (m.getName().equals("nativeLoad")) {
                    nativeLoad = m;
                    break;
                }
            }

            if (nativeLoad != null) {
                nativeLoad.setAccessible(true);
                XposedBridge.hookMethod(nativeLoad, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String filename = (String) param.args[0];
                        if (filename != null && filename.contains("pairipcore")) {
                            Log.i(TAG, "nativeLoad BLOCKED: " + filename);
                            // Return null = sukses tanpa error (nativeLoad return String error msg)
                            param.setResult(null);
                        }
                    }
                });
                Log.i(TAG, "Runtime.nativeLoad hook installed");
            } else {
                Log.e(TAG, "Runtime.nativeLoad not found!");
            }

        } catch (Throwable t) {
            Log.e(TAG, "nativeLoad hook failed: " + t.getMessage());
        }

        // Hook tambahan: android.os.SystemProperties atau cara lain Pairip
        // cek environment — block akses ke /proc/self/maps via syscall jika bisa
        hookDlopenPath();
    }

    private void hookDlopenPath() {
        // Hook System.loadLibrary sebagai fallback
        try {
            XposedHelpers.findAndHookMethod(
                System.class, "loadLibrary",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String lib = (String) param.args[0];
                        if (lib != null && lib.contains("pairipcore")) {
                            param.setResult(null);
                            Log.i(TAG, "System.loadLibrary BLOCKED: " + lib);
                        }
                    }
                }
            );
            Log.i(TAG, "System.loadLibrary hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "System.loadLibrary hook failed: " + t.getMessage());
        }

        // Hook Runtime.loadLibrary0
        try {
            XposedHelpers.findAndHookMethod(
                Runtime.class, "loadLibrary0",
                ClassLoader.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String lib = (String) param.args[1];
                        if (lib != null && lib.contains("pairipcore")) {
                            param.setResult(null);
                            Log.i(TAG, "Runtime.loadLibrary0 BLOCKED: " + lib);
                        }
                    }
                }
            );
            Log.i(TAG, "Runtime.loadLibrary0 hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Runtime.loadLibrary0 hook failed: " + t.getMessage());
        }
    }
}
