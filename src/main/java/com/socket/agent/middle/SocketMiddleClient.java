package com.socket.agent.middle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.model.SocketMiddleFoward;
import com.socket.agent.model.SocketMiddleFowardSocket;
import com.socket.agent.util.SocketCallback;
import com.socket.agent.util.TransferUtils;

/**
 * description: 
 * @author Administrator
 * time : 2015年11月17日 下午2:23:35
 */
public class SocketMiddleClient {
    private final static Logger logger = LoggerFactory.getLogger(SocketMiddleClient.class);
    private List<SocketMiddleFoward> middles = new ArrayList<SocketMiddleFoward>();
    private int clientSoTimeout;
    private int serverSoTimeout;

    public synchronized void start() {
        for (final SocketMiddleFoward socketMiddleFoward : middles) {
            final SocketMiddleFowardSocket fowardSocket = new SocketMiddleFowardSocket();
            new Thread(new Runnable() {

                public void run() {
                    while (true) {
                        Socket serverSocket = fowardSocket.getMiddleServerSocket();
                        try {
                            if (serverSocket == null || serverSocket.isClosed()) {
                                serverSocket = connectServer(socketMiddleFoward);
                                fowardSocket.setMiddleServerSocket(serverSocket);
                                TransferUtils.addSocket(serverSocket, new SocketCallback() {

                                    public void dataReceived(Socket socket, byte[] data) {
                                        if (socket.equals(fowardSocket.getMiddleServerSocket())) {
                                            // 从server端收到消息
                                            if (TransferUtils.isToAddrClosed(socket, fowardSocket.getForwardTo())) {
                                                try {
                                                    Socket forwardToSocket = connectForward(socketMiddleFoward);
                                                    fowardSocket.setForwardToSocket(forwardToSocket);
                                                    TransferUtils.addSocket(socket, this, new SocketCopySocket(true, forwardToSocket));
                                                } catch (IOException e) {
                                                    logger.error("connect to forward " + socketMiddleFoward.getForwardTo() + " failed , " + e.getMessage());
                                                }
                                            }
                                        }
                                        if (socket.equals(fowardSocket.getForwardToSocket())) {
                                            // 从forward端收到消息
                                            if (TransferUtils.isToAddrClosed(socket, socketMiddleFoward.getMiddleServer())) {
                                                try {
                                                    Socket newServerSocket = connectServer(socketMiddleFoward);
                                                    fowardSocket.setMiddleServerSocket(newServerSocket);
                                                    TransferUtils.addSocket(socket, this, new SocketCopySocket(true, newServerSocket));
                                                } catch (IOException e) {
                                                    logger.error("connect to middle " + socketMiddleFoward.getMiddleServer() + " failed , " + e.getMessage());
                                                }
                                            }
                                        }
                                    }
                                }, (SocketCopySocket) null);
                            }
                        } catch (IOException e) {
                            logger.error("connect to middle server " + socketMiddleFoward.getMiddleServer() + " failed , " + e.getMessage());
                            IOUtils.closeQuietly(serverSocket);
                        }
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e2) {
                        }
                    }
                }

                private Socket connectForward(final SocketMiddleFoward socketMiddleFoward) throws IOException {
                    Socket forwardToSocket;
                    forwardToSocket = new Socket();
                    logger.info("connecting to forward to " + socketMiddleFoward.getForwardTo());
                    forwardToSocket.connect(new InetSocketAddress(socketMiddleFoward.getForwardTo().split(":")[0], Integer.parseInt(socketMiddleFoward.getForwardTo().split(":")[1])), 5000);
                    logger.info("connected to forward to " + forwardToSocket.getRemoteSocketAddress());
                    if (clientSoTimeout > 0) {
                        forwardToSocket.setSoTimeout(clientSoTimeout);
                    }
                    return forwardToSocket;
                }
            }, "SocketMiddleClient-loop").start();
        }
    }

    private Socket connectServer(final SocketMiddleFoward socketMiddleFoward) throws SocketException, IOException {
        Socket serverSocket;
        serverSocket = new Socket();
        if (serverSoTimeout > 0) {
            serverSocket.setSoTimeout(serverSoTimeout);
        }
        logger.info("connecting to middle server " + socketMiddleFoward.getMiddleServer());
        serverSocket.connect(new InetSocketAddress(socketMiddleFoward.getMiddleServer().split(":")[0], Integer.parseInt(socketMiddleFoward.getMiddleServer().split(":")[1])), 5000);
        logger.info("connected to middle server " + serverSocket.getRemoteSocketAddress());
        return serverSocket;
    }

    public List<SocketMiddleFoward> getMiddles() {
        return middles;
    }

    public void setMiddles(List<SocketMiddleFoward> middles) {
        this.middles = middles;
    }

    public int getClientSoTimeout() {
        return clientSoTimeout;
    }

    public void setClientSoTimeout(int clientSoTimeout) {
        this.clientSoTimeout = clientSoTimeout;
    }

    public int getServerSoTimeout() {
        return serverSoTimeout;
    }

    public void setServerSoTimeout(int serverSoTimeout) {
        this.serverSoTimeout = serverSoTimeout;
    }
}
