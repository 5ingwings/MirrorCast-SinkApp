package cn.sihao.mirrorcast.rtsp;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import com.orhanobut.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

class RtspSocket {
    private static final String TAG = "miraRtspSocket";
    private Socket mSocket;

    private String mRtspHost;
    private int mRtspPort;

    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private BufferedReader mBufferedReader;

    private Handler mHandler;
    private Thread mReceiveThread;

    private OnReceiveRTSPListener mReceiveRTSPListener;

    public RtspSocket(String host, int port) {
        mRtspHost = host;
        mRtspPort = port;
    }

    public int start(OnReceiveRTSPListener rListener) {
        mReceiveRTSPListener = rListener;

        if (TextUtils.isEmpty(mRtspHost)) {
            Logger.t(TAG).d("mRtspHost is null");
            return -1;
        }
        if (0 == mRtspPort) {
            Logger.t(TAG).d("mRtspPort is null");
            return -1;
        }
        mSocket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(mRtspHost, mRtspPort);
        try {
            mSocket.connect(socketAddress, 5000);
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
            mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));
        } catch (IOException e) {
            Logger.t(TAG).e("RTSPSocket start error:" + e.toString());
            return -1;
        }

        final Semaphore signal = new Semaphore(0);
        mReceiveThread = new HandlerThread("RTSPSocketThread") {
            protected void onLooperPrepared() {
                mHandler = new Handler();
                signal.release();
            }
        };
        mReceiveThread.start();
        signal.acquireUninterruptibly();

        if (!mSocket.isClosed()) {
            mHandler.post(receiveOperationRunnable);
        }
        return 0;
    }

    public void close() {
        try {
            mHandler.removeCallbacksAndMessages(null);
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
            mReceiveThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendRequest(RtspRequestMessage request) {
        try {
            Logger.t(TAG).d(">>>>>>>>>> RTSP Send Message:\r\n" +
                    request.toStringMsg(false) +
                    "<<<<<<<<<<");
            mOutputStream.write(request.toByteArray(false));
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendResponse(RtspResponseMessage response) {
        try {
            Logger.t(TAG).d(">>>>>>>>>> RTSP Send Message:\r\n" +
                    response.toStringMsg(false) +
                    "<<<<<<<<<<");
            mOutputStream.write(response.toByteArray(false));
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRtspData(String data) {
        try {
            mOutputStream.write(data.getBytes("UTF-8"));
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Runnable receiveOperationRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // Read start line
                String line = mBufferedReader.readLine();
                if (line != null && line.length() > 0) {
                    Logger.t(TAG).d("here: 7 line:" + line);
                    String cmd[] = line.split("\\s");
                    if (cmd.length == 3) {
                        RtspMessage message;
                        if (cmd[0].startsWith("RTSP/")) {
                            // This is a response message
                            RtspResponseMessage response = new RtspResponseMessage();
                            response.protocolVersion = cmd[0];
                            response.statusCode = Integer.parseInt(cmd[1]);
                            message = response;
                        } else {
                            // This is a request message
                            RtspRequestMessage request = new RtspRequestMessage();
                            request.methodType = cmd[0];
                            request.path = cmd[1];
                            request.protocolVersion = cmd[2];
                            message = request;
                        }

                        // Read headers
                        int contentLength = 0;
                        String header = mBufferedReader.readLine();
                        while (header.length() > 0) {
                            String ss[] = header.split(":\\s");
                            if (ss.length == 2) {
                                message.headers.put(ss[0], ss[1]);
                                if (ss[0].equalsIgnoreCase("Content-Length")) {
                                    contentLength = Integer.parseInt(ss[1]);
                                }
                            }
                            header = mBufferedReader.readLine();
                        }

                        // Read body
                        if (contentLength > 0) {
                            message.body = new byte[contentLength];
                            // read body as byte
                            // 注意长度和编码问题 content-Length 是字节数 而这里读的是字符 若含有中文 可能读取的内容比实际内容长 数组后面会有''(无内容）的元素
                            // mBufferedReader.read(message.body,0,contentLength);
                            mInputStream.read(message.body, 0, contentLength);
                            // read body as hashMap
                            String bodyStr = new String(message.body, "UTF-8");
                            // fixme max split 成功吗
                            String[] bodyLineArray = bodyStr.split("\r\n");
                            Logger.t(TAG).d("here:1 bodyLineArray length: " + bodyLineArray.length);
                            for (String bl : bodyLineArray) {
                                String[] bodyLine = bl.split(":\\s");
                                if (bodyLine.length == 2) {
                                    message.bodyMap.put(bodyLine[0], bodyLine[1]);
                                }
                            }
                        }
                        Logger.t(TAG).d(">>>>>>>>>> RTSP Receive Message:\r\n" +
                                message.toStringMsg(true) +
                                "<<<<<<<<<<");
                        if (mReceiveRTSPListener != null) {
                            if (message instanceof RtspRequestMessage) {
                                mReceiveRTSPListener.onRtspRequest((RtspRequestMessage) message);
                            } else {
                                mReceiveRTSPListener.onRtspResponse((RtspResponseMessage) message);
                            }
                        }
                    } else {
                        Logger.t(TAG).d("RTSP Receive Message is null.");
                    }
                }
            } catch (Exception e) {
                Logger.t(TAG).e("Exception:" + e.toString());
            }

            // Post another receive operation
            if (!mSocket.isClosed()) {
                mHandler.post(receiveOperationRunnable);
            }
        }
    };

}
