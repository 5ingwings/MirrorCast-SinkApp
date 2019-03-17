package cn.sihao.mirrorcast.rtsp;

import android.text.TextUtils;
import android.util.SparseArray;
import com.orhanobut.logger.Logger;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class RtspResponseMessage extends RtspMessage {
    private static final String TAG = "RtspResponseMessage";

    static final SparseArray<String> RTSP_STATUS = new SparseArray<>();

    static {
        RtspResponseMessage.RTSP_STATUS.put(100, "100 Continue");
        RtspResponseMessage.RTSP_STATUS.put(101, "101 Switching Protocols");
        RtspResponseMessage.RTSP_STATUS.put(200, "200 OK");
        RtspResponseMessage.RTSP_STATUS.put(201, "201 Created");
        RtspResponseMessage.RTSP_STATUS.put(202, "202 Accepted");
        RtspResponseMessage.RTSP_STATUS.put(203, "203 Non-Authoritative Information");
        RtspResponseMessage.RTSP_STATUS.put(204, "204 No Content");
        RtspResponseMessage.RTSP_STATUS.put(205, "205 Reset Content");
        RtspResponseMessage.RTSP_STATUS.put(206, "206 Partial Content");
        RtspResponseMessage.RTSP_STATUS.put(300, "300 Multiple Choices");
        RtspResponseMessage.RTSP_STATUS.put(301, "301 Moved Permanently");
        RtspResponseMessage.RTSP_STATUS.put(302, "302 Found");
        RtspResponseMessage.RTSP_STATUS.put(303, "303 See Other");
        RtspResponseMessage.RTSP_STATUS.put(304, "304 Not Modified");
        RtspResponseMessage.RTSP_STATUS.put(305, "305 Use Proxy");
        RtspResponseMessage.RTSP_STATUS.put(306, "306 (Unused)");
        RtspResponseMessage.RTSP_STATUS.put(307, "307 Temporary Redirect");
        RtspResponseMessage.RTSP_STATUS.put(400, "400 Bad Request");
        RtspResponseMessage.RTSP_STATUS.put(401, "401 Unauthorized");
        RtspResponseMessage.RTSP_STATUS.put(402, "402 Payment Required");
        RtspResponseMessage.RTSP_STATUS.put(403, "403 Forbidden");
        RtspResponseMessage.RTSP_STATUS.put(404, "404 Not Found");
        RtspResponseMessage.RTSP_STATUS.put(405, "405 Method Not Allowed");
        RtspResponseMessage.RTSP_STATUS.put(406, "406 Not Acceptable");
        RtspResponseMessage.RTSP_STATUS.put(407, "407 Proxy Authentication Required");
        RtspResponseMessage.RTSP_STATUS.put(408, "408 Request Timeout");
        RtspResponseMessage.RTSP_STATUS.put(409, "409 Conflict");
        RtspResponseMessage.RTSP_STATUS.put(410, "410 Gone");
        RtspResponseMessage.RTSP_STATUS.put(411, "411 Length Required");
        RtspResponseMessage.RTSP_STATUS.put(412, "412 Precondition Failed");
        RtspResponseMessage.RTSP_STATUS.put(413, "413 Request Entity Too Large");
        RtspResponseMessage.RTSP_STATUS.put(414, "414 Request-URI Too Long");
        RtspResponseMessage.RTSP_STATUS.put(415, "415 Unsupported Media Type");
        RtspResponseMessage.RTSP_STATUS.put(416, "416 Requested Range Not Satisfiable");
        RtspResponseMessage.RTSP_STATUS.put(417, "417 Expectation Failed");
        RtspResponseMessage.RTSP_STATUS.put(500, "500 Internal Server Error");
        RtspResponseMessage.RTSP_STATUS.put(501, "501 Not Implemented");
        RtspResponseMessage.RTSP_STATUS.put(502, "502 Bad Gateway");
        RtspResponseMessage.RTSP_STATUS.put(503, "503 Service Unavailable");
        RtspResponseMessage.RTSP_STATUS.put(504, "504 Gateway Timeout");
        RtspResponseMessage.RTSP_STATUS.put(505, "505 HTTP Version Not Supported");
    }

    int statusCode;

    public RtspResponseMessage withStatus(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public RtspResponseMessage withHeader(String k, String v) {
        this.headers.put(k, v);
        return this;
    }

    public RtspResponseMessage withBody(String body) {
        this.bodyStr = body;
        return this;
    }


    @Override
    public byte[] toByteArray(Boolean isOnReceiveMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append(protocolVersion).append(" ")
                .append(RtspResponseMessage.RTSP_STATUS.get(statusCode)).append("\r\n");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        if (!isOnReceiveMessage) { // 若是封装发送信息则加上以下字段
            if (!TextUtils.isEmpty(this.bodyStr)) {
                sb.append("Content-Length: ").append(this.bodyStr.length()).append("\r\n");
                sb.append("Content-type: ").append("text/parameters").append("\r\n");
            }
        }
        sb.append("\r\n");

        byte[] data = null;
        try {
            byte[] h = sb.toString().getBytes("UTF-8");
            if (TextUtils.isEmpty(this.bodyStr)) {
                data = h;
            } else {
                byte[] bodyByte = this.bodyStr.getBytes("UTF-8");
                data = new byte[h.length + bodyByte.length];
                System.arraycopy(h, 0, data, 0, h.length);
                System.arraycopy(bodyByte, 0, data, h.length, bodyByte.length);
            }
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e("UnsupportedEncodingException:" + e.toString());
        }
        return data;
    }

}
