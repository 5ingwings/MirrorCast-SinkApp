package cn.sihao.mirrorcast.rtsp;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import cn.sihao.mirrorcast.OnMirrorListener;
import cn.sihao.mirrorcast.rtp.RTPServer;
import com.orhanobut.logger.Logger;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * RTSP client for miracast wifidirect protocol
 * reference: https://blog.csdn.net/u011897062/article/details/79445560
 * http://blog.ifjy.me/wifi/2016/07/16/miracast.html   notice: no "Line-based"
 */
public class RtspClient {
    private final static String TAG = "MiraRtspClient";

    private final static String METHOD_UDP = "udp";
    private final static String METHOD_TCP = "tcp";

    private final static int STATE_STARTED = 0x00;
    private final static int STATE_STARTING = 0x01;
    private final static int STATE_STOPPING = 0x02;
    private final static int STATE_STOPPED = 0x03;
    private final static int STATE_PLAYING = 0x04;
    private final static int STATE_PAUSE = 0x05;

    private final static String METHOD_OPTIONS = "OPTIONS *";
    private final static String METHOD_GET_PARAMETER = "GET_PARAMETER";
    private final static String METHOD_SET_PARAMETER = "SET_PARAMETER";
    private final static String METHOD_PLAY = "PLAY";
    private final static String METHOD_PAUSE = "PAUSE";
    private final static String METHOD_TEARDOWN = "TEARDOWN";
    private final static String METHOD_SETUP = "SETUP";

    private final static String KEY_RTSP_VERSION = "RTSP/1.0";
    private final static String KEY_DATE = "Date";
    private final static String KEY_REQUIRE = "Require";
    private final static String KEY_SESSION = "Session";
    private final static String KEY_CSEQ = "CSeq";
    private final static String KEY_TRANSPORT = "Transport";
    private final static String KEY_PUBLIC = "Public";
    private final static String KEY_WFD_TRIGGER_METHOD = "wfd_trigger_method";
    private final static String KEY_WFD_AUDIO_CODECS = "wfd_audio_codecs";
    private final static String KEY_WFD_VIDEO_FORMATS = "wfd_video_formats";


    private final static int MAX_CONN_TIME = 5;

    private Handler mHandler;

    private int mCurConnTime = 0;
    private RtspSocket mRtspSocket;
    private int mRtpPort;

    private RtspParameters mRtspParams;

    private int mCurState;

    private RTPServer mRtpServer;

    private String audioCodecs = "";
    private String videoFormats = "";

    private OnMirrorListener mMirrorListener;

    /**
     * 传入地址，需以rtsp://开头，如果只有IP，需以/结尾，如rtsp://xxx.xxx.xxx.xxx/
     * 支持地址后加入端口地址，"rtsp://ip:port/xxx"
     * 如未加入端口地址，或将其作为参数传入，则使用默认地址7236
     * 如不传入method 默认为udp
     */
    public RtspClient(String method, String address) {
        String url = address.substring(address.indexOf("//") + 2);
        url = url.substring(0, url.indexOf("/"));
        String[] tmp = url.split(":");
        if (tmp.length == 1) {
            initClientConfig(method, tmp[0], address, 7236);
        } else if (tmp.length == 2) {
            initClientConfig(method, tmp[0], address, Integer.parseInt(tmp[1]));
        }
        initialHandler();
    }

    public RtspClient(String address, int port) {
        String host = address.substring(address.indexOf("//") + 2);
        host = host.substring(0, host.indexOf("/"));
        initClientConfig("udp", host, address, port);
        initialHandler();
    }

    public RtspClient(String method, String address, int port) {
        String host = address.substring(address.indexOf("//") + 2);
        host = host.substring(0, host.indexOf("/"));
        initClientConfig(method, host, address, port);
        initialHandler();
    }

    public void setMirrorListener(OnMirrorListener listener) {
        mMirrorListener = listener;
    }

    public void start() {
        if (!isStopped()) return;
        mHandler.post(startConnectRunnable);
    }

