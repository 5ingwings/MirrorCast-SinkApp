package cn.sihao.mirrorcast.rtsp;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;

abstract class RtspMessage {
    HashMap<String, String> headers = new HashMap<>();
    HashMap<String, String> bodyMap = new HashMap<>();

    String bodyStr;
    String protocolVersion;

    public String toStringMsg(Boolean isOnReceiveMessage) {
        String str = "Failed to format the message";
        try {
            str = new String(this.toByteArray(isOnReceiveMessage), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * @param isOnReceiveMessage true为封装回传的信息 false为封装发送的信息
     * @return byte数组
     */
    abstract public byte[] toByteArray(Boolean isOnReceiveMessage);

}
