package com.socket.agent.model;

/**
 * description: socket两边中转
 * @author raddle
 * time : 2015年11月17日 下午1:28:41
 */
public class SocketMiddle {
    private int listenPort1;
    private int listenPort2;

    public int getListenPort1() {
        return listenPort1;
    }

    public void setListenPort1(int listenPort1) {
        this.listenPort1 = listenPort1;
    }

    public int getListenPort2() {
        return listenPort2;
    }

    public void setListenPort2(int listenPort2) {
        this.listenPort2 = listenPort2;
    }

}
