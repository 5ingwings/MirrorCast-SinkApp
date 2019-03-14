package cn.sihao.mirrorcast.rtp;

import android.util.Log;
import cn.sihao.mirrorcast.OnMirrorListener;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class RTPServer {
    private static final String TAG = "MiraRTPServer";

    private UDPServer rtpSession;
    private UDPServer rtcpSession;
    private OnMirrorListener mMiraCastListener;

    public RTPServer(OnMirrorListener listener) {
        mMiraCastListener = listener;
    }

    public void start() {
        rtpSession = new UDPServer(1024 * 1024, 0, mMiraCastListener);
        rtpSession.start();
        Log.i(TAG, "RTP session is running on port " + rtpSession.mPort);

        rtcpSession = new UDPServer(0, null);
        rtcpSession.start();
        Log.i(TAG, "RTCP session is running on port " + rtcpSession.mPort);
    }

    public void stop() {
        rtpSession.stop();
        rtcpSession.stop();
    }

    public int getRtpPort() {
        if (null != rtpSession) {
            return rtpSession.mPort;
        }

        return 0;
    }

    public int getRtcpPort() {
        if (null != rtcpSession) {
            return rtcpSession.mPort;
        }

        return 0;
    }

    public static int lastSeq = -1;

    public static void printRTPInfo(byte[] data) {
        int v = (data[0] & 0xC0) >> 6;     // 当前协议版本号为2
        int p = (data[0] & 0x20) >> 5;     // 如果P=1，则在该报文的尾部填充一个或多个额外的八位组，它们不是有效载荷的一部分
        int x = (data[0] & 0x10) >> 4;     // 如果X=1，则在RTP报头后跟有一个扩展报头
        int cc = data[0] & 0x0F;           // CSRC计数器，占4位，指示CSRC 标识符的个数

        int m = (data[1] & 0x80) >> 7;     // 占1位，不同的有效载荷有不同的含义，对于视频，标记一帧的结束；对于音频，标记会话的开始。
        int pt = data[1] & 0x7F;         // 说明RTP报文中有效载荷的类型

        int seqNum = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);  // 用于标识发送者所发送的RTP报文的序列号，每发送一个报文，序列号增1
        int timestamp = (data[4] & 0xFF) << 24 | (data[5] & 0xFF) << 16
                | (data[6] & 0xFF) << 8 | data[7] & 0xFF;       // 必须使用90kHz时钟频率,时戳反映了该RTP报文的第一个八位组的采样时刻
        int ssrc = (data[8] & 0xFF) << 24 | (data[9] & 0xFF) << 16
                | (data[10] & 0xFF) << 8 | data[11] & 0xFF;// 用于标识同步信源

        if (seqNum != lastSeq + 1) {
            Logger.t(TAG).d("[%02X] v:%d p:%d x:%d cc:%d m:%d pt:%d seq_num:%d timestamp:%d ssrc:%d",
                    data[0], v, p, x, cc, m, pt, seqNum, timestamp, ssrc);
        }
        lastSeq = seqNum;
    }

    private static long getMirrorSeqNum(byte[] data) {
        return ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
    }

    /**
     * Represents the UDP server.
     */
    static class UDPServer {
        private OnMirrorListener mListener;

        class Worker extends Thread {
            public static final int MTU = 10 * 1024;
            private byte mReceiveBuffer[] = new byte[MTU];

            public Worker() {
            }

            public void run() {
                DatagramPacket packet = new DatagramPacket(mReceiveBuffer, MTU);
                while (null != mSocket && !mSocket.isClosed()) {
                    try {
                        mSocket.receive(packet);
                        if (mListener != null) {
                            byte[] raw = packet.getData();
                            byte[] data = new byte[packet.getLength() - 12];
                            System.arraycopy(raw, 12, data, 0, data.length);
                            mListener.onMirrorData(getMirrorSeqNum(raw), data);
                        }
                    } catch (IOException e) {
                        Logger.t(TAG).e("IOException " + e.toString());
                        if (e.toString().contains("Socket closed")) {
                            break;
                        }
                    }
                }
            }
        }

        private int mPort = 0;
        private Worker mWorker;
        private DatagramSocket mSocket = null;
        private int mSocketBufferSize = 0;

        UDPServer(int bufferSize, int port, OnMirrorListener listener) {
            init(bufferSize, port, listener);
        }

        UDPServer(int port, OnMirrorListener listener) {
            init(0, port, listener);
        }

        private void init(int bufferSize, int port, OnMirrorListener listener) {
            mSocketBufferSize = bufferSize;
            mListener = listener;
            try {
                mSocket = new DatagramSocket(port);
                if (mSocketBufferSize > 0) {
                    mSocket.setReceiveBufferSize(mSocketBufferSize);
                }
                if (mSocket.isBound()) {
                    mPort = mSocket.getLocalPort();
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            mWorker = new Worker();
        }

        public void start() {
            mWorker.start();
        }

        public void stop() {
            try {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
                mWorker.join();
            } catch (Exception e) {
                Logger.t(TAG).e("udp server stop Exception " + e.toString());
            }
        }
    }
}
