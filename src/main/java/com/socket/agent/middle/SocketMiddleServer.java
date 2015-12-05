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
import com.socket.agent.util.SocketCallback;
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
    private int soTimeout;
    /**
     * 短连接
     */
    private boolean shortConnect;

    public synchronized void start() {
        for (SocketMiddle socketMiddle : middles) {
            try {
                final SocketMiddleSockets sockets = new SocketMiddleSockets();
                final ServerSocket server1 = new ServerSocket(socketMiddle.getListenPort1());
                logger.info("listening on " + socketMiddle.getListenPort1());
                sockets.setServer1(server1);
                new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                final Socket socket = server1.accept();
                                logger.info("accepted " + socket);
                                if (soTimeout > 0) {
                                    socket.setSoTimeout(soTimeout);
                                }
                                logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                                sockets.getPort1Sockets().add(new SocketCopySocket(false, socket));
                                TransferUtils.addSocket(socket, new SocketCallback() {

                                    @Override
                                    public boolean isDiscardData(Socket fromSocket, Socket toSocket, byte[] data) {
                                        if (socket.equals(fromSocket)) {
                                            // 往接收端全部发送
                                            return false;
                                        } else {
                                            return super.isDiscardData(fromSocket, toSocket, data);
                                        }
                                    }
                                    
                                }, sockets.getPort2Sockets());
                            } catch (IOException e) {
                                logger.error("accept socket failed", e);
                                continue;
                            }
                        }
                    }
                }, "SocketMiddleServer-" + server1.getLocalPort()).start();
                final ServerSocket server2 = new ServerSocket(socketMiddle.getListenPort2());
                logger.info("listening on " + socketMiddle.getListenPort2());
                sockets.setServer2(server2);
                new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            try {
                                final Socket socket = server2.accept();
                                logger.info("accepted " + socket);
                                if (soTimeout > 0) {
                                    socket.setSoTimeout(soTimeout);
                                }
                                logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                                sockets.getPort2Sockets().add(new SocketCopySocket(false, socket));
                                TransferUtils.addSocket(socket, new SocketCallback() {

                                    @Override
                                    public void dataSent(Socket srcSocket, Socket toSocket, byte[] data) {
                                        if (socket.equals(srcSocket)) {
                                            if (shortConnect) {
                                                // 短连接，将数据发送给发送端后，关闭发送端
                                                IOUtils.closeQuietly(toSocket);
                                                logger.info("close socket " + toSocket.getRemoteSocketAddress() + " , data has sent");
                                            }
                                        }
                                    }

                                    @Override
                                    public boolean isDiscardData(Socket fromSocket, Socket toSocket, byte[] data) {
                                        if (socket.equals(fromSocket)) {
                                            // 多个发送端，只将响应数据发给一个发送端
                                            return TransferUtils.isDiscardDataMultiTo(fromSocket, toSocket);
                                        } else {
                                            return super.isDiscardData(fromSocket, toSocket, data);
                                        }
                                    }
                                }, sockets.getPort1Sockets());
                            } catch (IOException e) {
                                logger.error("accept socket failed", e);
                                continue;
                            }
                        }
                    }
                }, "SocketMiddleServer-" + server2.getLocalPort()).start();
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

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public boolean isShortConnect() {
        return shortConnect;
    }

    public void setShortConnect(boolean shortConnect) {
        this.shortConnect = shortConnect;
    }
}
