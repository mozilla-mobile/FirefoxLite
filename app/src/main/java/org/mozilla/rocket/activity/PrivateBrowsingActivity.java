package org.mozilla.rocket.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;

public class PrivateBrowsingActivity extends LocaleAwareAppCompatActivity implements View.OnClickListener {

    View backBtn;

    @Override
    protected void onResume() {
        super.onResume();

        backBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pb_bounce));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_browsing);

        backBtn = findViewById(R.id.btn_tab_tray);
        backBtn.setOnClickListener(this);

        int visibility = getWindow().getDecorView().getSystemUiVisibility();
        // do not overwrite existing value
        visibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(visibility);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_tab_tray:
                pushToBack();
                break;
            default:
                break;

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        pushToBack();
    }

    private void pushToBack() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, R.anim.pb_exit);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void applyLocale() {

    }
}
