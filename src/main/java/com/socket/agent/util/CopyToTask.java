package com.socket.agent.util;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.socket.agent.model.SocketCopySocket;

/**
 * description: 
 * @author Administrator
 * time : 2015年11月23日 下午9:18:30
 */
public class CopyToTask implements Runnable {
    private Socket fromSocket;
    private List<SocketCopySocket> toSockets;

    public CopyToTask(Socket fromSocket, List<SocketCopySocket> toSockets) {
        this.fromSocket = fromSocket;
        this.toSockets = toSockets;
    }

    public CopyToTask(Socket fromSocket, SocketCopySocket toSocket) {
        this.fromSocket = fromSocket;
        List<SocketCopySocket> ss = new ArrayList<SocketCopySocket>();
        ss.add(toSocket);
        this.toSockets = ss;
    }

    public void run() {
        boolean hasPrimary = false;
        for (SocketCopySocket socketCopySocket : toSockets) {
            if (socketCopySocket.isPrimary()) {
                hasPrimary = true;
            }
        }
        int i = 0;
        for (SocketCopySocket socketCopySocket2 : toSockets) {
            if (hasPrimary) {
                if (socketCopySocket2.isPrimary()) {
                    new Thread(new SocketTranferTask(fromSocket, socketCopySocket2.getToSocket())).start();
                    new Thread(new SocketTranferTask(socketCopySocket2.getToSocket(), fromSocket)).start();
                } else {
                    new Thread(new SocketTranferTask(fromSocket, socketCopySocket2.getToSocket())).start();
                    new Thread(new SocketTranferTask(socketCopySocket2.getToSocket(), fromSocket, true)).start();
                }
            } else {
                if (i == 0) {
                    new Thread(new SocketTranferTask(fromSocket, socketCopySocket2.getToSocket())).start();
                    new Thread(new SocketTranferTask(socketCopySocket2.getToSocket(), fromSocket)).start();
                } else {
                    new Thread(new SocketTranferTask(fromSocket, socketCopySocket2.getToSocket())).start();
                    new Thread(new SocketTranferTask(socketCopySocket2.getToSocket(), fromSocket, true)).start();
                }
            }
            i++;
        }
    }
}
