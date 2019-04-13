package cn.sihao.mirrorcast.player;

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import cn.sihao.mirrorcast.widget.IjkVideoView;

/**
 * 视频播放控制器（控制播放+播放的信息获取并回调给发送端）
 */
public class PlayerControlManager {
    private static final PlayerControlManager ourInstance = new PlayerControlManager();

    public static PlayerControlManager getInstance() {
        return ourInstance;
    }

    private PlayerControlManager() {
    }

    private Context mAppContext;
    private Handler mMainHandler;
    private float mStoredVolume;
    private IPlayerControl mPlayerController;

    private boolean mIsStopped = false;

    private IjkVideoView.OnVideoStateListener mVideoStateListener
            = new IjkVideoView.OnVideoStateListener() {
        @Override
        public void onStart() {
            mIsStopped = false;
        }

        @Override
        public void onPause() {

        }

        @Override
        public void onStop() {
            mIsStopped = true;

        }

        @Override
        public void onEndOfMedia() {

        }

        @Override
        public void onPositionChanged(int position) {

        }

        @Override
        public void onDurationChanged(int duration) {

        }

        @Override
        public void onError(int frameworkError, int implError) {

        }
    };

    public void init(Context context) {
        mAppContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void setPlayerController(IPlayerControl controller) {
        mPlayerController = controller;
        mPlayerController.setVideoPlayListener(mVideoStateListener);
    }

    synchronized public void setVolume(float volume) {
        mStoredVolume = getVolume();
        if (mPlayerController != null) {
            mPlayerController.setVolume(volume);
        }
    }

    synchronized public void setMute(boolean desiredMute) {
        if (desiredMute && getVolume() > 0) {
            setVolume(0);
        } else if (!desiredMute && getVolume() == 0) {
            setVolume(mStoredVolume);
        }
    }

    public float getStoredVolume() {
        return mStoredVolume;
    }

    public float getVolume() {
        AudioManager audioManager = (AudioManager) mAppContext.getSystemService(Service.AUDIO_SERVICE);
        if (audioManager != null) {
            float volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            return volume / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 1.0f;
    }

    public void start() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayerController != null) {
                    mPlayerController.start();
                }
                mIsStopped = false;
            }
        });
    }

    public void stop() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayerController != null && !mIsStopped) {
                    mPlayerController.stop();
                    mIsStopped = true;
                }
            }
        });

    }

    public void seek(final int position) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayerController != null) {
                    mPlayerController.seekTo(position);
                }
            }
        });
    }

    public void pause() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayerController != null) {
                    mPlayerController.pause();
                }
            }
        });
    }

}
