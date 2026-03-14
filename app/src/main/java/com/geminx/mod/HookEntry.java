package com.geminx.mod;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG        = "GeminX";
    private static final String TARGET_PKG = "com.xd.SettlementSurvival.gp.global"; // sesuaikan package game

    // ── Class names dari script.json ──────────────────────────────────────────
    // CollectAttr$$collectItem      → RVA 0xD22238
    // ArchivePart2$$collectItem1010 → RVA 0xD222DC
    // CC$$addItemC3                 → RVA 0xD252AC
    // GameData$$SetGameSpeed        → RVA 0xE59CC0
    // CoinMgr$$AddCoin              → RVA 0xDAB6A0
    // CoinMgr$$AddCoinForce         → RVA 0xDAB790

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
        if (!lpp.packageName.equals(TARGET_PKG)) return;
        Log.i(TAG, "Target loaded: " + TARGET_PKG);

        try {
            hookInit(lpp.classLoader);
        } catch (Throwable t) {
            Log.e(TAG, "handleLoadPackage error", t);
        }
    }

    private void hookInit(ClassLoader cl) {
        // Hook GameData.SetGameSpeed sebagai trigger game loaded
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
        } catch (Throwable t) {
            Log.e(TAG, "startOverlay failed", t);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC API — dipanggil dari ModOverlay
    // ═════════════════════════════════════════════════════════════════════════

    /** Tambah item. Coba collectItem dulu, fallback ke addItemC3 */
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

    /** Tambah item batch (untuk apply all items sekaligus) */
    public static void collectItems(int[][] items) {
        for (int[] item : items) {
            if (item[1] > 0) collectItem(item[0], item[1]);
        }
    }

    /** Tambah koin */
    public static void addCoin(int amount) {
        if (!hooksReady) { pending.add(() -> addCoin(amount)); return; }
        try {
            XposedHelpers.callStaticMethod(clsCoinMgr, "AddCoinForce", amount);
            Log.d(TAG, "AddCoinForce OK amount=" + amount);
        } catch (Throwable t) {
            Log.e(TAG, "addCoin failed", t);
        }
    }

    /** Baca koin saat ini */
    public static int getCoin() {
        if (!hooksReady || clsCoinMgr == null) return -1;
        try {
            return (int) XposedHelpers.callStaticMethod(clsCoinMgr, "GetAllCoin");
        } catch (Throwable t) { return -1; }
    }

    /** Set game speed
     *  speed 1-5 → pakai GameData.SetGameSpeed (native)
     *  speed 6-10 → bypass limit via UnityEngine.Time.timeScale */
    public static void setGameSpeed(int speed) {
        if (!hooksReady) { pending.add(() -> setGameSpeed(speed)); return; }
        try {
            if (speed <= 5) {
                XposedHelpers.callStaticMethod(clsGameData, "SetGameSpeed", speed);
            } else {
                // Bypass game limit — set timeScale langsung
                Class<?> clsTime = XposedHelpers.findClass("UnityEngine.Time", null);
                XposedHelpers.callStaticMethod(clsTime, "set_timeScale", (float) speed);
            }
            Log.d(TAG, "setGameSpeed OK speed=" + speed);
        } catch (Throwable t) {
            Log.e(TAG, "setGameSpeed failed", t);
        }
    }

    /** Pause / unpause game */
    public static void setGamePause(boolean pause) {
        if (!hooksReady) return;
        try {
            XposedHelpers.callStaticMethod(clsGameData, "SetGamePause", pause);
        } catch (Throwable t) {
            Log.e(TAG, "setGamePause failed", t);
        }
    }
}
