package cn.sihao.mirrorcast.player;


import android.widget.MediaController;

import cn.sihao.mirrorcast.widget.IjkVideoView.OnVideoStateListener;

public interface IPlayerControl extends MediaController.MediaPlayerControl {
    void setVolume(float volume);

    void stop();

    void setVideoPlayListener(OnVideoStateListener listener);
}

