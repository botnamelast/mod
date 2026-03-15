package com.geminx.mod;

import android.util.Log;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Method;

public class ZygoteInit implements IXposedHookZygoteInit {

    private static final String TAG        = "GeminX";
    private static final String TARGET_PKG = "com.xd.SettlementSurvival.gp.global";

    @Override
    public void initZygote(StartupParam startupParam) {
        Log.i(TAG, "initZygote called — hooking nativeLoad early");

        // PENTING: initZygote berjalan di Zygote process yang di-share semua app.
        // JANGAN hook System.loadLibrary atau loadLibrary0 di sini karena akan
        // mempengaruhi semua app dan bisa crash system!
        // Satu-satunya yang aman adalah hook Runtime.nativeLoad dengan filter KETAT.

        try {
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
                        // Filter SANGAT KETAT — hanya block pairipcore di package target
                        if (filename != null
                                && filename.contains("pairipcore")
                                && filename.contains(TARGET_PKG.replace(".", "/"))) {
                            Log.i(TAG, "nativeLoad BLOCKED: " + filename);
                            // nativeLoad return String: null = sukses, string = error msg
                            param.setResult(null);
                        }
                    }
                });
                Log.i(TAG, "Runtime.nativeLoad hook installed (strict filter)");
            } else {
                Log.e(TAG, "Runtime.nativeLoad not found!");
            }

        } catch (Throwable t) {
            Log.e(TAG, "nativeLoad hook failed: " + t.getMessage());
        }
    }
}
