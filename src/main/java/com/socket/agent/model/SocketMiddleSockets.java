package com.socket.agent.model;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * description: 
 * @author raddle
 * time : 2015年11月17日 下午1:43:46
 */
public class SocketMiddleSockets {
    private ServerSocket server1;
    private ServerSocket server2;
    private List<Socket> port1Sockets = new ArrayList<Socket>();
    private List<Socket> port2Sockets = new ArrayList<Socket>();

    public List<Socket> getPort1Sockets() {
        return port1Sockets;
    }

    public void setPort1Sockets(List<Socket> port1Sockets) {
        this.port1Sockets = port1Sockets;
    }

    public List<Socket> getPort2Sockets() {
        return port2Sockets;
    }

    public void setPort2Sockets(List<Socket> port2Sockets) {
        this.port2Sockets = port2Sockets;
    }

    public ServerSocket getServer1() {
        return server1;
    }

    public void setServer1(ServerSocket server1) {
        this.server1 = server1;
    }

    public ServerSocket getServer2() {
        return server2;
    }

    public void setServer2(ServerSocket server2) {
        this.server2 = server2;
    }
}
