package cn.sihao.mirrorcast

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import cn.sihao.mirrorcast.wifidirect.WiFiDirectMgr
import com.orhanobut.logger.Logger
import cn.sihao.mirrorcast.OnMirrorListener
import cn.sihao.mirrorcast.player.BytesMediaDataSource
import cn.sihao.mirrorcast.player.MiraPlayer
import cn.sihao.mirrorcast.rtsp.RtspClient

class MiraMgr {
    private var mWiFiDirectMgr: WiFiDirectMgr? = null
    private var mMiraCastListener: OnMirrorListener? = null

    private var mMiraHandler: Handler? = null
    private var mRTSPClient: RtspClient? = null

    private var mWiFiDirectRunnable: Runnable? = null
    private var mRtspRunnable: Runnable? = null

    private var mContext: Context? = null

    private var mMirrorDataSource: BytesMediaDataSource? = null

    private var mMiraCastPlayer: MiraPlayer? = null


    companion object {

        const val TAG = "MiraMgr"

        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MiraMgr()
        }
    }

    init {
        initMiraListener()

        if (mWiFiDirectMgr == null) {
            mWiFiDirectMgr = WiFiDirectMgr(mMiraCastListener)
        }

        initWiFiDirectRunnable()
        initRtspRunnable()

        val mMiraHandlerThread = HandlerThread("miraMgrThread")
        mMiraHandlerThread.start()
        mMiraHandler = Handler(mMiraHandlerThread.looper)
    }

    public fun start(context: Context) {
        this.mContext = context
        if (mMiraHandler != null) {
            mMiraHandler!!.post(mWiFiDirectRunnable)
        }
        if (mMiraCastPlayer == null) {
            mMiraCastPlayer = MiraPlayer(mContext)
        }
    }

    public fun stop() {
        // RTSP关闭
        if (mRTSPClient != null) {
            mRTSPClient!!.stop()
            mRTSPClient = null
        }

        // 直接退房时清理数据
        if (mMirrorDataSource != null) {
            mMirrorDataSource?.reset()
            mMirrorDataSource = null
        }

        // p2p发现关闭
        mWiFiDirectMgr?.stop()
        mMiraHandler?.removeCallbacks(mWiFiDirectRunnable)
        mMiraHandler?.removeCallbacks(mRtspRunnable)

        CastingActivity.finishThis(mContext)
    }

    private fun initWiFiDirectRunnable() {
        mWiFiDirectRunnable = Runnable {
            if (mWiFiDirectMgr == null) {
                Logger.t(TAG).e("WiFiDirectMgr is null.")
                return@Runnable
            }
            mWiFiDirectMgr!!.start(mContext)
        }
    }

    private fun initRtspRunnable() {
        mRtspRunnable = Runnable {
            if (!mWiFiDirectMgr!!.isGroupFormed) {
                Logger.t(TAG).d("wifi p2p group is not formed,try again.")
                mMiraHandler?.postDelayed(mRtspRunnable, 1000L)
            }

            if (mWiFiDirectMgr!!.isGroupFormed) {
                if (mRTSPClient != null && !mRTSPClient!!.isStopped) {
                    mRTSPClient!!.stop()
                    mRTSPClient = null
                }

                mRTSPClient = RtspClient(
                        "rtsp://" + mWiFiDirectMgr?.sourceIp + "/",
                        mWiFiDirectMgr!!.sourcePort
                )
                mRTSPClient!!.setMirrorListener(mMiraCastListener)
                mRTSPClient!!.start()
            }
        }
    }


    private fun initMiraListener() {
        mMiraCastListener = object : OnMirrorListener {
            override fun onSessionBegin() {
                mMiraHandler?.post(mRtspRunnable)
                mMirrorDataSource = null
                Logger.t(TAG).d("onSessionBegin.")
            }

            override fun onSessionEnd() {
                mMiraHandler?.removeCallbacks(mRtspRunnable)
                if (mMirrorDataSource != null) {
                    mMirrorDataSource?.reset()
                    mMirrorDataSource = null
                    mMiraCastPlayer?.stop()
                }
                Logger.t(TAG).d("onSessionEnd.")
            }

            override fun onMirrorStart() {
                Logger.t(TAG).d("onMirrorStart.")
            }

            override fun onMirrorStop() {
                Logger.t(TAG).d("onMirrorStop.")
            }

            override fun onMirrorData(seqNum: Long, data: ByteArray?) {
                if (mMirrorDataSource == null) {
                    Logger.t(TAG).d("on mirror first data come.")
                    mMirrorDataSource = BytesMediaDataSource()
                    mMiraCastPlayer?.setVideoSource(mMirrorDataSource)
                }

                if (mMirrorDataSource != null) {
                    mMirrorDataSource?.putNewData(data)
                }
//                Logger.t(TAG).d("onMirrorData: seqNum:$seqNum, dataLength:${data?.size}")
            }
        }
    }

}
