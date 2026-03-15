package com.geminx.mod;

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

    private static final String TAG        = "GeminX";
    private static final String TARGET_PKG = "com.xd.SettlementSurvival.gp.global";
    private static final String MOD_PKG    = "com.geminx.mod";

    private static Class<?> clsCollectAttr  = null;
    private static Class<?> clsArchivePart2 = null;
    private static Class<?> clsCC           = null;
    private static Class<?> clsGameData     = null;
    private static Class<?> clsCoinMgr      = null;

    private static boolean hooksReady = false;
    private static final java.util.Queue<Runnable> pending = new java.util.LinkedList<>();

    // ── Entry point ───────────────────────────────────────────────────────────
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpp) {

        // Fix 2: Hook isModuleActive() di app modul sendiri → return true
        if (lpp.packageName.equals(MOD_PKG)) {
            hookModuleActive(lpp.classLoader);
            return;
        }

        if (!lpp.packageName.equals(TARGET_PKG)) return;
        Log.i(TAG, "Target loaded: " + TARGET_PKG);

        try {
            hookPairip(lpp.classLoader);
            hookInit(lpp.classLoader);
        } catch (Throwable t) {
            Log.e(TAG, "handleLoadPackage error", t);
        }
    }

    // ── Fix 2: Hook isModuleActive ────────────────────────────────────────────
    private void hookModuleActive(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.geminx.mod.MainApp", cl,
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

    // ── Pairip bypass ─────────────────────────────────────────────────────────
    private void hookPairip(ClassLoader cl) {

        // Hook 1: Runtime.loadLibrary0 — intercept saat System.loadLibrary dipanggil
        // Catatan: ini mungkin tidak menangkap libpairipcore.so karena dimuat via
        // native namespace, tapi tetap kita pasang sebagai lapisan pertama
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.Runtime", cl,
                "loadLibrary0",
                ClassLoader.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String libName = (String) param.args[1];
                        if (libName != null && libName.contains("pairipcore")) {
                            param.setResult(null);
                            Log.i(TAG, "Pairip loadLibrary0 BLOCKED: " + libName);
                        }
                    }
                }
            );
            Log.i(TAG, "loadLibrary0 hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "loadLibrary0 hook failed: " + t.getMessage());
        }

        // Hook 2: System.load (full path)
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.System", cl,
                "load", String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String path = (String) param.args[0];
                        if (path != null && path.contains("pairipcore")) {
                            param.setResult(null);
                            Log.i(TAG, "Pairip System.load BLOCKED: " + path);
                        }
                    }
                }
            );
            Log.i(TAG, "System.load hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "System.load hook failed: " + t.getMessage());
        }

        // Hook 3: com.pairip.application.Application
        // Block attachBaseContext agar Pairip tidak sempat init sama sekali
        try {
            XposedHelpers.findAndHookMethod(
                "com.pairip.application.Application", cl,
                "attachBaseContext", android.content.Context.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        Log.i(TAG, "Pairip attachBaseContext REPLACED (no-op)");
                        // Panggil super Application.attachBaseContext agar app tidak crash
                        try {
                            XposedHelpers.callMethod(param.thisObject,
                                "superAttachBaseContext", param.args[0]);
                        } catch (Throwable ignored) {
                            // fallback: skip saja
                        }
                        return null;
                    }
                }
            );
            Log.i(TAG, "Pairip attachBaseContext hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Pairip attachBaseContext hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.pairip.application.Application", cl,
                "onCreate",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        Log.i(TAG, "Pairip onCreate REPLACED (no-op)");
                        return null;
                    }
                }
            );
            Log.i(TAG, "Pairip onCreate hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Pairip onCreate hook failed: " + t.getMessage());
        }

        // Hook 4: Coba hook class-class Pairip lain yang mungkin ada
        // com.pairip.PackageVerifier atau sejenisnya
        String[] pairipClasses = {
            "com.pairip.PackageVerifier",
            "com.pairip.LicenseVerifier",
            "com.pairip.Verifier",
            "com.pairip.PairipCore"
        };
        for (String cls : pairipClasses) {
            try {
                Class<?> c = XposedHelpers.findClass(cls, cl);
                Log.i(TAG, "Found Pairip class: " + cls);
                // Kalau ketemu, coba hook semua method yang return boolean
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (m.getReturnType() == boolean.class) {
                        try {
                            XposedBridge.hookMethod(m, new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam param) {
                                    Log.i(TAG, "Pairip boolean method hooked: " + param.method.getName() + " → true");
                                    return true;
                                }
                            });
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {
                // Class tidak ada, lanjut
            }
        }

        Log.i(TAG, "All Pairip hooks installed");
    }

    // ── Game hooks ────────────────────────────────────────────────────────────
    private void hookInit(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "GameData", cl, "SetGameSpeed", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!hooksReady) {
                            initClasses(cl);
                            startOverlay(param.thisObject);
                        }
                    }
                }
            );
            Log.i(TAG, "GameData.SetGameSpeed hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "hookInit failed: " + t.getMessage());
        }
    }

    private void initClasses(ClassLoader cl) {
        try {
            clsCollectAttr  = XposedHelpers.findClass("CollectAttr",  cl);
            clsArchivePart2 = XposedHelpers.findClass("ArchivePart2", cl);
            clsCC           = XposedHelpers.findClass("CC",           cl);
            clsGameData     = XposedHelpers.findClass("GameData",     cl);
            clsCoinMgr      = XposedHelpers.findClass("CoinMgr",      cl);

            hooksReady = true;
            Log.i(TAG, "All classes loaded!");

            while (!pending.isEmpty()) pending.poll().run();

        } catch (Throwable t) {
            Log.e(TAG, "initClasses failed", t);
        }
    }

    private void startOverlay(Object ctx) {
        try {
            Context context = (Context) ctx;
            Intent i = new Intent(context, ModOverlay.class);
            context.startService(i);
            Log.i(TAG, "Overlay service started");
        } catch (Throwable t) {
            Log.e(TAG, "startOverlay failed", t);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static void collectItem(int id, int num) {
        if (!hooksReady) { pending.add(() -> collectItem(id, num)); return; }
        try {
            XposedHelpers.callStaticMethod(clsCollectAttr, "collectItem", id, num);
            Log.d(TAG, "collectItem OK id=" + id + " num=" + num);
        } catch (Throwable t) {
            try {
                XposedHelpers.callStaticMethod(clsCC, "addItemC3", id, num);
                Log.d(TAG, "addItemC3 fallback OK id=" + id);
            } catch (Throwable t2) {
                Log.e(TAG, "collectItem failed id=" + id, t2);
            }
        }
    }

    public static void collectItems(int[][] items) {
        for (int[] item : items) {
            if (item[1] > 0) collectItem(item[0], item[1]);
        }
    }

    public static void addCoin(int amount) {
        if (!hooksReady) { pending.add(() -> addCoin(amount)); return; }
        try {
            XposedHelpers.callStaticMethod(clsCoinMgr, "AddCoinForce", amount);
            Log.d(TAG, "AddCoinForce OK amount=" + amount);
        } catch (Throwable t) {
            Log.e(TAG, "addCoin failed", t);
        }
    }

    public static int getCoin() {
        if (!hooksReady || clsCoinMgr == null) return -1;
        try {
            return (int) XposedHelpers.callStaticMethod(clsCoinMgr, "GetAllCoin");
        } catch (Throwable t) { return -1; }
    }

    public static void setGameSpeed(int speed) {
        if (!hooksReady) { pending.add(() -> setGameSpeed(speed)); return; }
        try {
            if (speed <= 5) {
                XposedHelpers.callStaticMethod(clsGameData, "SetGameSpeed", speed);
            } else {
                Class<?> clsTime = XposedHelpers.findClass("UnityEngine.Time", null);
                XposedHelpers.callStaticMethod(clsTime, "set_timeScale", (float) speed);
            }
            Log.d(TAG, "setGameSpeed OK speed=" + speed);
        } catch (Throwable t) {
            Log.e(TAG, "setGameSpeed failed", t);
        }
    }

    public static void setGamePause(boolean pause) {
        if (!hooksReady) return;
        try {
            XposedHelpers.callStaticMethod(clsGameData, "SetGamePause", pause);
        } catch (Throwable t) {
            Log.e(TAG, "setGamePause failed", t);
        }
    }
}
