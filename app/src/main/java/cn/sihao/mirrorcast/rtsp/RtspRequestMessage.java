package cn.sihao.mirrorcast.rtsp;

import android.text.TextUtils;
import com.orhanobut.logger.Logger;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class RtspRequestMessage extends RtspMessage {
    private static final String TAG = "RtspRequestMessage";

    String methodType;
    String path;

    public RtspRequestMessage withMethod(String methodType) {
        this.methodType = methodType;
        return this;
    }

    public RtspRequestMessage withPath(String path) {
        this.path = path;
        return this;
    }

    public RtspRequestMessage withHeader(String k, String v) {
        this.headers.put(k, v);
        return this;
    }

    public RtspRequestMessage withBody(String body) {
        this.bodyStr = body;
        return this;
    }

    public RtspResponseMessage createResponse() {
        RtspResponseMessage response = new RtspResponseMessage();
        response.protocolVersion = this.protocolVersion;
        if (this.headers.containsKey("CSeq")) {
            response.headers.put("CSeq", this.headers.get("CSeq"));
        }
        return response;
    }

    @Override
    public byte[] toByteArray(Boolean isOnReceiveMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append(methodType).append(" ").append(path).append(" ").append(protocolVersion).append("\r\n");

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
