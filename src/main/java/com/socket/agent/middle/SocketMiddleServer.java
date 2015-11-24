package com.socket.agent.middle;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.model.SocketMiddle;
import com.socket.agent.model.SocketMiddleSockets;
import com.socket.agent.util.TransferUtils;

/**
 * description: 
 * @author raddle
 * time : 2015年11月17日 下午1:37:33
 */
public class SocketMiddleServer {
    private final static Logger logger = LoggerFactory.getLogger(SocketMiddleServer.class);
    private List<SocketMiddle> middles = new ArrayList<SocketMiddle>();
    private List<SocketMiddleSockets> servers = new ArrayList<SocketMiddleSockets>();

    public synchronized void start() {
        for (SocketMiddle socketMiddle : middles) {
            try {
                final SocketMiddleSockets sockets = new SocketMiddleSockets();
                final ServerSocket server1 = new ServerSocket(socketMiddle.getListenPort1());
                sockets.setServer1(server1);
                new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                logger.info("accepting on :" + server1.getLocalPort());
                                final Socket socket = server1.accept();
                                socket.setSoTimeout(60000);
                                logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                                sockets.getPort1Sockets().add(new SocketCopySocket(false, socket));
                                TransferUtils.addSocket(socket, sockets.getPort2Sockets());
                            } catch (IOException e) {
                                logger.error("accept socket failed", e);
                                continue;
                            }
                        }
                    }
                }).start();
                final ServerSocket server2 = new ServerSocket(socketMiddle.getListenPort2());
                sockets.setServer2(server2);
                new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                logger.info("accepting on :" + server2.getLocalPort());
                                final Socket socket = server2.accept();
                                socket.setSoTimeout(60000);
                                logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                                sockets.getPort2Sockets().add(new SocketCopySocket(false, socket));
                                TransferUtils.addSocket(socket, sockets.getPort1Sockets());
                            } catch (IOException e) {
                                logger.error("accept socket failed", e);
                                continue;
                            }
                        }
                    }
                }).start();
                servers.add(sockets);
            } catch (IOException e) {
                logger.error("starting SocketMiddleServer failed , " + socketMiddle.getListenPort1() + "<->" + socketMiddle.getListenPort2(), e);
                continue;
            }
        }
    }

    public void close() {
        for (SocketMiddleSockets server : servers) {
            IOUtils.closeQuietly(server.getServer1());
            for (SocketCopySocket socket : server.getPort1Sockets()) {
                IOUtils.closeQuietly(socket.getToSocket());
            }
            IOUtils.closeQuietly(server.getServer2());
            for (SocketCopySocket socket : server.getPort2Sockets()) {
                IOUtils.closeQuietly(socket.getToSocket());
            }
        }
    }

    public List<SocketMiddle> getMiddles() {
        return middles;
    }

    public void setMiddles(List<SocketMiddle> middles) {
        this.middles = middles;
    }
}
