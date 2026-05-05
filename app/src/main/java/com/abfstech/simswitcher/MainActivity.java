package com.abfstech.simswitcher;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int CLR_BG         = 0xFF0F0F0F;
    private static final int CLR_CARD       = 0xFF1C1C1E;
    private static final int CLR_ACTIVE     = 0xFF1A4731;   // green tint
    private static final int CLR_ACTIVE_BRD = 0xFF34C759;   // green border sim
    private static final int CLR_BTN        = 0xFF2C2C2E;
    private static final int CLR_WHITE      = 0xFFFFFFFF;
    private static final int CLR_GREY       = 0xFF8E8E93;
    private static final int CLR_GREEN      = 0xFF34C759;
    private static final int CLR_YELLOW     = 0xFFFFD60A;
    private static final int CLR_RED        = 0xFFFF453A;
    private static final int CLR_BLUE       = 0xFF0A84FF;

    private SubscriptionManager subManager;
    private TextView statusBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        subManager = (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);

        // ── Root scroll layout ────────────────────────────────────────────────
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CLR_BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 64);
        scroll.addView(root);

        // ── Title ─────────────────────────────────────────────────────────────
        TextView title = makeText("SIM Data Switcher", 24, CLR_WHITE, Typeface.BOLD);
        title.setPadding(0, 0, 0, 6);
        root.addView(title);

        TextView subtitle = makeText("Tap a SIM to set it as your mobile data provider.
Voice SIM is unaffected.", 13, CLR_GREY, Typeface.NORMAL);
        subtitle.setPadding(0, 0, 0, 32);
        root.addView(subtitle);

        // ── Permission status banner ──────────────────────────────────────────
        boolean hasPermission = checkPermission();
        TextView permBanner = makeText(
            hasPermission
                ? "✅  MODIFY_PHONE_STATE granted — switching is enabled."
                : "⚠️  Permission not granted yet.
Run this ADB command once, then reopen the app:

adb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE",
            13,
            hasPermission ? CLR_GREEN : CLR_YELLOW,
            Typeface.NORMAL
        );
        permBanner.setBackgroundColor(CLR_CARD);
        permBanner.setPadding(32, 28, 32, 28);
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerLp.setMargins(0, 0, 0, 28);
        root.addView(permBanner, bannerLp);

        // ── SIM list ──────────────────────────────────────────────────────────
        List<SubscriptionInfo> subs = null;
        String simLoadError = null;
        try {
            subs = subManager.getActiveSubscriptionInfoList();
        } catch (SecurityException e) {
            simLoadError = "Cannot read SIM list — READ_PHONE_STATE permission missing.

Run:
adb shell pm grant com.abfstech.simswitcher android.permission.READ_PHONE_STATE";
        }

        if (simLoadError != null) {
            root.addView(errorCard(simLoadError));
            setContentView(scroll);
            return;
        }

        if (subs == null || subs.isEmpty()) {
            root.addView(errorCard(
                "No active SIMs detected.

" +
                "Make sure your SIMs are toggled ON in:
" +
                "Settings → Connections → SIM manager

" +
                "Then reopen this app."
            ));
            setContentView(scroll);
            return;
        }

        root.addView(makeText("Active SIMs  (" + subs.size() + ")", 12, CLR_GREY, Typeface.BOLD));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerLp.setMargins(0, 0, 0, 10);

        int currentDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();

        for (SubscriptionInfo info : subs) {
            root.addView(buildSimCard(info, currentDataSubId, hasPermission));
        }

        // ── Status box (shows result of tapping) ─────────────────────────────
        statusBox = makeText("", 13, CLR_BLUE, Typeface.NORMAL);
        statusBox.setBackgroundColor(CLR_CARD);
        statusBox.setPadding(32, 28, 32, 28);
        statusBox.setVisibility(View.GONE);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sbLp.setMargins(0, 24, 0, 0);
        root.addView(statusBox, sbLp);

        // ── Refresh button ────────────────────────────────────────────────────
        Button refresh = new Button(this);
        refresh.setText("↻  Refresh");
        refresh.setAllCaps(false);
        refresh.setTextSize(14);
        refresh.setTextColor(CLR_WHITE);
        refresh.setBackgroundColor(CLR_BTN);
        refresh.setPadding(0, 28, 0, 28);
        LinearLayout.LayoutParams rfLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rfLp.setMargins(0, 16, 0, 0);
        refresh.setOnClickListener(v -> recreate());
        root.addView(refresh, rfLp);

        setContentView(scroll);
    }

    // ── Build a single SIM card ───────────────────────────────────────────────
    private View buildSimCard(SubscriptionInfo info, int currentDataSubId, boolean hasPermission) {
        boolean isCurrent = info.getSubscriptionId() == currentDataSubId;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 28, 32, 28);
        card.setBackgroundColor(isCurrent ? CLR_ACTIVE : CLR_CARD);

        String name    = info.getDisplayName()  != null ? info.getDisplayName().toString()  : "SIM " + (info.getSimSlotIndex() + 1);
        String carrier = info.getCarrierName()  != null ? info.getCarrierName().toString()  : "Unknown carrier";
        String number  = (info.getNumber() != null && !info.getNumber().isEmpty()) ? info.getNumber() : "No number (data-only)";
        int    subId   = info.getSubscriptionId();

        // Name row
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = makeText(name, 17, CLR_WHITE, Typeface.BOLD);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameRow.addView(nameView);

        if (isCurrent) {
            TextView badge = makeText(" DATA ", 11, CLR_BG, Typeface.BOLD);
            badge.setBackgroundColor(CLR_GREEN);
            badge.setPadding(14, 6, 14, 6);
            nameRow.addView(badge);
        }
        card.addView(nameRow);

        // Details
        card.addView(makeText(carrier, 13, CLR_GREY, Typeface.NORMAL));
        card.addView(makeText(number,  13, CLR_GREY, Typeface.NORMAL));
        card.addView(makeText("Sub ID: " + subId, 11, 0xFF444448, Typeface.NORMAL));

        // Tap action
        if (hasPermission && !isCurrent) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> setDataSim(subId, name));

            TextView tapHint = makeText("Tap to set as data SIM →", 12, CLR_BLUE, Typeface.NORMAL);
            tapHint.setPadding(0, 12, 0, 0);
            card.addView(tapHint);
        } else if (!hasPermission) {
            TextView tapHint = makeText("Grant ADB permission first (see banner above)", 12, CLR_YELLOW, Typeface.NORMAL);
            tapHint.setPadding(0, 12, 0, 0);
            card.addView(tapHint);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        return card;
    }

    // ── Switch data SIM ───────────────────────────────────────────────────────
    private void setDataSim(int subId, String simName) {
        try {
            Method m = SubscriptionManager.class.getDeclaredMethod("setDefaultDataSubId", int.class);
            m.setAccessible(true);
            m.invoke(null, subId);

            showStatus("✅  Data SIM switched to: " + simName, CLR_GREEN);
            Toast.makeText(this, "Data SIM → " + simName, Toast.LENGTH_SHORT).show();

            // Refresh after short delay so user sees the toast
            statusBox.postDelayed(this::recreate, 1200);

        } catch (SecurityException e) {
            showStatus(
                "❌  Permission denied.

" +
                "The ADB grant may not have worked. Try again:

" +
                "adb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE

" +
                "Then reopen the app.",
                CLR_RED
            );
        } catch (NoSuchMethodException e) {
            showStatus(
                "❌  API not found on this Android version.

" +
                "setDefaultDataSubId() is unavailable.
" +
                "Device: " + android.os.Build.MODEL + "  Android " + android.os.Build.VERSION.RELEASE,
                CLR_RED
            );
        } catch (Exception e) {
            showStatus(
                "❌  Unexpected error: " + e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n\n" +
                "Try rebooting the phone and rerunning the ADB grant.",
                CLR_RED
            );
        }
    }

    // ── Check permission ──────────────────────────────────────────────────────
    private boolean checkPermission() {
        return checkSelfPermission("android.permission.MODIFY_PHONE_STATE")
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showStatus(String msg, int color) {
        statusBox.setText(msg);
        statusBox.setTextColor(color);
        statusBox.setVisibility(View.VISIBLE);
    }

    private View errorCard(String msg) {
        TextView tv = makeText("❌  " + msg, 14, CLR_RED, Typeface.NORMAL);
        tv.setBackgroundColor(CLR_CARD);
        tv.setPadding(32, 32, 32, 32);
        return tv;
    }

    private TextView makeText(String text, float sizeSp, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        tv.setTypeface(null, style);
        tv.setPadding(0, 4, 0, 4);
        return tv;
    }
}
