package cn.sihao.mirrorcast.rtsp;

/**
 * 接收Source->sink的request/response
 */
public interface OnReceiveRTSPListener {

    void onRtspRequest(RtspRequestMessage request);

    void onRtspResponse(RtspResponseMessage response);

}