package org.mozilla.rocket.banner;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class BannerConfigViewModel extends ViewModel {

    private MutableLiveData<String[]> bannerConfig;

    public MutableLiveData<String[]> getConfig() {
        if (bannerConfig == null) {
            bannerConfig = new MutableLiveData<>();
        }
        return bannerConfig;
    }


}
