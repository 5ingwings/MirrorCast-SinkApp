package cn.sihao.mirrorcast.rtsp;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

abstract class RtspMessage {
    HashMap<String, String> headers = new HashMap<>();
    HashMap<String, String> bodyMap = new HashMap<>();

    byte[] body;
    String protocolVersion;
    @NonNull
    @Override
    public String toString() {
        String str = "Failed to format the message";
        try {
            str = new String(this.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    abstract public byte[] toByteArray();
}