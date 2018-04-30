package org.mozilla.rocket.component;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.components.ComponentToggleService;

public class ConfigActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(getApplicationContext(), ComponentToggleService.class));

        final Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finishAndRemoveTask();
    }
}
