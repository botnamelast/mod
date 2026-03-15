package com.geminx.mod;

import android.util.Log;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Method;

public class ZygoteInit implements IXposedHookZygoteInit {

    private static final String TAG = "GeminX";

    @Override
    public void initZygote(StartupParam startupParam) {
        Log.i(TAG, "initZygote called");

        // Load native bypass — ini install dlopen hook via __attribute__((constructor))
        // yang otomatis jalan saat .so di-load, sebelum Pairip sempat dimuat
        try {
            NativeBypass.install();
            Log.i(TAG, "NativeBypass.install() called, loaded=" + NativeBypass.isLoaded());
        } catch (Throwable t) {
            Log.e(TAG, "NativeBypass failed: " + t.getMessage());
        }

        // Tetap pasang nativeLoad hook sebagai lapisan kedua
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
                        if (filename != null && filename.contains("pairipcore")) {
                            Log.i(TAG, "nativeLoad BLOCKED: " + filename);
                            param.setResult(null);
                        }
                    }
                });
                Log.i(TAG, "nativeLoad hook installed (backup layer)");
            }
        } catch (Throwable t) {
            Log.e(TAG, "nativeLoad hook failed: " + t.getMessage());
        }
    }
}
