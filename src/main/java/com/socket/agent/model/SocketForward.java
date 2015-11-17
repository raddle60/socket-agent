package com.socket.agent.model;

/**
 * description: socket转发
 * @author raddle
 * time : 2015年11月17日 下午1:28:41
 */
public class SocketForward {
    private int listenPort;
    private String forwardTo;

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getForwardTo() {
        return forwardTo;
    }

    public void setForwardTo(String forwardTo) {
        this.forwardTo = forwardTo;
    }
}
