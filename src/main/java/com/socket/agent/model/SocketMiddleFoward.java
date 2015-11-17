package com.socket.agent.model;

/**
 * description: socket中转转发
 * @author raddle
 * time : 2015年11月17日 下午1:28:41
 */
public class SocketMiddleFoward {
    private String middleServer;
    private String forwardTo;

    public SocketMiddleFoward(String middleServer, String forwardTo) {
        this.middleServer = middleServer;
        this.forwardTo = forwardTo;
    }

    public String getMiddleServer() {
        return middleServer;
    }

    public void setMiddleServer(String middleServer) {
        this.middleServer = middleServer;
    }

    public String getForwardTo() {
        return forwardTo;
    }

    public void setForwardTo(String forwardTo) {
        this.forwardTo = forwardTo;
    }

}
