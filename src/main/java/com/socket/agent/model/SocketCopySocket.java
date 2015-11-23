package com.socket.agent.model;

import java.net.Socket;

/**
 * description: 
 * @author Administrator
 * time : 2015年11月23日 下午9:02:01
 */
public class SocketCopySocket {
    /**
     * 非主要socket的返回数据将丢弃
     */
    private boolean isPrimary;

    private Socket toSocket;

    public SocketCopySocket(boolean isPrimary, Socket toSocket) {
        this.isPrimary = isPrimary;
        this.toSocket = toSocket;
    }

    public Socket getToSocket() {
        return toSocket;
    }

    public void setToSocket(Socket toSocket) {
        this.toSocket = toSocket;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