    public void pause() {
        if (isPlaying()) {
            sendRequestPause();
        }
    }

    public void play() {
        if (isPause()) {
            sendRequestPlay();
        }
    }

    public void stop() {
        sendRequestTeardown();
        cleanResource();
    }

    public boolean isStarted() {
        return mCurState == STATE_STARTED;
    }

    public boolean isStarting() {
        return mCurState == STATE_STARTING;
    }

    public boolean isStopped() {
        return mCurState == STATE_STOPPED;
    }

    public boolean isStopping() {
        return mCurState == STATE_STOPPING;
    }

    public boolean isPause() {
        return mCurState == STATE_PAUSE;
    }

    public boolean isPlaying() {
        return mCurState == STATE_PLAYING;
    }


    /**
     * @param host    host
     * @param address host+path
     * @param port    port
     */
    private void initClientConfig(String method, String host, String address, int port) {

        mCurState = STATE_STOPPED;

        mRtspParams = new RtspParameters();
        mRtspParams.cSeq = 0;
        if (method.equalsIgnoreCase(METHOD_UDP)) {
            mRtspParams.isTCPTranslate = false;
        } else if (method.equalsIgnoreCase(METHOD_TCP)) {
            mRtspParams.isTCPTranslate = true;
        }
        mRtspParams.host = host;
        mRtspParams.port = port;
        mRtspParams.session = "";
        mRtspParams.address = address.substring(7);
    }

    private void initialHandler() {
        final Semaphore signal = new Semaphore(0);
        HandlerThread rtspClientThread = new HandlerThread("rtspClientThread") {
            protected void onLooperPrepared() {
                mHandler = new Handler();
                signal.release();
            }
        };

        rtspClientThread.start();
        signal.acquireUninterruptibly();
    }

    private Runnable startConnectRunnable = new Runnable() {
        @Override
        public void run() {
            tryConnect();
        }
    };


    private void tryConnect() {
        try {
            if (mCurConnTime >= MAX_CONN_TIME) {
                Logger.t(TAG).d("try connect to the RTSP Socket " + MAX_CONN_TIME + " time failed.");
                return;
            }

            Logger.t(TAG).d("start try to connect the RTSP Socket,"
                    + "socket host:" + mRtspParams.host + ", port:" + mRtspParams.port);

            mCurConnTime++;

            mCurState = STATE_STARTING;

            mRtspSocket = new RtspSocket(mRtspParams.host, mRtspParams.port);
            int ret = mRtspSocket.start(initialOnReceiveRTSPListener());
            if (ret == 0) {
                mCurState = STATE_STARTED;
            } else {
                mCurState = STATE_STOPPED;
                tryConnect();
            }
        } catch (Exception e) {
            mCurState = STATE_STOPPED;
            Logger.t(TAG).e("tryConnect Exception:" + e.toString());
        }
    }


