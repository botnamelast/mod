package com.geminx.mod;

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

    private static final int C_BG      = Color.parseColor("#0D0D0F");
    private static final int C_SURFACE = Color.parseColor("#16161A");
    private static final int C_BORDER  = Color.parseColor("#2A2A35");
    private static final int C_ACCENT  = Color.parseColor("#F59E0B");
    private static final int C_RED     = Color.parseColor("#EF4444");
    private static final int C_GREEN   = Color.parseColor("#10B981");
    private static final int C_TEXT    = Color.parseColor("#F1F5F9");
    private static final int C_MUTED   = Color.parseColor("#64748B");

    private TextView tvModStatus;
    private TextView btnActivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(C_BG);

        // Hide action bar
        if (getActionBar() != null) getActionBar().hide();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(C_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(40));

        // ── Logo + Title ──────────────────────────────────────────────────────
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, 0, 0, dp(4));

        TextView logo = new TextView(this);
        logo.setText("◆");
        logo.setTextColor(C_ACCENT);
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        logo.setPadding(0, 0, dp(10), 0);
        titleRow.addView(logo);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("GeminX");
        tvTitle.setTextColor(C_TEXT);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Mod Menu · Settlement Survival");
        tvSub.setTextColor(C_MUTED);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleCol.addView(tvSub);

        titleRow.addView(titleCol);
        root.addView(titleRow);
        root.addView(vspace(24));

        // ── Status Card ───────────────────────────────────────────────────────
        root.addView(makeCard(new CardBuilder() {
            @Override public void build(LinearLayout card) {
                card.addView(makeLabel("STATUS"));
                card.addView(vspace(10));

                // LSPosed status
                LinearLayout rowLsp = makeStatusRow(
                    "LSPosed Module",
                    isModuleActive() ? "Aktif" : "Tidak Aktif",
                    isModuleActive() ? C_GREEN : C_RED
                );
                card.addView(rowLsp);
                card.addView(vspace(8));

                // Overlay permission
                boolean hasOverlay = hasOverlayPermission();
                card.addView(makeStatusRow(
                    "Izin Overlay",
                    hasOverlay ? "Diberikan" : "Belum Diberikan",
                    hasOverlay ? C_GREEN : C_MUTED
                ));
                card.addView(vspace(8));

                // Target game
                card.addView(makeStatusRow(
                    "Target Game",
                    "Settlement Survival",
                    C_ACCENT
                ));
            }
        }));
        root.addView(vspace(14));

        // ── Mod Status ────────────────────────────────────────────────────────
        root.addView(makeCard(new CardBuilder() {
            @Override public void build(LinearLayout card) {
                card.addView(makeLabel("MOD"));
                card.addView(vspace(10));

                tvModStatus = new TextView(MainApp.this);
                tvModStatus.setText(isModuleActive()
                    ? "● Mod siap digunakan"
                    : "⚠ Aktifkan module di LSPosed Manager");
                tvModStatus.setTextColor(isModuleActive() ? C_GREEN : C_ACCENT);
                tvModStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                card.addView(tvModStatus);

                if (!isModuleActive()) {
                    card.addView(vspace(6));
                    TextView hint = new TextView(MainApp.this);
                    hint.setText("1. Buka LSPosed Manager\n2. Pilih module GeminX\n3. Centang Settlement Survival\n4. Reboot atau Force Stop game");
                    hint.setTextColor(C_MUTED);
                    hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    hint.setLineSpacing(dp(2), 1f);
                    card.addView(hint);
                }
            }
        }));
        root.addView(vspace(14));

        // ── Tombol Aktifkan ───────────────────────────────────────────────────
        btnActivate = new TextView(this);
        btnActivate.setText(hasOverlayPermission()
            ? "▶  AKTIFKAN MOD MENU"
            : "⚙  MINTA IZIN OVERLAY");
        btnActivate.setTextColor(Color.BLACK);
        btnActivate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnActivate.setTypeface(Typeface.DEFAULT_BOLD);
        btnActivate.setGravity(Gravity.CENTER);
        btnActivate.setPadding(0, dp(16), 0, dp(16));
        btnActivate.setBackground(makeRoundRect(C_ACCENT, dp(10)));
        btnActivate.setOnClickListener(v -> onActivateClick());
        root.addView(btnActivate);
        root.addView(vspace(10));

        // Tombol stop overlay
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
        root.addView(makeCard(new CardBuilder() {
            @Override public void build(LinearLayout card) {
                card.addView(makeLabel("INFO"));
                card.addView(vspace(8));
                card.addView(makeInfoRow("Versi Mod",    "1.0.0"));
                card.addView(vspace(6));
                card.addView(makeInfoRow("Target",       "Settlement Survival"));
                card.addView(vspace(6));
                card.addView(makeInfoRow("Package",      "com.xd.SettlementSurvival.gp.global"));
                card.addView(vspace(6));
                card.addView(makeInfoRow("Framework",    "LSPosed / Zygisk"));
            }
        }));

        scroll.addView(root);
        setContentView(scroll);
    }

    private void onActivateClick() {
        if (!hasOverlayPermission()) {
            // Minta izin overlay
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        } else {
            // Start overlay service
            startService(new Intent(this, ModOverlay.class));
            toast("Mod Menu diaktifkan — buka game sekarang!");
            // Minimize app
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OVERLAY) {
            boolean granted = hasOverlayPermission();
            btnActivate.setText(granted ? "▶  AKTIFKAN MOD MENU" : "⚙  MINTA IZIN OVERLAY");
            if (granted) toast("Izin diberikan! Tekan tombol untuk aktifkan.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh status
        if (tvModStatus != null) {
            tvModStatus.setText(isModuleActive()
                ? "● Mod siap digunakan"
                : "⚠ Aktifkan module di LSPosed Manager");
            tvModStatus.setTextColor(isModuleActive() ? C_GREEN : C_ACCENT);
        }
        if (btnActivate != null) {
            btnActivate.setText(hasOverlayPermission()
                ? "▶  AKTIFKAN MOD MENU"
                : "⚙  MINTA IZIN OVERLAY");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Deteksi module aktif — kalau HookEntry sudah inject, field ini true */
    private boolean isModuleActive() {
        return false; // default false; HookEntry akan override via reflection trick
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ── UI Builders ───────────────────────────────────────────────────────────
    interface CardBuilder { void build(LinearLayout card); }

    private LinearLayout makeCard(CardBuilder builder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeCardBg());
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        builder.build(card);
        return card;
    }

    private LinearLayout makeStatusRow(String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(C_MUTED);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvLabel);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(valueColor);
        tvVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(tvVal);

        return row;
    }

    private LinearLayout makeInfoRow(String label, String value) {
        return makeStatusRow(label, value, C_TEXT);
    }

    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private View vspace(int dpVal) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(dpVal)));
        return v;
    }

    private GradientDrawable makeCardBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(C_SURFACE);
        gd.setCornerRadius(dp(12));
        gd.setStroke(dp(1), C_BORDER);
        return gd;
    }

    private GradientDrawable makeRoundRect(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val,
            getResources().getDisplayMetrics());
    }
}
