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
import com.socket.agent.util.CopyToTask;

/**
 * description: 
 * @author Administrator
 * time : 2015年11月17日 下午2:23:35
 */
public class SocketMiddleClient {
    private final static Logger logger = LoggerFactory.getLogger(SocketMiddleClient.class);
    private List<SocketMiddleFoward> middles = new ArrayList<SocketMiddleFoward>();

    public synchronized void start() {
        for (final SocketMiddleFoward socketMiddleFoward : middles) {
            final SocketMiddleFowardSocket fowardSocket = new SocketMiddleFowardSocket();
            try {
                Socket serverSocketInit = connectServer(socketMiddleFoward);
                fowardSocket.setMiddleServerSocket(serverSocketInit);
                new Thread(new Runnable() {

                    public void run() {
                        int i = 0;
                        Socket serverSocket = fowardSocket.getMiddleServerSocket();
                        while (true) {
                            try {
                                if (!serverSocket.isClosed()) {
                                    if (i == 0) {
                                        sendingData(fowardSocket, socketMiddleFoward, serverSocket);
                                    }
                                } else if (serverSocket.isClosed()) {
                                    logger.info(socketMiddleFoward.getMiddleServer() + " is closed");
                                    serverSocket = connectServer(socketMiddleFoward);
                                    fowardSocket.setMiddleServerSocket(serverSocket);
                                }
                                sendingData(fowardSocket, socketMiddleFoward, serverSocket);
                            } catch (IOException e) {
                                logger.error("connect to middle server " + socketMiddleFoward.getMiddleServer() + " failed", e);
                                IOUtils.closeQuietly(serverSocket);
                            }
                            i++;
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e2) {
                            }
                        }
                    }

                    private void sendingData(SocketMiddleFowardSocket fowardSocket, final SocketMiddleFoward socketMiddleFoward, Socket serverSocket) {
                        try {
                            Socket forwardToSocket = connectForward(socketMiddleFoward);
                            fowardSocket.setForwardToSocket(forwardToSocket);
                            new CopyToTask(serverSocket, new SocketCopySocket(true, forwardToSocket)).run();
                        } catch (IOException e) {
                            logger.error("connect to forward " + socketMiddleFoward.getForwardTo() + " failed", e);
                        }
                    }

                    private Socket connectForward(final SocketMiddleFoward socketMiddleFoward) throws IOException {
                        Socket forwardToSocket;
                        forwardToSocket = new Socket();
                        logger.info("connecting to forward to " + socketMiddleFoward.getForwardTo());
                        forwardToSocket.connect(new InetSocketAddress(socketMiddleFoward.getForwardTo().split(":")[0], Integer.parseInt(socketMiddleFoward.getForwardTo().split(":")[1])), 5000);
                        logger.info("connected to forward to " + forwardToSocket.getRemoteSocketAddress());
                        forwardToSocket.setSoTimeout(60000);
                        return forwardToSocket;
                    }
                }).start();
            } catch (NumberFormatException e) {
                logger.error("connect to middle server " + socketMiddleFoward.getMiddleServer() + " failed", e);
            } catch (IOException e) {
                logger.error("connect to middle server " + socketMiddleFoward.getMiddleServer() + " failed", e);
            }
        }
    }

    private Socket connectServer(final SocketMiddleFoward socketMiddleFoward) throws SocketException, IOException {
        Socket serverSocket;
        serverSocket = new Socket();
        serverSocket.setSoTimeout(600 * 1000);
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
}
