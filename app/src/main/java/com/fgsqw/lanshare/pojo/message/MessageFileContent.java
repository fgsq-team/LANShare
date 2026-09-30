package com.fgsqw.lanshare.pojo.message;

import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;

import java.io.Serializable;

public class MessageFileContent extends MessageContent implements Serializable {
    private int progress;
    private String path;
    private long length;
    private long completedSize;
    private int index;
    private String stateMessage;
    private boolean nextStep = true;
    // 收发两侧实时速度（字节/秒），由 setProgress 按 1 秒窗口采样计算，数值更稳
    private transient volatile long speed;
    private transient long speedBaseTime;
    private transient int speedBaseProgress;

    public MessageFileContent() {
        setTextSelection(false);
    }

    public boolean isNextStep() {
        return nextStep;
    }

    public void setNextStep(boolean nextStep) {
        this.nextStep = nextStep;
    }

    public int getViewType() {
        return isLeft() ? ChatAdabper.TYPE_FILE_MSG_LEFT : ChatAdabper.TYPE_FILE_MSG_RIGHT;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getProgress() {
        return progress;
    }

    public synchronized void setProgress(int progress) {
        this.progress = progress;
        if (length <= 0) {
            android.util.Log.d("SPD", "setProgress skip len<=0 p=" + progress);
            return;
        }
        long now = System.currentTimeMillis();
        if (speedBaseTime == 0 || progress < speedBaseProgress) {
            speedBaseTime = now;
            speedBaseProgress = progress;
            android.util.Log.d("SPD", "setProgress base p=" + progress + " len=" + length);
            return;
        }
        long dt = now - speedBaseTime;
        if (dt >= 1000) {
            speed = (progress - speedBaseProgress) * length / 100 * 1000 / dt;
            speedBaseTime = now;
            speedBaseProgress = progress;
            android.util.Log.d("SPD", "setProgress computed sp=" + speed + " dt=" + dt + " p=" + progress);
        }
    }

    public long getSpeed() {
        return speed;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getCompletedSize() {
        return completedSize;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }
}
