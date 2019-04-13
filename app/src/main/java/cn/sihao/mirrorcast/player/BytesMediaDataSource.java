package cn.sihao.mirrorcast.player;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

public class BytesMediaDataSource implements IMediaDataSource {
    private final static String TAG = "BytesMediaDataSource";

    private boolean mIsReset;
    private PipedInputStream mPipedInput;
    private PipedOutputStream mPipedOutput;
    private long mStartTime;
    private long mUpdateTime;

    public BytesMediaDataSource() {
        mIsReset = false;
        mStartTime = System.currentTimeMillis();
        try {
            mPipedInput = new PipedInputStream(1024 * 512);
            mPipedOutput = new PipedOutputStream();
            mPipedOutput.connect(mPipedInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void putNewData(byte[] data) {
        putNewData(data, 0, data.length);
    }

    public void putNewData(byte[] data, int offset, int length) {
        if (mIsReset || mPipedOutput == null) return;
        mUpdateTime = System.currentTimeMillis();

        try {
            mPipedOutput.write(data, offset, length);
            mPipedOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        if (mIsReset) {
            return 0;
        }

        int cnt = mPipedInput.available();
        if (System.currentTimeMillis() - mStartTime < 500 || size == 0 || cnt == 0)
            return 0;

        if (cnt > size) {
            return mPipedInput.read(buffer, 0, size);
        } else {
            return mPipedInput.read(buffer, 0, cnt);
        }
    }

    @Override
    public long getSize() throws IOException {
        return -1;
    }

    @Override
    public void close() throws IOException {
        Logger.t(TAG).d("close media source.");
        if (mPipedInput != null) {
            mPipedInput.close();
            mPipedInput = null;
        }

        if (mPipedOutput != null) {
            mPipedOutput.close();
            mPipedOutput = null;
        }
        reset();
    }

    public void reset() {
        mIsReset = true;
    }

    // 单位为ms的间隔表示到目前为止已经timeout毫秒没有新数据进入buffer了
    public boolean isTimeout(long timeout) {
        return System.currentTimeMillis() - mUpdateTime > timeout;    // timeout时间的数据更新超时
    }
}