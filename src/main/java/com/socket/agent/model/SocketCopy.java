package com.socket.agent.model;

/**
 * description: socket转发
 * @author raddle
 * time : 2015年11月17日 下午1:28:41
 */
public class SocketCopy {
    /**
     * 非主要socket的返回数据将丢弃
     */
    private boolean isPrimary;
    private String copyTo;

    public SocketCopy(String copyTo) {
        this.copyTo = copyTo;
    }

    public SocketCopy(boolean isPrimary, String copyTo) {
        this.isPrimary = isPrimary;
        this.copyTo = copyTo;
    }

    public String getCopyTo() {
        return copyTo;
    }

    public void setCopyTo(String copyTo) {
        this.copyTo = copyTo;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
