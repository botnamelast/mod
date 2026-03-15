package com.geminx.mod;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.util.TypedValue;

import java.util.*;

public class ModOverlay extends Service {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG        = Color.parseColor("#0D0D0F");
    private static final int C_SURFACE   = Color.parseColor("#16161A");
    private static final int C_BORDER    = Color.parseColor("#2A2A35");
    private static final int C_ACCENT    = Color.parseColor("#F59E0B");
    private static final int C_ACCENT2   = Color.parseColor("#EF4444");
    private static final int C_TEXT      = Color.parseColor("#F1F5F9");
    private static final int C_MUTED     = Color.parseColor("#64748B");
    private static final int C_SUCCESS   = Color.parseColor("#10B981");
    private static final int C_ITEM_BG   = Color.parseColor("#1E1E24");

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams params;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Pages
    private View pageMain, pageItems;
    private TextView tvPageTitle;

    // Main page widgets
    private TextView[] speedBtns;
    private EditText etGold, etCitizen;
    private int selectedSpeed = 1;

    // Items page
    private LinearLayout itemListContainer;
    private String currentCategory = "";
    private final Map<String, Object[][]> categoryMap = new LinkedHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForegroundNotification();
        buildCategoryMap();
        buildUI();
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "geminx_mod", "GeminX Mod",
                NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);

            Notification notif = new Notification.Builder(this, "geminx_mod")
                .setContentTitle("GeminX Mod Aktif")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
            startForeground(1, notif);
        }
    }

    private void buildCategoryMap() {
        categoryMap.put("Sayuran & Buah",    ItemData.FOOD_SAYURAN);
        categoryMap.put("Daging & Protein",  ItemData.FOOD_DAGING);
        categoryMap.put("Olahan & Masakan",  ItemData.FOOD_OLAHAN);
        categoryMap.put("Minuman",           ItemData.FOOD_MINUMAN);
        categoryMap.put("Tekstil & Pakaian", ItemData.RAW_TEKSTIL);
        categoryMap.put("Bangunan",          ItemData.RAW_BANGUNAN);
        categoryMap.put("Pertanian & Kimia", ItemData.RAW_PERTANIAN);
        categoryMap.put("Alat & Perkakas",   ItemData.RAW_ALAT);
        categoryMap.put("Benih & Tanaman",   ItemData.SEED);
        categoryMap.put("Hewan Ternak",      ItemData.ANIMAL);
    }

    private void buildUI() {
        // Root frame
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(dp(300), FrameLayout.LayoutParams.WRAP_CONTENT));

        // Card background
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeCardBg());
        card.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // ── Header ────────────────────────────────────────────────────────────
        card.addView(makeHeader());

        // ── Page container ────────────────────────────────────────────────────
        FrameLayout pageContainer = new FrameLayout(this);

        pageMain  = buildMainPage();
        pageItems = buildItemsPage();
        pageItems.setVisibility(View.GONE);

        pageContainer.addView(pageMain);
        pageContainer.addView(pageItems);
        card.addView(pageContainer);

        frame.addView(card);
        root = frame;

        // Drag
        params = makeParams();
        setupDrag(card);
        wm.addView(root, params);
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private View makeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setBackground(makeHeaderBg());
        h.setPadding(dp(14), dp(10), dp(10), dp(10));
        h.setGravity(Gravity.CENTER_VERTICAL);

        // Logo dot
        TextView dot = new TextView(this);
        dot.setText("◆");
        dot.setTextColor(C_ACCENT);
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        dot.setPadding(0, 0, dp(8), 0);
        h.addView(dot);

        // Title
        tvPageTitle = new TextView(this);
        tvPageTitle.setText("GeminX");
        tvPageTitle.setTextColor(C_TEXT);
        tvPageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPageTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvPageTitle.setLayoutParams(lp);
        h.addView(tvPageTitle);

        // Minimize btn
        h.addView(makeIconBtn("─", C_MUTED, v -> toggleMinimize()));
        // Close btn
        h.addView(makeIconBtn("✕", C_ACCENT2, v -> stopSelf()));

        return h;
    }

    // ── MAIN PAGE ─────────────────────────────────────────────────────────────
    private View buildMainPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));

        // Speed section
        page.addView(makeSectionLabel("GAME SPEED"));
        page.addView(makeSpeedRow());
        page.addView(makeDivider());

        // Gold
        page.addView(makeSectionLabel("GOLD / KOIN"));
        etGold = makeEditText("Jumlah gold...");
        page.addView(etGold);
        page.addView(vspace(6));

        // Citizen
        page.addView(makeSectionLabel("WARGA KOTA"));
        etCitizen = makeEditText("Jumlah warga...");
        page.addView(etCitizen);
        page.addView(vspace(8));

        // Apply main
        page.addView(makeApplyBtn("▶  TERAPKAN", C_SUCCESS, v -> applyMain()));
        page.addView(makeDivider());

        // Category buttons
        page.addView(makeSectionLabel("ITEM & SUMBER DAYA"));
        page.addView(vspace(4));

        // Food group
        page.addView(makeCatGroup("🌾 MAKANAN", new String[]{
            "Sayuran & Buah", "Daging & Protein", "Olahan & Masakan", "Minuman"
        }));
        page.addView(vspace(4));

        // Raw group
        page.addView(makeCatGroup("⚒ MATERIAL", new String[]{
            "Tekstil & Pakaian", "Bangunan", "Pertanian & Kimia", "Alat & Perkakas"
        }));
        page.addView(vspace(4));

        // Seed + Animal
        page.addView(makeCatGroup("🌱 LAINNYA", new String[]{
            "Benih & Tanaman", "Hewan Ternak"
        }));

        return page;
    }

    private View makeCatGroup(String groupLabel, String[] cats) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(makeRoundRect(C_ITEM_BG, dp(8)));
        group.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView lbl = new TextView(this);
        lbl.setText(groupLabel);
        lbl.setTextColor(C_ACCENT);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setPadding(0, 0, 0, dp(6));
        group.addView(lbl);

        for (String cat : cats) {
            group.addView(makeCatBtn(cat));
            if (!cat.equals(cats[cats.length - 1]))
                group.addView(vspace(4));
        }
        return group;
    }

    private View makeCatBtn(String cat) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setBackground(makeRoundRect(Color.parseColor("#252530"), dp(6)));
        btn.setPadding(dp(10), dp(8), dp(10), dp(8));
        btn.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(cat);
        name.setTextColor(C_TEXT);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        name.setLayoutParams(lp);
        btn.addView(name);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(C_MUTED);
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        btn.addView(arrow);

        btn.setOnClickListener(v -> openItemPage(cat));
        return btn;
    }

    private View makeSpeedRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackground(makeRoundRect(C_ITEM_BG, dp(8)));
        row.setPadding(dp(6), dp(6), dp(6), dp(6));

        int[] speeds = {1, 2, 3, 5, 6, 8, 10};
        speedBtns = new TextView[speeds.length];

        for (int i = 0; i < speeds.length; i++) {
            final int spd = speeds[i];
            TextView btn = new TextView(this);
            btn.setText("×" + spd);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(dp(4), dp(6), dp(4), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            btn.setLayoutParams(lp);
            updateSpeedBtn(btn, spd == 1);
            btn.setOnClickListener(v -> selectSpeed(spd));
            speedBtns[i] = btn;
            row.addView(btn);
        }
        return row;
    }

    // ── ITEMS PAGE ────────────────────────────────────────────────────────────
    private View buildItemsPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));

        // Back + title row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, 0, 0, dp(10));

        TextView btnBack = new TextView(this);
        btnBack.setText("‹ Kembali");
        btnBack.setTextColor(C_ACCENT);
        btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnBack.setOnClickListener(v -> showMainPage());
        topRow.addView(btnBack);
        page.addView(topRow);

        // Scrollable item list
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(320)));

        itemListContainer = new LinearLayout(this);
        itemListContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(itemListContainer);
        page.addView(scroll);

        page.addView(vspace(8));
        page.addView(makeApplyBtn("▶  TERAPKAN ITEM", C_SUCCESS, v -> applyItems()));

        return page;
    }

    private void openItemPage(String cat) {
        currentCategory = cat;
        tvPageTitle.setText(cat);
        itemListContainer.removeAllViews();

        Object[][] items = categoryMap.get(cat);
        if (items == null) return;

        // Header row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(4), 0, dp(4), dp(6));
        header.addView(makeColHeader("ITEM", 0));
        header.addView(makeColHeader("JUMLAH", 1));
        itemListContainer.addView(header);
        itemListContainer.addView(makeDivider());

        for (Object[] item : items) {
            itemListContainer.addView(makeItemRow((int) item[0], (String) item[1]));
        }

        pageMain.setVisibility(View.GONE);
        pageItems.setVisibility(View.VISIBLE);
        animateFadeIn(pageItems);
    }

    private View makeItemRow(int id, String name) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(4), dp(7), dp(4), dp(7));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(makeDividerBg());

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(C_TEXT);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(lp);
        row.addView(tvName);

        EditText etNum = new EditText(this);
        etNum.setTag(id);
        etNum.setHint("0");
        etNum.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etNum.setTextColor(C_TEXT);
        etNum.setHintTextColor(C_MUTED);
        etNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        etNum.setBackground(makeRoundRect(C_ITEM_BG, dp(6)));
        etNum.setPadding(dp(8), dp(4), dp(8), dp(4));
        etNum.setWidth(dp(80));
        etNum.setGravity(Gravity.CENTER);
        row.addView(etNum);

        return row;
    }

    private void showMainPage() {
        tvPageTitle.setText("GeminX");
        pageItems.setVisibility(View.GONE);
        pageMain.setVisibility(View.VISIBLE);
        animateFadeIn(pageMain);
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────
    private void selectSpeed(int spd) {
        selectedSpeed = spd;
        int[] speeds = {1, 2, 3, 5, 6, 8, 10};
        for (int i = 0; i < speedBtns.length; i++) {
            updateSpeedBtn(speedBtns[i], speeds[i] == spd);
        }
        HookEntry.setGameSpeed(spd);
    }

    private void applyMain() {
        String goldStr = etGold.getText().toString().trim();
        String citStr  = etCitizen.getText().toString().trim();
        if (!goldStr.isEmpty()) {
            try { HookEntry.addCoin(Integer.parseInt(goldStr)); } catch (Exception ignored) {}
        }
        if (!citStr.isEmpty()) {
            // citizen logic nanti
        }
        showToast("✓ Diterapkan!");
    }

    private void applyItems() {
        int applied = 0;
        for (int i = 0; i < itemListContainer.getChildCount(); i++) {
            View child = itemListContainer.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            for (int j = 0; j < row.getChildCount(); j++) {
                View v = row.getChildAt(j);
                if (v instanceof EditText) {
                    EditText et = (EditText) v;
                    String s = et.getText().toString().trim();
                    if (!s.isEmpty() && !s.equals("0")) {
                        try {
                            int id  = (int) et.getTag();
                            int num = Integer.parseInt(s);
                            HookEntry.collectItem(id, num);
                            applied++;
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        showToast("✓ " + applied + " item diterapkan!");
    }

    private boolean minimized = false;
    private View pageContainer;

    private void toggleMinimize() {
        // minimized toggling — sembunyikan content, hanya header kelihatan
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void showToast(String msg) {
        handler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private void animateFadeIn(View v) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(150).start();
    }

    private TextView makeIconBtn(String text, int color, View.OnClickListener l) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(color);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        btn.setOnClickListener(l);
        return btn;
    }

    private TextView makeSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(6), 0, dp(4));
        return tv;
    }

    private EditText makeEditText(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(C_MUTED);
        et.setTextColor(C_TEXT);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        et.setBackground(makeRoundRect(C_ITEM_BG, dp(8)));
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        et.setLayoutParams(lp);
        return et;
    }

    private View makeApplyBtn(String text, int color, View.OnClickListener l) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(12), 0, dp(12));
        btn.setBackground(makeRoundRect(color, dp(8)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(l);
        return btn;
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

    private View vspace(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(dp)));
        return v;
    }

    private TextView makeColHeader(String text, int weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_MUTED);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        if (weight == 0) {
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(dp(80),
                LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setGravity(Gravity.CENTER);
        }
        return tv;
    }

    private void updateSpeedBtn(TextView btn, boolean active) {
        if (active) {
            btn.setBackground(makeRoundRect(C_ACCENT, dp(6)));
            btn.setTextColor(Color.BLACK);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            btn.setBackground(makeRoundRect(Color.parseColor("#252530"), dp(6)));
            btn.setTextColor(C_MUTED);
            btn.setTypeface(Typeface.DEFAULT);
        }
    }

    // ── DRAWABLES ─────────────────────────────────────────────────────────────
    private Drawable makeCardBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(C_BG);
        gd.setCornerRadius(dp(12));
        gd.setStroke(dp(1), C_BORDER);
        return gd;
    }

    private Drawable makeHeaderBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(C_SURFACE);
        float[] r = {dp(12), dp(12), dp(12), dp(12), 0, 0, 0, 0};
        gd.setCornerRadii(r);
        return gd;
    }

    private Drawable makeRoundRect(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private Drawable makeDividerBg() {
        LayerDrawable ld = new LayerDrawable(new Drawable[]{
            new ColorDrawable(Color.TRANSPARENT)
        });
        return ld;
    }

    // ── DRAG ──────────────────────────────────────────────────────────────────
    private void setupDrag(View header) {
        final int[] lastX = {0}, lastY = {0};
        final boolean[] dragging = {false};
        header.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX[0] = (int) e.getRawX();
                    lastY[0] = (int) e.getRawY();
                    dragging[0] = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) e.getRawX() - lastX[0];
                    int dy = (int) e.getRawY() - lastY[0];
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) dragging[0] = true;
                    params.x += dx;
                    params.y += dy;
                    lastX[0] = (int) e.getRawX();
                    lastY[0] = (int) e.getRawY();
                    wm.updateViewLayout(root, params);
                    break;
            }
            return dragging[0];
        });
    }

    // ── WINDOW PARAMS ─────────────────────────────────────────────────────────
    private WindowManager.LayoutParams makeParams() {
        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 20; p.y = 120;
        return p;
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val,
            getResources().getDisplayMetrics());
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        if (root != null) wm.removeView(root);
        super.onDestroy();
    }
}
