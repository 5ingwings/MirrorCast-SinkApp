package cn.sihao.mirrorcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.content.LocalBroadcastManager
import cn.sihao.mirrorcast.player.MyAndroidMediaController
import cn.sihao.mirrorcast.player.BytesMediaDataSource
import cn.sihao.mirrorcast.player.PlayerControlManager
import cn.sihao.mirrorcast.widget.IjkVideoView
import com.orhanobut.logger.Logger
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class CastingActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CastingActivity"
        @JvmField
        public val PLAYING_TYPE_IJKURI = 0
        @JvmField
        public val PLAYING_TYPE_IJKPLAYER_STREAM = 1
        @JvmField
        public val PLAYING_TYPE_WEBVIEW = 2

        private var mPlayingType = PLAYING_TYPE_IJKURI

        private var mPlaySource: BytesMediaDataSource? = null

        @JvmStatic
        fun setPlaySource(playSource: BytesMediaDataSource) {
            mPlaySource = playSource
        }

        @JvmStatic
        fun updatePlaySource(context: Context, playSource: BytesMediaDataSource) {
            mPlaySource = playSource
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("change_codec"))
        }

        @JvmStatic
        fun finishThis(context: Context?) {
            if (null != context) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("close_video"))
            }
        }
    }

    private var mIsCloseable: Boolean = false

    private var mWakeLock: PowerManager.WakeLock? = null

    private var mIjkVideoView: IjkVideoView? = null

    private var mMyMediaController: MyAndroidMediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.t(TAG).d("onCreate.")
        setContentView(R.layout.activity_casting)

        Logger.t(TAG).d("playType:" + intent.getStringExtra("playType"))

        initIjkPlayer()
        initIjkPlayerView()
        registerBroadcast()
    }

    override fun onStart() {
        super.onStart()
        Logger.t(TAG).d("onStart.")
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (mWakeLock == null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.FULL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                    "mira:wakelock")
            mWakeLock?.acquire()
        }
    }

    override fun onStop() {
        super.onStop()

        releaseIjkPlayer()

        if (mWakeLock != null && mWakeLock!!.isHeld) {
            mWakeLock?.release()
            mWakeLock = null
        }
        Logger.t(TAG).d("onStop.")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcast()
        Logger.t(TAG).d("onDestroy.")
    }


    private fun initIjkPlayer() {
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_ERROR)
    }


    private fun initIjkPlayerView() {
        mIjkVideoView = findViewById(R.id.video_view)
        PlayerControlManager.getInstance().setPlayerController(mIjkVideoView)

        mMyMediaController = MyAndroidMediaController(this, true)
        mIjkVideoView?.setMediaController(mMyMediaController)

        mIjkVideoView?.setVideoSource(mPlaySource)

        mIjkVideoView?.start()
    }

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private fun registerBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("close_video")
        intentFilter.addAction("change_codec")
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent == null || intent.action == null) {
                    return
                }

                if (intent.action == "close_video") {
                    Logger.t(TAG).d("finish Video Playing Activity.")
                    mIsCloseable = true
                    finish()
                } else if (intent.action == "change_codec") {
                    changeStreamVideoCodec()
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver!!, intentFilter)
    }

    private fun unregisterBroadcast() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver!!)
        mBroadcastReceiver = null
    }

    private fun changeStreamVideoCodec() {
        mIjkVideoView?.setVideoSource(mPlaySource)
        mIjkVideoView?.start()
    }

    private fun releaseIjkPlayer() {
        if (mIsCloseable || !mIjkVideoView!!.isBackgroundPlayEnabled) {
            mIjkVideoView?.stop()
            mIjkVideoView?.release(true)
            IjkMediaPlayer.native_profileEnd()
        }
    }


}
