package cn.sihao.mirrorcast.player;


import android.content.Context;

public class IjkPlayerSetting {
    public static final int PV_PLAYER_AUTO = 0;
    public static final int PV_PLAYER_ANDROID_MEDIA_PLAYER = 1;
    public static final int PV_PLAYER_IJK_MEDIA_PLAYER = 2;
    public static final int PV_PLAYER_IJK_EXO_MEDIA_PLAYER = 3;

    private Context mAppContext;

    public IjkPlayerSetting(Context context) {
        mAppContext = context.getApplicationContext();
    }

    public boolean getEnableBackgroundPlay() {
        return true;
    }

    public int getPlayer() {
        return PV_PLAYER_IJK_MEDIA_PLAYER;
    }

    public boolean getUsingMediaCodec() {
        return true;
    }

    public boolean getUsingMediaCodecAutoRotate() {
        return false;
    }

    public boolean getMediaCodecHandleResolutionChange() {
        return false;
    }

    public boolean getUsingOpenSLES() {
        return false;
    }

    public String getPixelFormat() {
        return "";
    }

    public boolean getEnableNoView() {
        return false;
    }

    public boolean getEnableSurfaceView() {
        return true;
    }

    public boolean getEnableTextureView() {
        return false;
    }

    public boolean getEnableDetachedSurfaceTextureView() {
        return false;
    }

    public boolean getUsingMediaDataSource() {
        return false;
    }
}

