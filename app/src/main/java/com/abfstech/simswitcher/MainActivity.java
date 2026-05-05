package com.abfstech.simswitcher;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int CLR_BG         = 0xFF0F0F0F;
    private static final int CLR_CARD       = 0xFF1C1C1E;
    private static final int CLR_ACTIVE     = 0xFF1A4731;
    private static final int CLR_WHITE      = 0xFFFFFFFF;
    private static final int CLR_GREY       = 0xFF8E8E93;
    private static final int CLR_GREEN      = 0xFF34C759;
    private static final int CLR_YELLOW     = 0xFFFFD60A;
    private static final int CLR_RED        = 0xFFFF453A;
    private static final int CLR_BLUE       = 0xFF0A84FF;
    private static final int CLR_DIM        = 0xFF444448;
    private static final int CLR_BTN        = 0xFF2C2C2E;

    private TextView statusBox;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(CLR_BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 64);
        scroll.addView(root);

        // Title
        TextView title = makeText("SIM Data Switcher", 24, CLR_WHITE, Typeface.BOLD);
        title.setPadding(0, 0, 0, 6);
        root.addView(title);

        TextView subtitle = makeText(
            "Tap a SIM to set it as your mobile data provider. Voice SIM is unaffected.",
            13, CLR_GREY, Typeface.NORMAL);
        subtitle.setPadding(0, 0, 0, 32);
        root.addView(subtitle);

        // Device info strip
        root.addView(deviceInfoCard());

        // Permission banner
        boolean hasPermission = checkPermission();
        root.addView(permissionBanner(hasPermission));

        // SIM list (wrapped in its own try/catch)
        SubscriptionManager subManager = null;
        List<SubscriptionInfo> subs = null;
        try {
            subManager = (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);
            if (subManager == null) throw new IllegalStateException("getSystemService returned null for TELEPHONY_SUBSCRIPTION_SERVICE");
            subs = subManager.getActiveSubscriptionInfoList();
        } catch (Throwable t) {
            root.addView(errorCard("Failed to read SIM list", t,
                "Make sure READ_PHONE_STATE is granted:\n" +
                "adb shell pm grant com.abfstech.simswitcher android.permission.READ_PHONE_STATE"));
            root.addView(refreshButton());
            setContentView(scroll);
            return;
        }

        if (subs == null || subs.isEmpty()) {
            root.addView(warningCard(
                "No active SIMs detected",
                "Toggle your SIMs ON in:\nSettings → Connections → SIM manager\n\nThen tap Refresh."));
            root.addView(refreshButton());
            setContentView(scroll);
            return;
        }

        // Section header
        root.addView(sectionHeader("Active SIMs  (" + subs.size() + ")"));

        int currentDataSubId;
        try {
            currentDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        } catch (Throwable t) {
            currentDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            root.addView(warningCard("Could not read current data SIM ID", formatThrowable(t)));
        }

        for (SubscriptionInfo info : subs) {
            try {
                root.addView(buildSimCard(info, currentDataSubId, hasPermission));
            } catch (Throwable t) {
                root.addView(errorCard("Failed to render SIM card (subId=" + info.getSubscriptionId() + ")", t, null));
            }
        }

        // Status box — hidden until an action fires
        statusBox = makeText("", 13, CLR_BLUE, Typeface.NORMAL);
        statusBox.setBackgroundColor(CLR_CARD);
        statusBox.setPadding(32, 28, 32, 28);
        statusBox.setVisibility(View.GONE);
        LinearLayout.LayoutParams sbLp = cardLp(24, 0);
        root.addView(statusBox, sbLp);

        root.addView(refreshButton());
        setContentView(scroll);
    }

    // ── Build one SIM card ────────────────────────────────────────────────────
    private View buildSimCard(SubscriptionInfo info, int currentDataSubId, boolean hasPermission) {
        boolean isCurrent = info.getSubscriptionId() == currentDataSubId;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 28, 32, 28);
        card.setBackgroundColor(isCurrent ? CLR_ACTIVE : CLR_CARD);
        card.setLayoutParams(cardLp(0, 16));

        String name    = safeLabel(info.getDisplayName(),  "SIM " + (info.getSimSlotIndex() + 1));
        String carrier = safeLabel(info.getCarrierName(),  "Unknown carrier");
        String number  = (info.getNumber() != null && !info.getNumber().isEmpty())
                         ? info.getNumber() : "No number — data-only SIM";

        // Name row + DATA badge
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView nameView = makeText(name, 17, CLR_WHITE, Typeface.BOLD);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameRow.addView(nameView);

        if (isCurrent) {
            TextView badge = makeText("  DATA  ", 11, CLR_BG, Typeface.BOLD);
            badge.setBackgroundColor(CLR_GREEN);
            badge.setPadding(14, 6, 14, 6);
            nameRow.addView(badge);
        }
        card.addView(nameRow);

        card.addView(makeText(carrier, 13, CLR_GREY, Typeface.NORMAL));
        card.addView(makeText(number,  13, CLR_GREY, Typeface.NORMAL));
        card.addView(makeText("Sub ID: " + info.getSubscriptionId() + "   Slot: " + info.getSimSlotIndex(), 11, CLR_DIM, Typeface.NORMAL));

        // Action hint
        if (!hasPermission) {
            card.addView(makeText("⚠ Grant ADB permission first (see banner above)", 12, CLR_YELLOW, Typeface.NORMAL));
        } else if (!isCurrent) {
            card.setClickable(true);
            card.setFocusable(true);
            final int subId = info.getSubscriptionId();
            card.setOnClickListener(v -> setDataSim(subId, name));
            card.addView(makeText("Tap to set as data SIM →", 12, CLR_BLUE, Typeface.NORMAL));
        }

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
            statusBox.postDelayed(this::recreate, 1400);

        } catch (SecurityException e) {
            showStatus(buildErrorMsg(
                "Permission denied",
                "MODIFY_PHONE_STATE was not granted or was revoked.",
                "adb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE",
                e), CLR_RED);

        } catch (NoSuchMethodException e) {
            showStatus(buildErrorMsg(
                "Hidden API unavailable",
                "setDefaultDataSubId() not found on this ROM.\n" +
                "Android: " + Build.VERSION.RELEASE + "  SDK: " + Build.VERSION.SDK_INT + "\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL,
                null, e), CLR_RED);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            showStatus(buildErrorMsg(
                "Method threw an exception",
                "The API call itself failed. Cause: " + cause.getClass().getName(),
                "Try rebooting and re-granting the ADB permission.",
                cause), CLR_RED);

        } catch (IllegalAccessException e) {
            showStatus(buildErrorMsg(
                "Reflection access denied",
                "setAccessible() was blocked — likely by a restricted API policy on this ROM.",
                "This device may not support hidden API access.",
                e), CLR_RED);

        } catch (Throwable t) {
            // Absolute catch-all — nothing escapes silently
            showStatus(buildErrorMsg(
                "Unexpected error: " + t.getClass().getName(),
                t.getMessage(),
                "Please report this with the full stack trace below.",
                t), CLR_RED);
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private View deviceInfoCard() {
        String info =
            "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
            "Android: " + Build.VERSION.RELEASE + "  (SDK " + Build.VERSION.SDK_INT + ")\n" +
            "Build: " + Build.DISPLAY;
        TextView tv = makeText(info, 11, CLR_DIM, Typeface.NORMAL);
        tv.setBackgroundColor(CLR_CARD);
        tv.setPadding(32, 20, 32, 20);
        tv.setLayoutParams(cardLp(0, 16));
        return tv;
    }

    private View permissionBanner(boolean hasPermission) {
        String msg = hasPermission
            ? "✅  MODIFY_PHONE_STATE granted — switching is enabled."
            : "⚠️  Permission not granted.\n\n" +
              "Run this once from your PC, then reopen the app:\n\n" +
              "adb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE";
        TextView tv = makeText(msg, 13, hasPermission ? CLR_GREEN : CLR_YELLOW, Typeface.NORMAL);
        tv.setBackgroundColor(CLR_CARD);
        tv.setPadding(32, 28, 32, 28);
        tv.setLayoutParams(cardLp(0, 28));
        return tv;
    }

    private View sectionHeader(String text) {
        TextView tv = makeText(text, 12, CLR_GREY, Typeface.BOLD);
        tv.setLayoutParams(cardLp(0, 10));
        return tv;
    }

    /** Red error card: title + full exception details + optional fix hint. */
    private View errorCard(String title, Throwable t, String fixHint) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF2A0A0A);
        card.setPadding(32, 28, 32, 28);
        card.setLayoutParams(cardLp(0, 16));

        card.addView(makeText("❌  " + title, 15, CLR_RED, Typeface.BOLD));
        card.addView(makeText(formatThrowable(t), 12, 0xFFFF8A80, Typeface.NORMAL));

        if (fixHint != null && !fixHint.isEmpty()) {
            card.addView(makeText("Fix: " + fixHint, 12, CLR_YELLOW, Typeface.NORMAL));
        }
        return card;
    }

    /** Yellow warning card: no exception, just a message. */
    private View warningCard(String title, String detail) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF2A1F00);
        card.setPadding(32, 28, 32, 28);
        card.setLayoutParams(cardLp(0, 16));

        card.addView(makeText("⚠️  " + title, 15, CLR_YELLOW, Typeface.BOLD));
        if (detail != null) card.addView(makeText(detail, 13, 0xFFFFE57F, Typeface.NORMAL));
        return card;
    }

    private Button refreshButton() {
        Button btn = new Button(this);
        btn.setText("↻  Refresh");
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(CLR_WHITE);
        btn.setBackgroundColor(CLR_BTN);
        btn.setPadding(0, 28, 0, 28);
        btn.setLayoutParams(cardLp(16, 0));
        btn.setOnClickListener(v -> recreate());
        return btn;
    }

    private void showStatus(String msg, int color) {
        statusBox.setText(msg);
        statusBox.setTextColor(color);
        statusBox.setVisibility(View.VISIBLE);
    }

    // ── Build a rich error string ─────────────────────────────────────────────
    private String buildErrorMsg(String headline, String detail, String fix, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌  ").append(headline).append("\n");
        if (detail != null && !detail.isEmpty()) sb.append("\n").append(detail).append("\n");
        if (fix   != null && !fix.isEmpty())    sb.append("\nFix: ").append(fix).append("\n");
        sb.append("\n── Stack trace ─────────────────\n").append(formatThrowable(t));
        return sb.toString();
    }

    /** Formats a Throwable into a compact but complete string for display. */
    private String formatThrowable(Throwable t) {
        if (t == null) return "(null throwable)";
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();
        // Trim to avoid giant walls of text — keep first 1800 chars
        if (full.length() > 1800) full = full.substring(0, 1800) + "\n… (truncated)";
        return full;
    }

    private boolean checkPermission() {
        return checkSelfPermission("android.permission.MODIFY_PHONE_STATE")
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private String safeLabel(CharSequence cs, String fallback) {
        return (cs != null && !TextUtils.isEmpty(cs)) ? cs.toString() : fallback;
    }

    private LinearLayout.LayoutParams cardLp(int topMargin, int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, topMargin, 0, bottomMargin);
        return lp;
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
