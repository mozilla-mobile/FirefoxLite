package org.mozilla.rocket.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.rocket.widget.BetterBounceInterpolator;

public class PrivateBrowsingActivity extends LocaleAwareAppCompatActivity implements View.OnClickListener {

    View backBtn;
    View logoman;

    @Override
    protected void onResume() {
        super.onResume();

        ScaleAnimation scaleAnimation = new ScaleAnimation(2.0f, 1.0f, 2.0f, 1.0f);
        Animation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.5f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -0.5f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        Animation alphaAnimation = new AlphaAnimation(0.66f, 1.0f);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new BetterBounceInterpolator(1, -0.8d));
        animationSet.setDuration(720);

        backBtn.startAnimation(animationSet);
        logoman.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pb_logoman));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_browsing);

        backBtn = findViewById(R.id.btn_tab_tray);
        backBtn.setOnClickListener(this);
        logoman = findViewById(R.id.logo_man);

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
