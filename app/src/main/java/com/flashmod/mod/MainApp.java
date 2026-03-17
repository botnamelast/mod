package com.flashmod.mod;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.TypedValue;

public class MainApp extends Activity {

    private static final int REQ_OVERLAY = 1001;

    // Racing-themed colors
    private static final int C_BG      = Color.parseColor("#0A0A0F");
    private static final int C_SURFACE = Color.parseColor("#13131A");
    private static final int C_BORDER  = Color.parseColor("#252535");
    private static final int C_ACCENT  = Color.parseColor("#FF3C3C");  // Racing red
    private static final int C_ACCENT2 = Color.parseColor("#FF8C00");  // Orange
    private static final int C_GREEN   = Color.parseColor("#00E676");
    private static final int C_TEXT    = Color.parseColor("#F0F0FF");
    private static final int C_MUTED   = Color.parseColor("#606080");

    private TextView tvModStatus;
    private TextView btnActivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(C_BG);
        if (getActionBar() != null) getActionBar().hide();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(C_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(40));

        // ── Header ────────────────────────────────────────────────────────────
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("⚡");
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        logo.setPadding(0, 0, dp(10), 0);
        titleRow.addView(logo);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("FlashMod");
        tvTitle.setTextColor(C_ACCENT);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Flash Racer Mini 4WD · Mod Menu");
        tvSub.setTextColor(C_MUTED);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleCol.addView(tvSub);

        titleRow.addView(titleCol);
        root.addView(titleRow);
        root.addView(vspace(24));

        // ── Status Card ───────────────────────────────────────────────────────
        root.addView(makeCard(card -> {
            card.addView(makeLabel("STATUS"));
            card.addView(vspace(10));
            card.addView(makeStatusRow("LSPosed Module",
                isModuleActive() ? "Aktif" : "Tidak Aktif",
                isModuleActive() ? C_GREEN : C_ACCENT));
            card.addView(vspace(8));
            card.addView(makeStatusRow("Izin Overlay",
                hasOverlayPermission() ? "Diberikan" : "Belum",
                hasOverlayPermission() ? C_GREEN : C_MUTED));
            card.addView(vspace(8));
            card.addView(makeStatusRow("Target", "Flash Racer Mini 4WD", C_ACCENT2));
        }));
        root.addView(vspace(14));

        // ── Mod status ────────────────────────────────────────────────────────
        root.addView(makeCard(card -> {
            card.addView(makeLabel("MOD"));
            card.addView(vspace(10));
            tvModStatus = new TextView(this);
            tvModStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            updateModStatus();
            card.addView(tvModStatus);

            if (!isModuleActive()) {
                card.addView(vspace(6));
                TextView hint = new TextView(this);
                hint.setText("1. Buka LSPosed Manager\n2. Pilih module FlashMod\n3. Centang Flash Racer\n4. Reboot atau Force Stop game");
                hint.setTextColor(C_MUTED);
                hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                hint.setLineSpacing(dp(2), 1f);
                card.addView(hint);
            }
        }));
        root.addView(vspace(14));

        // ── Tombol Aktifkan ───────────────────────────────────────────────────
        btnActivate = new TextView(this);
        btnActivate.setTextColor(Color.WHITE);
        btnActivate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnActivate.setTypeface(Typeface.DEFAULT_BOLD);
        btnActivate.setGravity(Gravity.CENTER);
        btnActivate.setPadding(0, dp(16), 0, dp(16));
        btnActivate.setBackground(makeRoundRect(C_ACCENT, dp(10)));
        btnActivate.setOnClickListener(v -> onActivateClick());
        updateActivateBtn();
        root.addView(btnActivate);
        root.addView(vspace(10));

        TextView btnStop = new TextView(this);
        btnStop.setText("◼  STOP MOD MENU");
        btnStop.setTextColor(C_MUTED);
        btnStop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnStop.setGravity(Gravity.CENTER);
        btnStop.setPadding(0, dp(14), 0, dp(14));
        btnStop.setBackground(makeRoundRect(C_SURFACE, dp(10)));
        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, ModOverlay.class));
            toast("Mod Menu dihentikan");
        });
        root.addView(btnStop);
        root.addView(vspace(24));

        // ── Info ──────────────────────────────────────────────────────────────
        root.addView(makeCard(card -> {
            card.addView(makeLabel("INFO"));
            card.addView(vspace(8));
            card.addView(makeInfoRow("Versi Mod", "1.0.0"));
            card.addView(vspace(6));
            card.addView(makeInfoRow("Target", "Flash Racer Mini 4WD"));
            card.addView(vspace(6));
            card.addView(makeInfoRow("Package", "com.track.mini.racer.legend2"));
            card.addView(vspace(6));
            card.addView(makeInfoRow("Framework", "LSPosed / Zygisk"));
        }));

        scroll.addView(root);
        setContentView(scroll);
    }

    private void onActivateClick() {
        if (!hasOverlayPermission()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        } else {
            startService(new Intent(this, ModOverlay.class));
            toast("Mod Menu diaktifkan — buka game sekarang!");
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            updateActivateBtn();
            if (hasOverlayPermission()) toast("Izin diberikan!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateModStatus();
        updateActivateBtn();
    }

    private void updateModStatus() {
        if (tvModStatus == null) return;
        boolean active = isModuleActive();
        tvModStatus.setText(active ? "⚡ Mod siap digunakan" : "⚠ Aktifkan module di LSPosed Manager");
        tvModStatus.setTextColor(active ? C_GREEN : C_ACCENT2);
    }

    private void updateActivateBtn() {
        if (btnActivate == null) return;
        btnActivate.setText(hasOverlayPermission() ? "▶  AKTIFKAN MOD MENU" : "⚙  MINTA IZIN OVERLAY");
    }

    private boolean isModuleActive() { return false; }
    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    // ── UI helpers ────────────────────────────────────────────────────────────
    interface CardBuilder { void build(LinearLayout card); }

    private LinearLayout makeCard(CardBuilder b) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeCardBg());
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        b.build(card);
        return card;
    }

    private LinearLayout makeStatusRow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvL = new TextView(this);
        tvL.setText(label); tvL.setTextColor(C_MUTED);
        tvL.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvL.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvL);
        TextView tvV = new TextView(this);
        tvV.setText(value); tvV.setTextColor(color);
        tvV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvV.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(tvV);
        return row;
    }

    private LinearLayout makeInfoRow(String l, String v) { return makeStatusRow(l, v, C_TEXT); }

    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private View vspace(int d) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(d)));
        return v;
    }

    private GradientDrawable makeCardBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(C_SURFACE); gd.setCornerRadius(dp(12));
        gd.setStroke(dp(1), C_BORDER); return gd;
    }

    private GradientDrawable makeRoundRect(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color); gd.setCornerRadius(radius); return gd;
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics());
    }
}
