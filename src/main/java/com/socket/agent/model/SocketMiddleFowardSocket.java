package com.socket.agent.model;

import java.net.Socket;

/**
 * description: socket中转转发
 * @author raddle
 * time : 2015年11月17日 下午1:28:41
 */
public class SocketMiddleFowardSocket {
    private Socket middleServerSocket;
    private String forwardTo;
    private Socket forwardToSocket;

    public Socket getMiddleServerSocket() {
        return middleServerSocket;
    }

    public void setMiddleServerSocket(Socket middleServerSocket) {
        this.middleServerSocket = middleServerSocket;
    }

    public String getForwardTo() {
        return forwardTo;
    }

    public void setForwardTo(String forwardTo) {
        this.forwardTo = forwardTo;
    }

    public Socket getForwardToSocket() {
        return forwardToSocket;
    }

    public void setForwardToSocket(Socket forwardToSocket) {
        this.forwardToSocket = forwardToSocket;
    }
}
