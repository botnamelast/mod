package com.flashmod.mod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

public class ModOverlay extends Service {

    private static final int C_BG      = Color.parseColor("#E6000000");
    private static final int C_SURFACE = Color.parseColor("#13131A");
    private static final int C_BORDER  = Color.parseColor("#252535");
    private static final int C_ACCENT  = Color.parseColor("#FF3C3C");
    private static final int C_ACCENT2 = Color.parseColor("#FF8C00");
    private static final int C_GREEN   = Color.parseColor("#00E676");
    private static final int C_TEXT    = Color.parseColor("#F0F0FF");
    private static final int C_MUTED   = Color.parseColor("#606080");
    private static final int C_ITEM_BG = Color.parseColor("#1A1A24");

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams params;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Toggle states
    private boolean speedHackOn  = false;
    private boolean maxSpeedOn   = false;
    private boolean timeScaleOn  = false;

    // Speed multiplier buttons
    private TextView[] timeBtns;
    private int[] timeValues = {1, 2, 3, 5, 8, 10};
    private int selectedTime = 2;

    // Max speed buttons
    private TextView[] speedBtns;
    private int[] speedValues = {100, 200, 300, 500, 999};
    private int selectedSpeed = 999;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForegroundNotification();
        buildUI();
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                "flashmod", "FlashMod", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
            Notification notif = new Notification.Builder(this, "flashmod")
                .setContentTitle("FlashMod Aktif")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
            startForeground(1, notif);
        }
    }

    private void buildUI() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeCardBg());

        // ── Header ────────────────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackground(makeHeaderBg());
        header.setPadding(dp(14), dp(10), dp(10), dp(10));
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("⚡");
        logo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        logo.setPadding(0, 0, dp(6), 0);
        header.addView(logo);

        TextView title = new TextView(this);
        title.setText("FlashMod");
        title.setTextColor(C_ACCENT);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(lp);
        header.addView(title);

        header.addView(makeIconBtn("✕", C_ACCENT, v -> stopSelf()));
        card.addView(header);

        // ── Body ──────────────────────────────────────────────────────────────
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(14));

        // Section: TimeScale (Race Speed)
        body.addView(makeSectionLabel("⚡ RACE SPEED (TIMESCALE)"));
        body.addView(vspace(6));
        body.addView(makeTimeScaleRow());
        body.addView(vspace(6));
        body.addView(makeToggleRow("Aktifkan TimeScale", timeScaleOn, C_ACCENT, v -> {
            timeScaleOn = !timeScaleOn;
            HookEntry.timeScaleActive = timeScaleOn;
            HookEntry.timeScaleValue  = selectedTime;
            updateToggle((TextView) v, timeScaleOn, C_ACCENT);
            showToast(timeScaleOn ? "TimeScale ON: " + selectedTime + "x" : "TimeScale OFF");
        }));

        body.addView(makeDivider());

        // Section: Car Max Speed
        body.addView(makeSectionLabel("🏎 MAX SPEED LIMIT"));
        body.addView(vspace(6));
        body.addView(makeMaxSpeedRow());
        body.addView(vspace(6));
        body.addView(makeToggleRow("Aktifkan Max Speed", maxSpeedOn, C_ACCENT2, v -> {
            maxSpeedOn = !maxSpeedOn;
            HookEntry.maxSpeedActive = maxSpeedOn;
            HookEntry.maxSpeedValue  = selectedSpeed;
            HookEntry.carMaxSpeed    = selectedSpeed;
            updateToggle((TextView) v, maxSpeedOn, C_ACCENT2);
            showToast(maxSpeedOn ? "Max Speed ON: " + selectedSpeed : "Max Speed OFF");
        }));

        body.addView(makeDivider());

        // Section: Speed Hack (Param_CarMaxSpeed direct)
        body.addView(makeSectionLabel("🔧 SPEED HACK"));
        body.addView(vspace(4));
        body.addView(makeToggleRow("Override CarMaxSpeed", speedHackOn, C_GREEN, v -> {
            speedHackOn = !speedHackOn;
            HookEntry.speedHackActive = speedHackOn;
            HookEntry.carMaxSpeed     = selectedSpeed;
            updateToggle((TextView) v, speedHackOn, C_GREEN);
            showToast(speedHackOn ? "Speed Hack ON" : "Speed Hack OFF");
        }));

        card.addView(body);
        root = card;

        params = makeParams();
        setupDrag(header);
        wm.addView(root, params);
    }

    // ── TimeScale row ─────────────────────────────────────────────────────────
    private View makeTimeScaleRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackground(makeRoundRect(C_ITEM_BG, dp(8)));
        row.setPadding(dp(6), dp(6), dp(6), dp(6));

        timeBtns = new TextView[timeValues.length];
        for (int i = 0; i < timeValues.length; i++) {
            final int val = timeValues[i];
            final int idx = i;
            TextView btn = new TextView(this);
            btn.setText(val + "x");
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(4), dp(6), dp(4), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            btn.setLayoutParams(lp);
            updateSpeedBtn(btn, val == selectedTime, C_ACCENT);
            btn.setOnClickListener(v -> {
                selectedTime = val;
                HookEntry.timeScaleValue = val;
                for (int j = 0; j < timeBtns.length; j++) {
                    updateSpeedBtn(timeBtns[j], timeValues[j] == val, C_ACCENT);
                }
            });
            timeBtns[i] = btn;
            row.addView(btn);
        }
        return row;
    }

    // ── Max Speed row ─────────────────────────────────────────────────────────
    private View makeMaxSpeedRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackground(makeRoundRect(C_ITEM_BG, dp(8)));
        row.setPadding(dp(6), dp(6), dp(6), dp(6));

        speedBtns = new TextView[speedValues.length];
        for (int i = 0; i < speedValues.length; i++) {
            final int val = speedValues[i];
            TextView btn = new TextView(this);
            btn.setText(val == 999 ? "MAX" : val + "");
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(4), dp(6), dp(4), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            btn.setLayoutParams(lp);
            updateSpeedBtn(btn, val == selectedSpeed, C_ACCENT2);
            btn.setOnClickListener(v -> {
                selectedSpeed = val;
                HookEntry.maxSpeedValue = val;
                HookEntry.carMaxSpeed   = val;
                for (int j = 0; j < speedBtns.length; j++) {
                    updateSpeedBtn(speedBtns[j], speedValues[j] == val, C_ACCENT2);
                }
            });
            speedBtns[i] = btn;
            row.addView(btn);
        }
        return row;
    }

    // ── Toggle row ────────────────────────────────────────────────────────────
    private TextView makeToggleRow(String label, boolean state, int color, View.OnClickListener l) {
        TextView btn = new TextView(this);
        updateToggle(btn, state, color);
        btn.setText((state ? "● ON  " : "○ OFF ") + label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(l);
        return btn;
    }

    private void updateToggle(TextView btn, boolean on, int color) {
        String text = btn.getText().toString();
        // Update prefix
        if (text.length() > 5) {
            String label = text.substring(5).trim();
            btn.setText((on ? "● ON  " : "○ OFF ") + label);
        }
        btn.setTextColor(on ? color : C_MUTED);
        btn.setBackground(makeRoundRect(on ? (color & 0x22FFFFFF | 0x22000000) : C_ITEM_BG, dp(8)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showToast(String msg) {
        handler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private void updateSpeedBtn(TextView btn, boolean active, int color) {
        if (active) {
            btn.setBackground(makeRoundRect(color, dp(6)));
            btn.setTextColor(Color.BLACK);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            btn.setBackground(makeRoundRect(Color.parseColor("#252530"), dp(6)));
            btn.setTextColor(C_MUTED);
            btn.setTypeface(Typeface.DEFAULT);
        }
    }

    private View makeSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private View makeDivider() {
        View d = new View(this);
        d.setBackgroundColor(C_BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(8), 0, dp(8));
        d.setLayoutParams(lp);
        return d;
    }

    private View vspace(int d) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(d)));
        return v;
    }

    private TextView makeIconBtn(String text, int color, View.OnClickListener l) {
        TextView btn = new TextView(this);
        btn.setText(text); btn.setTextColor(color);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        btn.setOnClickListener(l);
        return btn;
    }

    private void setupDrag(View handle) {
        final int[] lx = {0}, ly = {0};
        final boolean[] drag = {false};
        handle.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lx[0] = (int) e.getRawX(); ly[0] = (int) e.getRawY(); drag[0] = false; break;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) e.getRawX() - lx[0], dy = (int) e.getRawY() - ly[0];
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) drag[0] = true;
                    params.x += dx; params.y += dy;
                    lx[0] = (int) e.getRawX(); ly[0] = (int) e.getRawY();
                    wm.updateViewLayout(root, params); break;
            }
            return drag[0];
        });
    }

    private WindowManager.LayoutParams makeParams() {
        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            dp(280), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 20; p.y = 120;
        return p;
    }

    private Drawable makeCardBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(C_BG); gd.setCornerRadius(dp(12));
        gd.setStroke(dp(1), C_BORDER); return gd;
    }

    private Drawable makeHeaderBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(C_SURFACE);
        float[] r = {dp(12), dp(12), dp(12), dp(12), 0, 0, 0, 0};
        gd.setCornerRadii(r); return gd;
    }

    private GradientDrawable makeRoundRect(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color); gd.setCornerRadius(radius); return gd;
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val,
            getResources().getDisplayMetrics());
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        if (root != null && root.isAttachedToWindow()) wm.removeView(root);
        super.onDestroy();
    }
}
