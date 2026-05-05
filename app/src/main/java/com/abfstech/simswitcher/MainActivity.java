package com.abfstech.simswitcher;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {

    private SubscriptionManager subManager;
    private List<SubscriptionInfo> subs;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        subManager = (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);
        subs = subManager.getActiveSubscriptionInfoList();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 80, 60, 80);
        root.setGravity(Gravity.TOP);
        root.setBackgroundColor(0xFF121212);

        TextView title = new TextView(this);
        title.setText("SIM Data Switcher");
        title.setTextSize(26);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 0, 0, 12);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tap a SIM to set it as your mobile data provider");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFFAAAAAA);
        subtitle.setPadding(0, 0, 0, 40);
        root.addView(subtitle);

        if (subs == null || subs.isEmpty()) {
            TextView noSim = new TextView(this);
            noSim.setText("No active SIMs found.\nMake sure SIMs are enabled in Settings → Connections → SIM manager.");
            noSim.setTextColor(0xFFFF6666);
            noSim.setTextSize(15);
            root.addView(noSim);
            setContentView(root);
            return;
        }

        int currentDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();

        for (SubscriptionInfo info : subs) {
            Button btn = new Button(this);
            String label = info.getDisplayName() != null ? info.getDisplayName().toString() : "SIM " + info.getSimSlotIndex();
            String carrier = info.getCarrierName() != null ? info.getCarrierName().toString() : "";
            String number = (info.getNumber() != null && !info.getNumber().isEmpty()) ? "  " + info.getNumber() : "  (no number)";
            boolean isCurrent = info.getSubscriptionId() == currentDataSubId;

            btn.setText((isCurrent ? "✓ " : "") + label + " — " + carrier + number);
            btn.setTextSize(15);
            btn.setAllCaps(false);
            btn.setPadding(30, 30, 30, 30);

            if (isCurrent) {
                btn.setBackgroundColor(0xFF1E6E1E);
                btn.setTextColor(0xFFFFFFFF);
            } else {
                btn.setBackgroundColor(0xFF2A2A2A);
                btn.setTextColor(0xFFDDDDDD);
            }

            final int subId = info.getSubscriptionId();
            final String simName = label;
            btn.setOnClickListener(v -> setDataSim(subId, simName));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 20);
            root.addView(btn, lp);
        }

        statusText = new TextView(this);
        statusText.setText("");
        statusText.setTextColor(0xFF88CCFF);
        statusText.setTextSize(14);
        statusText.setPadding(0, 20, 0, 0);
        root.addView(statusText);

        setContentView(root);
    }

    private void setDataSim(int subId, String simName) {
        try {
            Method m = SubscriptionManager.class.getMethod("setDefaultDataSubId", int.class);
            m.invoke(null, subId);
            statusText.setText("✓ Data SIM set to: " + simName + "\n(You may need to grant MODIFY_PHONE_STATE via ADB — see README)");
            Toast.makeText(this, "Data SIM → " + simName, Toast.LENGTH_SHORT).show();
            recreate();
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage() + "\n\nGrant permission via ADB:\nadb shell pm grant com.abfstech.simswitcher android.permission.MODIFY_PHONE_STATE");
            Toast.makeText(this, "Permission needed — see instructions", Toast.LENGTH_LONG).show();
        }
    }
}
