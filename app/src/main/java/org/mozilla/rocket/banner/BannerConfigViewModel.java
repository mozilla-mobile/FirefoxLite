package org.mozilla.rocket.banner;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BannerConfigViewModel extends ViewModel {

    private MutableLiveData<String[]> bannerConfig;

    public MutableLiveData<String[]> getConfig() {
        if (bannerConfig == null) {
            bannerConfig = new MutableLiveData<>();
        }
        return bannerConfig;
    }


}
