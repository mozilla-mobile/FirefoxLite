package org.mozilla.focus.navigation;

import android.view.animation.Animation;

public interface FragmentAnimationAccessor {
    Animation getCustomEnterTransition();
    Animation getCustomExitTransition();
}
