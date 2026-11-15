package com.vida.missions;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class AddMissionActivity extends Activity {
    private final TextView[] chips = new TextView[4];
    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        final EditText title = findViewById(R.id.in_title);
        final EditText obj = findViewById(R.id.in_obj);
        final EditText steps = findViewById(R.id.in_steps);

        int[] ids = {R.id.chip_main, R.id.chip_side, R.id.chip_daily, R.id.chip_event};
        for (int i = 0; i < ids.length; i++) {
            final int idx = i;
            chips[i] = findViewById(ids[i]);
            chips[i].setOnClickListener(v -> select(idx));
        }
        select(MissionStore.getTab(this));

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_accept).setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            if (t.isEmpty()) {
                title.setError("Required");
                return;
            }
            int n = 1;
            try {
                n = Integer.parseInt(steps.getText().toString().trim());
            } catch (NumberFormatException ignored) {
            }
            n = Math.max(1, Math.min(99, n));

            MissionStore.add(this, t, obj.getText().toString().trim(), selected, n);
            MissionStore.setTab(this, selected);
            MissionWidget.refresh(this);
            finish();
        });
    }

    /** The selected chip is "disabled" so its selector shows the gold pill. */
    private void select(int idx) {
        selected = idx;
        for (int i = 0; i < chips.length; i++) {
            chips[i].setEnabled(i != idx);
        }
    }
}