    private OnReceiveRTSPListener initialOnReceiveRTSPListener() {
        return new OnReceiveRTSPListener() {
            @Override
            public void onRtspResponse(RtspResponseMessage rParams) {
                if (rParams.statusCode == 200) { // 200 ok
                    if (!TextUtils.isEmpty(rParams.headers.get(KEY_SESSION)) && !TextUtils.isEmpty(rParams.headers.get(KEY_PUBLIC))) {
                        // source -> sink M6 response
                        mRtspParams.session = rParams.headers.get(KEY_SESSION);
                        sendRequestPlay();
                        return;
                    }
                    // source -> sink M2/M7 response or TEARDOWN/PAUSE/PLAY OK response
                } else {
                    Logger.t(TAG).e("onRtspResponse failed, reason:" + RtspResponseMessage.RTSP_STATUS.get(rParams.statusCode));
                }
            }

            @Override
            public void onRtspRequest(RtspRequestMessage rParams) {
                try {
                    if (TextUtils.isEmpty(rParams.headers.get(KEY_CSEQ))) {
                        Logger.t(TAG).e("Cseq is null.");
                        return;
                    }
                    mRtspParams.cSeq = Integer.parseInt(rParams.headers.get(KEY_CSEQ));

                    if (!TextUtils.isEmpty(rParams.methodType)) {
                        switch (rParams.methodType) {
                            case METHOD_OPTIONS: {
                                // 开启RTP SOCKET
                                startRTPReceiver();
                                sendResponseM1();
                                sendRequestM2();
                                break;
                            }
                            case METHOD_GET_PARAMETER: {
                                if (rParams.body == null || rParams.body.length <= 0) { // PLAY后 接收 source->sink 的不带body的GET_PARAMETER信息 用于keep alive
                                    sendResponseOK();
                                } else {
                                    sendResponseM3();
                                }
                                break;
                            }
                            case METHOD_SET_PARAMETER: {
                                if (TextUtils.isEmpty(rParams.bodyMap.get(KEY_WFD_TRIGGER_METHOD))) {  // source->sink M4 request
                                    if (!TextUtils.isEmpty(rParams.bodyMap.get(KEY_WFD_AUDIO_CODECS))) {
                                        audioCodecs = rParams.bodyMap.get(KEY_WFD_AUDIO_CODECS);
                                    }
                                    if (!TextUtils.isEmpty(rParams.bodyMap.get(KEY_WFD_VIDEO_FORMATS))) {
                                        videoFormats = rParams.bodyMap.get(KEY_WFD_VIDEO_FORMATS);
                                    }
                                    sendResponseOK();
                                } else {
                                    if (rParams.bodyMap.get(KEY_WFD_TRIGGER_METHOD).equals(METHOD_SETUP)) { // source->sink M5 request
                                        sendResponseOK();
                                        sendRequestM6();
                                    }
                                    if (rParams.bodyMap.get(KEY_WFD_TRIGGER_METHOD).equals(METHOD_TEARDOWN)) { // source->sink TearDown request
                                        sendResponseTeardown();
                                        cleanResource();
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.t(TAG).e("Exception:" + e.toString());
                }
            }
        };
    }

    private void cleanResource() {
        try {
            mCurState = STATE_STOPPED;

            mMirrorListener = null;

            mHandler.removeCallbacks(startConnectRunnable);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mRtpServer != null) {
                        Logger.t(TAG).d("stop RTP&RTCP socket.");
                        mRtpServer.stop();
                    }
                    if (mRtspSocket != null) {
                        Logger.t(TAG).d("stop RTSP socket.");
                        mRtspSocket.close();
                    }
                }
            }, 1000);
        } catch (Exception e) {
            Logger.t(TAG).d("RTSP stop() Exception: " + e.toString());
        }
    }

    private void sendResponseM1() {
        RtspResponseMessage rm = new RtspResponseMessage();
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.statusCode = 200;
        rm.headers = addCommonHeader();
        String date = "yyyy HH:mm:ss z";
        SimpleDateFormat sdf = new SimpleDateFormat(date);
        rm.headers.put(KEY_DATE, sdf.format(new Date()));
        rm.headers.put(KEY_PUBLIC, "org.wfa.wfd1.0, GET_PARAMETER, SET_PARAMETER");
        mRtspSocket.sendResponse(rm);
    }

    private void sendRequestM2() {
        RtspRequestMessage rm = new RtspRequestMessage();
        rm.methodType = METHOD_OPTIONS;
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.headers = addCommonHeader();
        rm.headers.put(KEY_REQUIRE, "org.wfa.wfd1.0");
        mRtspSocket.sendRequest(rm);
    }

    private void sendResponseM3() {
        RtspResponseMessage rm = new RtspResponseMessage();
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.statusCode = 200;
        rm.headers = addCommonHeader();
        String m3Body = "wfd_audio_codecs: " + getWfdAudioCodecs() + "\r\n" +
                "wfd_video_formats: " + getWfdVideoFormats() + "\r\n" +
                "wfd_uibc_capability: input_category_list=HIDC;hidc_cap_list=Keyboard/USB, Mouse/USB, MultiTouch/USB, Gesture/USB, RemoteControl/USB;port=none\r\n" +
                "wfd_client_rtp_ports: RTP/AVP/" + (mRtspParams.isTCPTranslate ? "TCP" : "UDP") + ";unicast " + mRtpPort + " 0 mode=play\r\n" +
                "wfd_content_protection: none\r\n";
        try {
            rm.body = m3Body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e("UnsupportedEncodingException:" + e.toString());
        }
        mRtspSocket.sendResponse(rm);
    }

    /**
     * sink->source M4/M5/KeepAlive response
     */
    private void sendResponseOK() {
        RtspResponseMessage rm = new RtspResponseMessage();
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.statusCode = 200;
        rm.headers = addCommonHeader();
        mRtspSocket.sendResponse(rm);
    }

    private void sendResponseTeardown() {
        mCurState = STATE_STOPPING;
        // fixme 是直接回ok吧？
        String request = "TEARDOWN rtsp://" + mRtspParams.host + "/wfd1.0/streamid=0 RTSP/1.0\r\n" + addCommonHeader();
        mRtspSocket.sendRtspData(request);
    }

    private void sendRequestM6() {
        RtspRequestMessage rm = new RtspRequestMessage();
        rm.methodType = METHOD_SETUP;
        rm.path = "rtsp://" + mRtspParams.host + "/wfd1.0/streamid=0";
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.headers = addCommonHeader();
        rm.headers.put(KEY_TRANSPORT, "RTP/AVP/" + (mRtspParams.isTCPTranslate ? "TCP;" : "UDP;") + "unicast;client_port=" + mRtpPort);
        mRtspSocket.sendRequest(rm);
    }

    private void sendRequestPlay() {
        mCurState = STATE_PLAYING;

        RtspRequestMessage rm = new RtspRequestMessage();
        rm.methodType = METHOD_PLAY;
        rm.path = "rtsp://" + mRtspParams.host + "/wfd1.0/streamid=0";
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.headers = addCommonHeader();
        rm.headers.put(KEY_SESSION, mRtspParams.session);
        mRtspSocket.sendRequest(rm);
    }


    private void sendRequestTeardown() {
        // fixme 是直接TEARDOWN作method吧
        mCurState = STATE_STOPPING;

        RtspRequestMessage rm = new RtspRequestMessage();
        rm.methodType = METHOD_SET_PARAMETER;
        rm.path = "rtsp://" + mRtspParams.host + "/wfd1.0/streamid=0";
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.headers = addCommonHeader();
        rm.headers.put(KEY_SESSION, mRtspParams.session);
        try {
            rm.body = "wfd_trigger_method: TEARDOWN".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e("UnsupportedEncodingException:" + e.toString());
        }
        mRtspSocket.sendRequest(rm);
    }

    private void sendRequestPause() {
        mCurState = STATE_PAUSE;
        // fixme 是method打头还是在set_Parameter后
        RtspRequestMessage rm = new RtspRequestMessage();
        rm.methodType = METHOD_PAUSE;
        rm.path = "rtsp://" + mRtspParams.host + "/wfd1.0/streamid=0";
        rm.protocolVersion = KEY_RTSP_VERSION;
        rm.headers = addCommonHeader();
        mRtspSocket.sendRequest(rm);
    }

    private HashMap<String, String> addCommonHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(KEY_CSEQ, String.valueOf(mRtspParams.cSeq++));
        return headers;
    }

    private String getWfdAudioCodecs() {
        return "LPCM 00000002 00, AAC 00000001 00";
    }

    private String getWfdVideoFormats() {
        return "78 00 01 01 00008400 00000000 00000000 00 0000 0000 00 none none";
    }

    private void startRTPReceiver() {
        mRtpServer = new RTPServer(mMirrorListener);
        mRtpServer.start();
        mRtpPort = mRtpServer.getRtpPort();
        Logger.t(TAG).d("Start to connect the RTP Server. RTP Port is: " + mRtpPort);
    }

    private class RtspParameters {
        int cSeq = 0;
        Boolean isTCPTranslate = false;
        String host = "";
        String address = "";
        int port = 7236;
        String session = "";
    }

}
