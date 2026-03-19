package com.flashmod.mod;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG        = "FlashMod";
    private static final String TARGET_PKG = "com.track.mini.racer.legend2";
    private static final String MOD_PKG    = "com.flashmod.mod";

    // State
    public static boolean moduleActive    = false;
    public static boolean speedHackActive = false;
    public static boolean maxSpeedActive  = false;
    public static boolean timeScaleActive = false;

    public static float   timeScaleValue  = 2.0f;  // default 2x speed
    public static float   maxSpeedValue   = 999f;  // default max speed
    public static float   carMaxSpeed     = 200f;  // Param_CarMaxSpeed

    private static boolean hooksReady = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {

        // Hook isModuleActive di app modul sendiri
        if (lpp.packageName.equals(MOD_PKG)) {
            hookModuleActive(lpp.classLoader);
            return;
        }

        if (!lpp.packageName.equals(TARGET_PKG)) return;
        Log.i(TAG, "Target loaded: " + TARGET_PKG);

        try {
            hookRaceManager(lpp.classLoader);
            hookCarSpeedChange(lpp.classLoader);
            hookGlobalTimeScale(lpp.classLoader);
            hookMiniCarValue(lpp.classLoader);
            hookApplicationEntry(lpp.classLoader);
        } catch (Throwable t) {
            Log.e(TAG, "handleLoadPackage error", t);
        }
    }

    // ── Fix isModuleActive ────────────────────────────────────────────────────
    private void hookModuleActive(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.flashmod.mod.MainApp", cl,
                "isModuleActive",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return true;
                    }
                }
            );
            Log.i(TAG, "isModuleActive hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "isModuleActive hook failed: " + t.getMessage());
        }
    }

    // ── Hook RaceManager.SetTimeScale ─────────────────────────────────────────
    private void hookRaceManager(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "RaceManager", cl,
                "SetTimeScale", float.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (timeScaleActive) {
                            param.args[0] = timeScaleValue;
                            Log.d(TAG, "RaceManager.SetTimeScale overridden: " + timeScaleValue);
                        }
                    }
                }
            );
            Log.i(TAG, "RaceManager.SetTimeScale hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "RaceManager.SetTimeScale hook failed: " + t.getMessage());
        }

        // Hook startOverlay trigger — cari entry point game loaded
        try {
            XposedHelpers.findAndHookMethod(
                "RaceManager", cl,
                "SetTimeScale", float.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!hooksReady) {
                            hooksReady = true;
                            moduleActive = true;
                            Log.i(TAG, "Game loaded! Starting overlay...");
                            startOverlay((Context) param.thisObject);
                        }
                    }
                }
            );
        } catch (Throwable ignored) {}
    }

    // ── Hook CarSpeedChange ───────────────────────────────────────────────────
    private void hookCarSpeedChange(ClassLoader cl) {
        // Hook SetSpeedAndTorQue — dipanggil saat race start/update
        try {
            XposedHelpers.findAndHookMethod(
                "CarSpeedChange", cl,
                "SetSpeedAndTorQue",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (speedHackActive) {
                            // Set Param_CarMaxSpeed langsung via reflection
                            try {
                                XposedHelpers.setFloatField(param.thisObject, "Param_CarMaxSpeed", carMaxSpeed);
                                Log.d(TAG, "Param_CarMaxSpeed set to: " + carMaxSpeed);
                            } catch (Throwable t) {
                                Log.e(TAG, "Set Param_CarMaxSpeed failed: " + t.getMessage());
                            }
                        }
                    }
                }
            );
            Log.i(TAG, "CarSpeedChange.SetSpeedAndTorQue hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "CarSpeedChange hook failed: " + t.getMessage());
        }

        // Hook PlayActionSetSpeed
        try {
            XposedHelpers.findAndHookMethod(
                "CarSpeedChange", cl,
                "PlayActionSetSpeed", float.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (speedHackActive) {
                            param.args[0] = carMaxSpeed / 100f;
                            Log.d(TAG, "PlayActionSetSpeed overridden");
                        }
                    }
                }
            );
            Log.i(TAG, "CarSpeedChange.PlayActionSetSpeed hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "PlayActionSetSpeed hook failed: " + t.getMessage());
        }
    }

    // ── Hook GlobalTimeScale ──────────────────────────────────────────────────
    private void hookGlobalTimeScale(ClassLoader cl) {
        // Hook SetTimeScale (Fix64 version)
        try {
            // Fix64 adalah struct custom, tapi dipassing sebagai long di IL2CPP
            // Coba hook dengan berbagai signature
            Class<?> fix64Class = null;
            try {
                fix64Class = XposedHelpers.findClass("Fix64", cl);
            } catch (Throwable ignored) {}

            if (fix64Class != null) {
                XposedHelpers.findAndHookMethod(
                    "GlobalTimeScale", cl,
                    "SetTimeScale", fix64Class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (timeScaleActive) {
                                Log.d(TAG, "GlobalTimeScale.SetTimeScale intercepted");
                                // Note: Fix64 override complex, log saja dulu
                            }
                        }
                    }
                );
                Log.i(TAG, "GlobalTimeScale.SetTimeScale hook installed");
            }
        } catch (Throwable t) {
            Log.e(TAG, "GlobalTimeScale hook failed: " + t.getMessage());
        }

        // Hook GetTimeScale — return nilai yang kita mau
        try {
            XposedHelpers.findAndHookMethod(
                "GlobalTimeScale", cl,
                "GetTimeScale",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (timeScaleActive) {
                            // Return timeScale yang dimodifikasi
                            Log.d(TAG, "GlobalTimeScale.GetTimeScale intercepted");
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Log.e(TAG, "GlobalTimeScale.GetTimeScale hook failed: " + t.getMessage());
        }
    }

    // ── Hook MiniCarValue ─────────────────────────────────────────────────────
    private void hookMiniCarValue(ClassLoader cl) {
        // Hook get_MaxSpeed — return max speed yang tinggi
        try {
            XposedHelpers.findAndHookMethod(
                "MiniCarValue", cl,
                "get_MaxSpeed",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (maxSpeedActive) {
                            // Fix64 biasanya disimpan sebagai long (raw bits)
                            // Konversi float ke Fix64 format (multiply by 2^16 = 65536)
                            long fix64Val = (long)(maxSpeedValue * 65536.0);
                            param.setResult(fix64Val);
                            Log.d(TAG, "get_MaxSpeed overridden: " + maxSpeedValue);
                        }
                    }
                }
            );
            Log.i(TAG, "MiniCarValue.get_MaxSpeed hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "MiniCarValue.get_MaxSpeed hook failed: " + t.getMessage());
        }

        // Hook CurveMaxSpeed
        try {
            XposedHelpers.findAndHookMethod(
                "MiniCarValue", cl,
                "CurveMaxSpeed",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (maxSpeedActive) {
                            long fix64Val = (long)(maxSpeedValue * 65536.0);
                            param.setResult(fix64Val);
                            Log.d(TAG, "CurveMaxSpeed overridden");
                        }
                    }
                }
            );
            Log.i(TAG, "MiniCarValue.CurveMaxSpeed hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "CurveMaxSpeed hook failed: " + t.getMessage());
        }
    }

    // ── Hook Application entry untuk start overlay lebih awal ─────────────────
    private void hookApplicationEntry(ClassLoader cl) {
        // Hook Application.onCreate sebagai trigger paling awal
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application", cl,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!hooksReady) {
                            hooksReady = true;
                            moduleActive = true;
                            Log.i(TAG, "Application.onCreate — starting overlay");
                            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                            h.postDelayed(() -> {
                                try {
                                    android.content.Context ctx = (android.content.Context) param.thisObject;
                                    android.content.Intent i = new android.content.Intent();
                                    i.setClassName("com.flashmod.mod", "com.flashmod.mod.ModOverlay");
                                    ctx.startService(i);
                                    Log.i(TAG, "Overlay service started via Application");
                                } catch (Throwable t) {
                                    Log.e(TAG, "startOverlay via Application failed: " + t.getMessage());
                                }
                            }, 3000); // delay 3 detik biar game selesai load dulu
                        }
                    }
                }
            );
            Log.i(TAG, "Application.onCreate hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Application.onCreate hook failed: " + t.getMessage());
        }
    }

    // ── Start Overlay ─────────────────────────────────────────────────────────
    private void startOverlay(Context ctx) {
        try {
            android.content.Intent i = new android.content.Intent();
            i.setClassName("com.flashmod.mod", "com.flashmod.mod.ModOverlay");
            ctx.startService(i);
            Log.i(TAG, "Overlay service started");
        } catch (Throwable t) {
            Log.e(TAG, "startOverlay failed: " + t.getMessage());
        }
    }
}
