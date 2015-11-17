package com.socket.agent.middle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketMiddleFoward;
import com.socket.agent.model.SocketMiddleFowardSocket;

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
            SocketMiddleFowardSocket fowardSocket = new SocketMiddleFowardSocket();
            logger.info("connecting to middle server " + socketMiddleFoward.getMiddleServer());
            final Socket serverSocket = new Socket();
            try {
                serverSocket.connect(new InetSocketAddress(socketMiddleFoward.getMiddleServer().split(":")[0], Integer.parseInt(socketMiddleFoward.getMiddleServer().split(":")[1])));
                logger.info("connected to middle server " + socketMiddleFoward.getMiddleServer());
                fowardSocket.setMiddleServerSocket(serverSocket);
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            byte[] buffer = new byte[1024 * 32];
                            InputStream input = serverSocket.getInputStream();
                            logger.info("wating data from " + serverSocket.getRemoteSocketAddress());
                            if (serverSocket.isClosed()) {
                                logger.info(serverSocket.getRemoteSocketAddress() + " is closed");
                                return;
                            }
                            int n = 0;
                            // 复制到另外一个端口
                            Socket forwardToSocket = null;
                            while (-1 != (n = input.read(buffer))) {
                                logger.info("received data from " + serverSocket.getRemoteSocketAddress() + " size : " + n);
                                if (n > 0) {
                                    if (forwardToSocket == null) {
                                        forwardToSocket = reconnect(socketMiddleFoward);
                                        new Thread(new TransferData(forwardToSocket, serverSocket)).start();
                                    }
                                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                                    bo.write(buffer, 0, n);
                                    logger.info("receive data to string :\n" + new String(bo.toByteArray(), "utf-8"));
                                    try {
                                        if (forwardToSocket.isClosed()) {
                                            forwardToSocket = reconnect(socketMiddleFoward);
                                            new Thread(new TransferData(forwardToSocket, serverSocket)).start();
                                        }
                                        logger.info("write data to " + forwardToSocket.getRemoteSocketAddress());
                                        forwardToSocket.getOutputStream().write(buffer, 0, n);
                                    } catch (IOException e) {
                                        logger.info(forwardToSocket.getRemoteSocketAddress() + " error :" + e.getMessage(), e);
                                        IOUtils.closeQuietly(forwardToSocket);
                                        try {
                                            forwardToSocket = reconnect(socketMiddleFoward);
                                            new Thread(new TransferData(forwardToSocket, serverSocket)).start();
                                            logger.info("write data to " + forwardToSocket.getRemoteSocketAddress());
                                            forwardToSocket.getOutputStream().write(buffer, 0, n);
                                        } catch (Exception e1) {
                                            logger.info("rewrite data to " + forwardToSocket.getRemoteSocketAddress() + " error ", e);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            logger.info(serverSocket.getRemoteSocketAddress() + " error :" + e.getMessage(), e);
                            IOUtils.closeQuietly(serverSocket);
                        }
                    }

                    private Socket reconnect(final SocketMiddleFoward socketMiddleFoward) throws IOException {
                        Socket forwardToSocket;
                        forwardToSocket = new Socket();
                        forwardToSocket.connect(new InetSocketAddress(socketMiddleFoward.getForwardTo().split(":")[0], Integer.parseInt(socketMiddleFoward.getForwardTo().split(":")[1])));
                        logger.info("reconnected to forward to " + socketMiddleFoward.getForwardTo());
                        forwardToSocket.setSoTimeout(1000);
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

    private class TransferData implements Runnable {

        private Socket srcSocket;
        private Socket toSocket;

        public TransferData(Socket srcSocket, Socket toSocket) {
            this.srcSocket = srcSocket;
            this.toSocket = toSocket;
        }

        public void run() {
            try {
                InputStream input = srcSocket.getInputStream();
                logger.info("wating data from " + srcSocket.getRemoteSocketAddress());
                int n = 0;
                byte[] buffer = new byte[1024 * 32];
                if (srcSocket.isClosed()) {
                    logger.info(srcSocket.getRemoteSocketAddress() + " is closed");
                }
                while (-1 != (n = input.read(buffer))) {
                    logger.info("received data from " + srcSocket.getRemoteSocketAddress() + " size : " + n);
                    if (n > 0) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        bo.write(buffer, 0, n);
                        logger.info("receive data to string :\n" + new String(bo.toByteArray(), "utf-8"));
                        // 发送给目标socket
                        logger.info("sending data to " + toSocket.getRemoteSocketAddress());
                        toSocket.getOutputStream().write(buffer, 0, n);
                        toSocket.getOutputStream().flush();
                    }
                }
            } catch (SocketTimeoutException e) {
                logger.info("close socket ,time out from " + srcSocket.getRemoteSocketAddress());
                IOUtils.closeQuietly(srcSocket);
            } catch (IOException e) {
                logger.error("transfer data from " + srcSocket.getRemoteSocketAddress() + " failed", e);
                IOUtils.closeQuietly(srcSocket);
            }
        }
    }

    public List<SocketMiddleFoward> getMiddles() {
        return middles;
    }

    public void setMiddles(List<SocketMiddleFoward> middles) {
        this.middles = middles;
    }
}
