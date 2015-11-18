package com.socket.agent.middle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketMiddle;
import com.socket.agent.model.SocketMiddleSockets;

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
                                logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                                sockets.getPort1Sockets().add(socket);
                                new Thread(new ForwardSockets(socket, sockets.getPort2Sockets())).start();
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
                                logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                                sockets.getPort2Sockets().add(socket);
                                new Thread(new ForwardSockets(socket, sockets.getPort1Sockets())).start();
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
            for (Socket socket : server.getPort1Sockets()) {
                IOUtils.closeQuietly(socket);
            }
            IOUtils.closeQuietly(server.getServer2());
            for (Socket socket : server.getPort2Sockets()) {
                IOUtils.closeQuietly(socket);
            }
        }
    }

    private class ForwardSockets implements Runnable {
        private Socket srcSocket;
        private List<Socket> toSockets;

        public ForwardSockets(Socket srcSocket, List<Socket> toSockets) {
            this.srcSocket = srcSocket;
            this.toSockets = toSockets;
        }

        public void run() {
            try {
                byte[] buffer = new byte[1024 * 32];
                InputStream input = srcSocket.getInputStream();
                logger.info("wating data from " + srcSocket.getRemoteSocketAddress());
                if (srcSocket.isClosed()) {
                    logger.info(srcSocket.getRemoteSocketAddress() + " is closed");
                    return;
                }
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    logger.info("received data from " + srcSocket.getRemoteSocketAddress() + " size : " + n);
                    if (n > 0) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        bo.write(buffer, 0, n);
                        logger.info("receive data to string :\n" + new String(bo.toByteArray(), "utf-8"));
                    }
                    // 复制到另外一个端口
                    for (Socket socket2 : toSockets) {
                        if (!socket2.isClosed() && n > 0) {
                            try {
                                logger.info("write data to " + socket2.getRemoteSocketAddress());
                                socket2.getOutputStream().write(buffer, 0, n);
                            } catch (IOException e) {
                                logger.info(socket2.getRemoteSocketAddress() + " error :" + e.getMessage(), e);
                                IOUtils.closeQuietly(socket2);
                            }
                        }
                    }
                    logger.info("wating data from " + srcSocket.getRemoteSocketAddress());
                }
            } catch (IOException e) {
                logger.info(srcSocket.getRemoteSocketAddress() + " error :" + e.getMessage(), e);
                IOUtils.closeQuietly(srcSocket);
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
