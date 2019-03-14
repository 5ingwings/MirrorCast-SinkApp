package cn.sihao.mirrorcast

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import cn.sihao.mirrorcast.wifidirect.WiFiDirectMgr
import com.orhanobut.logger.Logger
import cn.sihao.mirrorcast.OnMirrorListener
import cn.sihao.mirrorcast.rtsp.RtspClient

class MiraMgr {

    private var mWiFiDirectMgr: WiFiDirectMgr? = null
    private var mMiraCastListener: OnMirrorListener? = null

    private var mMiraHandler: Handler? = null
    private var mRTSPClient: RtspClient? = null
    private var mMiraRunnable: Runnable? = null
    private var isStart: Boolean = false

    companion object {

        const val TAG = "MiraMgr"

        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MiraMgr()
        }
    }

    init {
        initMiraListener()
        initMiraRunnable()

        if (mWiFiDirectMgr == null) {
            mWiFiDirectMgr = WiFiDirectMgr(mMiraCastListener)
        }

        val mMiraHandlerThread = HandlerThread("miraMgrThread")
        mMiraHandlerThread.start()
        mMiraHandler = Handler(mMiraHandlerThread.looper)


    }

    public fun start(context: Context) {
        mWiFiDirectMgr?.start(context)

        if (!isStart && mMiraHandler != null) {
            mMiraHandler!!.post(mMiraRunnable)
        }
    }

    public fun stop() {
        isStart = false
        // RTSP关闭
        if (mRTSPClient != null) {
            mRTSPClient!!.stop()
            mRTSPClient = null
        }

        // 直接退房时清理数据
        /*if (mMirrorDataSource != null) {
            mMirrorDataSource.reset()
            mMirrorDataSource = null
        }*/

        // p2p发现关闭
        mWiFiDirectMgr?.stop()
    }

    private fun initMiraRunnable() {
        mMiraRunnable = Runnable {
            while (!isStart) {
                if (mWiFiDirectMgr == null) {
                    Logger.t(TAG).e("WiFiDirectMgr is null.")
                    return@Runnable
                }
                if (mWiFiDirectMgr!!.isGroupFormed) {
                    if (mRTSPClient != null && !mRTSPClient!!.isStopped) {
                        mRTSPClient!!.stop()
                        mRTSPClient = null
                    }

                    mRTSPClient = RtspClient(
                        "rtsp://" + mWiFiDirectMgr!!.sourceIp + "/",
                        mWiFiDirectMgr!!.sourcePort
                    )
                    mRTSPClient!!.setMirrorListener(mMiraCastListener)
                    mRTSPClient!!.start()

                    isStart = true
                    break
                }

                try {
                    Thread.sleep(2000)
                } catch (ignore: Exception) {
                }

                // Logger.t(TAG).d("next query RTSP connection state.");
            }
        }
    }

    private fun initMiraListener() {
        mMiraCastListener = object : OnMirrorListener {
            override fun onSessionEnd() {
                Logger.t(TAG).d("onSessionEnd.")
            }

            override fun onMirrorStart() {
                Logger.t(TAG).d("onMirrorStart.")
            }

            override fun onMirrorData(seqNum: Long, data: ByteArray?) {
                Logger.t(TAG).d("onMirrorData: seqNum:$seqNum, dataLength:${data?.size}")
            }

            override fun onMirrorStop() {
                Logger.t(TAG).d("onMirrorStop.")
            }

            override fun onSessionBegin() {
                Logger.t(TAG).d("onSessionBegin.")
            }
        }
    }

}
