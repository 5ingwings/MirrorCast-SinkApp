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
        try {
            this.body = body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
    public byte[] toByteArray() {
        StringBuilder sb = new StringBuilder();
        if (TextUtils.isEmpty(path)) {
            sb.append(methodType).append(" ").append(protocolVersion).append("\r\n");
        } else {
            sb.append(methodType).append(" ").append(path).append(" ").append(protocolVersion).append("\r\n");
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        if (this.body != null && this.body.length > 0) {
            sb.append("Content-Length: ").append(this.body.length).append("\r\n");
            sb.append("Content-type: ").append("text/parameters").append("\r\n");
        }
        sb.append("\r\n");

        byte[] data = null;
        try {
            byte[] h = sb.toString().getBytes("UTF-8");
            if (this.body == null) {
                data = h;
            } else {
                data = new byte[h.length + this.body.length];
                System.arraycopy(h, 0, data, 0, h.length);
                System.arraycopy(this.body, 0, data, h.length, this.body.length);
            }
        } catch (UnsupportedEncodingException e) {
            Logger.t(TAG).e("UnsupportedEncodingException:" + e.toString());

        }
        return data;
    }

}
