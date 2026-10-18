package com.fgsqw.lanshare.player;

import android.media.MediaPlayer;

/** 网络变化回调。 */
public interface NetworkListener {
    void onWifi(MediaPlayer mediaPlayer);

    void onMobile(MediaPlayer mediaPlayer);

    void onUnavailable(MediaPlayer mediaPlayer);
}
