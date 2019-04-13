package cn.sihao.mirrorcast.player;

import android.content.Context;
import android.content.Intent;

import cn.sihao.mirrorcast.CastingActivity;

// 操作播放器播放
public class MiraPlayer {
    private final static String TAG = "MiraPlayer";
    private Context mContext;

    public MiraPlayer(Context context) {
        mContext = context;
        PlayerControlManager.getInstance().init(context);
    }


    synchronized public void setVideoSource(BytesMediaDataSource mediaDataSource) {
        CastingActivity.setPlaySource(mediaDataSource);
        Intent intent = new Intent();
        intent.setClass(mContext, CastingActivity.class);
        intent.putExtra("playType", CastingActivity.PLAYING_TYPE_IJKPLAYER_STREAM);
        mContext.startActivity(intent);
    }

    synchronized public void updateVideoSource(BytesMediaDataSource dataSource) {
        CastingActivity.updatePlaySource(mContext, dataSource);
    }

    synchronized public void setVolume(float volume) {
        PlayerControlManager.getInstance().setVolume(volume);
    }

    synchronized public void setMute(boolean desiredMute) {
        PlayerControlManager.getInstance().setMute(desiredMute);
    }

    public float getVolume() {
        return PlayerControlManager.getInstance().getVolume();
    }

    public void play() {
        PlayerControlManager.getInstance().start();
    }

    public void pause() {
        PlayerControlManager.getInstance().pause();
    }

    public void stop() {
        PlayerControlManager.getInstance().stop();
    }

    public void seek(int position) {
        PlayerControlManager.getInstance().seek(position);
    }

}
